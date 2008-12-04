/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import junit.framework.TestCase;

public class TickerTokenTest extends TestCase {

  private TickerContextManager manager;

  @Override
  protected void setUp() throws Exception {
    manager = new TickerContextManager<TestTickerContext, TestTickerToken>();
  }

  public void testTickerTokens() {
    // manager.checkTicker(tickerContext, currentToken);
    final int listsize = 5;
    int tick = 0;

    TestTickerContext tickerContext = new TestTickerContext(listsize);

    TestTickerThread thread = new TestTickerThread(tickerContext);
    thread.start();

    List list = createTickerTokens(tick, listsize);

    int listIndex = 0;
    manager.startTicker(tickerContext, (TestTickerToken) list.get(listIndex));
    listIndex++;

    for (boolean continueTicker = true; continueTicker; listIndex++) {
      TestTickerToken token = (TestTickerToken) list.get(listIndex);
      tickerContext.collectToken(token);
      continueTicker = !manager.checkTicker(tickerContext, token);
    }

    try {
      thread.join();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  private List createTickerTokens(int tick, int listsize) {
    final WrappedArrayList list = new WrappedArrayList(listsize);
    TestTickerToken firstToken = new TestTickerToken(true, tick);
    list.add(firstToken);
    for (int i = 0; i < listsize - 1; i++) {
      TestTickerToken token = new TestTickerToken(tick);
      list.add(token);
    }
    return list;
  }

  private static final class TestTickerThread extends Thread {

    private TestTickerContext tickerContext;

    public TestTickerThread(TestTickerContext tickerContext) {
      this.tickerContext = tickerContext;
    }

    public void run() {

      try {
        tickerContext.waitUntil();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }

      System.out.println("No longer waiting...");

    }

  }

  private static final class WrappedArrayList implements List {

    private final ArrayList list;

    public WrappedArrayList(int size) {
      list = new ArrayList(size);
    }

    public boolean add(Object o) {
      return list.add(o);
    }

    public void add(int index, Object element) {
      list.add(index, element);
    }

    public boolean addAll(Collection c) {
      return list.addAll(c);
    }

    public boolean addAll(int index, Collection c) {
      return list.addAll(index, c);
    }

    public void clear() {
      list.clear();
    }

    public boolean contains(Object o) {
      return list.contains(o);
    }

    public boolean containsAll(Collection c) {
      return list.containsAll(c);
    }

    public Object get(int index) {
      return list.get(index % list.size());
    }

    public int indexOf(Object o) {
      return list.indexOf(o);
    }

    public boolean isEmpty() {
      return list.isEmpty();
    }

    public Iterator iterator() {
      return list.iterator();
    }

    public int lastIndexOf(Object o) {
      return list.lastIndexOf(o);
    }

    public ListIterator listIterator() {
      return list.listIterator();
    }

    public ListIterator listIterator(int index) {
      return list.listIterator(index);
    }

    public boolean remove(Object o) {
      return list.remove(o);
    }

    public Object remove(int index) {
      return list.remove(index);
    }

    public boolean removeAll(Collection c) {
      return list.removeAll(c);
    }

    public boolean retainAll(Collection c) {
      return list.retainAll(c);
    }

    public Object set(int index, Object element) {
      return list.set(index, element);
    }

    public int size() {
      return list.size();
    }

    public List subList(int fromIndex, int toIndex) {
      return list.subList(fromIndex, toIndex);
    }

    public Object[] toArray() {
      return list.toArray();
    }

    public Object[] toArray(Object[] a) {
      return list.toArray(a);
    }

  }
  /**
   * private static final class CircularArrayList extends AbstractList implements List, Serializable { private Object[]
   * elementData; private int head = 0; private int tail = 0; private int size = 0; public CircularArrayList() {
   * this(10); } public CircularArrayList(int size) { elementData = new Object[size]; } public
   * CircularArrayList(Collection c) { tail = c.size(); elementData = new Object[c.size()]; c.toArray(elementData); }
   * private int convert(int index) { return (index + head) % elementData.length; } public boolean isEmpty() { return
   * head == tail; // or size == 0 } public int size() { return size; } public boolean contains(Object elem) { return
   * indexOf(elem) >= 0; } public int indexOf(Object elem) { if (elem == null) { for (int i = 0; i < size; i++) if
   * (elementData[convert(i)] == null) return i; } else { for (int i = 0; i < size; i++) if
   * (elem.equals(elementData[convert(i)])) return i; } return -1; } public int lastIndexOf(Object elem) { if (elem ==
   * null) { for (int i = size - 1; i >= 0; i--) if (elementData[convert(i)] == null) return i; } else { for (int i =
   * size - 1; i >= 0; i--) if (elem.equals(elementData[convert(i)])) return i; } return -1; } public Object[] toArray()
   * { return toArray(new Object[size]); } public Object[] toArray(Object a[]) { if (a.length < size) a = (Object[])
   * java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size); if (head < tail) {
   * System.arraycopy(elementData, head, a, 0, tail - head); } else { System.arraycopy(elementData, head, a, 0,
   * elementData.length - head); System.arraycopy(elementData, 0, a, elementData.length - head, tail); } if (a.length >
   * size) a[size] = null; return a; } private void rangeCheck(int index) { if (index >= size || index < 0) throw new
   * IndexOutOfBoundsException("Index: " + index + ", Size: " + size); } public Object get(int index) { //
   * rangeCheck(index); return elementData[convert(index)]; } public Object set(int index, Object element) { modCount++;
   * rangeCheck(index); Object oldValue = elementData[convert(index)]; elementData[convert(index)] = element; return
   * oldValue; } public boolean add(Object o) { modCount++; ensureCapacity(size + 1 + 1); elementData[tail] = o; tail =
   * (tail + 1) % elementData.length; size++; return true; } public Object remove(int index) { modCount++;
   * rangeCheck(index); int pos = convert(index); try { return elementData[pos]; } finally { elementData[pos] = null; if
   * (pos == head) { head = (head + 1) % elementData.length; } else if (pos == tail) { tail = (tail - 1 +
   * elementData.length) % elementData.length; } else { if (pos > head && pos > tail) { System.arraycopy(elementData,
   * head, elementData, head + 1, pos - head); head = (head + 1) % elementData.length; } else {
   * System.arraycopy(elementData, pos + 1, elementData, pos, tail - pos - 1); tail = (tail - 1 + elementData.length) %
   * elementData.length; } } size--; } } public void clear() { modCount++; for (int i = head; i != tail; i = (i + 1) %
   * elementData.length) elementData[i] = null; head = tail = size = 0; } public boolean addAll(Collection c) {
   * modCount++; int numNew = c.size(); ensureCapacity(size + numNew + 1); Iterator e = c.iterator(); for (int i = 0; i
   * < numNew; i++) { elementData[tail] = e.next(); tail = (tail + 1) % elementData.length; size++; } return numNew !=
   * 0; } public void add(int index, Object element) { throw new
   * UnsupportedOperationException("This method left as an exercise to the reader ;-)"); } public boolean addAll(int
   * index, Collection c) { throw new
   * UnsupportedOperationException("This method left as an exercise to the reader ;-)"); } private void
   * ensureCapacity(int minCapacity) { int oldCapacity = elementData.length; if (minCapacity > oldCapacity) { int
   * newCapacity = (oldCapacity * 3) / 2 + 1; if (newCapacity < minCapacity) newCapacity = minCapacity; Object newData[]
   * = new Object[newCapacity]; toArray(newData); tail = size; head = 0; elementData = newData; } } }
   **/
}
