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
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.service.ShutdownServiceV2;

import java.util.Set;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * A resource service for performing TSA shutdown.
 * 
 * @author Ludovic Orban
 */
@Path("/v2/agents/shutdown")
public class ShutdownResourceServiceImplV2 {

  private static final Logger LOG = LoggerFactory.getLogger(ShutdownResourceServiceImplV2.class);

  private final ShutdownServiceV2 shutdownService;
  private final RequestValidator requestValidator;

  public ShutdownResourceServiceImplV2() {
    this.shutdownService = ServiceLocator.locate(ShutdownServiceV2.class);
    this.requestValidator = ServiceLocator.locate(RequestValidator.class);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public boolean shutdown(@Context UriInfo info) {
    LOG.debug(String.format("Invoking ShutdownResourceServiceImplV2.shutdown: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> serverNames = UriInfoUtils.extractLastSegmentMatrixParameterAsSet(info, "names");

      shutdownService.shutdown(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to shutdown TSA", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
    
    return true;
  }

}
