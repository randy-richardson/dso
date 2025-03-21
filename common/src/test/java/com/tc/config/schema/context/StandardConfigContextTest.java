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
package com.tc.config.schema.context;

import com.tc.config.schema.MockIllegalConfigurationChangeHandler;
import com.tc.config.schema.MockSchemaType;
import com.tc.config.schema.MockXmlObject;
import com.tc.config.schema.defaults.MockDefaultValueProvider;
import com.tc.config.schema.repository.MockBeanRepository;
import com.tc.test.TCTestCase;

/**
 * Unit test for {@link StandardConfigContext}.
 */
public class StandardConfigContextTest extends TCTestCase {

  private MockSchemaType                        schemaType;
  private MockBeanRepository                    beanRepository;
  private MockDefaultValueProvider              defaultValueProvider;
  private MockIllegalConfigurationChangeHandler illegalConfigurationChangeHandler;

  private ConfigContext                         context;

  @Override
  public void setUp() throws Exception {
    this.schemaType = new MockSchemaType();
    this.beanRepository = new MockBeanRepository();
    this.beanRepository.setReturnedRootBeanSchemaType(this.schemaType);
    this.defaultValueProvider = new MockDefaultValueProvider();
    this.illegalConfigurationChangeHandler = new MockIllegalConfigurationChangeHandler();

    this.context = new StandardConfigContext(this.beanRepository, this.defaultValueProvider,
                                             this.illegalConfigurationChangeHandler);
  }

  public void testEnsureRepositoryProvides() throws Exception {
    this.beanRepository.setExceptionOnEnsureBeanIsOfClass(null);

    this.context.ensureRepositoryProvides(Number.class);
    assertEquals(1, this.beanRepository.getNumEnsureBeanIsOfClasses());
    assertEquals(Number.class, this.beanRepository.getLastClass());
    this.beanRepository.reset();

    RuntimeException exception = new RuntimeException("foo");
    this.beanRepository.setExceptionOnEnsureBeanIsOfClass(exception);

    try {
      this.context.ensureRepositoryProvides(Object.class);
      fail("Didn't get expected exception");
    } catch (RuntimeException re) {
      assertSame(exception, re);
      assertEquals(1, this.beanRepository.getNumEnsureBeanIsOfClasses());
      assertEquals(Object.class, this.beanRepository.getLastClass());
    }
  }

  public void testConstruction() throws Exception {
    try {
      new StandardConfigContext(null, this.defaultValueProvider, this.illegalConfigurationChangeHandler);
      fail("Didn't get NPE on no bean repository");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new StandardConfigContext(this.beanRepository, null, this.illegalConfigurationChangeHandler);
      fail("Didn't get NPE on no default value provider");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new StandardConfigContext(this.beanRepository, this.defaultValueProvider, null);
      fail("Didn't get NPE on no illegal configuration change handler");
    } catch (NullPointerException npe) {
      // ok
    }
  }

  public void testHasDefaultFor() throws Exception {
    this.defaultValueProvider.setReturnedPossibleForXPathToHaveDefault(false);
    this.defaultValueProvider.setReturnedHasDefault(false);

    assertFalse(this.context.hasDefaultFor("foobar/baz"));
    assertEquals(1, this.defaultValueProvider.getNumPossibleForXPathToHaveDefaults());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastPossibleForXPathToHaveDefaultsXPath());
    assertEquals(0, this.defaultValueProvider.getNumHasDefaults());

    this.defaultValueProvider.reset();
    this.defaultValueProvider.setReturnedPossibleForXPathToHaveDefault(true);
    this.defaultValueProvider.setReturnedHasDefault(false);

    assertFalse(this.context.hasDefaultFor("foobar/baz"));
    assertEquals(1, this.defaultValueProvider.getNumPossibleForXPathToHaveDefaults());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastPossibleForXPathToHaveDefaultsXPath());
    assertEquals(1, this.defaultValueProvider.getNumHasDefaults());
    assertSame(this.schemaType, this.defaultValueProvider.getLastHasDefaultsSchemaType());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastHasDefaultsXPath());

    this.defaultValueProvider.reset();
    this.defaultValueProvider.setReturnedPossibleForXPathToHaveDefault(false);
    this.defaultValueProvider.setReturnedHasDefault(true);

    assertFalse(this.context.hasDefaultFor("foobar/baz"));
    assertEquals(1, this.defaultValueProvider.getNumPossibleForXPathToHaveDefaults());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastPossibleForXPathToHaveDefaultsXPath());
    assertEquals(0, this.defaultValueProvider.getNumHasDefaults());

    this.defaultValueProvider.reset();
    this.defaultValueProvider.setReturnedPossibleForXPathToHaveDefault(true);
    this.defaultValueProvider.setReturnedHasDefault(true);

    assertTrue(this.context.hasDefaultFor("foobar/baz"));
    assertEquals(1, this.defaultValueProvider.getNumPossibleForXPathToHaveDefaults());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastPossibleForXPathToHaveDefaultsXPath());
    assertEquals(1, this.defaultValueProvider.getNumHasDefaults());
    assertSame(this.schemaType, this.defaultValueProvider.getLastHasDefaultsSchemaType());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastHasDefaultsXPath());
  }

  public void testDefaultFor() throws Exception {
    MockXmlObject object = new MockXmlObject();
    this.defaultValueProvider.setReturnedDefaultFor(object);

    assertSame(object, this.context.defaultFor("foobar/baz"));
    assertEquals(1, this.defaultValueProvider.getNumDefaultFors());
    assertSame(this.schemaType, this.defaultValueProvider.getLastBaseType());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastXPath());
  }

  public void testIsOptional() throws Exception {
    this.defaultValueProvider.setReturnedIsOptional(false);

    assertFalse(this.context.isOptional("foobar/baz"));
    assertEquals(1, this.defaultValueProvider.getNumIsOptionals());
    assertSame(this.schemaType, this.defaultValueProvider.getLastBaseType());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastXPath());
  }

  public void testBean() throws Exception {
    MockXmlObject object = new MockXmlObject();
    this.beanRepository.setReturnedBean(object);

    assertSame(object, this.context.bean());
    assertEquals(1, this.beanRepository.getNumBeans());
  }

}
