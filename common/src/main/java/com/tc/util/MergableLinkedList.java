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
package com.tc.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This singly linked list was written for performance reason. In addition to being a regular linked list, it has the
 * ability to merge other linked lists into this list without traversing through the entire list. <br>
 * <p>
 * Only the basic necessary methods are implemented as of now. But I can see this list implement the List interface
 * someday.
 */
public class MergableLinkedList {
  private int                     size = 0;
  private MergableLinkedList.Node head;
  private MergableLinkedList.Node tail;

  /**
   * Adds the collection to the end of the list(ServerTransaction) txnQ.removeFirst()
   */
  public void addAll(Collection c) {
    for (Iterator i = c.iterator(); i.hasNext();) {
      add(i.next());
    }
  }

  /**
   * This is the reason this class exists. This is an optimized way to add all the elements in List m to the front of
   * this List. At the end of this call the List m is cleared.
   */
  public void mergeToFront(MergableLinkedList m) {
    if (m.isEmpty()) {
      // Nothing to merge
      return;
    }
    m.tail.next = head;
    head = m.head;
    if (tail == null) {
      tail = m.tail;
    }
    size += m.size();
    m.clear();
  }

  public void clear() {
    head = tail = null;
    size = 0;
  }

  public int size() {
    return size;
  }

  public Object removeFirst() {
    if (head == null) { throw new NoSuchElementException(); }
    Object toReturn = head.data;
    if (head == tail) {
      // Only one element in the list
      head = tail = null;
    } else {
      head = head.next;
    }
    size--;
    return toReturn;
  }

  public boolean isEmpty() {
    return (size == 0);
  }

  /**
   * Adds to the end of the list
   */
  public void add(Object o) {
    MergableLinkedList.Node n = new Node(o);
    if (head == null) {
      // first node
      head = n;
    } else {
      tail.next = n;
    }
    tail = n;
    size++;
  }

  private static final class Node {

    private MergableLinkedList.Node next;
    private Object                  data;

    public Node(Object data) {
      this.data = data;
    }
  }
}
