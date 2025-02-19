/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.apache.commons.lang3.ClassUtils;

/**
 * A {@link Stringifier}that generally uses {@link Object#toString()}on objects, but uses special cases for certain
 * objects (like arrays) for which {@link Object#toString()}doesn't do a very good job.
 */
public class StandardStringifier implements Stringifier {
  
  public static final StandardStringifier INSTANCE = new StandardStringifier();
  
  private StandardStringifier() {
    // Use INSTANCE instead.
  }

  @Override
  public String toString(Object o) {
    if (o == null) return "<null>";
    else if (o instanceof Object[]) {
      return toString((Object[]) o);
    } else if (o instanceof byte[]) {
      return toString((byte[]) o);
    } else if (o instanceof short[]) {
      return toString((short[]) o);
    } else if (o instanceof int[]) {
      return toString((int[]) o);
    } else if (o instanceof long[]) {
      return toString((long[]) o);
    } else if (o instanceof boolean[]) {
      return toString((boolean[]) o);
    } else if (o instanceof char[]) {
      return toString((char[]) o);
    } else if (o instanceof float[]) {
      return toString((float[]) o);
    } else if (o instanceof double[]) {
      return toString((double[]) o);
    } else {
      return o.toString();
    }
  }
  
  private static final int MAX_ITEMS_TO_SHOW = 20;
  
  private String toString(Object[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append(toString(arr[i]));
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

  private String toString(byte[] arr) {
    return HexDump.dump(arr);
  }

  private String toString(short[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append("" + arr[i]);
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

  private String toString(int[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append("" + arr[i]);
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

  private String toString(long[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append("" + arr[i]);
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

  private String toString(char[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append("" + arr[i]);
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

  private String toString(float[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append("" + arr[i]);
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

  private String toString(double[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append("" + arr[i]);
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

  private String toString(boolean[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append("" + arr[i]);
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

}
