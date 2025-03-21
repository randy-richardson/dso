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
package com.tc.object;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public interface TCObjectSelf extends TCObject {

  public void initializeTCObject(final ObjectID id, final TCClass clazz, final boolean isNew);

  public void serialize(ObjectOutput out) throws IOException;

  public void deserialize(ObjectInput in) throws IOException;

  public void initClazzIfRequired(TCClass tcc);

  public boolean isInitialized();

}
