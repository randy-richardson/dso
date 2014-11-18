package com.tc.object.applicator;

import com.tc.object.ClientObjectManager;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.platform.PlatformService;

import java.io.IOException;

/**
 * @author twu
 */
public class PassthroughApplicator implements ChangeApplicator {
  @Override
  public void hydrate(final ClientObjectManager objectManager, final TCObject tcObject, final DNA dna, final Object pojo) throws IOException, ClassNotFoundException {
    getApplicatorFor(tcObject).hydrate(objectManager, tcObject, dna, pojo);
  }

  @Override
  public void dehydrate(final ClientObjectManager objectManager, final TCObject tcObject, final DNAWriter writer, final Object pojo) {
    getApplicatorFor(tcObject).dehydrate(objectManager, tcObject, writer, pojo);
  }

  @Override
  public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
    return getApplicatorForPeerObject(pojo).getPortableObjects(pojo, addTo);
  }

  @Override
  public Object getNewInstance(final ClientObjectManager objectManager, final DNA dna, final PlatformService platformService) throws IOException, ClassNotFoundException {
    throw new UnsupportedOperationException("Hmm what to do here...");
  }

  private static ChangeApplicator getApplicatorFor(TCObject tcObject) {
    return getApplicatorForPeerObject(tcObject.getPeerObject());
  }

  private static ChangeApplicator getApplicatorForPeerObject(Object object) {
    if (object instanceof SelfApplicable) {
      return ((SelfApplicable) object).getApplicator();
    } else {
      throw new IllegalArgumentException("Object of class " + object.getClass().getName()
           + " is not SelfApplicable.");
    }
  }
}
