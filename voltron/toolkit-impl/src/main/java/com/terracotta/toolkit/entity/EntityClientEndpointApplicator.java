package com.terracotta.toolkit.entity;

import com.tc.logging.TCLogger;
import com.tc.object.ClientObjectManager;
import com.tc.object.LogicalOperation;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.TraversedReferencesImpl;
import com.tc.object.applicator.BaseApplicator;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.platform.PlatformService;

import java.io.IOException;

/**
 * @author twu
 */
public class EntityClientEndpointApplicator extends BaseApplicator {

  public EntityClientEndpointApplicator(final DNAEncoding encoding, final TCLogger logger) {
    super(encoding, logger);
  }

  @Override
  public void hydrate(final ClientObjectManager objectManager, final TCObject tcObject, final DNA dna, final Object pojo) throws IOException, ClassNotFoundException {
    EntityClientEndpointImpl endpoint = (EntityClientEndpointImpl) tcObject;
    final DNACursor cursor = dna.getCursor();
    if (cursor.next()) {
      final LogicalAction typeNameAction = cursor.getLogicalAction();
      if (typeNameAction.getLogicalOperation() == LogicalOperation.CREATE_ENTITY) {
        endpoint.setTypeName((String) typeNameAction.getParameters()[0]);
      }
    }
  }

  @Override
  public void dehydrate(final ClientObjectManager objectManager, final TCObject tcObject, final DNAWriter writer, final Object pojo) {
    EntityClientEndpointImpl endpoint = (EntityClientEndpointImpl) tcObject;
    writer.addLogicalAction(LogicalOperation.CREATE_ENTITY, new Object[] { endpoint.getTypeName() });
  }

  @Override
  public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
    return new TraversedReferencesImpl();
  }

  @Override
  public Object getNewInstance(final ClientObjectManager objectManager, final DNA dna, final PlatformService platformService) throws IOException, ClassNotFoundException {
    return new EntityClientEndpointImpl();
  }
}
