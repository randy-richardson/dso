package com.bigmemory.samples.readwritethrough;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.writebehind.operations.SingleOperationType;

import java.util.Collection;

public class MyCacheWriter implements CacheWriter {
  public CacheWriter clone(final Ehcache ehcache) throws CloneNotSupportedException {
    return null;
  }

  // This is where you would initialize the connection to your SOR
  public void init() {

  }

  // This is where you would dispose of the connection to your SOR
  public void dispose() throws CacheException {

  }

  public void write(final Element element) throws CacheException {
    System.out.println("*** CacheWriter : We wrote to the SOR, key = " + element.getObjectKey());
  }

  public void writeAll(final Collection<Element> collection) throws CacheException {

  }

  public void delete(final CacheEntry cacheEntry) throws CacheException {
    System.out.println("*** CacheWriter : We removed from the SOR, key = " + cacheEntry.getKey());
  }

  public void deleteAll(final Collection<CacheEntry> collection) throws CacheException {

  }

  public void throwAway(final Element element, final SingleOperationType singleOperationType, final RuntimeException e) {

  }
}
