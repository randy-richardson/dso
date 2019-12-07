/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.nonstop;

import com.terracotta.toolkit.util.ToolkitInstanceProxy;

import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.terracotta.toolkit.util.ToolkitInstanceProxy.newToolkitProxy;

public class ExceptionOnTimeoutBehaviorResolver {
  private final ExceptionOnTimeoutInvocationHandler handler                     = new ExceptionOnTimeoutInvocationHandler();
  private final ConcurrentMap<Class, Object>        exceptionOnTimeoutBehaviors = new ConcurrentHashMap<Class, Object>();

  public <E> E resolve(Class<E> klazz) {
    Object rv = exceptionOnTimeoutBehaviors.get(klazz);
    if (rv == null) {
      Object newProxyInstance = newToolkitProxy(klazz, handler);
      Object oldProxyInstance = exceptionOnTimeoutBehaviors.putIfAbsent(klazz, newProxyInstance);
      rv = oldProxyInstance != null ? oldProxyInstance : newProxyInstance;
    }
    return (E) rv;
  }
}
