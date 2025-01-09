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
package com.tc.async.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.text.PrettyPrinter;

/**
 * @author orion
 */
public class MockStage implements Stage {

  private final String  name;
  public final MockSink sink;

  public MockStage(String name) {
    this.name = name;
    this.sink = new MockSink();
  }

  @Override
  public void destroy() {
    //
  }

  @Override
  public synchronized Sink getSink() {
    return sink;
  }

  @Override
  public void start(ConfigurationContext context) {
    //
  }

  public void turnTracingOn() {
    //
  }

  public void turnTracingOff() {
    //
  }

  @Override
  public String toString() {
    return "MockStage(" + name + ")";
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return null;
  }

}