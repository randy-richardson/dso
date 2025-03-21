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
package com.tc.config.test.schema;

public class TcPropertyBuilder extends BaseConfigBuilder{

  private static final String[] ALL_PROPERTIES = concat(new Object[]{"name", "value"});
  private String name;
  private String value;
  
  public TcPropertyBuilder(String name, String value){
    super(3, ALL_PROPERTIES);
    this.name = name;
    this.value = value;
  }
  
  public void setTcProperty(String name, String value){
    this.name = name;
    this.value = value;
  }
  
  @Override
  public String toString() {
    String out = "";
    
    out += indent() + "<property " + (this.name != null ? "name=\"" + this.name + "\"" : "") + (this.value != null ? " value=\"" + this.value + "\"" : "") + "/>";
    
    return out;
  }
}
