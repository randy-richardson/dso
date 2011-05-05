/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.memcached;

import org.jboss.netty.buffer.ChannelBuffers;

import com.tc.test.TCTestCase;
import com.thimbleware.jmemcached.Key;
import com.thimbleware.jmemcached.LocalCacheElement;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public class TCMemcacheStorageTest extends TCTestCase {

  public void testBasic() throws IOException, ClassNotFoundException {

    Key key = new Key(ChannelBuffers.wrappedBuffer("key-key".getBytes()));
    LocalCacheElement element = new LocalCacheElement(key);
    element.setData(ChannelBuffers.wrappedBuffer("value-value-1".getBytes()));

    System.out.println("XXX IN " + element + " - " + Arrays.toString(element.getData().array()) + " - "
                       + element.getData().array().length + " - " + element.getData().capacity());

    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
    objOut.writeObject(element);
    objOut.close();

    byte[] b = byteOut.toByteArray();
    ByteArrayInputStream byteIn = new ByteArrayInputStream(b);
    ObjectInputStream objIn = new ObjectInputStream(byteIn);
    System.out.println("XXX OUT " + objIn.readObject());

  }

}
