package com.dashjoin.jsonata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

public class Utils {
    public static boolean isNumeric(Object v) throws JException {
        boolean isNum = false;
        if (v instanceof Double) {
            double d = (Double)v;
            isNum = !Double.isNaN(d);
            if (isNum && !Double.isFinite((Double)v)) {
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
        return o instanceof Callable;
    }

    /**
     * Create an empty sequence to contain query results
     * @returns {Array} - empty sequence
     */
    public static List<Object> createSequence() { return createSequence(null); }

    public static List<Object> createSequence(Object el) {
        JList<Object> sequence = new JList<>();
        sequence.sequence = true;
        if (el!=null) {
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
}
