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

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class TCRandomFileAccessImpl implements TCRandomFileAccess {
  private RandomAccessFile randomAccessFile;
  
  public TCRandomFileAccessImpl() {
    randomAccessFile = null;
  }

  @Override
  public TCFileChannel getChannel(TCFile tcFile, String mode) throws FileNotFoundException {
    randomAccessFile = new RandomAccessFile(tcFile.getFile(), mode);
    return new TCFileChannelImpl(randomAccessFile.getChannel());
  }
}
