/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.server.protoadapters;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class GlobalStorageManager {

  private static final String              RESPONSE_STATUS                  = "status";
  private static final String              RESPONSE_ERROR_DETAILS           = "error-details";
  private static final String              RESPONSE_RESULT                  = "result";

  private static final Map<Object, Object> UNKNOWN_OPERATION_RESPONSE;
  static {
    Map<Object, Object> tmp = new HashMap<Object, Object>();
    tmp.put("Cause", "Unknown Command");
    UNKNOWN_OPERATION_RESPONSE = createErrorResponseFor(tmp);
  }

  private final String                     GLOBAL_STORAGE_CACHEMANAGER_NAME = "__tc_globalStorageCacheManager";
  private final String                     GLOBAL_STORAGE_CACHE_NAME        = "__tc_globalStorageCache";

  private CacheManager                     cacheManager;
  private Ehcache                          cache;

  public void start() {
    Configuration configuration = new Configuration();
    configuration.name(GLOBAL_STORAGE_CACHEMANAGER_NAME);
    configuration.terracotta(new TerracottaClientConfiguration().url("localhost:9510"));

    cacheManager = new CacheManager(configuration);

    CacheConfiguration cc = new CacheConfiguration(GLOBAL_STORAGE_CACHE_NAME, 0);
    cc.terracotta(new TerracottaConfiguration());
    cacheManager.addCache(new Cache(cc));
    cache = cacheManager.getEhcache(GLOBAL_STORAGE_CACHE_NAME);
  }

  public Map<Object, Object> processMessage(Map<Object, Object> request) {
    Map<Object, Object> rv = new HashMap<Object, Object>();
    debug("Handling input: " + toString(request));
    if (request == null) return UNKNOWN_OPERATION_RESPONSE;

    Object opObj = request.get("operation");
    Operation operation = Operation.UNKNOWN;
    if (opObj instanceof String) {
      String op = (String) opObj;
      if ("get".equalsIgnoreCase(op)) {
        operation = Operation.GET;
      } else if ("put".equalsIgnoreCase(op)) {
        operation = Operation.PUT;
      } else if ("remove".equalsIgnoreCase(op)) {
        operation = Operation.REMOVE;
      }
    }

    Exception error = null;
    switch (operation) {
      case UNKNOWN:
        return UNKNOWN_OPERATION_RESPONSE;
      case GET:
        Object get = null;
        try {
          get = doGet(request);
        } catch (Exception e) {
          error = e;
        }
        if (error != null) { return createErrorResponseFor("GET command got exception - " + error.getMessage()); }
        return createSuccessResponseFor(get);
      case PUT:
        try {
          doPut(request);
        } catch (Exception e) {
          error = e;
        }
        if (error != null) { return createErrorResponseFor("PUT command got exception - " + error.getMessage()); }
        return createSuccessResponseFor("");
      default:
        break;
    }
    return rv;
  }

  private void doPut(Map<Object, Object> request) throws Exception {
    Object key = request.get("key");
    if (key == null) { throw new Exception("PUT command should have a key"); }
    Object value = request.get("value");
    if (value == null) { throw new Exception("Value cannot be null"); }
    cache.put(new Element(key, value));
  }

  private Object doGet(Map<Object, Object> request) throws Exception {
    Object key = request.get("key");
    if (key == null) { throw new Exception("GET command should have a key"); }
    Element element = cache.get(key);
    return element == null ? null : element.getObjectValue();
  }

  public static String toString(Map<Object, Object> msgMap) {
    if (msgMap == null) return "NULL";
    String rv = "";
    for (Entry<Object, Object> e : msgMap.entrySet()) {
      Object v = e.getValue();
      rv += "{" + e.getKey() + " : " + (v instanceof Map ? toString((Map<Object, Object>) v) : v) + "}";
    }
    return rv;
  }

  private void debug(String msg) {
    System.out.println(msg);
  }

  public static Map<Object, Object> createSuccessResponseFor(Object result) {
    Map<Object, Object> rv = new HashMap<Object, Object>();
    rv.put(RESPONSE_STATUS, ResponseStatus.SUCCESS.name());
    rv.put(RESPONSE_RESULT, result);
    return rv;
  }

  public static Map<Object, Object> createErrorResponseFor(Object errorDetails) {
    Map<Object, Object> rv = new HashMap<Object, Object>();
    rv.put(RESPONSE_STATUS, ResponseStatus.ERROR.name());
    rv.put(RESPONSE_ERROR_DETAILS, errorDetails);
    return rv;
  }

  public static enum Operation {
    UNKNOWN, GET, PUT, REMOVE
  }

  public static enum ResponseStatus {
    SUCCESS, ERROR
  }
}
