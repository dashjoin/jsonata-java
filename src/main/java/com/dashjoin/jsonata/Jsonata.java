/**
 * © Copyright IBM Corp. 2016, 2017 All Rights Reserved
 *   Project name: JSONata
 *   This project is licensed under the MIT License, see LICENSE
 */

package com.dashjoin.jsonata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.dashjoin.jsonata.Parser.Infix;
import com.dashjoin.jsonata.Parser.Symbol;
import com.dashjoin.jsonata.Utils.JList;
import com.dashjoin.jsonata.utils.Signature;

/**
 * @module JSONata
 * @description JSON query and transformation language
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Jsonata {

 /**
  * jsonata
  * @function
  * @param {Object} expr - JSONata expression
  * @returns {{evaluate: evaluate, assign: assign}} Evaluated expression
  */
// var jsonata = (function() {
//     "use strict";
 
    //  var isNumeric = utils.isNumeric;
    //  var isArrayOfStrings = utils.isArrayOfStrings;
    //  var isArrayOfNumbers = utils.isArrayOfNumbers;
    //  var createSequence = utils.createSequence;
    //  var isSequence = utils.isSequence;
    //  var isFunction = utils.isFunction;
    //  var isLambda = utils.isLambda;
    //  var isIterable = utils.isIterable;
    //  var isPromise = utils.isPromise;
    //  var getFunctionArity = utils.getFunctionArity;
    //  var isDeepEqual = utils.isDeepEqual;
 
     // Start of Evaluator code
 
    public static interface EntryCallback {
        void callback(Symbol expr, Object input, Frame environment);
    }

    public static interface ExitCallback {
        void callback(Symbol expr, Object input, Frame environment, Object result);
    }

    public static class Frame {
        Map<String, Object> bindings = new TreeMap<String,Object>();

        Frame parent;

        public boolean isParallelCall;

        public Frame(Frame enclosingEnvironment) {
            parent = enclosingEnvironment;
        }

        public void bind(String name, Object val) {
            bindings.put(name, val);
        }
        public<A,R> void bind(String name, Fn1<A,R> lambda) { bind(name, (Object)lambda); }
        public<A,B,R> void bind(String name, Fn2<A,B,R> lambda) { bind(name, (Object)lambda); }

        public Object lookup(String name) {
            // Important: if we have a null value,
            // return it
            if (bindings.containsKey(name))
                return bindings.get(name);
            if (parent!=null)
                return parent.lookup(name);
            return null;
        }

        /**
         * Sets the runtime bounds for this environment
         * 
         * @param timeout Timeout in millis
         * @param maxRecursionDepth Max recursion depth
         */
        public void setRuntimeBounds(long timeout, int maxRecursionDepth) {
            new Timebox(this, timeout, maxRecursionDepth);
        }

        public void setEvaluateEntryCallback(EntryCallback cb) {
            bind("__evaluate_entry", cb);
        }

        public void setEvaluateExitCallback(ExitCallback cb) {
            bind("__evaluate_exit", cb);
        }
    }

    static Frame staticFrame;// = createFrame(null);
 
    /**
     * Evaluate expression against input data
     * @param {Object} expr - JSONata expression
     * @param {Object} input - Input data to evaluate against
     * @param {Object} environment - Environment
     * @returns {*} Evaluated input data
     */
    Object evaluate(Symbol expr, Object input, Frame environment) {
        Object result = null;

        // Store the current input
        // This is required by Functions.functionEval for current $eval() input context
        this.input = input;

        if (parser.dbg) System.out.println("eval expr="+expr+" type="+expr.type);//+" input="+input);

        var entryCallback = environment.lookup("__evaluate_entry");
        if(entryCallback!=null) {
            ((EntryCallback)entryCallback).callback(expr, input, environment);
        }
 
        if (expr.type!=null)
        switch (expr.type) {
            case "path":
                result = /* await */ evaluatePath(expr, input, environment);
                break;
            case "binary":
                result = /* await */ evaluateBinary(expr, input, environment);
                break;
            case "unary":
                result = /* await */ evaluateUnary(expr, input, environment);
                break;
            case "name":
                result = evaluateName(expr, input, environment);
                if (parser.dbg) System.out.println("evalName "+result);
                break;
            case "string":
            case "number":
            case "value":
                result = evaluateLiteral(expr); //, input, environment);
                break;
            case "wildcard":
                result = evaluateWildcard(expr, input); //, environment);
                break;
            case "descendant":
                result = evaluateDescendants(expr, input); //, environment);
                break;
            case "parent":
                result = environment.lookup(expr.slot.label);
                break;
            case "condition":
                result = /* await */ evaluateCondition(expr, input, environment);
                break;
            case "block":
                result = /* await */ evaluateBlock(expr, input, environment);
                break;
            case "bind":
                result = /* await */ evaluateBindExpression(expr, input, environment);
                break;
            case "regex":
                result = evaluateRegex(expr); //, input, environment);
                break;
            case "function":
                result = /* await */ evaluateFunction(expr, input, environment, null);
                break;
            case "variable":
                result = evaluateVariable(expr, input, environment);
                break;
            case "lambda":
                result = evaluateLambda(expr, input, environment);
                break;
            case "partial":
                result = /* await */ evaluatePartialApplication(expr, input, environment);
                break;
            case "apply":
                result = /* await */ evaluateApplyExpression(expr, input, environment);
                break;
            case "transform":
                result = evaluateTransformExpression(expr, input, environment);
                break;
        }
 
        if (expr.predicate!=null)
            for(var ii = 0; ii < expr.predicate.size(); ii++) {
                result = /* await */ evaluateFilter(expr.predicate.get(ii).expr, result, environment);
            }
 
        if (!expr.type.equals("path") && expr.group!=null) {
            result = /* await */ evaluateGroupExpression(expr.group, result, environment);
        }
 
        var exitCallback = environment.lookup("__evaluate_exit");
        if(exitCallback!=null) {
            ((ExitCallback)exitCallback).callback(expr, input, environment, result);
        }
        
        // mangle result (list of 1 element -> 1 element, empty list -> null)
        if(result!=null && Utils.isSequence(result) && !((JList)result).tupleStream) {
            JList _result = (JList)result;
            if(expr.keepArray) {
                _result.keepSingleton = true;
            }
            if(_result.isEmpty()) {
                result = null;
            } else if(_result.size() == 1) {
                result =  _result.keepSingleton ? _result : _result.get(0);
            }
        }

        return result;
    }
 
    /**
     * Evaluate path expression against input data
    * @param {Object} expr - JSONata expression
    * @param {Object} input - Input data to evaluate against
    * @param {Object} environment - Environment
    * @returns {*} Evaluated input data
    */
    /* async */ Object evaluatePath(Symbol expr, Object input, Frame environment) {
        List inputSequence;
        // expr is an array of steps
        // if the first step is a variable reference ($...), including root reference ($$),
        //   then the path is absolute rather than relative
        if (input instanceof List && !expr.steps.get(0).type.equals("variable")) {
            inputSequence = (List)input;
        } else {
            // if input is not an array, make it so
            inputSequence = Utils.createSequence(input);
        }

        Object resultSequence = null;
        var isTupleStream = false;
        List<Map> tupleBindings = null;

        // evaluate each step in turn
        for(var ii = 0; ii < expr.steps.size(); ii++) {
            var step = expr.steps.get(ii);

            if(step.tuple!=null) {
                isTupleStream = true;
            }

            // if the first step is an explicit array constructor, then just evaluate that (i.e. don"t iterate over a context array)
            if(ii == 0 && step.consarray) {
                resultSequence = (List)/* await */ evaluate(step, inputSequence, environment);
            } else {
                if(isTupleStream) {
                    tupleBindings = (List)/* await */ evaluateTupleStep(step, (List)inputSequence, (List)tupleBindings, environment);
                } else {
                    resultSequence = /* await */ evaluateStep(step, inputSequence, environment, ii == expr.steps.size() - 1);
                }
            }

            if (!isTupleStream && (resultSequence == null || ((List)resultSequence).size() == 0)) {
                break;
            }

            if(step.focus == null) {
                    inputSequence = (List)resultSequence;
            }

        }

        if(isTupleStream) {
            if(expr.tuple!=null) {
                // tuple stream is carrying ancestry information - keep this
                resultSequence = tupleBindings;
            } else {
                resultSequence = Utils.createSequence();
                for (int ii = 0; ii < tupleBindings.size(); ii++) {
                    ((List)resultSequence).add(tupleBindings.get(ii).get("@"));
                }
            }
        }

        if(expr.keepSingletonArray) {

            // If we only got an ArrayList, convert it so we can set the keepSingleton flag
            if (!(resultSequence instanceof JList))
                resultSequence = new JList<>((List)resultSequence);

            // if the array is explicitly constructed in the expression and marked to promote singleton sequences to array
            if((resultSequence instanceof JList) && ((JList)resultSequence).cons && !((JList)resultSequence).sequence) {
                resultSequence = Utils.createSequence(resultSequence);
            }
            ((JList)resultSequence).keepSingleton = true;
        }

        if (expr.group != null) {
            resultSequence = /* await */ evaluateGroupExpression(expr.group, isTupleStream ? tupleBindings : resultSequence, environment);
        }

        return resultSequence;
    }
 
    Frame createFrameFromTuple(Frame environment, Map<String, Object> tuple) {
        var frame = createFrame(environment);
        if (tuple!=null) for (var prop : tuple.keySet()) {
            frame.bind(prop, tuple.get(prop));
        }
        return frame;
    }
 
    /**
     * Evaluate a step within a path
    * @param {Object} expr - JSONata expression
    * @param {Object} input - Input data to evaluate against
    * @param {Object} environment - Environment
    * @param {boolean} lastStep - flag the last step in a path
    * @returns {*} Evaluated input data
    */
    /* async */ Object evaluateStep(Symbol expr, Object input, Frame environment, boolean lastStep) {
        Object result;
        if(expr.type.equals("sort")) {
            result = /* await */ evaluateSortExpression(expr, input, environment);
            if(expr.stages!=null) {
                result = /* await */ evaluateStages(expr.stages, result, environment);
            }
            return result;
        }

        result = Utils.createSequence();

        for(var ii = 0; ii < ((List)input).size(); ii++) {
            var res = /* await */ evaluate(expr, ((List)input).get(ii), environment);
            if(expr.stages!=null) {
                for(var ss = 0; ss < expr.stages.size(); ss++) {
                    res = /* await */ evaluateFilter(expr.stages.get(ss).expr, res, environment);
                }
            }
            if(res != null) {
                ((List) result).add(res);
            }
        }

        var resultSequence = Utils.createSequence();
        if(lastStep && ((List)result).size()==1 && (((List)result).get(0) instanceof List) && !Utils.isSequence(((List)result).get(0))) {
            resultSequence = (List) ((List) result).get(0);
        } else {
            // flatten the sequence
            for (Object res : (List)result) {
                if (!(res instanceof List) || (res instanceof JList && ((JList)res).cons)) {
                    // it's not an array - just push into the result sequence
                    resultSequence.add(res);
                } else {
                    // res is a sequence - flatten it into the parent sequence
                    resultSequence.addAll((List)res);
                }
            }
        }

        return resultSequence;
    }
 
    /* async */ Object evaluateStages(List<Symbol> stages, Object input, Frame environment) {
        var result = input;
        for(var ss = 0; ss < stages.size(); ss++) {
            var stage = stages.get(ss);
            switch(stage.type) {
                case "filter":
                    result = /* await */ evaluateFilter(stage.expr, result, environment);
                    break;
                case "index":
                    for(var ee = 0; ee < ((List)result).size(); ee++) {
                        var tuple = ((List)result).get(ee);
                        ((Map)tuple).put(""+stage.value, ee);
                    }
                    break;
            }
        }
        return result;
    }
 
    /**
     * Evaluate a step within a path
    * @param {Object} expr - JSONata expression
    * @param {Object} input - Input data to evaluate against
    * @param {Object} tupleBindings - The tuple stream
    * @param {Object} environment - Environment
    * @returns {*} Evaluated input data
    */
    /* async */ Object evaluateTupleStep(Symbol expr, List input, List<Map> tupleBindings, Frame environment) {
        List result = null;
        if(expr.type.equals("sort")) {
            if(tupleBindings!=null) {
                result = (List) /* await */ evaluateSortExpression(expr, tupleBindings, environment);
            } else {
                List sorted = (List) /* await */ evaluateSortExpression(expr, input, environment);
                result = Utils.createSequence();
                ((JList)result).tupleStream = true;
                for(var ss = 0; ss < ((List)sorted).size(); ss++) {
                    var tuple = Map.of("@", sorted.get(ss),
                    expr.index, ss);
                    result.add(tuple);
                }
            }
            if(expr.stages!=null) {
                result = /* await */ (List)evaluateStages(expr.stages, result, environment);
            }
            return result;
        }

        result = Utils.createSequence();
        ((JList)result).tupleStream = true;
        var stepEnv = environment;
        if(tupleBindings == null) {
            tupleBindings = input.stream().filter(item -> item!=null).map(item -> Map.of("@", item)).toList();
        }

        for(var ee = 0; ee < tupleBindings.size(); ee++) {
            stepEnv = createFrameFromTuple(environment, tupleBindings.get(ee));
            Object _res = /* await */ evaluate(expr, tupleBindings.get(ee).get("@"), stepEnv);
            // res is the binding sequence for the output tuple stream
            if (_res!=null) { //(typeof res !== "undefined") {
                List res;
                if (!(_res instanceof List)) {
                    res = new ArrayList<>(); res.add(_res);
                } else {
                    res = (List)_res;
                }
                for (var bb = 0; bb < res.size(); bb++) {
                    Map tuple = new LinkedHashMap<>();
                    tuple.putAll(tupleBindings.get(ee));
                    //Object.assign(tuple, tupleBindings[ee]);
                    if((res instanceof JList) && ((JList)res).tupleStream) {
                        tuple.putAll((Map)res.get(bb));
                    } else {
                        if (expr.focus!=null) {
                            tuple.put(expr.focus, res.get(bb));
                            tuple.put("@", tupleBindings.get(ee).get("@"));
                        } else {
                            tuple.put("@", res.get(bb));
                        }
                        if (expr.index!=null) {
                            tuple.put(expr.index, bb);
                        }
                        if (expr.ancestor!=null) {
                            tuple.put(expr.ancestor.label, tupleBindings.get(ee).get("@"));
                        }
                    }
                    result.add(tuple);
                }
            }
        }

        if(expr.stages!=null) {
            result = (List) /* await */ evaluateStages(expr.stages, result, environment);
        }

        return result;
    }
 
    /**
     * Apply filter predicate to input data
    * @param {Object} predicate - filter expression
    * @param {Object} input - Input data to apply predicates against
    * @param {Object} environment - Environment
    * @returns {*} Result after applying predicates
    */
    /* async */ Object evaluateFilter(Object _predicate, Object input, Frame environment) {
    Symbol predicate = (Symbol)_predicate;
        var results = Utils.createSequence();
        if( input instanceof JList && ((JList)input).tupleStream) {
            ((JList)results).tupleStream = true;
        }
        if (!(input instanceof List)) { // isArray
            input = Utils.createSequence(input);
        }
        if (predicate.type.equals("number")) {
            var index = ((Number)predicate.value).intValue();  // round it down - was Math.floor
            if (index < 0) {
                // count in from end of array
                index = ((List)input).size() + index;
            }
            var item = index<((List)input).size() ? ((List)input).get(index) : null;
            if(item != null) {
                if(item instanceof List) {
                    results = (List)item;
                } else {
                    results.add(item);
                }
            }
        } else {
            for (int index = 0; index < ((List)input).size(); index++) {
                var item = ((List)input).get(index);
                var context = item;
                var env = environment;
                if(input instanceof JList && ((JList)input).tupleStream) {
                    context = ((Map)item).get("@");
                    env = createFrameFromTuple(environment, (Map)item);
                }
                var res = /* await */ evaluate(predicate, context, env);
                if (Utils.isNumeric(res)) {
                    res = Utils.createSequence(res);
                }
                if (Utils.isArrayOfNumbers(res)) {
                for (Object ires : ((List)res)) {
                        // round it down
                        var ii = ((Number)ires).intValue(); // Math.floor(ires);
                        if (ii < 0) {
                            // count in from end of array
                            ii = ((List)input).size() + ii;
                        }
                        if (ii == index) {
                            results.add(item);
                        }
                    }
                } else if (boolize(res)) { // truthy
                    results.add(item);
                }
            }
        }
        return results;
    }
 
    /**
     * Evaluate binary expression against input data
    * @param {Object} expr - JSONata expression
    * @param {Object} input - Input data to evaluate against
    * @param {Object} environment - Environment
    * @returns {*} Evaluated input data
    */
    /* async */ Object evaluateBinary(Symbol _expr, Object input, Frame environment) {
    Infix expr = (Infix)_expr;
        Object result = null;
        var lhs = /* await */ evaluate(expr.lhs, input, environment);
        String op = ""+expr.value;

        if (op.equals("and") || op.equals("or")) {

        //defer evaluation of RHS to allow short-circuiting
        var evalrhs = /* async */ new Callable() {
            public Object call() throws Exception {
                return evaluate(expr.rhs, input, environment);
            }
        };

            try {
                return /* await */ evaluateBooleanExpression(lhs, evalrhs, op);
            } catch(Exception err) {
            if (!(err instanceof JException))
                throw new JException("Unexpected", expr.position);
                //err.position = expr.position;
                //err.token = op;
                throw (JException)err;
            }
        }

    var rhs = /* await */ evaluate(expr.rhs, input, environment); //evalrhs();
    try {
            switch (op) {
                case "+":
                case "-":
                case "*":
                case "/":
                case "%":
                    result = evaluateNumericExpression(lhs, rhs, op);
                    break;
                case "=":
                case "!=":
                    result = evaluateEqualityExpression(lhs, rhs, op);
                    break;
                case "<":
                case "<=":
                case ">":
                case ">=":
                    result = evaluateComparisonExpression(lhs, rhs, op);
                    break;
                case "&":
                    result = evaluateStringConcat(lhs, rhs);
                    break;
                case "..":
                    result = evaluateRangeExpression(lhs, rhs);
                    break;
                case "in":
                    result = evaluateIncludesExpression(lhs, rhs);
                    break;
            default:
                throw new JException("Unexpected operator "+op, expr.position);
            }
        } catch(Exception err) {
            //err.position = expr.position;
            //err.token = op;
            throw err;
        }
        return result;
    }
 
    final public static Object UNDEFINED = new Object();
    final public static Object NULL_VALUE = new Object() { public String toString() { return "null"; }};

    /**
     * Evaluate unary expression against input data
    * @param {Object} expr - JSONata expression
    * @param {Object} input - Input data to evaluate against
    * @param {Object} environment - Environment
    * @returns {*} Evaluated input data
    */
    /* async */ Object evaluateUnary(Symbol expr, Object input, Frame environment) {
        Object result = null;

        switch ((String)""+expr.value) { // Uli was: expr.value - where is value set???
            case "-":
                result = /* await */ evaluate(expr.expression, input, environment);
                if (result==null) { //(typeof result === "undefined") {
                    result = null;
                } else if (Utils.isNumeric(result)) {
                    result = Utils.convertNumber( -((Number)result).doubleValue() );
                } else {
                    throw new JException(
                        "D1002",
                        //stack: (new Error()).stack,
                        expr.position,
                        expr.value,
                        result
                    );
                }
                break;
            case "[":
            // array constructor - evaluate each item
            result = new JList<>(); // [];
            int idx = 0;
            for (var item : expr.expressions) {
                environment.isParallelCall = idx > 0;
                Object value = evaluate(item, input, environment);
                if (value!=null) {
                    if ((""+item.value).equals("["))
                        ((List)result).add(value);
                    else
                        result = Functions.append(result, value);
                }
                idx++;
            }
            if(expr.consarray) {
                if (!(result instanceof JList))
                    result = new JList((List)result);
                //System.out.println("const "+result);
                ((JList)result).cons = true; 
            }
            break;
            case "{":
                // object constructor - apply grouping
                result = /* await */ evaluateGroupExpression(expr, input, environment);
                break;

        }
        return result;
    }
 
    /**
     * Evaluate name object against input data
    * @param {Object} expr - JSONata expression
    * @param {Object} input - Input data to evaluate against
    * @param {Object} environment - Environment
    * @returns {*} Evaluated input data
    */
    Object evaluateName(Symbol expr, Object input, Frame environment) {
        // lookup the "name" item in the input
        return Functions.lookup(input, (String)expr.value);
    }

    /**
     * Evaluate literal against input data
     * @param {Object} expr - JSONata expression
     * @returns {*} Evaluated input data
     */
    Object evaluateLiteral(Symbol expr) {
        return expr.value!=null ? expr.value : NULL_VALUE;
    }
 
    /**
     * Evaluate wildcard against input data
    * @param {Object} expr - JSONata expression
    * @param {Object} input - Input data to evaluate against
    * @returns {*} Evaluated input data
    */
    Object evaluateWildcard(Symbol expr, Object input) {
        var results = Utils.createSequence();
        if ((input instanceof JList) && ((JList)input).outerWrapper && ((JList)input).size() > 0) {
            input = ((JList)input).get(0);
        }
        if (input != null && input instanceof Map) { // typeof input === "object") {
        for (Object key : ((Map)input).keySet()) {
        // Object.keys(input).forEach(Object (key) {
                var value = ((Map)input).get(key);
                if((value instanceof List)) {
                    value = flatten(value, null);
                    results = (List)Functions.append(results, value);
                } else {
                    results.add(value);
                }
            }
        } else if (input instanceof List) {
            // Java: need to handle List separately
            for (Object value : ((List)input)) {
                if((value instanceof List)) {
                    value = flatten(value, null);
                    results = (List)Functions.append(results, value);
                } else if (value instanceof Map) {
                // Call recursively do decompose the map
                results.addAll((List)evaluateWildcard(expr, value));
                } else {
                    results.add(value);
                }
            }
        }

        // result = normalizeSequence(results);
        return results;
    }

    /**
     * Returns a flattened array
    * @param {Array} arg - the array to be flatten
    * @param {Array} flattened - carries the flattened array - if not defined, will initialize to []
    * @returns {Array} - the flattened array
    */
    Object flatten(Object arg, List flattened) {
        if(flattened == null) {
            flattened = new ArrayList<>();
        }
        if(arg instanceof List) {
            for (Object item : ((List)arg)) {
                flatten(item, flattened);
            }
        } else {
            flattened.add(arg);
        }
        return flattened;
    }
 
    /**
     * Evaluate descendants against input data
    * @param {Object} expr - JSONata expression
    * @param {Object} input - Input data to evaluate against
    * @returns {*} Evaluated input data
    */
    Object evaluateDescendants(Symbol expr, Object input) {
        Object result = null;
        var resultSequence = Utils.createSequence();
        if (input != null) {
            // traverse all descendants of this object/array
            recurseDescendants(input, resultSequence);
            if (resultSequence.size() == 1) {
                result = resultSequence.get(0);
            } else {
                result = resultSequence;
            }
        }
        return result;
    }
 
    /**
     * Recurse through descendants
    * @param {Object} input - Input data
    * @param {Object} results - Results
    */
    void recurseDescendants(Object input, List results) {
        // this is the equivalent of //* in XPath
        if (!(input instanceof List)) {
            results.add(input);
        }
        if (input instanceof List) {
            for (Object member : ((List)input)) { //input.forEach(Object (member) {
                    recurseDescendants(member, results);
            }
        } else if (input != null && input instanceof Map) {
            //Object.keys(input).forEach(Object (key) {
            for (Object key : ((Map)input).keySet()) {
                    recurseDescendants(((Map)input).get(key), results);
            }
        }
    }

    /**
     * Evaluate numeric expression against input data
     * @param {Object} lhs - LHS value
     * @param {Object} rhs - RHS value
     * @param {Object} op - opcode
     * @returns {*} Result
     */
    Object evaluateNumericExpression(Object _lhs, Object _rhs, String op) {
        double result = 0;

        if (_lhs!=null && !Utils.isNumeric(_lhs)) {
            throw new JException("T2001", -1,
            op, _lhs
            );
        }
        if (_rhs!=null && !Utils.isNumeric(_rhs)) {
            throw new JException("T2002", -1,
            op, _rhs
            );
        }

        if (_lhs == null || _rhs == null) {
            // if either side is undefined, the result is undefined
            return null;
        }

        //System.out.println("op22 "+op+" "+_lhs+" "+_rhs);
        double lhs = ((Number)_lhs).doubleValue();
        double rhs = ((Number)_rhs).doubleValue();

        switch (op) {
            case "+":
                result = lhs + rhs;
                break;
            case "-":
                result = lhs - rhs;
                break;
            case "*":
                result = lhs * rhs;
                break;
            case "/":
                result = lhs / rhs;
                break;
            case "%":
                result = lhs % rhs;
                break;
        }
        return Utils.convertNumber(result);
    }
 
     /**
      * Evaluate equality expression against input data
      * @param {Object} lhs - LHS value
      * @param {Object} rhs - RHS value
      * @param {Object} op - opcode
      * @returns {*} Result
      */
    Object evaluateEqualityExpression(Object lhs, Object rhs, String op) {
        Object result = null;

        // type checks
        var ltype = lhs!=null ? lhs.getClass().getSimpleName() : null;
        var rtype = rhs!=null ? rhs.getClass().getSimpleName() : null;

        if (ltype == null || rtype == null) {
            // if either side is undefined, the result is false
            return false;
        }

        // JSON might come with integers,
        // convert all to double...
        // FIXME: semantically OK?
        if (lhs instanceof Number)
            lhs = ((Number)lhs).doubleValue();
        if (rhs instanceof Number)
            rhs = ((Number)rhs).doubleValue();

        switch (op) {
            case "=":
                result = lhs.equals(rhs); // isDeepEqual(lhs, rhs);
                break;
            case "!=":
                result = !lhs.equals(rhs); // !isDeepEqual(lhs, rhs);
                break;
        }
        return result;
    }
 
     /**
      * Evaluate comparison expression against input data
      * @param {Object} lhs - LHS value
      * @param {Object} rhs - RHS value
      * @param {Object} op - opcode
      * @returns {*} Result
      */
    Object evaluateComparisonExpression(Object lhs, Object rhs, String op) {
        Object result = null;

        // type checks
        var lcomparable = lhs == null || lhs instanceof String || lhs instanceof Number;
        var rcomparable = rhs == null || rhs instanceof String || rhs instanceof Number;

        // if either aa or bb are not comparable (string or numeric) values, then throw an error
        if (!lcomparable || !rcomparable) {
            throw new JException(
                "T2010",
                0, //position,
                //stack: (new Error()).stack,
                op, lhs!=null ? lhs : rhs
            );
        }

        // if either side is undefined, the result is undefined
        if (lhs == null || rhs==null) {
            return null;
        }
        
        //if aa and bb are not of the same type
        if (!lhs.getClass().equals(rhs.getClass())) {

        if (lhs instanceof Number && rhs instanceof Number) {
            // Java : handle Double / Integer / Long comparisons
            // convert all to double -> loss of precision (64-bit long to double) be a problem here?
            lhs = ((Number)lhs).doubleValue();
            rhs = ((Number)rhs).doubleValue();

        } else

            throw new JException(
                "T2009",
                0, // location?
                // stack: (new Error()).stack,
                lhs,
                rhs
            );
        }

        Comparable _lhs = (Comparable)lhs;

        switch (op) {
            case "<":
                result = _lhs.compareTo(rhs) < 0;
                break;
            case "<=":
                result = _lhs.compareTo(rhs) <= 0; //lhs <= rhs;
                break;
            case ">":
                result = _lhs.compareTo(rhs) > 0; // lhs > rhs;
                break;
            case ">=":
                result = _lhs.compareTo(rhs) >= 0; // lhs >= rhs;
                break;
        }
        return result;
    }
 
     /**
      * Inclusion operator - in
      *
      * @param {Object} lhs - LHS value
      * @param {Object} rhs - RHS value
      * @returns {boolean} - true if lhs is a member of rhs
      */
    Object evaluateIncludesExpression(Object lhs, Object rhs) {
        var result = false;

        if (lhs == null || rhs == null) {
            // if either side is undefined, the result is false
            return false;
        }

        if(!(rhs instanceof List)) {
            var _rhs = new ArrayList<>(); _rhs.add(rhs);
            rhs = _rhs;
        }

        for(var i = 0; i < ((List)rhs).size(); i++) {
            if(((List)rhs).get(i).equals(lhs)) {
                result = true;
                break;
            }
        }

        return result;
    }
 
    /**
     * Evaluate boolean expression against input data
     * @param {Object} lhs - LHS value
     * @param {Function} evalrhs - Object to evaluate RHS value
     * @param {Object} op - opcode
     * @returns {*} Result
     */
    /* async */ Object evaluateBooleanExpression(Object lhs, Callable evalrhs, String op) throws Exception {
        Object result = null;

        var lBool = boolize(lhs);

        switch (op) {
            case "and":
                result = lBool && boolize(/* await */ evalrhs.call());
                break;
            case "or":
                result = lBool || boolize(/* await */ evalrhs.call());
                break;
        }
        return result;
    }

    public static boolean boolize(Object value) {
        var booledValue = Functions.toBoolean(value);
        return booledValue == null ? false : booledValue;
    }
 
    /**
     * Evaluate string concatenation against input data
     * @param {Object} lhs - LHS value
     * @param {Object} rhs - RHS value
     * @returns {string|*} Concatenated string
     */
    Object evaluateStringConcat(Object lhs, Object rhs) {
        String result;

        var lstr = "";
        var rstr = "";
        if (lhs != null) {
            lstr = Functions.string(lhs,null);
        }
        if (rhs != null) {
            rstr = Functions.string(rhs,null);
        }

        result = lstr + rstr;
        return result;
    }
 
    static class GroupEntry {
        Object data;
        int exprIndex;
    }
     
    /**
     * Evaluate group expression against input data
     * @param {Object} expr - JSONata expression
     * @param {Object} input - Input data to evaluate against
     * @param {Object} environment - Environment
     * @returns {{}} Evaluated input data
     */
    /* async */ Object evaluateGroupExpression(Symbol expr, Object _input, Frame environment) {
        var result = new LinkedHashMap<Object,Object>();
        var groups = new LinkedHashMap<Object,GroupEntry>();
        var reduce = (_input instanceof JList) && ((JList)_input).tupleStream ? true : false;
        // group the input sequence by "key" expression
        if (!(_input instanceof List)) {
            _input = Utils.createSequence(_input);
        }
        List input = (List)_input;

        // if the array is empty, add an undefined entry to enable literal JSON object to be generated
        if (input.isEmpty()) {
            input.add(null);
        }

        for(var itemIndex = 0; itemIndex < input.size(); itemIndex++) {
            var item = input.get(itemIndex);
            var env = reduce ? createFrameFromTuple(environment, (Map)item) : environment;
            for(var pairIndex = 0; pairIndex < expr.lhsObject.size(); pairIndex++) {
                var pair = expr.lhsObject.get(pairIndex);
                var key = /* await */ evaluate(pair[0], reduce ? ((Map)item).get("@") : item, env);
                // key has to be a string
                if (key!=null && !(key instanceof String)) {
                    throw new JException("T1003",
                        //stack: (new Error()).stack,
                        expr.position,
                        key
                    );
                }

                if (key != null) {
                    var entry = new GroupEntry();
                    entry.data = item; entry.exprIndex = pairIndex;
                    if (groups.get(key)!=null) {
                        // a value already exists in this slot
                        if(groups.get(key).exprIndex != pairIndex) {
                            // this key has been generated by another expression in this group
                            // when multiple key expressions evaluate to the same key, then error D1009 must be thrown
                            throw new JException("D1009",
                                //stack: (new Error()).stack,
                                expr.position,
                                key
                            );
                        }

                        // append it as an array
                        groups.get(key).data = Functions.append(groups.get(key).data, item);
                    } else {
                        groups.put(key, entry);
                    }
                }
            }
        }

        // iterate over the groups to evaluate the "value" expression
        //let generators = /* await */ Promise.all(Object.keys(groups).map(/* async */ (key, idx) => {
        int idx = 0;
        for (Entry<Object,GroupEntry> e : groups.entrySet()) {
            var entry = e.getValue();
            var context = entry.data;
            var env = environment;
            if (reduce) {
                var tuple = reduceTupleStream(entry.data);
                context = ((Map)tuple).get("@");
                ((Map)tuple).remove("@");
                env = createFrameFromTuple(environment, (Map)tuple);
            }
            env.isParallelCall = idx > 0;
            //return [key, /* await */ evaluate(expr.lhs[entry.exprIndex][1], context, env)];
            Object res = evaluate(expr.lhsObject.get(entry.exprIndex)[1], context, env);
            if (res!=null)
                result.put(e.getKey(), res);

            idx++;
        }

    //  for (let generator of generators) {
    //      var [key, value] = /* await */ generator;
    //      if(typeof value !== "undefined") {
    //          result[key] = value;
    //      }
    //  }

        return result;
    }

    Object reduceTupleStream(Object _tupleStream) {
        if(!(_tupleStream instanceof List)) {
            return _tupleStream;
        }
        List<Map> tupleStream = (List)_tupleStream;

        var result = new LinkedHashMap<>();
        result.putAll(tupleStream.get(0));

        //Object.assign(result, tupleStream[0]);
        for(var ii = 1; ii < tupleStream.size(); ii++) {

        Map el = tupleStream.get(ii);
        for (var prop : el.keySet()) {

//             for(const prop in tupleStream[ii]) {

            result.put(prop, Functions.append(result.get(prop), el.get(prop)));

//               result[prop] = fn.append(result[prop], tupleStream[ii][prop]);
            }
        }
        return result;
    }

    /**
     * Evaluate range expression against input data
     * @param {Object} lhs - LHS value
     * @param {Object} rhs - RHS value
     * @returns {Array} Resultant array
     */
    Object evaluateRangeExpression(Object lhs, Object rhs) {
        Object result = null;

        if (lhs != null && (!(lhs instanceof Long) && !(lhs instanceof Integer))) {
            throw new JException("T2003",
                //stack: (new Error()).stack,
                -1,
                lhs
            );
        }
        if (rhs != null && (!(rhs instanceof Long) && !(rhs instanceof Integer))) {
            throw new JException("T2004",
            //stack: (new Error()).stack,
            -1,
            rhs
            );
        }

        if (rhs==null || lhs==null) {
            // if either side is undefined, the result is undefined
            return result;
        }

        long _lhs = ((Number)lhs).longValue(), _rhs = ((Number)rhs).longValue();

        if (_lhs > _rhs) {
            // if the lhs is greater than the rhs, return undefined
            return result;
        }

        // limit the size of the array to ten million entries (1e7)
        // this is an implementation defined limit to protect against
        // memory and performance issues.  This value may increase in the future.
        var size = _rhs - _lhs + 1;
        if(size > 1e7) {
            throw new JException("D2014",
                //stack: (new Error()).stack,
                -1,
                size
            );
        }

        return new Utils.RangeList(_lhs, _rhs);
    }
 
    /**
     * Evaluate bind expression against input data
     * @param {Object} expr - JSONata expression
     * @param {Object} input - Input data to evaluate against
     * @param {Object} environment - Environment
     * @returns {*} Evaluated input data
     */
    /* async */ Object evaluateBindExpression(Symbol expr, Object input, Frame environment) {
        // The RHS is the expression to evaluate
        // The LHS is the name of the variable to bind to - should be a VARIABLE token (enforced by parser)
        var value = /* await */ evaluate(expr.rhs, input, environment);
        environment.bind(""+expr.lhs.value, value);
        return value;
    }
 
    /**
     * Evaluate condition against input data
     * @param {Object} expr - JSONata expression
     * @param {Object} input - Input data to evaluate against
     * @param {Object} environment - Environment
     * @returns {*} Evaluated input data
     */
    /* async */ Object evaluateCondition(Symbol expr, Object input, Frame environment) {
        Object result = null;
        var condition = /* await */ evaluate(expr.condition, input, environment);
        if (boolize(condition)) {
            result = /* await */ evaluate(expr.then, input, environment);
        } else if (expr._else != null) {
            result = /* await */ evaluate(expr._else, input, environment);
        }
        return result;
    }
 
     /**
      * Evaluate block against input data
      * @param {Object} expr - JSONata expression
      * @param {Object} input - Input data to evaluate against
      * @param {Object} environment - Environment
      * @returns {*} Evaluated input data
      */
    /* async */ Object evaluateBlock(Symbol expr, Object input, Frame environment) {
        Object result = null;
        // create a new frame to limit the scope of variable assignments
        // TODO, only do this if the post-parse stage has flagged this as required
        var frame = createFrame(environment);
        // invoke each expression in turn
        // only return the result of the last one
        for(var ex : expr.expressions) {
            result = /* await */ evaluate(ex, input, frame);
        }

        return result;
    }
 
     /**
      * Prepare a regex
      * @param {Object} expr - expression containing regex
      * @returns {Function} Higher order Object representing prepared regex
      */
     Object evaluateRegex(Symbol expr) {
        return expr.value;

        //  var re = new jsonata.RegexEngine(expr.value);
        //  var closure = function(str, fromIndex) {
        //      var result;
        //      re.lastIndex = fromIndex || 0;
        //      var match = re.exec(str);
        //      if(match !== null) {
        //          result = {
        //              match: match[0],
        //              start: match.index,
        //              end: match.index + match[0].length,
        //              groups: []
        //          };
        //          if(match.length > 1) {
        //              for(var i = 1; i < match.length; i++) {
        //                  result.groups.push(match[i]);
        //              }
        //          }
        //          result.next = function() {
        //              if(re.lastIndex >= str.length) {
        //                  return undefined;
        //              } else {
        //                  var next = closure(str, re.lastIndex);
        //                  if(next && next.match === "") {
        //                      // matches zero length string; this will never progress
        //                      throw {
        //                          code: "D1004",
        //                          stack: (new Error()).stack,
        //                          position: expr.position,
        //                          value: expr.value.source
        //                      };
        //                  }
        //                  return next;
        //              }
        //          };
        //      }
 
        //      return result;
        //  };
        //  return closure;
     }
 
     /**
      * Evaluate variable against input data
      * @param {Object} expr - JSONata expression
      * @param {Object} input - Input data to evaluate against
      * @param {Object} environment - Environment
      * @returns {*} Evaluated input data
      */
    Object evaluateVariable(Symbol expr, Object input, Frame environment) {
        // lookup the variable value in the environment
        Object result = null;
        // if the variable name is empty string, then it refers to context value
        if (expr.value.equals("")) {
        // Empty string == "$" !
            result = input instanceof JList && ((JList)input).outerWrapper ? ((JList)input).get(0) : input;
        } else  {
            result = environment.lookup((String)expr.value);
            if (parser.dbg) System.out.println("variable name="+expr.value+" val="+result);
        }
        return result;
    }
 
     /**
      * sort / order-by operator
      * @param {Object} expr - AST for operator
      * @param {Object} input - Input data to evaluate against
      * @param {Object} environment - Environment
      * @returns {*} Ordered sequence
      */
    /* async */ Object evaluateSortExpression(Symbol expr, Object input, Frame environment) {
        Object result;

        // evaluate the lhs, then sort the results in order according to rhs expression
        var lhs = (List)input;
        var isTupleSort = (input instanceof JList && ((JList)input).tupleStream) ? true : false;

        // sort the lhs array
        // use comparator function
        var comparator = new Comparator() { 

        @Override
        public int compare(Object a, Object b) {

            // expr.terms is an array of order-by in priority order
            var comp = 0;
            for(var index = 0; comp == 0 && index < expr.terms.size(); index++) {
                var term = expr.terms.get(index);
                //evaluate the sort term in the context of a
                var context = a;
                var env = environment;
                if(isTupleSort) {
                    context = ((Map)a).get("@");
                    env = createFrameFromTuple(environment, (Map)a);
                }
                Object aa = /* await */ evaluate(term.expression, context, env);

                 //evaluate the sort term in the context of b
                 context = b;
                 env = environment;
                 if(isTupleSort) {
                     context = ((Map)b).get("@");
                     env = createFrameFromTuple(environment, (Map)b);
                 }
                 Object bb = /* await */ evaluate(term.expression, context, env);
 
                // type checks
                //  var atype = typeof aa;
                //  var btype = typeof bb;
                // undefined should be last in sort order
                if(aa == null) {
                    // swap them, unless btype is also undefined
                    comp = (bb == null) ? 0 : 1;
                    continue;
                }
                if(bb == null) {
                    comp = -1;
                    continue;
                }
 
                // if aa or bb are not string or numeric values, then throw an error
                if(!(aa instanceof Number || aa instanceof String) ||
                !(bb instanceof Number || bb instanceof String) 
                ) {
                    throw new JException("T2008",
                        expr.position,
                        aa,
                        bb
                    );
                }
 
                 //if aa and bb are not of the same type
                 boolean sameType = false;
                if (aa instanceof Number && bb instanceof Number)
                    sameType = true;
                else if (aa.getClass().isAssignableFrom(bb.getClass()) ||
                    bb.getClass().isAssignableFrom(aa.getClass())) {
                    sameType = true;
                }

                if(!sameType) {
                    throw new JException("T2007",
                        expr.position,
                        aa,
                        bb
                    );
                }
                if(aa.equals(bb)) {
                    // both the same - move on to next term
                    continue;
                } else if (((Comparable)aa).compareTo(bb)<0) {
                    comp = -1;
                } else {
                    comp = 1;
                }
                if(term.descending == true) {
                    comp = -comp;
                }
            }
            // only swap a & b if comp equals 1
            // return comp == 1;
            return comp;
        }
        };
 
        //  var focus = {
        //      environment: environment,
        //      input: input
        //  };
        //  // the `focus` is passed in as the `this` for the invoked function
        //  result = /* await */ fn.sort.apply(focus, [lhs, comparator]);
 
        result = Functions.sort(lhs, comparator);
        return result;        
    }
 
     /**
      * create a transformer function
      * @param {Object} expr - AST for operator
      * @param {Object} input - Input data to evaluate against
      * @param {Object} environment - Environment
      * @returns {*} tranformer function
      */
    Object evaluateTransformExpression(Symbol expr, Object input, Frame environment) {
         // create a Object to implement the transform definition
        JFunctionCallable transformer = (_input, args) -> {
        // /* async */ Object (obj) { // signature <(oa):o>

            var obj = ((List)args).get(0);

            // undefined inputs always return undefined
            if(obj == null) {
                return null;
            }
 
            // this Object returns a copy of obj with changes specified by the pattern/operation
            Object result = Functions.functionClone(obj);

            var _matches = /* await */ evaluate(expr.pattern, result, environment);
            if(_matches != null) {
                if(!(_matches instanceof List)) {
                    _matches = new ArrayList<>(List.of(_matches));
                }
                List matches = (List)_matches;
                for(var ii = 0; ii < matches.size(); ii++) {
                    var match = matches.get(ii);
                    // evaluate the update value for each match
                    var update = /* await */ evaluate(expr.update, match, environment);
                    // update must be an object
                    //var updateType = typeof update;
                    //if(updateType != null) 
                    
                    if (update!=null) {
                    if(!(update instanceof Map)) {
                            // throw type error
                            throw new JException("T2011",
                                expr.update.position,
                                update
                            );
                        }
                        // merge the update
                        for(var prop : ((Map)update).keySet()) {
                            ((Map)match).put(prop, ((Map)update).get(prop));
                        }
                    }

                    // delete, if specified, must be an array of strings (or single string)
                    if(expr.delete != null) {
                        var deletions = /* await */ evaluate(expr.delete, match, environment);
                        if(deletions != null) {
                            var val = deletions;
                            if (!(deletions instanceof List)) {
                                deletions = new ArrayList<>(List.of(deletions));
                            }
                            if (!Utils.isArrayOfStrings(deletions)) {
                                // throw type error
                                throw new JException("T2012",
                                    expr.delete.position,
                                    val
                                );
                            }
                            List _deletions = (List)deletions;
                            for (var jj = 0; jj < _deletions.size(); jj++) {
                                if(match instanceof Map) {
                                ((Map)match).remove(_deletions.get(jj));
                                    //delete match[deletions[jj]];
                                }
                            }
                        }
                    }
                }
            }

            return result;
        };

        return new JFunction(transformer, "<(oa):o>");
    }
 
    static Symbol chainAST; // = new Parser().parse("function($f, $g) { function($x){ $g($f($x)) } }");
 
    static Symbol chainAST() {
        if (chainAST==null) {
            // only create on demand
            chainAST = new Parser().parse("function($f, $g) { function($x){ $g($f($x)) } }");
        }
        return chainAST;
    }

     /**
      * Apply the Object on the RHS using the sequence on the LHS as the first argument
      * @param {Object} expr - JSONata expression
      * @param {Object} input - Input data to evaluate against
      * @param {Object} environment - Environment
      * @returns {*} Evaluated input data
      */
    /* async */ Object evaluateApplyExpression(Symbol expr, Object input, Frame environment) {
        Object result = null;


        var lhs = /* await */ evaluate(expr.lhs, input, environment);

        // Map null to NULL_VALUE before applying to functions
        // TODO: fix more generically!
        if (lhs==null)
            lhs = Jsonata.NULL_VALUE;

        if(expr.rhs.type.equals("function")) {
        //Symbol applyTo = new Symbol(); applyTo.context = lhs;
            // this is a Object _invocation_; invoke it with lhs expression as the first argument
            result = /* await */ evaluateFunction(expr.rhs, input, environment, lhs);
        } else {
            var func = /* await */ evaluate(expr.rhs, input, environment);

            if(!isFunctionLike(func) &&
            !isFunctionLike(lhs)) {
                throw new JException("T2006",
                    //stack: (new Error()).stack,
                    expr.position,
                    func
                );
            }

            if(isFunctionLike(lhs)) {
                // this is Object chaining (func1 ~> func2)
                // λ($f, $g) { λ($x){ $g($f($x)) } }
                var chain = /* await */ evaluate(chainAST(), null, environment);
                List args = new ArrayList<>(); args.add(lhs); args.add(func); // == [lhs, func]
                result = /* await */ apply(chain, args, null, environment);
            } else {
                List args = new ArrayList<>(); args.add(lhs); // == [lhs]
                result = /* await */ apply(func, args, null, environment);
            }

        }

        return result;
    }

    boolean isFunctionLike(Object o) {
        return Utils.isFunction(o) || Functions.isLambda(o) || (o instanceof Pattern);
    }
     
     static ThreadLocal<Jsonata> current = new ThreadLocal<>();

     /**
      * Evaluate Object against input data
      * @param {Object} expr - JSONata expression
      * @param {Object} input - Input data to evaluate against
      * @param {Object} environment - Environment
      * @returns {*} Evaluated input data
      */
     /* async */ Object evaluateFunction(Symbol expr, Object input, Frame environment, Object applytoContext) {
         Object result = null;

         current.set(this);
 
         // create the procedure
         // can"t assume that expr.procedure is a lambda type directly
         // could be an expression that evaluates to a Object (e.g. variable reference, parens expr etc.
         // evaluate it generically first, then check that it is a function.  Throw error if not.
         var proc = /* await */ evaluate(expr.procedure, input, environment);
 
         if (proc == null && (expr).procedure.type == "path" && environment.lookup((String)((Infix)expr).procedure.steps.get(0).value)!=null) {
             // help the user out here if they simply forgot the leading $
             throw new JException(
                 "T1005",
                 //stack: (new Error()).stack,
                 expr.position,
                 (expr).procedure.steps.get(0).value
             );
         }
 
        List<Object> evaluatedArgs = new ArrayList();

         if (applytoContext != null) {
            evaluatedArgs.add(applytoContext);
         }
         // eager evaluation - evaluate the arguments
         for (int jj = 0; jj < expr.arguments.size(); jj++) {
             Object arg = /* await */ evaluate(expr.arguments.get(jj), input, environment);
             if(Utils.isFunction(arg) || Functions.isLambda(arg)) {
                // wrap this in a closure
                // Java: not required, already a JFunction
                //  const closure = /* async */ Object (...params) {
                //      // invoke func
                //      return /* await */ apply(arg, params, null, environment);
                //  };
                //  closure.arity = getFunctionArity(arg);

                // JFunctionCallable fc = (ctx,params) ->
                //     apply(arg, params, null, environment);

                // JFunction cl = new JFunction(fc, "<o:o>");

                //Object cl = apply(arg, params, null, environment);
                evaluatedArgs.add(arg);
             } else {
                evaluatedArgs.add(arg);
             }
         }
         // apply the procedure
         var procName = expr.procedure.type == "path" ? expr.procedure.steps.get(0).value : expr.procedure.value;

        // Error if proc is null
        if (proc==null)
            throw new JException("T1006", expr.position, procName);

         try {
             if(proc instanceof Symbol) {
                 ((Symbol)proc).token = procName;
                 ((Symbol)proc).position = expr.position;
             }
             result = /* await */ apply(proc, evaluatedArgs, input, environment);
         } catch (JException jex) {
            throw jex;
         } catch (Exception err) {
            //  if(!err.position) {
            //      // add the position field to the error
            //      err.position = expr.position;
            //  }
            //  if (!err.token) {
            //      // and the Object identifier
            //      err.token = procName;
            //  }
            //if (parser.dbg) 
            err.printStackTrace();
            throw new JException("Error calling function "+procName, expr.position); //err;
         }
         return result;
     }
 
     /**
      * Apply procedure or function
      * @param {Object} proc - Procedure
      * @param {Array} args - Arguments
      * @param {Object} input - input
      * @param {Object} environment - environment
      * @returns {*} Result of procedure
      */
    /* async */ Object apply(Object proc, Object args, Object input, Object environment) {
        var result = /* await */ applyInner(proc, args, input, environment);
        while(Functions.isLambda(result) && ((Symbol)result).thunk == true) {
            // trampoline loop - this gets invoked as a result of tail-call optimization
            // the Object returned a tail-call thunk
            // unpack it, evaluate its arguments, and apply the tail call
            var next = /* await */ evaluate(((Symbol)result).body.procedure, ((Symbol)result).input, ((Symbol)result).environment);
            if(((Symbol)result).body.procedure.type == "variable") {
                if (next instanceof Symbol) // Java: not if JFunction
                    ((Symbol)next).token = ((Symbol)result).body.procedure.value;
                }
                if (next instanceof Symbol) // Java: not if JFunction
                ((Symbol)next).position = ((Symbol)result).body.procedure.position;
                var evaluatedArgs = new ArrayList<>();
                for(var ii = 0; ii < ((Symbol)result).body.arguments.size(); ii++) {
                    evaluatedArgs.add(/* await */ evaluate(((Symbol)result).body.arguments.get(ii), ((Symbol)result).input, ((Symbol)result).environment));
            }

            result = /* await */ applyInner(next, evaluatedArgs, input, environment);
        }
        return result;
    }
 
     /**
      * Apply procedure or function
      * @param {Object} proc - Procedure
      * @param {Array} args - Arguments
      * @param {Object} input - input
      * @param {Object} environment - environment
      * @returns {*} Result of procedure
      */
     /* async */ Object applyInner(Object proc, Object args, Object input, Object environment) {
         Object result = null;
         try {
             var validatedArgs = args;
             if (proc != null) {
                 validatedArgs = validateArguments(proc, args, input);
             }
 
             if (Functions.isLambda(proc)) {
                 result = /* await */ applyProcedure(proc, validatedArgs);
             } /* FIXME: need in Java??? else if (proc && proc._jsonata_Object == true) {
                 var focus = {
                     environment: environment,
                     input: input
                 };
                 // the `focus` is passed in as the `this` for the invoked function
                 result = proc.implementation.apply(focus, validatedArgs);
                 // `proc.implementation` might be a generator function
                 // and `result` might be a generator - if so, yield
                 if (isIterable(result)) {
                     result = result.next().value;
                 }
                 if (isPromise(result)) {
                     result = /await/ result;
                 } 
             } */ else if (proc instanceof JFunction) {
                 // typically these are functions that are returned by the invocation of plugin functions
                 // the `input` is being passed in as the `this` for the invoked function
                 // this is so that functions that return objects containing functions can chain
                 // e.g. /* await */ (/* await */ $func())

                // handling special case of Javascript:
                // when calling a function with fn.apply(ctx, args) and args = [undefined]
                // Javascript will convert to undefined (without array)
                if (validatedArgs instanceof List && ((List)validatedArgs).size()==1 && ((List)validatedArgs).get(0)==null) {
                    //validatedArgs = null;
                }

                 result = ((JFunction)proc).call(input, (List)validatedArgs);
                //  if (isPromise(result)) {
                //      result = /* await */ result;
                //  }
             } else if (proc instanceof JLambda) {
                System.err.println("Lambda "+proc);
                List _args = (List)validatedArgs;
                if (proc instanceof Fn1) {
                    result = ((Fn1)proc).apply(_args.get(0));
                } else if (proc instanceof Fn2) {
                    result = ((Fn2)proc).apply(_args.get(0), _args.get(1));
                }
             } else if (proc instanceof Pattern) {
                List _res = new ArrayList<>();
                for (String s : (List<String>)validatedArgs) {
                //System.err.println("PAT "+proc+" input "+s);
                    if (((Pattern)proc).matcher(s).find()) {
                        //System.err.println("MATCH");
                        _res.add(s);
                    }
                }
                result = _res;
             } else {
                System.out.println("Proc not found "+proc);
                 throw new JException(
                     "T1006", 0
                     //stack: (new Error()).stack
                 );
             }
         } catch(JException err) {
            //  if(proc) {
            //      if (typeof err.token == "undefined" && typeof proc.token !== "undefined") {
            //          err.token = proc.token;
            //      }
            //      err.position = proc.position;
            //  }
             throw err;
         }
         return result;
     }
 
     /**
      * Evaluate lambda against input data
      * @param {Object} expr - JSONata expression
      * @param {Object} input - Input data to evaluate against
      * @param {Object} environment - Environment
      * @returns {{lambda: boolean, input: *, environment: *, arguments: *, body: *}} Evaluated input data
      */
    Object evaluateLambda(Symbol expr, Object input, Frame environment) {
        // make a Object (closure)
        var procedure = parser.new Symbol();
        
        procedure._jsonata_lambda = true;
        procedure.input = input;
        procedure.environment = environment;
        procedure.arguments = expr.arguments;
        procedure.signature = expr.signature;
        procedure.body = expr.body;
        
        if(expr.thunk == true)
                procedure.thunk = true;
        
        // procedure.apply = /* async */ function(self, args) {
        //     return /* await */ apply(procedure, args, input, !!self ? self.environment : environment);
        // };
        return procedure;
    }
 
     /**
      * Evaluate partial application
      * @param {Object} expr - JSONata expression
      * @param {Object} input - Input data to evaluate against
      * @param {Object} environment - Environment
      * @returns {*} Evaluated input data
      */
    /* async */ Object evaluatePartialApplication(Symbol expr, Object input, Frame environment) {
        // partially apply a function
        Object result = null;
        // evaluate the arguments
        var evaluatedArgs = new ArrayList<>();
        for(var ii = 0; ii < expr.arguments.size(); ii++) {
            var arg = expr.arguments.get(ii);
            if (arg.type.equals("operator") && (arg.value.equals("?"))) {
                evaluatedArgs.add(arg);
            } else {
                evaluatedArgs.add(/* await */ evaluate(arg, input, environment));
            }
        }
        // lookup the procedure
        var proc = /* await */ evaluate(expr.procedure, input, environment);
        if (proc != null && expr.procedure.type.equals("path") && environment.lookup((String)expr.procedure.steps.get(0).value)!=null) {
            // help the user out here if they simply forgot the leading $
            throw new JException("T1007",
                expr.position,
                expr.procedure.steps.get(0).value
            );
        }
        if (Functions.isLambda(proc)) {
            result = partialApplyProcedure((Symbol)proc, (List)evaluatedArgs);
        } else if (Utils.isFunction(proc)) {
            result = partialApplyNativeFunction((JFunction)proc /*.implementation*/, evaluatedArgs);
    //  } else if (typeof proc === "function") {
    //      result = partialApplyNativeFunction(proc, evaluatedArgs);
        } else {
            throw new JException("T1008",
                //stack: (new Error()).stack,
                expr.position,
                expr.procedure.type.equals("path") ? expr.procedure.steps.get(0).value : expr.procedure.value
            );
        }
        return result;
    }
 
     /**
      * Validate the arguments against the signature validator (if it exists)
      * @param {Function} signature - validator function
      * @param {Array} args - Object arguments
      * @param {*} context - context value
      * @returns {Array} - validated arguments
      */
    Object validateArguments(Object signature, Object args, Object context) {
        var validatedArgs = args;
        if (Utils.isFunction(signature)) {
            validatedArgs = ((JFunction)signature).validate(args, context);
        } else if (Functions.isLambda(signature)) {
            Signature sig = ((Signature) ((Symbol)signature).signature);
            if (sig != null)
                validatedArgs = sig.validate(args, context);
        }
        return validatedArgs;
    }
 
     /**
      * Apply procedure
      * @param {Object} proc - Procedure
      * @param {Array} args - Arguments
      * @returns {*} Result of procedure
      */
    /* async */ Object applyProcedure(Object _proc, Object _args) {
        List args = (List)_args;
        Symbol proc = (Symbol)_proc;
        Object result = null;
        var env = createFrame(proc.environment);
        for (int i=0; i<proc.arguments.size(); i++) {
            if (i>=args.size()) break;
            env.bind(""+proc.arguments.get(i).value, args.get(i));
        }
        if (proc.body instanceof Symbol) {
            result = evaluate(proc.body, proc.input, env);
        } else throw new Error("Cannot execute procedure: "+proc+" "+proc.body);
        //  if (typeof proc.body === "function") {
        //      // this is a lambda that wraps a native Object - generated by partially evaluating a native
        //      result = /* await */ applyNativeFunction(proc.body, env);
        return result;
    }
 
     /**
      * Partially apply procedure
      * @param {Object} proc - Procedure
      * @param {Array} args - Arguments
      * @returns {{lambda: boolean, input: *, environment: {bind, lookup}, arguments: Array, body: *}} Result of partially applied procedure
      */
    Object partialApplyProcedure(Symbol proc, List<Symbol> args) {
        // create a closure, bind the supplied parameters and return a Object that takes the remaining (?) parameters
        // Note Uli: if no env, bind to default env so the native functions can be found
        var env = createFrame(proc.environment!=null ? proc.environment : this.environment);
        var unboundArgs = new ArrayList<Symbol>();
        int index = 0;
        for (var param : proc.arguments) {
//         proc.arguments.forEach(Object (param, index) {
            Object arg = index<args.size() ? args.get(index) : null;
            if ((arg==null) || (arg instanceof Symbol && ("operator".equals(((Symbol)arg).type) && "?".equals(((Symbol)arg).value)))) {
                unboundArgs.add(param);
            } else {
                env.bind((String)param.value, arg);
            }
            index++;
        }
        var procedure = parser.new Symbol();
        procedure._jsonata_lambda = true;
        procedure.input = proc.input;
        procedure.environment = env;
        procedure.arguments = unboundArgs;
        procedure.body = proc.body;

        return procedure;
    }
 
     /**
      * Partially apply native function
      * @param {Function} native - Native function
      * @param {Array} args - Arguments
      * @returns {{lambda: boolean, input: *, environment: {bind, lookup}, arguments: Array, body: *}} Result of partially applying native function
      */
    Object partialApplyNativeFunction(JFunction _native, List args) {
        // create a lambda Object that wraps and invokes the native function
        // get the list of declared arguments from the native function
        // this has to be picked out from the toString() value


        //var body = "function($a,$c) { $substring($a,0,$c) }";

        List sigArgs = new ArrayList<>();
        List partArgs = new ArrayList<>();
        for (int i=0; i<_native.getNumberOfArgs(); i++) {
            String argName = "$" + (char)('a'+i);
            sigArgs.add(argName);
            if (i>=args.size() || args.get(i)==null)
                partArgs.add(argName);
            else
                partArgs.add(args.get(i));
        }

        var body = "function(" + String.join(", ", sigArgs) + "){";
        body += "$"+_native.functionName+"("+String.join(", ", sigArgs) + ") }";

        if (parser.dbg) System.out.println("partial trampoline = "+body);

        //  var sigArgs = getNativeFunctionArguments(_native);
        //  sigArgs = sigArgs.stream().map(sigArg -> {
        //      return "$" + sigArg;
        //  }).toList();
        //  var body = "function(" + String.join(", ", sigArgs) + "){ _ }";

        var bodyAST = parser.parse(body);
        //bodyAST.body = _native;

        var partial = partialApplyProcedure(bodyAST, (List)args);
        return partial;
    }
 
     /**
      * Apply native function
      * @param {Object} proc - Procedure
      * @param {Object} env - Environment
      * @returns {*} Result of applying native function
      */
    /* async */ Object applyNativeFunction(JFunction proc, Frame env) {
        // Not called in Java - JFunction call directly calls native function
        return null;
    }
 
     /**
      * Get native Object arguments
      * @param {Function} func - Function
      * @returns {*|Array} Native Object arguments
      */
    List getNativeFunctionArguments(JFunction func) {
        // Not called in Java
        return null;
    }
 
     /**
      * Creates a Object definition
      * @param {Function} func - Object implementation in Javascript
      * @param {string} signature - JSONata Object signature definition
      * @returns {{implementation: *, signature: *}} Object definition
      */
    static JFunction defineFunction(String func, String signature) {
        return defineFunction(func, signature, func);
    }
    static JFunction defineFunction(String func, String signature, String funcImplMethod) {
        JFunction fn = new JFunction(func, signature, Functions.class, null, funcImplMethod);
        staticFrame.bind(func, fn);
        return fn;
    }

    public static JFunction function(String name, String signature, Class clazz, Object instance, String methodName) {
        return new JFunction(name, signature, clazz, instance, methodName);
    }

    public static<A,B,R> JFunction function(String name, FnVarArgs<R> func, String signature) {
        return new JFunction(func.getJFunctionCallable(), signature);
    }
    public static<A,R> JFunction function(String name, Fn0<R> func, String signature) {
        return new JFunction(func.getJFunctionCallable(), signature);
    }
    public static<A,B,R> JFunction function(String name, Fn1<A,R> func, String signature) {
        return new JFunction(func.getJFunctionCallable(), signature);
    }
    public static<A,B,R> JFunction function(String name, Fn2<A,B,R> func, String signature) {
        return new JFunction(func.getJFunctionCallable(), signature);
    }
    public static<A,B,C,R> JFunction function(String name, Fn3<A,B,C,R> func, String signature) {
        return new JFunction(func.getJFunctionCallable(), signature);
    }
    public static<A,B,C,D,R> JFunction function(String name, Fn4<A,B,C,D,R> func, String signature) {
        return new JFunction(func.getJFunctionCallable(), signature);
    }
    public static<A,B,C,D,E,R> JFunction function(String name, Fn5<A,B,C,D,E,R> func, String signature) {
        return new JFunction(func.getJFunctionCallable(), signature);
    }
    public static<A,B,C,D,E,F,R> JFunction function(String name, Fn6<A,B,C,D,E,F,R> func, String signature) {
        return new JFunction(func.getJFunctionCallable(), signature);
    }
    public static<A,B,C,D,E,F,G,R> JFunction function(String name, Fn7<A,B,C,D,E,F,G,R> func, String signature) {
        return new JFunction(func.getJFunctionCallable(), signature);
    }
    public static<A,B,C,D,E,F,G,H,R> JFunction function(String name, Fn8<A,B,C,D,E,F,G,H,R> func, String signature) {
        return new JFunction(func.getJFunctionCallable(), signature);
    }

     /**
      * parses and evaluates the supplied expression
      * @param {string} expr - expression to evaluate
      * @returns {*} - result of evaluating the expression
      */
     /* async */ 
     //Object functionEval(String expr, Object focus) {
        // moved to Functions !
     //}
 
     /**
      * Clones an object
      * @param {Object} arg - object to clone (deep copy)
      * @returns {*} - the cloned object
      */
     //Object functionClone(Object arg) {
        // moved to Functions !
     //}
 
     /**
      * Create frame
      * @param {Object} enclosingEnvironment - Enclosing environment
      * @returns {{bind: bind, lookup: lookup}} Created frame
      */
    public Frame createFrame() { return createFrame(null); }
    public Frame createFrame(Frame enclosingEnvironment) {
        return new Frame(enclosingEnvironment);

        // The following logic is in class Frame:
        //  var bindings = {};
        //  return {
        //      bind: Object (name, value) {
        //          bindings[name] = value;
        //      },
        //      lookup: Object (name) {
        //          var value;
        //          if(bindings.hasOwnProperty(name)) {
        //              value = bindings[name];
        //          } else if (enclosingEnvironment) {
        //              value = enclosingEnvironment.lookup(name);
        //          }
        //          return value;
        //      },
        //      timestamp: enclosingEnvironment ? enclosingEnvironment.timestamp : null,
        //      async: enclosingEnvironment ? enclosingEnvironment./* async */ : false,
        //      isParallelCall: enclosingEnvironment ? enclosingEnvironment.isParallelCall : false,
        //      global: enclosingEnvironment ? enclosingEnvironment.global : {
        //          ancestry: [ null ]
        //      }
        //  };
    }

    public static interface JLambda {
    }

    public static interface FnVarArgs<R> extends JLambda, Function<List<?>, R> {
        default JFunctionCallable getJFunctionCallable() {
            return (input, args) -> {
                    return apply((List<?>) args);
            };
        }
    }
    public static interface Fn0<R> extends JLambda, Supplier<R> {
        default JFunctionCallable getJFunctionCallable() {
            return (input, args) -> get();
        }
    }
    public static interface Fn1<A,R> extends JLambda, Function<A,R> {
        default JFunctionCallable getJFunctionCallable() {
            return (input, args) -> apply((A) args.get(0));
        }
    }
    public static interface Fn2<A,B,R> extends JLambda, BiFunction<A,B,R> {
        default JFunctionCallable getJFunctionCallable() {
            return (input, args) -> apply((A) args.get(0), (B) args.get(1));
        }
    }
    public static interface Fn3<A,B,C,R> extends JLambda {
        R apply(A a, B b, C c);
        default JFunctionCallable getJFunctionCallable() {
            return (input, args) -> apply((A) args.get(0), (B) args.get(1),
                (C) args.get(2));
        }
    }
    public static interface Fn4<A,B,C,D,R> extends JLambda {
        R apply(A a, B b, C c, D d);
        default JFunctionCallable getJFunctionCallable() {
            return (input, args) -> apply((A) args.get(0), (B) args.get(1),
                (C) args.get(2), (D) args.get(3));
        }
    }
    public static interface Fn5<A,B,C,D,E,R> extends JLambda {
        R apply(A a, B b, C c, D d, E e);
        default JFunctionCallable getJFunctionCallable() {
            return (input, args) -> apply((A) args.get(0), (B) args.get(1),
                (C) args.get(2), (D) args.get(3), (E) args.get(4));
        }
    }
    public static interface Fn6<A,B,C,D,E,F,R> extends JLambda {
        R apply(A a, B b, C c, D d, E e, F f);
        default JFunctionCallable getJFunctionCallable() {
            return (input, args) -> apply((A) args.get(0), (B) args.get(1),
                (C) args.get(2), (D) args.get(3), (E) args.get(4),
                (F) args.get(5));
        }
    }
    public static interface Fn7<A,B,C,D,E,F,G,R> extends JLambda {
        R apply(A a, B b, C c, D d, E e, F f, G g);
        default JFunctionCallable getJFunctionCallable() {
            return (input, args) -> apply((A) args.get(0), (B) args.get(1),
                (C) args.get(2), (D) args.get(3), (E) args.get(4),
                (F) args.get(5), (G) args.get(6));
        }
    }
    public static interface Fn8<A,B,C,D,E,F,G,H,R> extends JLambda {
        R apply(A a, B b, C c, D d, E e, F f, G g, H h);
        default JFunctionCallable getJFunctionCallable() {
            return (input, args) -> apply((A) args.get(0), (B) args.get(1),
                (C) args.get(2), (D) args.get(3), (E) args.get(4),
                (F) args.get(5), (G) args.get(6), (H) args.get(7));
        }
    }

    /**
     * JFunction callable Lambda interface
     */
    public static interface JFunctionCallable {
        Object call(Object input, List args) throws Throwable;
    }

    public static interface JFunctionSignatureValidation {
        Object validate(Object args, Object context);
    }

    /**
     * JFunction definition class
     */
    public static class JFunction implements JFunctionCallable, JFunctionSignatureValidation {
        JFunctionCallable function;
        String functionName;
        Signature signature;
        Method method;
        Object methodInstance;

        public JFunction(JFunctionCallable function, String signature) {
            this.function = function;
            if (signature!=null)
                this.signature = new Signature(signature, function.getClass().getName());
        }

        public JFunction(String functionName, String signature, Class clz, Object instance, String implMethodName) {
            this.functionName = functionName;
            this.signature = new Signature(signature, functionName);
            this.method = Functions.getFunction(clz, implMethodName);
            this.methodInstance = instance;
            if (method==null) {
                System.err.println("Function not implemented: "+functionName+" impl="+implMethodName);
            }
        }

        @Override
        public Object call(Object input, List args) {
            try {
                if (function!=null) {
                    return function.call(input, args);
                } else {
                    return Functions.call(methodInstance, method, args);
                }
            } catch (JException e) {
                throw e;
            } catch (Throwable e) {
                throw new JException(e, "T0410", -1, args, functionName);
            }
        }

        @Override
        public Object validate(Object args, Object context) {
            if (signature!=null)
                return signature.validate(args, context);
            else
                return args;
        }

        public int getNumberOfArgs() {
            return method.getParameterTypes().length;
        }
    }

     // Function registration
    static void registerFunctions() {
        defineFunction("sum", "<a<n>:n>");
        defineFunction("count", "<a:n>");
        defineFunction("max", "<a<n>:n>");
        defineFunction("min", "<a<n>:n>");
        defineFunction("average", "<a<n>:n>");
        defineFunction("string", "<x-b?:s>");
        defineFunction("substring", "<s-nn?:s>");
        defineFunction("substringBefore", "<s-s:s>");
        defineFunction("substringAfter", "<s-s:s>");
        defineFunction("lowercase", "<s-:s>");
        defineFunction("uppercase", "<s-:s>");
        defineFunction("length", "<s-:n>");
        defineFunction("trim", "<s-:s>");
        defineFunction("pad", "<s-ns?:s>");
        defineFunction("match", "<s-f<s:o>n?:a<o>>");
        defineFunction("contains", "<s-(sf):b>"); // TODO <s-(sf<s:o>):b>
        defineFunction("replace", "<s-(sf)(sf)n?:s>"); // TODO <s-(sf<s:o>)(sf<o:s>)n?:s>
        defineFunction("split", "<s-(sf)n?:a<s>>"); // TODO <s-(sf<s:o>)n?:a<s>>
        defineFunction("join", "<a<s>s?:s>");
        defineFunction("formatNumber", "<n-so?:s>");
        defineFunction("formatBase", "<n-n?:s>");
        defineFunction("formatInteger", "<n-s:s>");
        defineFunction("parseInteger", "<s-s:n>");
        defineFunction("number", "<(nsb)-:n>");
        defineFunction("floor", "<n-:n>");
        defineFunction("ceil", "<n-:n>");
        defineFunction("round", "<n-n?:n>");
        defineFunction("abs", "<n-:n>");
        defineFunction("sqrt", "<n-:n>");
        defineFunction("power", "<n-n:n>");
        defineFunction("random", "<:n>");
        defineFunction("boolean", "<x-:b>", "toBoolean");
        defineFunction("not", "<x-:b>");
        defineFunction("map", "<af>");
        defineFunction("zip", "<a+>");
        defineFunction("filter", "<af>");
        defineFunction("single", "<af?>");
        defineFunction("reduce", "<afj?:j>", "foldLeft"); // TODO <f<jj:j>a<j>j?:j>
        defineFunction("sift", "<o-f?:o>");
        defineFunction("keys", "<x-:a<s>>");
        defineFunction("lookup", "<x-s:x>");
        defineFunction("append", "<xx:a>");
        defineFunction("exists", "<x:b>");
        defineFunction("spread", "<x-:a<o>>");
        defineFunction("merge", "<a<o>:o>");
        defineFunction("reverse", "<a:a>");
        defineFunction("each", "<o-f:a>");
        defineFunction("error", "<s?:x>");
        defineFunction("assert", "<bs?:x>", "assertFn");
        defineFunction("type", "<x:s>");
        defineFunction("sort", "<af?:a>");
        defineFunction("shuffle", "<a:a>");
        defineFunction("distinct", "<x:x>");
        defineFunction("base64encode", "<s-:s>");
        defineFunction("base64decode", "<s-:s>");
        defineFunction("encodeUrlComponent", "<s-:s>");
        defineFunction("encodeUrl", "<s-:s>");
        defineFunction("decodeUrlComponent", "<s-:s>");
        defineFunction("decodeUrl", "<s-:s>");
        defineFunction("eval", "<sx?:x>", "functionEval");
        defineFunction("toMillis", "<s-s?:n>", "dateTimeToMillis");
        defineFunction("fromMillis", "<n-s?s?:s>", "dateTimeFromMillis");
        defineFunction("clone", "<(oa)-:o>", "functionClone");

        defineFunction("now", "<s?s?:s>");
        defineFunction("millis", "<:n>");

        //  environment.bind("now", defineFunction(function(picture, timezone) {
        //      return datetime.fromMillis(timestamp.getTime(), picture, timezone);
        //  }, "<s?s?:s>"));
        //  environment.bind("millis", defineFunction(function() {
        //      return timestamp.getTime();
        //  }, "<:n>"));

    }

     /**
      * Error codes
      *
      * Sxxxx    - Static errors (compile time)
      * Txxxx    - Type errors
      * Dxxxx    - Dynamic errors (evaluate time)
      *  01xx    - tokenizer
      *  02xx    - parser
      *  03xx    - regex parser
      *  04xx    - Object signature parser/evaluator
      *  10xx    - evaluator
      *  20xx    - operators
      *  3xxx    - functions (blocks of 10 for each function)
      */
     static HashMap<String,String> errorCodes = new HashMap<>() {{
        put("S0101", "String literal must be terminated by a matching quote");
        put("S0102", "Number out of range: {{token}}");
        put("S0103", "Unsupported escape sequence: \\{{token}}");
        put("S0104", "The escape sequence \\u must be followed by 4 hex digits");
        put("S0105", "Quoted property name must be terminated with a backquote (`)");
        put("S0106", "Comment has no closing tag");
        put("S0201", "Syntax error: {{token}}");
        put("S0202", "Expected {{value}}, got {{token}}");
        put("S0203", "Expected {{value}} before end of expression");
        put("S0204", "Unknown operator: {{token}}");
        put("S0205", "Unexpected token: {{token}}");
        put("S0206", "Unknown expression type: {{token}}");
        put("S0207", "Unexpected end of expression");
        put("S0208", "Parameter {{value}} of Object definition must be a variable name (start with $)");
        put("S0209", "A predicate cannot follow a grouping expression in a step");
        put("S0210", "Each step can only have one grouping expression");
        put("S0211", "The symbol {{token}} cannot be used as a unary operator");
        put("S0212", "The left side of := must be a variable name (start with $)");
        put("S0213", "The literal value {{value}} cannot be used as a step within a path expression");
        put("S0214", "The right side of {{token}} must be a variable name (start with $)");
        put("S0215", "A context variable binding must precede any predicates on a step");
        put("S0216", "A context variable binding must precede the \"order-by\" clause on a step");
        put("S0217", "The object representing the \"parent\" cannot be derived from this expression");
        put("S0301", "Empty regular expressions are not allowed");
        put("S0302", "No terminating / in regular expression");
        put("S0402", "Choice groups containing parameterized types are not supported");
        put("S0401", "Type parameters can only be applied to functions and arrays");
        put("S0500", "Attempted to evaluate an expression containing syntax error(s)");
        put("T0410", "Argument {{index}} of Object {{token}} does not match Object signature");
        put("T0411", "Context value is not a compatible type with argument {{index}} of Object {{token}}");
        put("T0412", "Argument {{index}} of Object {{token}} must be an array of {{type}}");
        put("D1001", "Number out of range: {{value}}");
        put("D1002", "Cannot negate a non-numeric value: {{value}}");
        put("T1003", "Key in object structure must evaluate to a string; got: {{value}}");
        put("D1004", "Regular expression matches zero length string");
        put("T1005", "Attempted to invoke a non-function. Did you mean ${{{token}}}?");
        put("T1006", "Attempted to invoke a non-function");
        put("T1007", "Attempted to partially apply a non-function. Did you mean ${{{token}}}?");
        put("T1008", "Attempted to partially apply a non-function");
        put("D1009", "Multiple key definitions evaluate to same key: {{value}}");
        put("T1010", "The matcher Object argument passed to Object {{token}} does not return the correct object structure");
        put("T2001", "The left side of the {{token}} operator must evaluate to a number");
        put("T2002", "The right side of the {{token}} operator must evaluate to a number");
        put("T2003", "The left side of the range operator (..) must evaluate to an integer");
        put("T2004", "The right side of the range operator (..) must evaluate to an integer");
        put("D2005", "The left side of := must be a variable name (start with $)");  // defunct - replaced by S0212 parser error
        put("T2006", "The right side of the Object application operator ~> must be a function");
        put("T2007", "Type mismatch when comparing values {{value}} and {{value2}} in order-by clause");
        put("T2008", "The expressions within an order-by clause must evaluate to numeric or string values");
        put("T2009", "The values {{value}} and {{value2}} either side of operator {{token}} must be of the same data type");
        put("T2010", "The expressions either side of operator {{token}} must evaluate to numeric or string values");
        put("T2011", "The insert/update clause of the transform expression must evaluate to an object: {{value}}");
        put("T2012", "The delete clause of the transform expression must evaluate to a string or array of strings: {{value}}");
        put("T2013", "The transform expression clones the input object using the $clone() function.  This has been overridden in the current scope by a non-function.");
        put("D2014", "The size of the sequence allocated by the range operator (..) must not exceed 1e6.  Attempted to allocate {{value}}.");
        put("D3001", "Attempting to invoke string Object on Infinity or NaN");
        put("D3010", "Second argument of replace Object cannot be an empty string");
        put("D3011", "Fourth argument of replace Object must evaluate to a positive number");
        put("D3012", "Attempted to replace a matched string with a non-string value");
        put("D3020", "Third argument of split Object must evaluate to a positive number");
        put("D3030", "Unable to cast value to a number: {{value}}");
        put("D3040", "Third argument of match Object must evaluate to a positive number");
        put("D3050", "The second argument of reduce Object must be a Object with at least two arguments");
        put("D3060", "The sqrt Object cannot be applied to a negative number: {{value}}");
        put("D3061", "The power Object has resulted in a value that cannot be represented as a JSON number: base={{value}}, exponent={{exp}}");
        put("D3070", "The single argument form of the sort Object can only be applied to an array of strings or an array of numbers.  Use the second argument to specify a comparison function");
        put("D3080", "The picture string must only contain a maximum of two sub-pictures");
        put("D3081", "The sub-picture must not contain more than one instance of the \"decimal-separator\" character");
        put("D3082", "The sub-picture must not contain more than one instance of the \"percent\" character");
        put("D3083", "The sub-picture must not contain more than one instance of the \"per-mille\" character");
        put("D3084", "The sub-picture must not contain both a \"percent\" and a \"per-mille\" character");
        put("D3085", "The mantissa part of a sub-picture must contain at least one character that is either an \"optional digit character\" or a member of the \"decimal digit family\"");
        put("D3086", "The sub-picture must not contain a passive character that is preceded by an active character and that is followed by another active character");
        put("D3087", "The sub-picture must not contain a \"grouping-separator\" character that appears adjacent to a \"decimal-separator\" character");
        put("D3088", "The sub-picture must not contain a \"grouping-separator\" at the end of the integer part");
        put("D3089", "The sub-picture must not contain two adjacent instances of the \"grouping-separator\" character");
        put("D3090", "The integer part of the sub-picture must not contain a member of the \"decimal digit family\" that is followed by an instance of the \"optional digit character\"");
        put("D3091", "The fractional part of the sub-picture must not contain an instance of the \"optional digit character\" that is followed by a member of the \"decimal digit family\"");
        put("D3092", "A sub-picture that contains a \"percent\" or \"per-mille\" character must not contain a character treated as an \"exponent-separator\"");
        put("D3093", "The exponent part of the sub-picture must comprise only of one or more characters that are members of the \"decimal digit family\"");
        put("D3100", "The radix of the formatBase Object must be between 2 and 36.  It was given {{value}}");
        put("D3110", "The argument of the toMillis Object must be an ISO 8601 formatted timestamp. Given {{value}}");
        put("D3120", "Syntax error in expression passed to Object eval: {{value}}");
        put("D3121", "Dynamic error evaluating the expression passed to Object eval: {{value}}");
        put("D3130", "Formatting or parsing an integer as a sequence starting with {{value}} is not supported by this implementation");
        put("D3131", "In a decimal digit pattern, all digits must be from the same decimal group");
        put("D3132", "Unknown component specifier {{value}} in date/time picture string");
        put("D3133", "The \"name\" modifier can only be applied to months and days in the date/time picture string, not {{value}}");
        put("D3134", "The timezone integer format specifier cannot have more than four digits");
        put("D3135", "No matching closing bracket \"]\" in date/time picture string");
        put("D3136", "The date/time picture string is missing specifiers required to parse the timestamp");
        put("D3137", "{{{message}}}");
        put("D3138", "The $single() Object expected exactly 1 matching result.  Instead it matched more.");
        put("D3139", "The $single() Object expected exactly 1 matching result.  Instead it matched 0.");
        put("D3140", "Malformed URL passed to ${{{functionName}}}(): {{value}}");
        put("D3141", "{{{message}}}");
     }};
 
     /**
      * lookup a message template from the catalog and substitute the inserts.
      * Populates `err.message` with the substituted message. Leaves `err.message`
      * untouched if code lookup fails.
      * @param {string} err - error code to lookup
      * @returns {undefined} - `err` is modified in place
      */
     Exception populateMessage(Exception err) {
        //  var template = errorCodes[err.code];
        //  if(typeof template !== "undefined") {
        //      // if there are any handlebars, replace them with the field references
        //      // triple braces - replace with value
        //      // double braces - replace with json stringified value
        //      var message = template.replace(/\{\{\{([^}]+)}}}/g, function() {
        //          return err[arguments[1]];
        //      });
        //      message = message.replace(/\{\{([^}]+)}}/g, function() {
        //          return JSON.stringify(err[arguments[1]]);
        //      });
        //      err.message = message;
        //  }
         // Otherwise retain the original `err.message`
         return err;
     }
 
    List<Exception> errors;
    Frame environment;
    Symbol ast;
    long timestamp;
    Object input;

    static {
        staticFrame = new Frame(null);
        registerFunctions();
    }

     /**
      * JSONata
      * @param {Object} expr - JSONata expression
      * @param {Object} options
      * @param {boolean} options.recover: attempt to recover on parse error
      * @param {Function} options.RegexEngine: RegEx class constructor to use
      * @returns {{evaluate: evaluate, assign: assign}} Evaluated expression
      */
    public Jsonata(String expr, boolean optionsRecover) {
        try {
            ast = parser.parse(expr);//, optionsRecover);
            errors = ast.errors;
            ast.errors = null; //delete ast.errors;
        } catch(JException err) {
            // insert error message into structure
            //populateMessage(err); // possible side-effects on `err`
            throw err;
        }
        environment = createFrame(staticFrame);

        timestamp = System.currentTimeMillis(); // will be overridden on each call to evalute()

        // Note: now and millis are implemented in Functions
        //  environment.bind("now", defineFunction(function(picture, timezone) {
        //      return datetime.fromMillis(timestamp.getTime(), picture, timezone);
        //  }, "<s?s?:s>"));
        //  environment.bind("millis", defineFunction(function() {
        //      return timestamp.getTime();
        //  }, "<:n>"));

        // FIXME: options.RegexEngine not impl
        //  if(options && options.RegexEngine) {
        //      jsonata.RegexEngine = options.RegexEngine;
        //  } else {
        //      jsonata.RegexEngine = RegExp;
        //  }
    }

    /* async */
    public Object evaluate(Object input, Frame bindings) { // FIXME:, callback) {
        // throw if the expression compiled with syntax errors
        if(errors != null) {
            throw new JException("S0500", 0);
        }

        Frame exec_env;
        if (bindings != null) {
            //var exec_env;
            // the variable bindings have been passed in - create a frame to hold these
            exec_env = createFrame(environment);
            for (var v : bindings.bindings.keySet()) {
                exec_env.bind(v, bindings.lookup(v));
            }
        } else {
            exec_env = environment;
        }
        // put the input document into the environment as the root object
        exec_env.bind("$", input);

        // capture the timestamp and put it in the execution environment
        // the $now() and $millis() functions will return this value - whenever it is called
        //timestamp = new Date();
        //exec_env.timestamp = timestamp;

        // if the input is a JSON array, then wrap it in a singleton sequence so it gets treated as a single input
        if((input instanceof List) && !Utils.isSequence(input)) {
            input = Utils.createSequence(input);
            ((JList)input).outerWrapper = true;
        }

        Object it;
        try {
            it = /* await */ evaluate(ast, input, exec_env);
        //  if (typeof callback === "function") {
        //      callback(null, it);
        //  }
            return it;
        } catch (Exception err) {
            // insert error message into structure
            populateMessage(err); // possible side-effects on `err`
            throw err;
        }
    }
    
    public void assign(String name, Object value) {
                 environment.bind(name, value);
    }
    
    public void registerFunction(String name, Function implementation, String signature) {
        throw new Error("not implemented");
            //      var func = defineFunction(implementation, signature);
            //      environment.bind(name, func);
            //  },
            //  ast: function() {
            //      return ast;
            //  },
    }

    public List<Exception> getErrors() {
        return errors;
    }
 
    static Parser parser = new Parser();
//      jsonata.parser = parser; // TODO remove this in a future release - use ast() instead
 
//      return jsonata;
 
//  })();
 
//  module.exports = jsonata;
}
