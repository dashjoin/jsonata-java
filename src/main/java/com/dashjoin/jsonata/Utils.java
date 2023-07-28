/**
 * jsonata-java is the JSONata Java reference port
 * 
 * Copyright Dashjoin GmbH. https://dashjoin.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dashjoin.jsonata;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.dashjoin.jsonata.Jsonata.JFunction;
import com.dashjoin.jsonata.Jsonata.JFunctionCallable;

@SuppressWarnings({"rawtypes"})
public class Utils {
    public static boolean isNumeric(Object v) {
        if (v instanceof Long) return true;
        boolean isNum = false;
        if (v instanceof Number) {
            double d = ((Number)v).doubleValue();
            isNum = !Double.isNaN(d);
            if (isNum && !Double.isFinite(d)) {
                throw new JException("D1001", 0, v);
            }
        }
        return isNum;
    }

    public static boolean isArrayOfStrings(Object v) {
        boolean result = false;
        if (v instanceof Collection) {
            for (Object o : ((Collection)v))
                if (!(o instanceof String))
                    return false;
            return true;
        }
        return false;
    }
    public static boolean isArrayOfNumbers(Object v) {
        boolean result = false;
        if (v instanceof Collection) {
            for (Object o : ((Collection)v))
                if (!isNumeric(o))
                    return false;
            return true;
        }
        return false;
    }

    public static boolean isFunction(Object o) {
        return o instanceof JFunction || o instanceof JFunctionCallable;
    }

    static Object NONE = new Object();

    /**
     * Create an empty sequence to contain query results
     * @returns {Array} - empty sequence
     */
    public static List<Object> createSequence() { return createSequence(NONE); }

    public static List<Object> createSequence(Object el) {
        JList<Object> sequence = new JList<>();
        sequence.sequence = true;
        if (el!=NONE) {
            if (el instanceof List && ((List)el).size()==1)
                sequence.add(((List)el).get(0));
            else
            // This case does NOT exist in Javascript! Why?
                sequence.add(el);
        }
        return sequence;
    }

    public static class JList<E> extends ArrayList<E> {
        public JList() { super(); }
        public JList(int capacity) { super(capacity); }
        public JList(Collection<? extends E> c) {
            super(c);
        }

        // Jsonata specific flags
        public boolean sequence;

        public boolean outerWrapper;

        public boolean tupleStream;

        public boolean keepSingleton;

        public boolean cons;
    }

    public static boolean isSequence(Object result) {
        return result instanceof JList && ((JList)result).sequence;
    }

    /**
     * List representing an int range [a,b]
     * Both sides are included. Read-only + immutable.
     * 
     * Used for late materialization of ranges.
     */
    public static class RangeList extends AbstractList<Number> {

        final long a, b;
        final int size;

        public RangeList(long left, long right) {
            assert(left<=right);
            assert(right-left < Integer.MAX_VALUE);
            a = left; b = right;
            size = (int) (b-a+1);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean addAll(Collection<? extends Number> c) {
            throw new UnsupportedOperationException("RangeList does not support 'addAll'");
        }

        @Override
        public Number get(int index) {
            if (index < size) {
                try {
                    return Utils.convertNumber( a + index );
                } catch (JException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            throw new IndexOutOfBoundsException(index);
        }        
    }
     
    public static Number convertNumber(Number n) {
        // Use long if the number is not fractional
        if (!isNumeric(n)) return null;
        if (n.longValue()==n.doubleValue()) {
            long l = n.longValue();
            if (((int)l)==l)
                return (int)l;
            else
                return l;
        }
        return n.doubleValue();
    }

    public static void checkUrl(String str) {
      boolean isHigh = false;
      for ( int i=0; i<str.length(); i++) {
        boolean wasHigh = isHigh;
        isHigh = Character.isHighSurrogate(str.charAt(i));
        if (wasHigh && isHigh)
          throw new JException("Malformed URL", i);
      }
      if (isHigh)
        throw new JException("Malformed URL", 0);
    }

    static Object convertValue(Object val) {
        return val != Jsonata.NULL_VALUE ? val : null;
    }

    static void convertNulls(Map<String, Object> res) {
        for (Entry<String, Object> e : res.entrySet()) {
            Object val = e.getValue();
            Object l = convertValue(val);
            if (l!=val)
                e.setValue(l);
            recurse(val);
        }
    }

    static void convertNulls(List<Object> res) {
        for (int i=0; i<res.size(); i++) {
            Object val = res.get(i);
            Object l = convertValue(val);
            if (l!=val)
                res.set(i, l);
            recurse(val);
        }
    }

    static void recurse(Object val) {
        if (val instanceof Map)
            convertNulls((Map)val);
        if (val instanceof List)
            convertNulls((List)val);
    }

    public static Object convertNulls(Object res) {
        recurse(res);
        return convertValue(res);
    }
}
