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
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

/**
 * A class that knows how to parse TCMessages out of raw bytes
 */
class TCMessageParser {
   private static final TCLogger logger = TCLogging.getLogger(TCMessageParser.class);
  private final TCMessageFactory factory;

  TCMessageParser(TCMessageFactory factory) {
    this.factory = factory;
  }

  TCMessage parseMessage(MessageChannel source, TCByteBuffer[] data) {
    TCMessageHeader hdr = new TCMessageHeaderImpl(data[0].duplicate().limit(TCMessageHeader.HEADER_LENGTH));
    final int headerLength = hdr.getHeaderByteLength();

    if (headerLength != TCMessageHeader.HEADER_LENGTH) {
      logger.error("Invalid header length ! length = " + headerLength);
      logger.error("error header = " + hdr);
      logger.error(" buffer data is " + toString(data));
      throw new RuntimeException("Invalid header length: " + headerLength);
    }

    final TCByteBuffer msgData[];

    if (data[0].limit() > headerLength) {
      msgData = new TCByteBuffer[data.length];
      System.arraycopy(data, 0, msgData, 0, msgData.length);
      msgData[0] = msgData[0].position(headerLength).slice();
    } else {
      Assert.eval(data.length > 1);
      msgData = new TCByteBuffer[data.length - 1];
      System.arraycopy(data, 1, msgData, 0, msgData.length);
    }

    final int msgType = hdr.getMessageType();
    final TCMessageType type = TCMessageType.getInstance(hdr.getMessageType());

    if (type == null) {
      throw new RuntimeException("Can't find message type for type: " + msgType);
    }

    return factory.createMessage(source, type, hdr, msgData);
  }

  private String toString(TCByteBuffer[] data) {
    if(data == null || data.length == 0) { return "null or size 0"; }
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < data.length; i++) {
      sb.append(data[i]);
      sb.append(" { ");
      byte b[] = data[i].array();
      for (int j = 0; j < b.length; j++) {
        sb.append(b[j]).append(" ");
      }
      sb.append(" } ");
    }
    return sb.toString();
  }
}
