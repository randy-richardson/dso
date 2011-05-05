/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import com.tc.config.schema.setup.ConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.exception.MortbayMultiExceptionHelper;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.server.protoadapters.ProtocolAdapterManager;

public class TCServerMain {

  public static void main(final String[] args) {
    ThrowableHandler throwableHandler = new ThrowableHandler(TCLogging.getLogger(TCServerMain.class));
    throwableHandler.addHelper(new MortbayMultiExceptionHelper());

    try {
      TCThreadGroup threadGroup = new TCThreadGroup(throwableHandler);

      ConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(
                                                                                              args,
                                                                                              StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                                              new FatalIllegalConfigurationChangeHandler());
      AbstractServerFactory serverFactory = AbstractServerFactory.getFactory();
      TCServer server = serverFactory.createServer(factory.createL2TVSConfigurationSetupManager(null), threadGroup);
      ProtocolAdapterManager protocolAdapterManager = new ProtocolAdapterManager();
      server.setProtocolAdapterManager(protocolAdapterManager);
      server.start();
      startProtocolAdapterManager(protocolAdapterManager, server);

      server.waitUntilShutdown();

    } catch (Throwable t) {
      throwableHandler.handleThrowable(Thread.currentThread(), t);
    }
  }

  private static void startProtocolAdapterManager(ProtocolAdapterManager protocolAdapterManager, TCServer server) {
    int elapsed = 0;
    while (!server.isStarted()) {
      System.out.println("Waiting for server to start... (sleeping for 1 sec) "
                         + (elapsed == 0 ? "" : "(elapsed " + elapsed + " seconds)"));
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // ignored
      }
      elapsed++;
    }
    System.out.println("Terracotta server has started");
    System.out.println("Starting protocol adapter services..");
    protocolAdapterManager.start();
  }
}
