/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.bundles.LegacyDefaultModuleBase;
import com.tc.util.runtime.Vm;

public class Jdk15PreInstrumentedConfiguration extends LegacyDefaultModuleBase {

  public Jdk15PreInstrumentedConfiguration(StandardDSOClientConfigHelper configHelper) {
    super(configHelper);
  }

  @Override
  public void apply() {
    addJDK15PreInstrumentedSpec();
  }

  private void addJDK15PreInstrumentedSpec() {
    if (Vm.getMegaVersion() >= 1 && Vm.getMajorVersion() > 4) {
      getOrCreateSpec("java.util.concurrent.CyclicBarrier");
      TransparencyClassSpec spec = getOrCreateSpec("java.util.concurrent.CyclicBarrier$Generation");
      spec.setHonorJDKSubVersionSpecific(true);
      getOrCreateSpec("java.util.concurrent.TimeUnit");

      spec = getOrCreateSpec("java.util.concurrent.atomic.AtomicBoolean");
      spec.setHonorVolatile(true);

      if (!Vm.isIBM()) {
        spec = getOrCreateSpec("java.util.concurrent.atomic.AtomicInteger");
        spec.setHonorVolatile(true);

        if (!Vm.isAzul()) {
          spec = getOrCreateSpec("java.util.concurrent.atomic.AtomicLong");
          spec.setHonorVolatile(true);
        }
      }

      spec = getOrCreateSpec("java.util.concurrent.atomic.AtomicReference");
      spec.setHonorVolatile(true);

      // ---------------------------------------------------------------------
      // The following section of specs are specified in the BootJarTool
      // also.
      // They are placed again so that the honorTransient flag will
      // be honored during runtime.
      // ---------------------------------------------------------------------

      // ---------------------------------------------------------------------
      // SECTION BEGINS
      // ---------------------------------------------------------------------

      spec = getOrCreateSpec("java.util.concurrent.locks.ReentrantLock");
      spec.setHonorTransient(true);
      spec.setCallConstructorOnLoad(true);

      // addJavaUtilConcurrentHashMapSpec();
      // addLogicalAdaptedLinkedBlockingQueueSpec();

      // ---------------------------------------------------------------------
      // SECTION ENDS
      // ---------------------------------------------------------------------
    }
  }

}
