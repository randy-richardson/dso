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
package com.tc.object.applicator;

import com.tc.object.ClientObjectManager;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.platform.PlatformService;

import java.io.IOException;

/**
 * Applies a serialized change to an object.
 */
public interface ChangeApplicator {

  /**
   * Reconstitute the state of an object from DNA.
   * 
   * @param objectManager The client-side object manager
   * @param tcObject The manager for the object
   * @param dna The DNA, representing the state of the object
   * @param pojo A new instance of the object to reconstitute - this object will be modified with the values from the
   *        DNA
   */
  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object pojo) throws IOException,
      ClassNotFoundException;

  /**
   * Write an object's state to DNA
   * 
   * @param objectManager The client-side object manager
   * @param tcObject The manager for the object
   * @param writer The DNA writer for writing the DNA
   * @param pojo The object to write to writer
   */
  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo);

  /**
   * Traverse an object and find all object references within it.
   * 
   * @param pojo The object instance
   * @param addTo A collection of traversed references found
   * @return The addTo collection
   */
  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo);

  /**
   * Instantiate a new instance of the object from DNA. May not be supported on all applicators.
   * 
   * @param objectManager The client-side object manager
   * @param dna The DNA for the new object
   * @param platformService
   * @return The new instance
   */
  public Object getNewInstance(ClientObjectManager objectManager, DNA dna, PlatformService platformService)
      throws IOException, ClassNotFoundException;
}
