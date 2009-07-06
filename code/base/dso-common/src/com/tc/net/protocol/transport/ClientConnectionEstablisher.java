/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.async.api.Stage;
import com.tc.async.impl.StageManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressIterator;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOOEventHandler;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionManagerImpl;
import com.tc.object.session.SessionProvider;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;
import com.terracottatech.config.L1ReconnectPropertiesDocument;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

/**
 * This guy establishes a connection to the server for the Client.
 */
public class ClientConnectionEstablisher {

  private static final long               CONNECT_RETRY_INTERVAL;

  private static final long               MIN_RETRY_INTERVAL = 10;

  static {
    TCLogger logger = TCLogging.getLogger(ClientConnectionEstablisher.class);

    long value = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.L1_SOCKET_RECONNECT_WAIT_INTERVAL);
    if (value < MIN_RETRY_INTERVAL) {
      logger.warn("Forcing reconnect wait interval to " + MIN_RETRY_INTERVAL + " (configured value was " + value + ")");
      value = MIN_RETRY_INTERVAL;
    }

    CONNECT_RETRY_INTERVAL = value;
  }

  private final String                    desc;
  private final int                       maxReconnectTries;
  private final int                       timeout;
  private final ConnectionAddressProvider connAddressProvider;
  private final TCConnectionManager       connManager;

  private final SynchronizedBoolean       asyncReconnecting  = new SynchronizedBoolean(false);

  private Thread                          connectionEstablisher;

  private NoExceptionLinkedQueue          reconnectRequest   = new NoExceptionLinkedQueue();  // <ConnectionRequest>

  public ClientConnectionEstablisher(TCConnectionManager connManager, ConnectionAddressProvider connAddressProvider,
                                     int maxReconnectTries, int timeout) {
    this.connManager = connManager;
    this.connAddressProvider = connAddressProvider;
    this.maxReconnectTries = maxReconnectTries;
    this.timeout = timeout;

    if (maxReconnectTries == 0) desc = "none";
    else if (maxReconnectTries < 0) desc = "unlimited";
    else desc = "" + maxReconnectTries;

  }

  /**
   * Blocking open. Causes a connection to be made. Will throw exceptions if the connect fails.
   * 
   * @throws TCTimeoutException
   * @throws IOException
   * @throws TCTimeoutException
   * @throws MaxConnectionsExceededException
   */
  public TCConnection open(ClientMessageTransport cmt) throws TCTimeoutException, IOException {
    synchronized (asyncReconnecting) {
      Assert.eval("Can't call open() while asynch reconnect occurring", !asyncReconnecting.get());
      return connectTryAllOnce(cmt);
    }
  }

  private TCConnection connectTryAllOnce(ClientMessageTransport cmt) throws TCTimeoutException, IOException {
    final ConnectionAddressIterator addresses = connAddressProvider.getIterator();
    TCConnection rv = null;
    while (addresses.hasNext()) {
      final ConnectionInfo connInfo = addresses.next();
      try {
        final TCSocketAddress csa = new TCSocketAddress(connInfo);
        rv = connect(csa, cmt);
        break;
      } catch (TCTimeoutException e) {
        if (!addresses.hasNext()) { throw e; }
      } catch (IOException e) {
        if (!addresses.hasNext()) { throw e; }
      }
    }
    return rv;
  }

  /**
   * Tries to make a connection. This is a blocking call.
   * 
   * @return
   * @throws TCTimeoutException
   * @throws IOException
   * @throws MaxConnectionsExceededException
   */
  TCConnection connect(TCSocketAddress sa, ClientMessageTransport cmt) throws TCTimeoutException, IOException {

    TCConnection connection = this.connManager.createConnection(cmt.getProtocolAdapter());
    cmt.fireTransportConnectAttemptEvent();
    try {
      connection.connect(sa, timeout);
    } catch (IOException e) {
      connection.close(100);
      throw e;
    } catch (TCTimeoutException e) {
      connection.close(100);
      throw e;
    }
    return connection;
  }

  public String toString() {
    return "ClientConnectionEstablisher[" + connAddressProvider + ", timeout=" + timeout + "]";
  }

  private void reconnect(ClientMessageTransport cmt) throws MaxConnectionsExceededException {
    try {

      boolean connected = cmt.isConnected();
      if (connected) {
        cmt.logger.warn("Got reconnect request for ClientMessageTransport that is connected.  skipping");
        return;
      }

      asyncReconnecting.set(true);
      int connTimeoutToAllL2s = 1;
      for (int i = 0; ((maxReconnectTries < 0) || (i < maxReconnectTries)) && !connected; i++) {
        ConnectionAddressIterator addresses = connAddressProvider.getIterator();
        int l2Count = 0;
        int connTimeoutCount = 0;
        while (addresses.hasNext() && !connected) {
          TCConnection connection = null;
          final ConnectionInfo connInfo = addresses.next();
          try {
            if (i % 20 == 0) {
              cmt.logger.warn("Reconnect attempt " + i + " of " + desc + " reconnect tries to " + connInfo
                              + ", timeout=" + timeout);
            }
            l2Count++;
            connection = connect(new TCSocketAddress(connInfo), cmt);
            cmt.reconnect(connection);
            connected = true;
          } catch (MaxConnectionsExceededException e) {
            throw e;
          } catch (TCTimeoutException e) {
            connTimeoutCount++;
            handleConnectException(e, false, cmt.logger, connection);
          } catch (IOException e) {
            handleConnectException(e, false, cmt.logger, connection);
          } catch (Exception e) {
            handleConnectException(e, true, cmt.logger, connection);
          }
        }

        if (l2Count == connTimeoutCount) {
          connTimeoutToAllL2s++;
        }

        // DEV-1956, DEV-2344: connection to both L2s are timing out ..
        // starting extra process check
        if ((connTimeoutToAllL2s % 5) == 0) {
          addresses = connAddressProvider.getIterator();
          while (addresses.hasNext()) {
            DEV1956Debugger dev1956 = new DEV1956Debugger(addresses.next(), connTimeoutToAllL2s);
            try {
              dev1956.setName("DEV1956-DEBUG-THREAD-" + connTimeoutToAllL2s);
              dev1956.start();
              dev1956.join(2 * timeout);

              if (dev1956.isAlive()) {
                dev1956.interrupt();
              }

            } catch (Exception e) {
              cmt.logger.warn("DEV1956: " + e);
            }
            ThreadUtil.reallySleep(2000);
          }
        }

      }
      cmt.endIfDisconnected();
    } finally {
      asyncReconnecting.set(false);
    }
  }

  private void restoreConnection(ClientMessageTransport cmt, TCSocketAddress sa, long timeoutMillis,
                                 RestoreConnectionCallback callback) throws MaxConnectionsExceededException {
    final long deadline = System.currentTimeMillis() + timeoutMillis;
    boolean connected = cmt.isConnected();
    if (connected) {
      cmt.logger.warn("Got restoreConnection request for ClientMessageTransport that is connected.  skipping");
    }

    asyncReconnecting.set(true);
    for (int i = 0; !connected; i++) {
      TCConnection connection = null;
      try {
        connection = connect(sa, cmt);
        cmt.reconnect(connection);
        connected = true;
      } catch (MaxConnectionsExceededException e) {
        throw e;
      } catch (TCTimeoutException e) {
        handleConnectException(e, false, cmt.logger, connection);
      } catch (IOException e) {
        handleConnectException(e, false, cmt.logger, connection);
      } catch (Exception e) {
        handleConnectException(e, true, cmt.logger, connection);
      }
      if (connected || System.currentTimeMillis() > deadline) {
        break;
      }
    }
    asyncReconnecting.set(false);
    if (!connected) {
      callback.restoreConnectionFailed(cmt);
    }
  }

  private void handleConnectException(Exception e, boolean logFullException, TCLogger logger, TCConnection connection) {
    if (connection != null) connection.close(100);

    if (logger.isDebugEnabled() || logFullException) {
      logger.error("Connect Exception", e);
    } else {
      logger.warn(e.getMessage());
    }

    if (CONNECT_RETRY_INTERVAL > 0) {
      try {
        Thread.sleep(CONNECT_RETRY_INTERVAL);
      } catch (InterruptedException e1) {
        //
      }
    }
  }

  public void asyncReconnect(ClientMessageTransport cmt) {
    synchronized (asyncReconnecting) {
      if (asyncReconnecting.get()) return;
      putReconnectRequest(new ConnectionRequest(ConnectionRequest.RECONNECT, cmt));
    }
  }

  public void asyncRestoreConnection(ClientMessageTransport cmt, TCSocketAddress sa,
                                     RestoreConnectionCallback callback, long timeoutMillis) {
    synchronized (asyncReconnecting) {
      if (asyncReconnecting.get()) return;
      putReconnectRequest(new RestoreConnectionRequest(cmt, sa, callback, timeoutMillis));
    }
  }

  private void putReconnectRequest(ConnectionRequest request) {
    if (connectionEstablisher == null) {
      // First time
      // Allow the async thread reconnects/restores only when cmt was
      // connected atleast once
      if ((request.getClientMessageTransport() == null) || (!request.getClientMessageTransport().wasOpened())) return;

      connectionEstablisher = new Thread(new AsyncReconnect(this), "ConnectionEstablisher");
      connectionEstablisher.setDaemon(true);
      connectionEstablisher.start();

    }

    // DEV-1140 : avoiding the race condition
    // asyncReconnecting.set(true);
    reconnectRequest.put(request);
  }

  public void quitReconnectAttempts() {
    putReconnectRequest(new ConnectionRequest(ConnectionRequest.QUIT, null));
  }

  static class AsyncReconnect implements Runnable {
    private final ClientConnectionEstablisher cce;

    public AsyncReconnect(ClientConnectionEstablisher cce) {
      this.cce = cce;
    }

    public void run() {
      ConnectionRequest request = null;
      while ((request = (ConnectionRequest) cce.reconnectRequest.take()) != null) {
        if (request.isReconnect()) {
          ClientMessageTransport cmt = request.getClientMessageTransport();
          try {
            cce.reconnect(cmt);
          } catch (MaxConnectionsExceededException e) {
            cmt.logger.warn(e);
            cmt.logger.warn("No longer trying to reconnect.");
            return;
          } catch (Throwable t) {
            cmt.logger.warn("Reconnect failed !", t);
          }
        } else if (request.isRestoreConnection()) {
          RestoreConnectionRequest req = (RestoreConnectionRequest) request;
          ClientMessageTransport cmt = request.getClientMessageTransport();
          try {
            cce.restoreConnection(req.getClientMessageTransport(), req.getSocketAddress(), req.getTimeoutMillis(), req
                .getCallback());
          } catch (MaxConnectionsExceededException e) {
            cmt.logger.warn(e);
            cmt.logger.warn("No longer trying to reconnect.");
            return;
          } catch (Throwable t) {
            cmt.logger.warn("Reconnect failed !", t);
          }
        } else if (request.isQuit()) {
          break;
        }
      }
    }
  }

  static class ConnectionRequest {

    public static final int              RECONNECT          = 1;
    public static final int              QUIT               = 2;
    public static final int              RESTORE_CONNECTION = 3;

    private final int                    type;
    private final TCSocketAddress        sa;
    private final ClientMessageTransport cmt;

    public ConnectionRequest(int type, ClientMessageTransport cmt) {
      this(type, cmt, null);
    }

    public ConnectionRequest(final int type, final ClientMessageTransport cmt, final TCSocketAddress sa) {
      this.type = type;
      this.cmt = cmt;
      this.sa = sa;
    }

    public boolean isReconnect() {
      return type == RECONNECT;
    }

    public boolean isQuit() {
      return type == QUIT;
    }

    public boolean isRestoreConnection() {
      return type == RESTORE_CONNECTION;
    }

    public TCSocketAddress getSocketAddress() {
      return sa;
    }

    public ClientMessageTransport getClientMessageTransport() {
      return cmt;
    }
  }

  static class RestoreConnectionRequest extends ConnectionRequest {

    private final RestoreConnectionCallback callback;
    private final long                      timeoutMillis;

    public RestoreConnectionRequest(ClientMessageTransport cmt, final TCSocketAddress sa,
                                    RestoreConnectionCallback callback, long timeoutMillis) {
      super(RESTORE_CONNECTION, cmt, sa);
      this.callback = callback;
      this.timeoutMillis = timeoutMillis;
    }

    public RestoreConnectionCallback getCallback() {
      return callback;
    }

    public long getTimeoutMillis() {
      return timeoutMillis;
    }
  }

  private class DEV1956Debugger extends Thread {

    private final TCLogger       logger;
    private final ConnectionInfo connInfo;
    private final int            id;

    public DEV1956Debugger(final ConnectionInfo conInfo, int id) {
      this.connInfo = conInfo;
      this.logger = CustomerLogging.getDSOGenericLogger();
      this.id = id;
    }

    public void run() {

      logger.info("DEV1956: extra connect check to servers START - " + id + " :" + this.connInfo);
      InputStream in = getL1PropertiesFromL2Stream2(this.connInfo);
      if (in == null) {
        logger.info("DEV1956: not able to get l1 reconnect props. END - " + id);
        return;
      }

      L1ReconnectConfigImpl l1ReconnectConfig = getL1ReconnectConfigFromStream(in);
      if (l1ReconnectConfig == null) {
        logger.info("DEV1956: parsing stream error. END - " + id);
        return;
      }

      final boolean useOOOLayer = l1ReconnectConfig.getReconnectEnabled();

      final NetworkStackHarnessFactory networkStackHarnessFactory;
      if (useOOOLayer) {
        StageManagerImpl stageManager = new StageManagerImpl(new TCThreadGroup(new ThrowableHandler(TCLogging
            .getLogger(StageManagerImpl.class))), new QueueFactory(BoundedLinkedQueue.class.getName()));
        final Stage oooSendStage = stageManager.createStage("OOONetSendStage", new OOOEventHandler(), 1, 500);
        final Stage oooReceiveStage = stageManager.createStage("OOONetReceiveStage", new OOOEventHandler(), 1, 500);
        networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
                                                                       new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                       oooSendStage.getSink(), oooReceiveStage
                                                                           .getSink(), l1ReconnectConfig);

      } else {
        networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
      }

      TCProperties tcProperties = TCPropertiesImpl.getProperties();
      MessageMonitor mm = MessageMonitorImpl.createMonitor(tcProperties, CustomerLogging.getDSOGenericLogger());
      CommunicationsManager communicationsManager = new CommunicationsManagerImpl(mm, networkStackHarnessFactory,
                                                                                  new NullConnectionPolicy());

      final Sequence sessionSequence = new SimpleSequence();
      final SessionManager sessionManager = new SessionManagerImpl(sessionSequence);
      final SessionProvider sessionProvider = (SessionProvider) sessionManager;

      logger.info("DEV1956: opening a test client channel to " + connInfo);
      ClientMessageChannel channel = communicationsManager
          .createClientChannel(sessionProvider, maxReconnectTries, connInfo.getHostname(), connInfo.getPort(), timeout,
                               new ConnectionAddressProvider(new ConnectionInfo[] { connInfo }));
      try {
        channel.open();
      } catch (TCTimeoutException e) {
        logger.warn("DEV1956: channel open timeout. connecting to " + connInfo.getHostname() + ":" + connInfo.getPort()
                    + ". " + e);
      } catch (UnknownHostException e) {
        logger.warn("DEV1956: channel open - unknown host " + connInfo.getHostname() + ":" + connInfo.getPort() + ". "
                    + e);
      } catch (MaxConnectionsExceededException e) {
        logger.warn("DEV1956: max conn exceeded. " + e);
      } catch (IOException e) {
        logger.warn("DEV1956: channel open io exception " + e);
      }

      ThreadUtil.reallySleep(1000);
      logger.info("DEV1956: closing the test client channel to " + connInfo);
      channel.close();
      logger.info("DEV1956: extra connect check to servers END - " + this.id);
    }

    private InputStream getL1PropertiesFromL2Stream2(ConnectionInfo connectInfo) {
      InetSocketAddress inetAddr = new InetSocketAddress(connectInfo.getHostname(), connectInfo.getPort());
      try {
        SocketChannel sc = SocketChannel.open();
        sc.socket().connect(inetAddr, timeout);
        char[] getRequest = "GET /l1reconnectproperties".toCharArray();
        PrintWriter writer = new PrintWriter(sc.socket().getOutputStream(), true);
        writer.println(getRequest);
        return sc.socket().getInputStream();
      } catch (Exception e) {
        logger.info("DEV1956: socket connect to " + inetAddr + " error: " + e);
      }
      return null;
    }

    private L1ReconnectConfigImpl getL1ReconnectConfigFromStream(InputStream in) {
      L1ReconnectPropertiesDocument l1ReconnectPropFromL2;
      try {
        if (in.markSupported()) in.mark(100);
        l1ReconnectPropFromL2 = L1ReconnectPropertiesDocument.Factory.parse(in);
      } catch (Exception e) {
        if (in.markSupported()) {
          byte[] l1prop = new byte[100];
          int bytesRead = -1;
          try {
            in.reset();
            bytesRead = in.read(l1prop, 0, l1prop.length);
          } catch (IOException ioe) {
            logger.warn("DEV1956: " + ioe);
          }
          if (bytesRead > 0) logger.error("DEV1956: Error parsing l1 properties from server : " + new String(l1prop)
                                          + "...");
        }
        String errorMessage = "DEV1956: l1 reconnect props read and parse error. ";
        logger.error(errorMessage + e);
        return null;
      }

      boolean l1ReconnectEnabled = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectEnabled();
      int l1ReconnectTimeout = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectTimeout().intValue();
      int l1ReconnectSendqueuecap = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectSendqueuecap()
          .intValue();
      int l1ReconnectMaxdelayedacks = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectMaxDelayedAcks()
          .intValue();
      int l1ReconnectSendwindow = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectSendwindow()
          .intValue();
      return new L1ReconnectConfigImpl(l1ReconnectEnabled, l1ReconnectTimeout, l1ReconnectSendqueuecap,
                                       l1ReconnectMaxdelayedacks, l1ReconnectSendwindow);
    }
  }

}
