/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tc.io;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

public class BasicStreamTest extends TestCase {

  public void testBasic() throws IOException {
    TCByteBufferOutputStream os = new TCByteBufferOutputStream();
    
    os.write(new byte[] { -5, 5 });
    os.write(42);
    os.writeBoolean(true);
    os.writeBoolean(false);
    os.writeByte(11);
    os.writeChar('t');
    os.writeDouble(3.14D);
    os.writeFloat(2.78F);
    os.writeInt(12345678);
    os.writeLong(Long.MIN_VALUE);
    os.writeShort(Short.MAX_VALUE);
    os.writeString("yo yo yo");
    os.writeString(null);
    os.writeString(createString(100000));

    TCByteBufferInputStream is = new TCByteBufferInputStream(os.toArray());
    byte[] b = new byte[2];
    Arrays.fill(b, (byte) 69); // these values will be overwritten
    int read = is.read(b);
    assertEquals(2, read);
    assertTrue(Arrays.equals(new byte[] { -5, 5 }, b));
    assertEquals(42, is.read());
    assertEquals(true, is.readBoolean());
    assertEquals(false, is.readBoolean());
    assertEquals(11, is.readByte());
    assertEquals('t', is.readChar());
    assertEquals(Double.doubleToLongBits(3.14D), Double.doubleToLongBits(is.readDouble()));
    assertEquals(Float.floatToIntBits(2.78F), Float.floatToIntBits(is.readFloat()));
    assertEquals(12345678, is.readInt());
    assertEquals(Long.MIN_VALUE, is.readLong());
    assertEquals(Short.MAX_VALUE, is.readShort());
    assertEquals("yo yo yo", is.readString());
    assertEquals(null, is.readString());
    assertEquals(createString(100000), is.readString());
  }

  private static String createString(int length) {
    char[] chars = new char[length];
    Arrays.fill(chars, 't');
    return new String(chars);
  }

}
