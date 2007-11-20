package com.tc.net.core;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.TCInternalError;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NIOWorkarounds;
import com.tc.util.Assert;
import com.tc.util.Util;
import com.tc.util.runtime.Os;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * Some docs here
 */
public class CoreNIOServices extends Thread {
  private final Selector      selector;
  private final LinkedQueue   selectorTasks;
  private short               status;

  private TCCommJDK14         parentComm;
  private String              baseThreadName;
  private Thread              ownerThread;
  private TCWorkerCommManager workerCommMgr;

  private final TCLogger      logger              = TCLogging.getLogger(CoreNIOServices.class);

  public final short          NIO_THREAD_INIT     = 0x00;
  public final short          NIO_THREAD_STARTED  = 0x01;
  public final short          NIO_THREAD_STOPPED  = 0x02;
  public final short          NIO_THREAD_STOP_REQ = 0x02;

  public CoreNIOServices(String commThreadName, TCWorkerCommManager workerCommManager) {
    this(commThreadName, null, workerCommManager);
  }

  public CoreNIOServices(String commThreadName, TCCommJDK14 comm, TCWorkerCommManager workerCommManager) {
    setDaemon(true);
    setName(commThreadName);

    this.selector = createSelector();
    this.selectorTasks = new LinkedQueue();
    this.status = NIO_THREAD_INIT;

    baseThreadName = commThreadName;
    if (comm != null) this.parentComm = comm;
    if (workerCommManager != null) this.workerCommMgr = workerCommManager;
  }

  public void run() {
    ownerThread = Thread.currentThread();
    status = NIO_THREAD_STARTED;
    try {
      selectLoop();
    } catch (Throwable t) {
      logger.error("Unhandled exception from selectLoop", t);
      t.printStackTrace();
    } finally {
      dispose(selector, selectorTasks);
    }
  }

  public boolean isStarted() {
    if (status == NIO_THREAD_STARTED) { return true; }
    return false;
  }

  public void requestStop() {
    if (isStarted()) {
      status = NIO_THREAD_STOP_REQ;
      try {
        this.selector.wakeup();
      } catch (Exception e) {
        logger.error("Exception trying to stop " + ownerThread.getName() + ": ", e);
      }
    } else {
      logger.error("Stop requested for already stopped thread " + ownerThread.getName());
    }
  }

  public boolean isStopRequested() {
    if (status == NIO_THREAD_STOP_REQ) { return true; }
    return false;
  }

  public boolean isStopped() {
    if (status == NIO_THREAD_STOPPED) { return true; }
    return false;
  }

  String makeListenString(TCListener listener) {
    StringBuffer buf = new StringBuffer();
    buf.append("(listen ");
    buf.append(listener.getBindAddress().getHostAddress());
    buf.append(':');
    buf.append(listener.getBindPort());
    buf.append(')');
    return buf.toString();
  }

  synchronized void listenerRemoved() {
    updateThreadName("");
  }

  synchronized void listenerAdded(TCListener listener) {
    updateThreadName(makeListenString(listener));
  }

  private void updateThreadName(String appendStr) {
    StringBuffer buf = new StringBuffer(baseThreadName);
    buf.append(' ');
    buf.append(appendStr);
    setName(buf.toString());
  }

  protected Selector createSelector() {
    Selector selector1 = null;

    final int tries = 3;

    for (int i = 0; i < tries; i++) {
      try {
        selector1 = Selector.open();
        return selector1;
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      } catch (NullPointerException npe) {
        if (i < tries && NIOWorkarounds.selectorOpenRace(npe)) {
          System.err.println("Attempting to work around sun bug 6427854 (attempt " + (i + 1) + " of " + tries + ")");
          try {
            Thread.sleep(new Random().nextInt(20) + 5);
          } catch (InterruptedException ie) {
            //
          }
          continue;
        }
        throw npe;
      }
    }

    return selector1;
  }

  void addSelectorTask(final Runnable task) {
    Assert.eval(Thread.currentThread() != ownerThread);
    boolean isInterrupted = false;

    try {
      while (true) {
        try {
          this.selectorTasks.put(task);
          break;
        } catch (InterruptedException e) {
          logger.warn(e);
          isInterrupted = true;
        }
      }
    } finally {
      this.selector.wakeup();
    }

    Util.selfInterruptIfNeeded(isInterrupted);

  }

  void unregister(SelectableChannel channel) {
    SelectionKey key = null;
    Assert.eval(Thread.currentThread() == ownerThread);
    key = channel.keyFor(this.selector);

    if (key != null) {
      key.cancel();
      key.attach(null);
    }
  }

  void stopListener(final ServerSocketChannel ssc, final Runnable callback) {
    if (Thread.currentThread() != ownerThread) {
      // The default comm thread is always the listener.
      final CoreNIOServices commThread = parentComm.DEFAULT_COMM_THREAD;
      Runnable task = new Runnable() {
        public void run() {
          commThread.stopListener(ssc, callback);
        }
      };
      commThread.addSelectorTask(task);
      return;
    }

    try {
      cleanupChannel(ssc, null);
    } catch (Exception e) {
      logger.error(e);
    } finally {
      try {
        callback.run();
      } catch (Exception e) {
        logger.error(e);
      }
    }
  }

  void cleanupChannel(final Channel ch, final Runnable callback) {
    Selector localSelector = null;

    if (null == ch) {
      // not expected
      logger.warn("null channel passed to cleanupChannel()", new Throwable());
      return;
    }

    if (Thread.currentThread() != ownerThread) {
      if (logger.isDebugEnabled()) {
        logger.debug("queue'ing channel close operation");
      }

      CoreNIOServices commNIOThread = null;
      if (workerCommMgr != null && workerCommMgr.isStarted()) {
        commNIOThread = workerCommMgr.getWorkerCommForSocketChannel(ch);
      } else {
        commNIOThread = parentComm.DEFAULT_COMM_THREAD;
      }

      Assert.eval(commNIOThread != null);
      final CoreNIOServices commThread = commNIOThread;
      commNIOThread.addSelectorTask(new Runnable() {
        public void run() {
          commThread.cleanupChannel(ch, callback);
        }
      });
      return;
    } else {
      localSelector = selector;
    }

    try {
      if (ch instanceof SelectableChannel) {
        SelectableChannel sc = (SelectableChannel) ch;

        try {
          SelectionKey sk = sc.keyFor(localSelector);
          if (sk != null) {
            sk.attach(null);
            sk.cancel();
          }
        } catch (Exception e) {
          logger.warn("Exception trying to clear selection key", e);
        }
      }

      if (ch instanceof SocketChannel) {
        SocketChannel sc = (SocketChannel) ch;

        Socket s = sc.socket();

        if (null != s) {
          synchronized (s) {

            if (s.isConnected()) {
              try {
                if (!s.isOutputShutdown()) {
                  s.shutdownOutput();
                }
              } catch (Exception e) {
                logger.warn("Exception trying to shutdown socket output: " + e.getMessage());
              }

              try {
                if (!s.isClosed()) {
                  s.close();
                }
              } catch (Exception e) {
                logger.warn("Exception trying to close() socket: " + e.getMessage());
              }
            }
          }
        }
      } else if (ch instanceof ServerSocketChannel) {
        ServerSocketChannel ssc = (ServerSocketChannel) ch;

        try {
          ssc.close();
        } catch (Exception e) {
          logger.warn("Exception trying to close() server socket" + e.getMessage());
        }
      }

      try {
        ch.close();
      } catch (Exception e) {
        logger.warn("Exception trying to close channel", e);
      }
    } catch (Exception e) {
      // this is just a catch all to make sure that no exceptions will be thrown by this method, please do not remove
      logger.error("Unhandled exception in cleanupChannel()", e);
    } finally {
      try {
        if (callback != null) {
          callback.run();
        }
      } catch (Throwable t) {
        logger.error("Unhandled exception in cleanupChannel callback.", t);
      }
    }
  }

  void dispose(Selector localSelector, LinkedQueue localSelectorTasks) {
    Assert.eval(Thread.currentThread() == ownerThread);

    if (localSelector != null) {

      for (Iterator keys = localSelector.keys().iterator(); keys.hasNext();) {
        try {
          SelectionKey key = (SelectionKey) keys.next();
          cleanupChannel(key.channel(), null);
        }

        catch (Exception e) {
          logger.warn("Exception trying to close channel", e);
        }
      }

      try {
        localSelector.close();
      } catch (Exception e) {
        if ((Os.isMac()) && (Os.isUnix()) && (e.getMessage().equals("Bad file descriptor"))) {
          // I can't find a specific bug about this, but I also can't seem to prevent the exception on the Mac.
          // So just logging this as warning.
          logger.warn("Exception trying to close selector: " + e.getMessage());
        } else {
          logger.error("Exception trying to close selector", e);
        }
      }
    }

    // drop any old selector tasks
    localSelectorTasks = new LinkedQueue();
  }

  void selectLoop() throws IOException {

    Assert.eval(Thread.currentThread() == ownerThread);

    Selector localSelector = this.selector;
    LinkedQueue localSelectorTasks = this.selectorTasks;

    while (true) {
      final int numKeys;
      try {
        numKeys = localSelector.select();
      } catch (IOException ioe) {
        if (NIOWorkarounds.linuxSelectWorkaround(ioe)) {
          logger.warn("working around Sun bug 4504001");
          continue;
        }
        throw ioe;
      }

      if (isStopRequested()) {
        if (logger.isDebugEnabled()) {
          logger.debug("Select loop terminating");
        }
        return;
      }

      boolean isInterrupted = false;
      // run any pending selector tasks
      while (true) {
        Runnable task = null;
        while (true) {
          try {
            task = (Runnable) localSelectorTasks.poll(0);
            break;
          } catch (InterruptedException ie) {
            logger.error("Error getting task from task queue", ie);
            isInterrupted = true;
          }
        }

        if (null == task) {
          break;
        }

        try {
          task.run();
        } catch (Exception e) {
          logger.error("error running selector task", e);
        }
      }
      Util.selfInterruptIfNeeded(isInterrupted);

      final Set selectedKeys = localSelector.selectedKeys();
      if ((0 == numKeys) && (0 == selectedKeys.size())) {
        continue;
      }

      for (Iterator iter = selectedKeys.iterator(); iter.hasNext();) {
        SelectionKey key = (SelectionKey) iter.next();
        iter.remove();

        if (null == key) {
          logger.error("Selection key is null");
          continue;
        }

        try {

          if (key.isAcceptable()) {
            doAccept(key);
            continue;
          }

          if (key.isConnectable()) {
            doConnect(key);
            continue;
          }

          if (key.isReadable()) {
            ((TCJDK14ChannelReader) key.attachment()).doRead((ScatteringByteChannel) key.channel());
          }

          if (key.isValid() && key.isWritable()) {
            ((TCJDK14ChannelWriter) key.attachment()).doWrite((GatheringByteChannel) key.channel());
          }

        } catch (CancelledKeyException cke) {
          logger.warn(cke.getClass().getName() + " occured");
        }
      } // for
    } // while (true)
  }

  private void doAccept(final SelectionKey key) {

    SocketChannel sc = null;

    final TCListenerJDK14 lsnr = (TCListenerJDK14) key.attachment();

    try {
      final ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
      sc = ssc.accept();
      sc.configureBlocking(false);
      final Socket s = sc.socket();

      try {
        s.setSendBufferSize(64 * 1024);
      } catch (IOException ioe) {
        logger.warn("IOException trying to setSendBufferSize()");
      }

      try {
        s.setTcpNoDelay(true);
      } catch (IOException ioe) {
        logger.warn("IOException trying to setTcpNoDelay()", ioe);
      }

      if (workerCommMgr != null && workerCommMgr.isStarted()) {
        // Multi threaded server model

        final CoreNIOServices workerCommThread = workerCommMgr.getNextFreeWorkerComm();
        final TCConnectionJDK14 conn = lsnr.createConnection(sc, workerCommThread);
        final SocketChannel sc1 = sc;

        workerCommMgr.setWorkerCommChannelMap(sc, workerCommThread);
        workerCommThread.addSelectorTask(new Runnable() {
          InterestRequest workerReq = InterestRequest
                                        .createAddInterestRequest(sc1, conn,
                                                                  (SelectionKey.OP_READ | SelectionKey.OP_WRITE),
                                                                  workerCommThread);

          public void run() {
            workerCommThread.handleRequest(workerReq);
          }
        });
      } else {
        // Single threaded server model
        final TCConnectionJDK14 conn = lsnr.createConnection(sc, parentComm.DEFAULT_COMM_THREAD);
        sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, conn);
      }
    } catch (IOException ioe) {
      if (logger.isInfoEnabled()) {
        logger.info("IO Exception accepting new connection", ioe);
      }

      cleanupChannel(sc, null);
    }
  }

  private void doConnect(SelectionKey key) {
    SocketChannel sc = (SocketChannel) key.channel();
    TCConnectionJDK14 conn = (TCConnectionJDK14) key.attachment();

    try {
      if (sc.finishConnect()) {
        sc.register(selector, SelectionKey.OP_READ, conn);
        conn.finishConnect();
      } else {
        String errMsg = "finishConnect() returned false, but no exception thrown";

        if (logger.isInfoEnabled()) {
          logger.info(errMsg);
        }

        conn.fireErrorEvent(new Exception(errMsg), null);
      }
    } catch (IOException ioe) {
      if (logger.isInfoEnabled()) {
        logger.info("IOException attempting to finish socket connection", ioe);
      }

      conn.fireErrorEvent(ioe, null);
    }
  }

  public void handleRequest(final InterestRequest req) {
    // ignore the request if we are stopped/stopping
    if (isStopped()) { return; }

    if (Thread.currentThread() == ownerThread) {
      modifyInterest(req);
    } else {
      final CoreNIOServices commTh = req.getCommNIOServiceThread();
      Assert.assertNotNull(commTh);
      commTh.addSelectorTask(new Runnable() {
        public void run() {
          commTh.handleRequest(req);
        }
      });
    }
  }

  private void modifyInterest(InterestRequest request) {
    Assert.eval(Thread.currentThread() == ownerThread);

    Selector localSelector = null;
    localSelector = selector;

    try {
      final int existingOps;

      SelectionKey key = request.channel.keyFor(localSelector);
      if (key != null) {
        existingOps = key.interestOps();
      } else {
        existingOps = 0;
      }

      if (logger.isDebugEnabled()) {
        logger.debug(request);
      }

      if (request.add) {
        request.channel.register(localSelector, existingOps | request.interestOps, request.attachment);
      } else if (request.set) {
        request.channel.register(localSelector, request.interestOps, request.attachment);
      } else if (request.remove) {
        request.channel.register(localSelector, existingOps ^ request.interestOps, request.attachment);
      } else {
        throw new TCInternalError();
      }
    } catch (ClosedChannelException cce) {
      logger.warn("Exception trying to process interest request: " + cce);

    } catch (CancelledKeyException cke) {
      logger.warn("Exception trying to process interest request: " + cke);
    }
  }

  void requestConnectInterest(TCConnectionJDK14 conn, SocketChannel sc) {
    handleRequest(InterestRequest.createSetInterestRequest(sc, conn, SelectionKey.OP_CONNECT,
                                                           (CoreNIOServices) ownerThread));
  }

  void requestReadInterest(TCJDK14ChannelReader reader, ScatteringByteChannel channel) {
    handleRequest(InterestRequest.createAddInterestRequest((SelectableChannel) channel, reader, SelectionKey.OP_READ,
                                                           (CoreNIOServices) ownerThread));
  }

  void requestWriteInterest(TCJDK14ChannelWriter writer, GatheringByteChannel channel) {
    handleRequest(InterestRequest.createAddInterestRequest((SelectableChannel) channel, writer, SelectionKey.OP_WRITE,
                                                           (CoreNIOServices) ownerThread));
  }

  void requestAcceptInterest(TCListenerJDK14 lsnr, ServerSocketChannel ssc) {
    handleRequest(InterestRequest.createSetInterestRequest(ssc, lsnr, SelectionKey.OP_ACCEPT,
                                                           (CoreNIOServices) ownerThread));
  }

  void removeWriteInterest(TCConnectionJDK14 conn, SelectableChannel channel) {
    handleRequest(InterestRequest.createRemoveInterestRequest(channel, conn, SelectionKey.OP_WRITE,
                                                              (CoreNIOServices) ownerThread));
  }

  void removeReadInterest(TCConnectionJDK14 conn, SelectableChannel channel) {
    handleRequest(InterestRequest.createRemoveInterestRequest(channel, conn, SelectionKey.OP_READ,
                                                              (CoreNIOServices) ownerThread));
  }

  private static class InterestRequest {
    final SelectableChannel channel;
    final Object            attachment;
    final boolean           set;
    final boolean           add;
    final boolean           remove;
    final int               interestOps;
    final CoreNIOServices   commNIOServiceThread;

    static InterestRequest createAddInterestRequest(SelectableChannel channel, Object attachment, int interestOps,
                                                    CoreNIOServices nioServiceThread) {
      return new InterestRequest(channel, attachment, interestOps, false, true, false, nioServiceThread);
    }

    static InterestRequest createSetInterestRequest(SelectableChannel channel, Object attachment, int interestOps,
                                                    CoreNIOServices nioServiceThread) {
      return new InterestRequest(channel, attachment, interestOps, true, false, false, nioServiceThread);
    }

    static InterestRequest createRemoveInterestRequest(SelectableChannel channel, Object attachment, int interestOps,
                                                       CoreNIOServices nioServiceThread) {
      return new InterestRequest(channel, attachment, interestOps, false, false, true, nioServiceThread);
    }

    private InterestRequest(SelectableChannel channel, Object attachment, int interestOps, boolean set, boolean add,
                            boolean remove, CoreNIOServices nioServiceThread) {
      Assert.eval(remove ^ set ^ add);
      Assert.eval(channel != null);

      this.channel = channel;
      this.attachment = attachment;
      this.set = set;
      this.add = add;
      this.remove = remove;
      this.interestOps = interestOps;
      this.commNIOServiceThread = nioServiceThread;
    }

    public CoreNIOServices getCommNIOServiceThread() {
      return commNIOServiceThread;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();

      buf.append("Interest modify request: ").append(channel.toString()).append("\n");
      buf.append("Ops: ").append(Constants.interestOpsToString(interestOps)).append("\n");
      buf.append("Set: ").append(set).append(", Remove: ").append(remove).append(", Add: ").append(add).append("\n");
      buf.append("Attachment: ");

      if (attachment != null) {
        buf.append(attachment.toString());
      } else {
        buf.append("null");
      }

      buf.append("\n");

      return buf.toString();
    }

  }

}