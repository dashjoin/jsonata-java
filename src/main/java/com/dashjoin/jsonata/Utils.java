package com.dashjoin.jsonata;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import com.dashjoin.jsonata.Jsonata.JFunction;
import com.dashjoin.jsonata.Jsonata.JFunctionCallable;

public class Utils {
    public static boolean isNumeric(Object v) throws JException {
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
    public static boolean isArrayOfNumbers(Object v) throws JException {
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
    public static class RangeList extends AbstractList<Integer> {

        final int a, b;

        public RangeList(int left, int right) {
            assert(left<=right);
            a = left; b = right;
        }

        @Override
        public int size() {
            return b-a+1;
        }

        @Override
        public boolean addAll(Collection<? extends Integer> c) {
            throw new UnsupportedOperationException("RangeList does not support 'addAll'");
        }

        @Override
        public Integer get(int index) {
            if (index < size()) {
                return a + index;
            }
            throw new IndexOutOfBoundsException(index);
        }        
    }

        // createSequence,
        // isSequence,
        // isFunction,
        // isLambda,
        // isIterable,
        // getFunctionArity,
        // isDeepEqual,
        // stringToArray,
        // isPromise

     
    public static Number convertNumber(Number n) throws JException {
        // Use int if the number is not fractional
        if (!isNumeric(n)) return null;
        if (n.intValue()==n.doubleValue())
            return n.intValue();
        return n.doubleValue();
    }

    public static void checkUrl(String str) throws JException {
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
}
