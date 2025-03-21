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
package com.tc.config.schema.repository;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.validate.ConfigurationValidator;

/**
 * A {@link BeanRepository} that lets clients change the bean in it.
 */
public interface MutableBeanRepository extends BeanRepository {

  void setBean(XmlObject bean, String sourceDescription) throws XmlException;

  void addValidator(ConfigurationValidator validator);

  /**
   * For <strong>TESTS ONLY</strong>.
   */
  void saveCopyOfBeanInAnticipationOfFutureMutation();

  /**
   * For <strong>TESTS ONLY</strong>.
   */
  void didMutateBean();

}
