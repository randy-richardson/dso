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
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.resource.BackupEntityV2;

import java.util.Set;

/**
 * An interface for service implementations providing TSA backing up facilities.

 * @author Ludovic Orban
 */
public interface BackupServiceV2 {

  /**
   * Get a collection {@link com.terracotta.management.resource.BackupEntityV2} objects each representing a server
   * config. Only requested servers are included, or all of them if serverNames is null.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @return a collection {@link com.terracotta.management.resource.ConfigEntityV2} objects.
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<BackupEntityV2> getBackupStatus(Set<String> serverNames) throws ServiceExecutionException;

  /**
   * Perform backup on the specified servers using the specified backup file name. If the backup name is left to null
   * one is generated automatically.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @param backupName The name of the backup, or null to generate a name.
   * @return a collection {@link com.terracotta.management.resource.ConfigEntityV2} objects.
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<BackupEntityV2> backup(Set<String> serverNames, String backupName) throws ServiceExecutionException;

}
