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
package com.terracotta.management.service.impl.util;

import org.terracotta.management.resource.ErrorEntity;

import com.terracotta.management.web.proxy.ProxyException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * @author Ludovic Orban
 */
public class L1MBeansSourceUtils {

  public static void proxyClientRequest(String activeL2WithMBeansUrls) throws ProxyException, WebApplicationException {
    if (activeL2WithMBeansUrls == null) {
      ErrorEntity errorEntity = new ErrorEntity();
      errorEntity.setError("No management coordinator");
      errorEntity.setDetails("No server is in the ACTIVE-COORDINATOR state in the coordinator group, try again later.");
      throw new WebApplicationException(Response.status(400).entity(errorEntity).build());
    }
    throw new ProxyException(activeL2WithMBeansUrls);
  }

}
