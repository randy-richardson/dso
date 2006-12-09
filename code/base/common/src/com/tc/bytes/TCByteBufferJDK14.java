/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bytes;

import java.nio.ByteBuffer;

/**
 * @author teck A thin wrapper to a real java.nio.ByteBuffer instance
 */

// XXX: Should we wrap the native java.nio overflow, underflow and readOnly exceptions with the TC versions?
// This would make the TCByteBuffer interface consistent w.r.t. exceptions (whilst being blind to JDK13 vs JDK14)
class TCByteBufferJDK14 extends AbstractTCByteBuffer {

  private final ByteBuffer        buffer;
  private final TCByteBufferJDK14 root;

  TCByteBufferJDK14(int capacity, boolean direct) {
    if (direct) {
      buffer = ByteBuffer.allocateDirect(capacity);
    } else {
      buffer = ByteBuffer.allocate(capacity);
    }
    root = this;
  }

  private TCByteBufferJDK14(ByteBuffer buf) {
    buffer = buf;
    this.root = null;
  }

  private TCByteBufferJDK14(ByteBuffer buf, TCByteBufferJDK14 root) {
    buffer = buf;
    this.root = root;
  }

  static TCByteBufferJDK14 wrap(byte[] data) {
    return new TCByteBufferJDK14(ByteBuffer.wrap(data));
  }

  protected ByteBuffer getBuffer() {
    return buffer;
  }

  public TCByteBuffer clear() {
    buffer.clear();
    return this;
  }

  public int capacity() {
    return buffer.capacity();
  }

  public int position() {
    return buffer.position();
  }

  public TCByteBuffer flip() {
    buffer.flip();
    return this;
  }

  public boolean hasRemaining() {
    return buffer.hasRemaining();
  }

  public int limit() {
    return buffer.limit();
  }

  public TCByteBuffer limit(int newLimit) {
    buffer.limit(newLimit);
    return this;
  }

  public TCByteBuffer position(int newPosition) {
    buffer.position(newPosition);
    return this;
  }

  public int remaining() {
    return buffer.remaining();
  }

  public com.tc.bytes.TCByteBuffer rewind() {
    buffer.rewind();
    return this;
  }

  public boolean isNioBuffer() {
    return true;
  }

  public Object getNioBuffer() {
    return buffer;
  }

  public boolean isDirect() {
    return buffer.isDirect();
  }

  public byte[] array() {
    return buffer.array();
  }

  public byte get() {
    return buffer.get();
  }

  public boolean getBoolean() {
    // XXX: Um-- why isn't there a getBoolean in ByteBuffer?
    return buffer.get() > 0;
  }

  public boolean getBoolean(int index) {
    return buffer.get(index) > 0;
  }

  public char getChar() {
    return buffer.getChar();
  }

  public char getChar(int index) {
    return buffer.getChar(index);
  }

  public double getDouble() {
    return buffer.getDouble();
  }

  public double getDouble(int index) {
    return buffer.getDouble(index);
  }

  public float getFloat() {
    return buffer.getFloat();
  }

  public float getFloat(int index) {
    return buffer.getFloat(index);
  }

  public int getInt() {
    return buffer.getInt();
  }

  public int getInt(int index) {
    return buffer.getInt(index);
  }

  public long getLong() {
    return buffer.getLong();
  }

  public long getLong(int index) {
    return buffer.getLong(index);
  }

  public short getShort() {
    return buffer.getShort();
  }

  public short getShort(int index) {
    return buffer.getShort(index);
  }

  public TCByteBuffer get(byte[] dst) {
    buffer.get(dst);
    return this;
  }

  public TCByteBuffer get(byte[] dst, int offset, int length) {
    buffer.get(dst, offset, length);
    return this;
  }

  public byte get(int index) {
    return buffer.get(index);
  }

  public TCByteBuffer put(byte b) {
    buffer.put(b);
    return this;
  }

  public TCByteBuffer put(byte[] src) {
    buffer.put(src);
    return this;
  }

  public TCByteBuffer put(byte[] src, int offset, int length) {
    buffer.put(src, offset, length);
    return this;
  }

  public TCByteBuffer put(int index, byte b) {
    buffer.put(index, b);
    return this;
  }

  public TCByteBuffer putBoolean(boolean b) {
    // XXX: Why isn't there a putBoolean in ByteBuffer?
    buffer.put((b) ? (byte) 1 : (byte) 0);
    return this;
  }

  public TCByteBuffer putBoolean(int index, boolean b) {
    buffer.put(index, (b) ? (byte) 1 : (byte) 0);
    return this;
  }

  public TCByteBuffer putChar(char c) {
    buffer.putChar(c);
    return this;
  }

  public TCByteBuffer putChar(int index, char c) {
    buffer.putChar(index, c);
    return this;
  }

  public TCByteBuffer putDouble(double d) {
    buffer.putDouble(d);
    return this;
  }

  public TCByteBuffer putDouble(int index, double d) {
    buffer.putDouble(index, d);
    return this;
  }

  public TCByteBuffer putFloat(float f) {
    buffer.putFloat(f);
    return this;
  }

  public TCByteBuffer putFloat(int index, float f) {
    buffer.putFloat(index, f);
    return this;
  }

  public TCByteBuffer putInt(int i) {
    buffer.putInt(i);
    return this;
  }

  public TCByteBuffer putInt(int index, int i) {
    buffer.putInt(index, i);
    return this;
  }

  public TCByteBuffer putLong(long l) {
    buffer.putLong(l);
    return this;
  }

  public TCByteBuffer putLong(int index, long l) {
    buffer.putLong(index, l);
    return this;
  }

  public TCByteBuffer putShort(short s) {
    buffer.putShort(s);
    return this;
  }

  public TCByteBuffer putShort(int index, short s) {
    buffer.putShort(index, s);
    return this;
  }

  public TCByteBuffer duplicate() {
    return new TCByteBufferJDK14(buffer.duplicate(), root);
  }

  public TCByteBuffer put(TCByteBuffer src) {
    if (!src.isNioBuffer()) { throw new IllegalArgumentException("src buffer is not backed by a java.nio.ByteBuffer"); }

    buffer.put((ByteBuffer) src.getNioBuffer());
    return this;
  }

  public TCByteBuffer slice() {
    return new TCByteBufferJDK14(buffer.slice(), root);
  }

  public int arrayOffset() {
    return buffer.arrayOffset();
  }

  public TCByteBuffer asReadOnlyBuffer() {
    return new TCByteBufferJDK14(buffer.asReadOnlyBuffer(), root);
  }

  public boolean isReadOnly() {
    return buffer.isReadOnly();
  }

  public String toString() {
    return (buffer == null) ? "TCByteBufferJDK14(null buffer)" : "TCByteBufferJDK14@" + System.identityHashCode(this)
                                                                 + "(" + buffer.toString() + ")";
  }

  public boolean hasArray() {
    return buffer.hasArray();
  }

  // Can be called only once on any of the views and the root is gone
  public void recycle() {
    if (root != null) {
      TCByteBufferFactory.returnBuffer(root.reInit());
    }
  }

  private TCByteBufferJDK14 reInit() {
    clear();
    return this;
  }

  /* This is the debug version. PLEASE DONT DELETE */

  // private static final TCLogger logger = TCLogging.getLogger(TCByteBufferJDK14.class);
  //
  // private final ByteBuffer buffer;
  // private final TCByteBufferJDK14 root;
  // private List childs;
  // private static final boolean debug = true;
  // private static final boolean debugFinalize = false;
  // private ActivityMonitor monitor;
  //
  // TCByteBufferJDK14(int capacity, boolean direct) {
  // if (direct) {
  // buffer = ByteBuffer.allocateDirect(capacity);
  // } else {
  // buffer = ByteBuffer.allocate(capacity);
  // }
  // root = this;
  // if (debug) {
  // childs = new ArrayList();
  // monitor = new ActivityMonitor();
  // monitor.addActivity("TCBB", "Created");
  // }
  // }
  //
  // private TCByteBufferJDK14(ByteBuffer buf) {
  // buffer = buf;
  // this.root = null;
  // if (debug) childs = new ArrayList();
  // }
  //
  // private TCByteBufferJDK14(ByteBuffer buf, TCByteBufferJDK14 root) {
  // buffer = buf;
  // childs = null;
  // this.root = root;
  // if (debug) this.root.addChild(this);
  // }
  //
  // private void addChild(TCByteBufferJDK14 child) {
  // if (debug) childs.add(child);
  // }
  //
  // static TCByteBufferJDK14 wrap(byte[] data) {
  // return new TCByteBufferJDK14(ByteBuffer.wrap(data));
  // }
  //
  // protected ByteBuffer getBuffer() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer;
  // }
  //
  // public TCByteBuffer clear() {
  // buffer.clear();
  // if (debug) {
  // childs.clear();
  // monitor.clear();
  // }
  // return this;
  // }
  //
  // public int capacity() {
  // if (debug) checkState();
  // return buffer.capacity();
  // }
  //
  // public int position() {
  // if (debug) checkState();
  // return buffer.position();
  // }
  //
  // public TCByteBuffer flip() {
  // if (debug) checkState();
  // buffer.flip();
  // return this;
  // }
  //
  // private void checkState() {
  // if (debug && this != root) {
  // // This doesnt check for the root itself, I dont know how to check for the root itself being modified once check
  // // back in
  // Assert.assertNotNull(root);
  // Assert.assertTrue(root.isChild(this));
  // }
  // }
  //
  // private boolean isChild(TCByteBufferJDK14 child) {
  // return childs.contains(child);
  // }
  //
  // public boolean hasRemaining() {
  // if (debug) checkState();
  // return buffer.hasRemaining();
  // }
  //
  // public int limit() {
  // if (debug) checkState();
  // return buffer.limit();
  // }
  //
  // public TCByteBuffer limit(int newLimit) {
  // if (debug) checkState();
  // buffer.limit(newLimit);
  // return this;
  // }
  //
  // public TCByteBuffer position(int newPosition) {
  // if (debug) checkState();
  // buffer.position(newPosition);
  // return this;
  // }
  //
  // public int remaining() {
  // if (debug) checkState();
  // return buffer.remaining();
  // }
  //
  // public com.tc.bytes.TCByteBuffer rewind() {
  // if (debug) checkState();
  // buffer.rewind();
  // return this;
  // }
  //
  // public boolean isNioBuffer() {
  // return true;
  // }
  //
  // public Object getNioBuffer() {
  // if (debug) checkState();
  // return buffer;
  // }
  //
  // public boolean isDirect() {
  // return buffer.isDirect();
  // }
  //
  // public byte[] array() {
  // // Not fool proof
  // if (debug) checkState();
  // return buffer.array();
  // }
  //
  // public byte get() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.get();
  // }
  //
  // public boolean getBoolean() {
  // // XXX: Um-- why isn't there a getBoolean in ByteBuffer?
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.get() > 0;
  // }
  //
  // public boolean getBoolean(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.get(index) > 0;
  // }
  //
  // public char getChar() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getChar();
  // }
  //
  // public char getChar(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getChar(index);
  // }
  //
  // public double getDouble() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getDouble();
  // }
  //
  // public double getDouble(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getDouble(index);
  // }
  //
  // public float getFloat() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getFloat();
  // }
  //
  // public float getFloat(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getFloat(index);
  // }
  //
  // public int getInt() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getInt();
  // }
  //
  // public int getInt(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getInt(index);
  // }
  //
  // public long getLong() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getLong();
  // }
  //
  // public long getLong(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getLong(index);
  // }
  //
  // public short getShort() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getShort();
  // }
  //
  // public short getShort(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getShort(index);
  // }
  //
  // public TCByteBuffer get(byte[] dst) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // buffer.get(dst);
  // return this;
  // }
  //
  // public TCByteBuffer get(byte[] dst, int offset, int length) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // buffer.get(dst, offset, length);
  // return this;
  // }
  //
  // public byte get(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.get(index);
  // }
  //
  // public TCByteBuffer put(byte b) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.put(b);
  // return this;
  // }
  //
  // public TCByteBuffer put(byte[] src) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.put(src);
  // return this;
  // }
  //
  // public TCByteBuffer put(byte[] src, int offset, int length) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.put(src, offset, length);
  // return this;
  // }
  //
  // public TCByteBuffer put(int index, byte b) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.put(index, b);
  // return this;
  // }
  //
  // public TCByteBuffer putBoolean(boolean b) {
  // // XXX: Why isn't there a putBoolean in ByteBuffer?
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.put((b) ? (byte) 1 : (byte) 0);
  // return this;
  // }
  //
  // public TCByteBuffer putBoolean(int index, boolean b) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.put(index, (b) ? (byte) 1 : (byte) 0);
  // return this;
  // }
  //
  // public TCByteBuffer putChar(char c) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putChar(c);
  // return this;
  // }
  //
  // public TCByteBuffer putChar(int index, char c) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putChar(index, c);
  // return this;
  // }
  //
  // public TCByteBuffer putDouble(double d) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putDouble(d);
  // return this;
  // }
  //
  // public TCByteBuffer putDouble(int index, double d) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putDouble(index, d);
  // return this;
  // }
  //
  // public TCByteBuffer putFloat(float f) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putFloat(f);
  // return this;
  // }
  //
  // public TCByteBuffer putFloat(int index, float f) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putFloat(index, f);
  // return this;
  // }
  //
  // public TCByteBuffer putInt(int i) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putInt(i);
  // return this;
  // }
  //
  // public TCByteBuffer putInt(int index, int i) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putInt(index, i);
  // return this;
  // }
  //
  // public TCByteBuffer putLong(long l) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putLong(l);
  // return this;
  // }
  //
  // public TCByteBuffer putLong(int index, long l) {
  // if (debug) checkState();
  // logPut();
  // buffer.putLong(index, l);
  // return this;
  // }
  //
  // public TCByteBuffer putShort(short s) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putShort(s);
  // return this;
  // }
  //
  // public TCByteBuffer putShort(int index, short s) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putShort(index, s);
  // return this;
  // }
  //
  // public TCByteBuffer duplicate() {
  // if (debug) checkState();
  // return new TCByteBufferJDK14(buffer.duplicate(), root);
  // }
  //
  // public TCByteBuffer put(TCByteBuffer src) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  //
  // if (!src.isNioBuffer()) { throw new IllegalArgumentException("src buffer is not backed by a java.nio.ByteBuffer");
  // }
  //
  // buffer.put((ByteBuffer) src.getNioBuffer());
  // return this;
  // }
  //
  // public TCByteBuffer slice() {
  // if (debug) checkState();
  // return new TCByteBufferJDK14(buffer.slice(), root);
  // }
  //
  // public int arrayOffset() {
  // if (debug) checkState();
  // return buffer.arrayOffset();
  // }
  //
  // public TCByteBuffer asReadOnlyBuffer() {
  // if (debug) checkState();
  // return new TCByteBufferJDK14(buffer.asReadOnlyBuffer(), root);
  // }
  //
  // public boolean isReadOnly() {
  // if (debug) checkState();
  // return buffer.isReadOnly();
  // }
  //
  // public String toString() {
  // if (debug) checkState();
  // return (buffer == null) ? "null buffer" : buffer.toString();
  // }
  //
  // public boolean hasArray() {
  // if (debug) checkState();
  // return buffer.hasArray();
  // }
  //
  // // Can be called only once on any of the views and the root is gone
  // public void recycle() {
  // if (debug) checkState();
  // if(root != null) TCByteBufferFactory.returnBuffer(root.reInit());
  // }
  //
  // private TCByteBufferJDK14 reInit() {
  // clear();
  // return this;
  // }
  //
  // void logGet() {
  // if(root !=null) {
  // root.monitor.clear();
  // root.monitor.addActivity("TCBB", "get");
  // }
  // }
  //
  // void logPut() {
  // if(root !=null) {
  // root.monitor.clear();
  // root.monitor.addActivity("TCBB", "put");
  // }
  // }
  //
  // static int count;
  // static {
  // CommonShutDownHook.addShutdownHook(new Runnable() {
  // public void run() {
  // logger.info("No of Root Buffers finalized = " + count);
  // }
  // });
  // }
  //
  // public void finalize() {
  // if (this == root) {
  // count++;
  // if (debugFinalize) monitor.printActivityFor("TCBB");
  // }
  // }
}