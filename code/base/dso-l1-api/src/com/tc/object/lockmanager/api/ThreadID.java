/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.util.AbstractIdentifier;

/**
 * Thread identifier
 */
public class ThreadID extends AbstractIdentifier {

  /** Null identifier */
  public static final ThreadID NULL_ID = new ThreadID();
  /** VM identifier */
  public static final ThreadID VM_ID   = new ThreadID(Long.MIN_VALUE);
  private String               name;

  /**
   * New thread id
   * 
   * @param id Identifier
   */
  public ThreadID(long id) {
    this(id, null);
  }

  public ThreadID(long id, String name) {
    super(id);
    this.name = name;
  }

  private ThreadID() {
    super();
  }

  public String getIdentifierType() {
    if (name != null) {
      return "Thread(" + name + ")ID";
    } else {
      return "ThreadID";
    }
  }

}
