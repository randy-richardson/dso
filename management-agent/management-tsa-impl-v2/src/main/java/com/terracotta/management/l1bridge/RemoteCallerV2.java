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
package com.terracotta.management.l1bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;
import org.terracotta.management.resource.AbstractEntityV2;
import org.terracotta.management.resource.ExceptionEntityV2;
import org.terracotta.management.resource.Representable;
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.exceptions.ExceptionUtils;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.web.utils.TSAConfig;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Ludovic Orban
 */
public class RemoteCallerV2 extends RemoteCaller {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteCallerV2.class);

  public RemoteCallerV2(RemoteAgentBridgeService remoteAgentBridgeService, ContextService contextService, ExecutorService executorService, RequestTicketMonitor ticketMonitor, UserService userService, TimeoutService timeoutService) {
    super(remoteAgentBridgeService, contextService, executorService, ticketMonitor, userService, timeoutService);
  }

  public <T extends AbstractEntityV2> ResponseEntityV2<T> fanOutResponseCall(final String serviceAgency, Set<String> nodes, final String serviceName, final Method method, final Object[] args) throws ServiceExecutionException {
    final Object userInfo = contextService.getUserInfo();
    Map<String, Future<ResponseEntityV2<T>>> futures = new HashMap<String, Future<ResponseEntityV2<T>>>();

    long beforeSubmission = System.nanoTime();

    for (final String node : nodes) {
      if (node.equals(Representable.EMBEDDED_AGENT_ID)) { continue; }
      try {
        Future<ResponseEntityV2<T>> future = executorService.submit(new Callable<ResponseEntityV2<T>>() {
          @Override
          public ResponseEntityV2<T> call() throws Exception {
            String ticket = requestTicketMonitor.issueRequestTicket();
            String token = userService.putUserInfo(userInfo);

            if (serviceAgency != null) {
              String nodeAgency = remoteAgentBridgeService.getRemoteAgentAgency(node);
              if (!serviceAgency.equals(nodeAgency)) {
                return new ResponseEntityV2<T>();
              }
            }

            RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(ticket, token, TSAConfig.getSecurityCallbackUrl(),
                serviceName, method.getName(), method.getParameterTypes(), args);
            byte[] bytes = remoteAgentBridgeService.invokeRemoteMethod(node, remoteCallDescriptor);
            return deserializeAndRewriteAgentId(bytes, node);
          }
        });
        futures.put(node, future);
      } catch (RejectedExecutionException ree) {
        ResponseEntityV2<T> rejectionResponse = new ResponseEntityV2<T>();
        Throwable rootCause = ExceptionUtils.getRootCause(ree);
        ExceptionEntityV2 ee = new ExceptionEntityV2(rootCause);
        ee.setAgentId(node);
        rejectionResponse.getExceptionEntities().add(ee);
        futures.put(node, new RejectionFuture<ResponseEntityV2<T>>(rejectionResponse));
      }
    }

    long submissionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - beforeSubmission);
    LOG.debug("fan-out call submission time : {}ms", submissionTime);
    if (submissionTime > timeoutService.getCallTimeout() / 2) {
      LOG.warn("Slow L1 management fan-out call submission detected ({}ms), is the JMX thread pool saturated? Try " +
              "increasing the '" + TCPropertiesConsts.L2_REMOTEJMX_MAXTHREADS + "' TC server property (current value is {})",
          submissionTime, TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_REMOTEJMX_MAXTHREADS));
    }
    long timeLeft = Math.max(timeoutService.getCallTimeout() - submissionTime, 0L);
    ResponseEntityV2<T> globalResult = new ResponseEntityV2<T>();

    for (Map.Entry<String, Future<ResponseEntityV2<T>>> entry : futures.entrySet()) {
      String node = entry.getKey();
      Future<ResponseEntityV2<T>> future = entry.getValue();

      long beforeCollection = System.nanoTime();
      long timeout = Math.max(1L, timeLeft);
      try {
        ResponseEntityV2<T> resp = future.get(timeout, TimeUnit.MILLISECONDS);
        globalResult.getEntities().addAll(resp.getEntities());
        globalResult.getExceptionEntities().addAll(resp.getExceptionEntities());
      } catch (Exception e) {
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        ExceptionEntityV2 e1 = new ExceptionEntityV2(rootCause);
        e1.setAgentId(node);
        globalResult.getExceptionEntities().add(e1);

        future.cancel(true);
        
        if (e instanceof TimeoutException) {
          LOG.debug("Future execution error in   {}.{} : agent '{}' failed to respond to call in {}ms", serviceName, method.getName(), node, timeout, e);
        }
      } finally {
        timeLeft -= TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - beforeCollection);
      }
    }
    return globalResult;
  }

  @Override
  protected void rewriteAgentId(Object obj, String agentId) {
    if (obj instanceof ResponseEntityV2) {
      ResponseEntityV2<AbstractEntityV2> entityV2 = (ResponseEntityV2)obj;
      for (AbstractEntityV2 abstractEntityV2 : entityV2.getEntities()) {
        abstractEntityV2.setAgentId(agentId);
      }
      for (ExceptionEntityV2 exceptionEntityV2 : entityV2.getExceptionEntities()) {
        exceptionEntityV2.setAgentId(agentId);
      }
    } else {
      super.rewriteAgentId(obj, agentId);
    }
  }
}
