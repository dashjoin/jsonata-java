/**
 * © Copyright IBM Corp. 2016, 2017 All Rights Reserved
 *   Project name: JSONata
 *   This project is licensed under the MIT License, see LICENSE
 */

package com.dashjoin.jsonata;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.Function;

import javax.swing.JEditorPane;

import com.dashjoin.jsonata.Parser.Infix;
import com.dashjoin.jsonata.Parser.Symbol;
import com.dashjoin.jsonata.Utils.JList;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @module JSONata
 * @description JSON query and transformation language
 */
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
 
     public static class Frame {
        Map<String, Object> bindings = new TreeMap<String,Object>();

        Frame parent;

        public Frame(Frame enclosingEnvironment) {
            parent = enclosingEnvironment;
        }

        public void bind(String name, Object val) {
            bindings.put(name, val);
        }

        public Object lookup(String name) {
            Object res = bindings.get(name);
            if (res!=null)
                return res;
            if (parent!=null)
                return parent.lookup(name);
            return null;
        }
     }

    Frame staticFrame;// = createFrame(null);
 
     /**
      * Evaluate expression against input data
      * @param {Object} expr - JSONata expression
      * @param {Object} input - Input data to evaluate against
      * @param {Object} environment - Environment
     * @throws JException
      * @returns {*} Evaluated input data
      */
     Object evaluate(Symbol expr, Object input, Frame environment) throws JException {
        Object result = null;
 
        System.out.println("eval expr="+expr);//+" input="+input);

        //  var entryCallback = environment.lookup("__evaluate_entry");
        //  if(entryCallback) {
        //      await entryCallback(expr, input, environment);
        //  }
 
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
                 System.out.println("evalName "+result);
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
                 result = null; // FIXME environment.lookup(expr.slot.label);
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
 

        // FIXME predicate
        if (expr.predicate!=null)
            for(var ii = 0; ii < expr.predicate.size(); ii++) {
                 result = /* await */ evaluateFilter(expr.predicate.get(ii).expr, result, environment);
             }

        //System.out.println("FILTER "+expr.predicate.get(0).type);
        //  if (Object.prototype.hasOwnProperty.call(expr, "predicate")) {
        //      for(var ii = 0; ii < expr.predicate.length; ii++) {
        //          result = /* await */ evaluateFilter(expr.predicate[ii].expr, result, environment);
        //      }
        //  }
 
        // FIXME path
        //  if (expr.type !== "path" && Object.prototype.hasOwnProperty.call(expr, "group")) {
        //      result = /* await */ evaluateGroupExpression(expr.group, result, environment);
        //  }
 
        //  var exitCallback = environment.lookup("__evaluate_exit");
        //  if(exitCallback) {
        //      /* await */ exitCallback(expr, input, environment, result);
        //  }

        
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
     * @throws JException
      * @returns {*} Evaluated input data
      */
     /* async */ Object evaluatePath(Symbol expr, Object input, Frame environment) throws JException {
         Object inputSequence;
         // expr is an array of steps
         // if the first step is a variable reference ($...), including root reference ($$),
         //   then the path is absolute rather than relative
         if (input instanceof List && !expr.steps.get(0).type.equals("variable")) {
             inputSequence = input;
         } else {
             // if input is not an array, make it so
            inputSequence = Utils.createSequence(input);
         }
 
         Object resultSequence = null;
         var isTupleStream = false;
         Object tupleBindings = null;
 
         // evaluate each step in turn
         for(var ii = 0; ii < expr.steps.size(); ii++) {
             var step = expr.steps.get(ii);
 
             if(step.tuple!=null) {
                 isTupleStream = true;
             }
 
             // if the first step is an explicit array constructor, then just evaluate that (i.e. don"t iterate over a context array)
             if(ii == 0 && step.consarray) {
                 resultSequence = /* await */ evaluate(step, inputSequence, environment);
             } else {
                 if(isTupleStream) {
                     tupleBindings = /* await */ evaluateTupleStep(step, inputSequence, tupleBindings, environment);
                 } else {
                     resultSequence = /* await */ evaluateStep(step, inputSequence, environment, ii == expr.steps.size() - 1);
                 }
             }
 
             if (!isTupleStream && (resultSequence == null || ((List)resultSequence).size() == 0)) {
                 break;
             }
 
            if(step.focus == null) {
                  inputSequence = resultSequence;
            }
 
         }
 
        //  if(isTupleStream) {
        //      if(expr.tuple!=null) {
        //          // tuple stream is carrying ancestry information - keep this
        //          resultSequence = tupleBindings;
        //      } else {
        //          resultSequence = Utils.createSequence();
        //          for (int ii = 0; ii < tupleBindings.size(); ii++) {
        //              resultSequence.add(tupleBindings.get(ii)["@"]);
        //          }
        //      }
        //  }
 
        if(expr.keepSingletonArray) {
            // if the array is explicitly constructed in the expression and marked to promote singleton sequences to array
            if((resultSequence instanceof JList) && ((JList)resultSequence).cons && !((JList)resultSequence).sequence) {
                resultSequence = Utils.createSequence(resultSequence);
            }
            ((JList)resultSequence).keepSingleton = true;
        }
 
        //  if (expr.hasOwnProperty("group")) {
        //      resultSequence = /* await */ evaluateGroupExpression(expr.group, isTupleStream ? tupleBindings : resultSequence, environment)
        //  }
 
         return resultSequence;
     }
 
     Frame createFrameFromTuple(Frame environment, Object tuple) {
        var frame = createFrame(environment);
        //  for(const prop in tuple) {
        //      frame.bind(prop, tuple[prop]);
        //  }
        //  return frame;
        return frame;
     }
 
     /**
      * Evaluate a step within a path
      * @param {Object} expr - JSONata expression
      * @param {Object} input - Input data to evaluate against
      * @param {Object} environment - Environment
      * @param {boolean} lastStep - flag the last step in a path
     * @throws JException
      * @returns {*} Evaluated input data
      */
     /* async */ Object evaluateStep(Symbol expr, Object input, Frame environment, boolean lastStep) throws JException {
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
 
     /* async */ Object evaluateStages(List<Symbol> stages, Object input, Frame environment) throws JException {
         var result = input;
         for(var ss = 0; ss < stages.size(); ss++) {
             var stage = stages.get(ss);
             switch(stage.type) {
                 case "filter":
                     result = /* await */ evaluateFilter(stage.expr, result, environment);
                     break;
                 case "index":
                     for(var ee = 0; ee < ((List)result).size(); ee++) {
                        // FIXME: completely unsure if this is correct 
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
     /* async */ Object evaluateTupleStep(Symbol expr, Object input, Object tupleBindings, Frame environment) {
         Object result = null;
         if(expr.type.equals("sort")) {
             if(tupleBindings!=null) {
                 result = /* await */ evaluateSortExpression(expr, tupleBindings, environment);
             } else {
                 var sorted = /* await */ evaluateSortExpression(expr, input, environment);
                 result = Utils.createSequence();
                 ((JList)result).tupleStream = true;
                 for(var ss = 0; ss < ((List)sorted).size(); ss++) {
                     var tuple = {"@": sorted[ss]};
                     tuple[expr.index] = ss;
                     result.push(tuple);
                 }
             }
             if(expr.stages) {
                 result = /* await */ evaluateStages(expr.stages, result, environment);
             }
             return result;
         }
 
         result = Utils.createSequence();
         result.tupleStream = true;
         var stepEnv = environment;
         if(tupleBindings == null) {
             tupleBindings = input.map(item => { return {"@": item} });
         }
 
         for(var ee = 0; ee < tupleBindings.length; ee++) {
             stepEnv = createFrameFromTuple(environment, tupleBindings[ee]);
             var res = /* await */ evaluate(expr, tupleBindings[ee]["@"], stepEnv);
             // res is the binding sequence for the output tuple stream
             if (res!=null) { //(typeof res !== "undefined") {
                 if (!Array.isArray(res)) {
                     res = new Object[] {res};
                 }
                 for (var bb = 0; bb < res.length; bb++) {
                     tuple = {};
                     Object.assign(tuple, tupleBindings[ee]);
                     if(res.tupleStream) {
                         Object.assign(tuple, res[bb]);
                     } else {
                         if (expr.focus) {
                             tuple[expr.focus] = res[bb];
                             tuple["@"] = tupleBindings[ee]["@"];
                         } else {
                             tuple["@"] = res[bb];
                         }
                         if (expr.index) {
                             tuple[expr.index] = bb;
                         }
                         if (expr.ancestor) {
                             tuple[expr.ancestor.label] = tupleBindings[ee]["@"];
                         }
                     }
                     result.push(tuple);
                 }
             }
         }
 
         if(expr.stages) {
             result = /* await */ evaluateStages(expr.stages, result, environment);
         }
 
         return result;
     }
 
     /**
      * Apply filter predicate to input data
      * @param {Object} predicate - filter expression
      * @param {Object} input - Input data to apply predicates against
      * @param {Object} environment - Environment
     * @throws JException
      * @returns {*} Result after applying predicates
      */
     /* async */ Object evaluateFilter(Object _predicate, Object input, Frame environment) throws JException {
        Symbol predicate = (Symbol)_predicate;
         var results = Utils.createSequence();
        //  if( input && input.tupleStream) {
        //      results.tupleStream = true;
        //  }
         if (!(input instanceof List)) { // isArray
             input = Utils.createSequence(input);
         }
         if (predicate.type.equals("number")) {
             var index = (int)(double)predicate.value;  // round it down - was Math.floor
             if (index < 0) {
                 // count in from end of array
                 index = ((List)input).size() + index;
             }
             var item = ((List)input).get(index);
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
                //  if(input) {
                //      context = item["@"];
                //      env = createFrameFromTuple(environment, item);
                //  }
                 var res = /* await */ evaluate(predicate, context, env);
                 if (Utils.isNumeric(res)) {
                     res = Utils.createSequence(res);
                 }
                 if (Utils.isArrayOfNumbers(res)) {
                    for (Object ires : ((List)res)) {
//                     res.forEach(Object (ires) {
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
                 } else if (Functions.toBoolean(res)) { // truthy
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
     * @throws JException
      * @returns {*} Evaluated input data
      */
     /* async */ Object evaluateBinary(Symbol _expr, Object input, Frame environment) throws JException {
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

     /**
      * Evaluate unary expression against input data
      * @param {Object} expr - JSONata expression
      * @param {Object} input - Input data to evaluate against
      * @param {Object} environment - Environment
     * @throws JException
      * @returns {*} Evaluated input data
      */
     /* async */ Object evaluateUnary(Symbol expr, Object input, Frame environment) throws JException {
         Object result = null;
 
         switch ((String)""+expr.value) { // Uli was: expr.value - where is value set???
             case "-":
                 result = /* await */ evaluate(expr.expression, input, environment);
                 if (result==null) { //(typeof result === "undefined") {
                     result = UNDEFINED;
                 } else if (Utils.isNumeric(result)) {
                     result = -(double)result;
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
                result = new ArrayList<>(); // [];
                for (var item : expr.expressions) {
                    Object value = evaluate(item, input, environment);
                    if (value!=null) {
                        if ((""+item.value).equals("["))
                            ((List)result).add(value);
                        else
                            result = Functions.append(result, value);
                    }
                }
                // FIXME array constructor
                //  let generators = /* await */ Promise.all(expr.expressions
                //      .map(/* async */ (item, idx) => {
                //          environment.isParallelCall = idx > 0
                //          return [item, /* await */ evaluate(item, input, environment)]
                //      }));
                //  for (let generator of generators) {
                //      var [item, value] = generator;
                //      if (typeof value !== "undefined") {
                //          if(item.value === "[") {
                //              result.push(value);
                //          } else {
                //              result = fn.append(result, value);
                //          }
                //      }
                //  }
                //  if(expr.consarray) {
                //      Object.defineProperty(result, "cons", {
                //          enumerable: false,
                //          configurable: false,
                //          value: true
                //      });
                //  }
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
        //return ((Map)input).get(expr.value);
        return Functions.lookup(input, (String)expr.value);
        //return fn.lookup(input, expr.value);
     }
 
     /**
      * Evaluate literal against input data
      * @param {Object} expr - JSONata expression
      * @returns {*} Evaluated input data
      */
     Object evaluateLiteral(Symbol expr) {
         return expr.value;
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
 
         //        result = normalizeSequence(results);
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
 
        //  if (typeof lhs !== "undefined" && !isNumeric(lhs)) {
        //      throw {
        //          code: "T2001",
        //          stack: (new Error()).stack,
        //          value: lhs
        //      };
        //  }
        //  if (typeof rhs !== "undefined" && !isNumeric(rhs)) {
        //      throw {
        //          code: "T2002",
        //          stack: (new Error()).stack,
        //          value: rhs
        //      };
        //  }
 
        //  if (typeof lhs === "undefined" || typeof rhs === "undefined") {
        //      // if either side is undefined, the result is undefined
        //      return result;
        //  }
 
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
         return result;
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
        if (lhs instanceof Integer)
            lhs = (double)(int)lhs;
        if (rhs instanceof Integer)
            rhs = (double)(int)rhs;

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
     Object evaluateComparisonExpression(Object lhs, Object rhs, String op) throws JException {
         Object result = null;
 
         // type checks
         var ltype = lhs!=null ? lhs.getClass().getSimpleName() : null;
         var rtype = rhs!=null ? rhs.getClass().getSimpleName() : null;
        System.out.println("evalComparison "+ltype+","+rtype);
         var lcomparable = (ltype == null || ltype.equals("String") || ltype.equals("Double"));
         var rcomparable = (rtype == null || rtype.equals("String") || rtype.equals("Double"));
 
         // if either side is undefined, the result is undefined
         if (ltype == null || rtype==null) {
             return null;
         }

         // if either aa or bb are not comparable (string or numeric) values, then throw an error
         if (!(lhs instanceof Comparable) || !(rhs instanceof Comparable)) {
             throw new JException(
                "T2010",
                0, //position,
                //stack: (new Error()).stack,
                lhs!=null ? lhs : rhs
             );
         }
 
         //if aa and bb are not of the same type
         if (!ltype.equals(rtype)) {

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
 
     boolean boolize(Object value) {
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
             lstr = ""+lhs; // was fn.string
         }
         if (rhs != null) {
             rstr = ""+rhs; // was fn.string
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
     /* async */ Object evaluateGroupExpression(Symbol expr, Object _input, Frame environment) throws JException {
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
             var env = reduce ? createFrameFromTuple(environment, item) : environment;
             for(var pairIndex = 0; pairIndex < expr.lhsObject.size(); pairIndex++) {
                 var pair = expr.lhsObject.get(pairIndex);
                 var key = /* await */ evaluate(pair[0], reduce ? ((Map)item).get("@") : item, env);
                 // key has to be a string
                 if (!(key instanceof String)) {
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
        for (Entry<Object,GroupEntry> e : groups.entrySet()) {
             var entry = e.getValue();
             var context = entry.data;
             var env = environment;
             if (reduce) {
                 var tuple = reduceTupleStream(entry.data);
                 context = ((Map)tuple).get("@");
                 ((Map)tuple).remove("@");
                 env = createFrameFromTuple(environment, tuple);
             }
            // currently not used in Java: environment.isParallelCall = idx > 0
            //return [key, /* await */ evaluate(expr.lhs[entry.exprIndex][1], context, env)];
            Object res = evaluate(expr.lhsObject.get(entry.exprIndex)[1], context, env);
            result.put(e.getKey(), res);
         }
 
        //  for (let generator of generators) {
        //      var [key, value] = /* await */ generator;
        //      if(typeof value !== "undefined") {
        //          result[key] = value;
        //      }
        //  }
 
         return result;
     }
 
     Object reduceTupleStream(Object tupleStream) {
         if(!Array.isArray(tupleStream)) {
             return tupleStream;
         }
         var result = {};
         Object.assign(result, tupleStream[0]);
         for(var ii = 1; ii < tupleStream.length; ii++) {
             for(const prop in tupleStream[ii]) {
                 result[prop] = fn.append(result[prop], tupleStream[ii][prop]);
             }
         }
         return result;
     }
 
     /**
      * Evaluate range expression against input data
      * @param {Object} lhs - LHS value
      * @param {Object} rhs - RHS value
     * @throws JException
      * @returns {Array} Resultant array
      */
     Object evaluateRangeExpression(Object lhs, Object rhs) throws JException {
         Object result = null;
 
         if (lhs != null && !(lhs instanceof Number)) {
             throw new JException("T2003",
                 //stack: (new Error()).stack,
                 -1,
                 lhs
             );
         }
         if (rhs != null && !(rhs instanceof Number)) {
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
 
         int _lhs = ((Number)lhs).intValue(), _rhs = ((Number)rhs).intValue();

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
 
         result = Utils.createSequence();
         for (int item = _lhs; item <= _rhs; item++) {
             ((List)result).add(item);
         }
         //result.sequence = true;
         return result;
     }
 
     /**
      * Evaluate bind expression against input data
      * @param {Object} expr - JSONata expression
      * @param {Object} input - Input data to evaluate against
      * @param {Object} environment - Environment
     * @throws JException
      * @returns {*} Evaluated input data
      */
     /* async */ Object evaluateBindExpression(Symbol expr, Object input, Frame environment) throws JException {
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
     * @throws JException
      * @returns {*} Evaluated input data
      */
     /* async */ Object evaluateCondition(Symbol expr, Object input, Frame environment) throws JException {
         Object result = null;
         var condition = /* await */ evaluate(expr.condition, input, environment);
         if (Functions.toBoolean(condition)) {
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
     * @throws JException
      * @returns {*} Evaluated input data
      */
     /* async */ Object evaluateBlock(Symbol expr, Object input, Frame environment) throws JException {
        Object result = null;
        //Infix expr = (Infix)_expr;
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
        throw new Error("not impl");

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
            System.out.println("variable name="+expr.value+" val="+result);
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
     /* async */ Object evaluateSortExpression(Symbol expr, Object input, Object environment) {
         var result;
 
         // evaluate the lhs, then sort the results in order according to rhs expression
         var lhs = input;
         var isTupleSort = input.tupleStream ? true : false;
 
         // sort the lhs array
         // use comparator function
         var comparator = /* async */ function(a, b) { 
             // expr.terms is an array of order-by in priority order
             var comp = 0;
             for(var index = 0; comp === 0 && index < expr.terms.length; index++) {
                 var term = expr.terms[index];
                 //evaluate the sort term in the context of a
                 var context = a;
                 var env = environment;
                 if(isTupleSort) {
                     context = a["@"];
                     env = createFrameFromTuple(environment, a);
                 }
                 var aa = /* await */ evaluate(term.expression, context, env);
                 //evaluate the sort term in the context of b
                 context = b;
                 env = environment;
                 if(isTupleSort) {
                     context = b["@"];
                     env = createFrameFromTuple(environment, b);
                 }
                 var bb = /* await */ evaluate(term.expression, context, env);
 
                 // type checks
                 var atype = typeof aa;
                 var btype = typeof bb;
                 // undefined should be last in sort order
                 if(atype === "undefined") {
                     // swap them, unless btype is also undefined
                     comp = (btype === "undefined") ? 0 : 1;
                     continue;
                 }
                 if(btype === "undefined") {
                     comp = -1;
                     continue;
                 }
 
                 // if aa or bb are not string or numeric values, then throw an error
                 if(!(atype === "string" || atype === "number") || !(btype === "string" || btype === "number")) {
                     throw {
                         code: "T2008",
                         stack: (new Error()).stack,
                         position: expr.position,
                         value: !(atype === "string" || atype === "number") ? aa : bb
                     };
                 }
 
                 //if aa and bb are not of the same type
                 if(atype !== btype) {
                     throw {
                         code: "T2007",
                         stack: (new Error()).stack,
                         position: expr.position,
                         value: aa,
                         value2: bb
                     };
                 }
                 if(aa === bb) {
                     // both the same - move on to next term
                     continue;
                 } else if (aa < bb) {
                     comp = -1;
                 } else {
                     comp = 1;
                 }
                 if(term.descending === true) {
                     comp = -comp;
                 }
             }
             // only swap a & b if comp equals 1
             return comp === 1;
         };
 
         var focus = {
             environment: environment,
             input: input
         };
         // the `focus` is passed in as the `this` for the invoked function
         result = /* await */ fn.sort.apply(focus, [lhs, comparator]);
 
         return result;
     }
 
     /**
      * create a transformer function
      * @param {Object} expr - AST for operator
      * @param {Object} input - Input data to evaluate against
      * @param {Object} environment - Environment
      * @returns {*} tranformer function
      */
     Object evaluateTransformExpression(Symbol expr, Object input, Object environment) {
         // create a Object to implement the transform definition
         var transformer = /* async */ Object (obj) { // signature <(oa):o>
             // undefined inputs always return undefined
             if(typeof obj === "undefined") {
                 return undefined;
             }
 
             // this Object returns a copy of obj with changes specified by the pattern/operation
             var cloneFunction = environment.lookup("clone");
             if(!isFunction(cloneFunction)) {
                 // throw type error
                 throw {
                     code: "T2013",
                     stack: (new Error()).stack,
                     position: expr.position
                 };
             }
             var result = /* await */ apply(cloneFunction, [obj], null, environment);
             var matches = /* await */ evaluate(expr.pattern, result, environment);
             if(typeof matches !== "undefined") {
                 if(!Array.isArray(matches)) {
                     matches = [matches];
                 }
                 for(var ii = 0; ii < matches.length; ii++) {
                     var match = matches[ii];
                     // evaluate the update value for each match
                     var update = /* await */ evaluate(expr.update, match, environment);
                     // update must be an object
                     var updateType = typeof update;
                     if(updateType !== "undefined") {
                         if(updateType !== "object" || update === null || Array.isArray(update)) {
                             // throw type error
                             throw {
                                 code: "T2011",
                                 stack: (new Error()).stack,
                                 position: expr.update.position,
                                 value: update
                             };
                         }
                         // merge the update
                         for(var prop in update) {
                             match[prop] = update[prop];
                         }
                     }
 
                     // delete, if specified, must be an array of strings (or single string)
                     if(typeof expr.delete !== "undefined") {
                         var deletions = /* await */ evaluate(expr.delete, match, environment);
                         if(typeof deletions !== "undefined") {
                             var val = deletions;
                             if (!Array.isArray(deletions)) {
                                 deletions = [deletions];
                             }
                             if (!isArrayOfStrings(deletions)) {
                                 // throw type error
                                 throw {
                                     code: "T2012",
                                     stack: (new Error()).stack,
                                     position: expr.delete.position,
                                     value: val
                                 };
                             }
                             for (var jj = 0; jj < deletions.length; jj++) {
                                 if(typeof match === "object" && match !== null) {
                                     delete match[deletions[jj]];
                                 }
                             }
                         }
                     }
                 }
             }
 
             return result;
         };
 
         return defineFunction(transformer, "<(oa):o>");
     }
 
    Symbol chainAST; // = new Parser().parse("function($f, $g) { function($x){ $g($f($x)) } }");
 
    Symbol chainAST() throws JException {
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
     * @throws JException
      * @returns {*} Evaluated input data
      */
     /* async */ Object evaluateApplyExpression(Symbol expr, Object input, Frame environment) throws JException {
         Object result = null;
 
 
         var lhs = /* await */ evaluate(expr.lhs, input, environment);
         if(expr.rhs.type.equals("function")) {
            //Symbol applyTo = new Symbol(); applyTo.context = lhs;
             // this is a Object _invocation_; invoke it with lhs expression as the first argument
             result = /* await */ evaluateFunction(expr.rhs, input, environment, lhs);
         } else {
             var func = /* await */ evaluate(expr.rhs, input, environment);
 
             if(!Utils.isFunction(func)) {
                 throw new JException("T2006",
                     //stack: (new Error()).stack,
                     expr.position,
                     func
                 );
             }
 
             if(Utils.isFunction(lhs)) {
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
 
     /**
      * Evaluate Object against input data
      * @param {Object} expr - JSONata expression
      * @param {Object} input - Input data to evaluate against
      * @param {Object} environment - Environment
      * @returns {*} Evaluated input data
      */
     /* async */ Object evaluateFunction(Symbol expr, Object input, Frame environment, Object applytoContext) throws JException {
         Object result = null;
 
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
             if(Utils.isFunction(arg)) {
                 // wrap this in a closure
                //  const closure = /* async */ Object (...params) {
                //      // invoke func
                //      return /* await */ apply(arg, params, null, environment);
                //  };
                //  closure.arity = getFunctionArity(arg);
                // evaluatedArgs.push(closure);
             } else {
                 evaluatedArgs.add(arg);
             }
         }
         // apply the procedure
         var procName = expr.procedure.type == "path" ? expr.procedure.steps.get(0).value : expr.procedure.value;
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
     /* async */ Object apply(Object proc, Object args, Object input, Object environment) throws JException {
         var result = /* await */ applyInner(proc, args, input, environment);
         while(Functions.isLambda(result) && ((Symbol)result).thunk == true) {
             // trampoline loop - this gets invoked as a result of tail-call optimization
             // the Object returned a tail-call thunk
             // unpack it, evaluate its arguments, and apply the tail call
             var next = /* await */ evaluate(((Infix)result).body.procedure, ((Symbol)result).input, ((Symbol)result).environment);
             if(((Infix)result).body.procedure.type == "variable") {
                ((Symbol)next).token = ((Symbol)result).body.procedure.value;
             }
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
     /* async */ Object applyInner(Object proc, Object args, Object input, Object environment) throws JException {
         Object result = null;
         try {
             var validatedArgs = args;
             if (proc!=null) {
                 validatedArgs = args; // FIXME validateArguments(proc.signature, args, input);
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
                 result = ((JFunction)proc).call(input, validatedArgs);
                //  if (isPromise(result)) {
                //      result = /* await */ result;
                //  }
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
        var procedure = new Symbol();
        
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
     /* async */ Object evaluatePartialApplication(Symbol expr, Object input, Object environment) {
         // partially apply a function
         var result;
         // evaluate the arguments
         var evaluatedArgs = [];
         for(var ii = 0; ii < expr.arguments.length; ii++) {
             var arg = expr.arguments[ii];
             if (arg.type === "operator" && arg.value === "?") {
                 evaluatedArgs.push(arg);
             } else {
                 evaluatedArgs.push(/* await */ evaluate(arg, input, environment));
             }
         }
         // lookup the procedure
         var proc = /* await */ evaluate(expr.procedure, input, environment);
         if (typeof proc === "undefined" && expr.procedure.type === "path" && environment.lookup(expr.procedure.steps[0].value)) {
             // help the user out here if they simply forgot the leading $
             throw {
                 code: "T1007",
                 stack: (new Error()).stack,
                 position: expr.position,
                 token: expr.procedure.steps[0].value
             };
         }
         if (isLambda(proc)) {
             result = partialApplyProcedure(proc, evaluatedArgs);
         } else if (proc && proc._jsonata_Object === true) {
             result = partialApplyNativeFunction(proc.implementation, evaluatedArgs);
         } else if (typeof proc === "function") {
             result = partialApplyNativeFunction(proc, evaluatedArgs);
         } else {
             throw {
                 code: "T1008",
                 stack: (new Error()).stack,
                 position: expr.position,
                 token: expr.procedure.type === "path" ? expr.procedure.steps[0].value : expr.procedure.value
             };
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
     Object validateArguments(Function signature, Object args, Object context) {
         if(signature==null) { //typeof signature === "undefined") {
             // nothing to validate
             return args;
         }
         var validatedArgs = signature.validate(args, context);
         return validatedArgs;
     }
 
     /**
      * Apply procedure
      * @param {Object} proc - Procedure
      * @param {Array} args - Arguments
     * @throws JException
      * @returns {*} Result of procedure
      */
     /* async */ Object applyProcedure(Object _proc, Object _args) throws JException {
        List args = (List)_args;
        Symbol proc = (Symbol)_proc;
        Object result = null;
        var env = createFrame(proc.environment);
        for (int i=0; i<proc.arguments.size(); i++) {
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
     Object partialApplyProcedure(Object proc, Object args) {
         // create a closure, bind the supplied parameters and return a Object that takes the remaining (?) parameters
         var env = createFrame(proc.environment);
         var unboundArgs = [];
         proc.arguments.forEach(Object (param, index) {
             var arg = args[index];
             if (arg && arg.type === "operator" && arg.value === "?") {
                 unboundArgs.push(param);
             } else {
                 env.bind(param.value, arg);
             }
         });
         var procedure = {
             _jsonata_lambda: true,
             input: proc.input,
             environment: env,
             arguments: unboundArgs,
             body: proc.body
         };
         return procedure;
     }
 
     /**
      * Partially apply native function
      * @param {Function} native - Native function
      * @param {Array} args - Arguments
      * @returns {{lambda: boolean, input: *, environment: {bind, lookup}, arguments: Array, body: *}} Result of partially applying native function
      */
     Object partialApplyNativeFunction(Function _native, Object args) {
         // create a lambda Object that wraps and invokes the native function
         // get the list of declared arguments from the native function
         // this has to be picked out from the toString() value
         var sigArgs = getNativeFunctionArguments(_native);
         sigArgs = sigArgs.map(Object (sigArg) {
             return "$" + sigArg.trim();
         });
         var body = "function(" + sigArgs.join(", ") + "){ _ }";
 
         var bodyAST = parser(body);
         bodyAST.body = _native;
 
         var partial = partialApplyProcedure(bodyAST, args);
         return partial;
     }
 
     /**
      * Apply native function
      * @param {Object} proc - Procedure
      * @param {Object} env - Environment
      * @returns {*} Result of applying native function
      */
     /* async */ Object applyNativeFunction(Function proc, Environment env) {
         var sigArgs = getNativeFunctionArguments(proc);
         // generate the array of arguments for invoking the Object - look them up in the environment
         var args = sigArgs.map(Object (sigArg) {
             return env.lookup(sigArg.trim());
         });
 
         var focus = {
             environment: env
         };
         var result = proc.apply(focus, args);
         if (isPromise(result)) {
             result = /* await */ result;
         }
         return result;
     }
 
     /**
      * Get native Object arguments
      * @param {Function} func - Function
      * @returns {*|Array} Native Object arguments
      */
     Object getNativeFunctionArguments(Function func) {
         var signature = func.toString();
         var sigParens = /\(([^)]*)\)/.exec(signature)[1]; // the contents of the parens
         var sigArgs = sigParens.split(",");
         return sigArgs;
     }
 
     /**
      * Creates a Object definition
      * @param {Function} func - Object implementation in Javascript
      * @param {string} signature - JSONata Object signature definition
      * @returns {{implementation: *, signature: *}} Object definition
      */
     Object defineFunction(JFunctionCallable func, String signature) {

        return new JFunction(func, signature);
        //  var definition = {
        //      _jsonata_function: true,
        //      implementation: func
        //  };
        //  if(typeof signature !== "undefined") {
        //      definition.signature = parseSignature(signature);
        //  }
        //  return definition;
     }
 
 
     /**
      * parses and evaluates the supplied expression
      * @param {string} expr - expression to evaluate
      * @returns {*} - result of evaluating the expression
      */
     /* async */ Object functionEval(String expr, Object focus) {
         // undefined inputs always return undefined
         if(typeof expr === "undefined") {
             return undefined;
         }
         var input = this.input;
         if(typeof focus !== "undefined") {
             input = focus;
             // if the input is a JSON array, then wrap it in a singleton sequence so it gets treated as a single input
             if(Array.isArray(input) && !isSequence(input)) {
                 input = createSequence(input);
                 input.outerWrapper = true;
             }
         }
 
         try {
             var ast = parser(expr, false);
         } catch(err) {
             // error parsing the expression passed to $eval
             populateMessage(err);
             throw {
                 stack: (new Error()).stack,
                 code: "D3120",
                 value: err.message,
                 error: err
             };
         }
         try {
             var result = /* await */ evaluate(ast, input, this.environment);
         } catch(err) {
             // error evaluating the expression passed to $eval
             populateMessage(err);
             throw {
                 stack: (new Error()).stack,
                 code: "D3121",
                 value:err.message,
                 error: err
             };
         }
 
         return result;
     }
 
     /**
      * Clones an object
      * @param {Object} arg - object to clone (deep copy)
      * @returns {*} - the cloned object
      */
     Object functionClone(Object arg) {
         // undefined inputs always return undefined
         if(typeof arg === "undefined") {
             return undefined;
         }
 
         return JSON.parse(fn.string(arg));
     }
 
     /**
      * Create frame
      * @param {Object} enclosingEnvironment - Enclosing environment
      * @returns {{bind: bind, lookup: lookup}} Created frame
      */
     Frame createFrame(Frame enclosingEnvironment) {

        return new Frame(enclosingEnvironment);

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

    /**
     * JFunction callable Lambda interface
     */
    public static interface JFunctionCallable {
        Object call(Object input, Object args);
    }

    /**
     * JFunction definition class
     */
    public static class JFunction implements JFunctionCallable {
        JFunctionCallable function;
        String signature;

        public JFunction(JFunctionCallable function, String signature) {
            this.function = function;
            this.signature = signature;
        }

        @Override
        public Object call(Object input, Object args) {
            return function.call(input, args);
        }
    }

    void registerFunctions0() {
        staticFrame.bind("count", defineFunction(Functions::count, "<a:n>"));
        staticFrame.bind("string", defineFunction(Functions::string, "<x-b?:s>"));
        staticFrame.bind("not", defineFunction(Functions::not, "<x-:b>"));
        staticFrame.bind("join", defineFunction(Functions::join, "<a<s>s?:s>"));
        staticFrame.bind("lowercase", defineFunction(Functions::lowercase, "<s-:s>"));
        staticFrame.bind("uppercase", defineFunction(Functions::uppercase, "<s-:s>"));
        staticFrame.bind("substring", defineFunction(Functions::substring, "<s-nn?:s>"));
    }

     // Function registration
    void registerFunctions() {
    /*
     staticFrame.bind("sum", defineFunction(Functions::sum, "<a<n>:n>"));
     staticFrame.bind("count", defineFunction(Functions::count, "<a:n>"));
     staticFrame.bind("max", defineFunction(Functions::max, "<a<n>:n>"));
     staticFrame.bind("min", defineFunction(Functions::min, "<a<n>:n>"));
     staticFrame.bind("average", defineFunction(Functions::average, "<a<n>:n>"));
     staticFrame.bind("string", defineFunction(Functions::string, "<x-b?:s>"));
     staticFrame.bind("substring", defineFunction(Functions::substring, "<s-nn?:s>"));
     staticFrame.bind("substringBefore", defineFunction(Functions::substringBefore, "<s-s:s>"));
     staticFrame.bind("substringAfter", defineFunction(Functions::substringAfter, "<s-s:s>"));
     staticFrame.bind("lowercase", defineFunction(Functions::lowercase, "<s-:s>"));
     staticFrame.bind("uppercase", defineFunction(Functions::uppercase, "<s-:s>"));
     staticFrame.bind("length", defineFunction(Functions::length, "<s-:n>"));
     staticFrame.bind("trim", defineFunction(Functions::trim, "<s-:s>"));
     staticFrame.bind("pad", defineFunction(Functions::pad, "<s-ns?:s>"));
     staticFrame.bind("match", defineFunction(Functions::match, "<s-f<s:o>n?:a<o>>"));
     staticFrame.bind("contains", defineFunction(Functions::contains, "<s-(sf):b>")); // TODO <s-(sf<s:o>):b>
     staticFrame.bind("replace", defineFunction(Functions::replace, "<s-(sf)(sf)n?:s>")); // TODO <s-(sf<s:o>)(sf<o:s>)n?:s>
     staticFrame.bind("split", defineFunction(Functions::split, "<s-(sf)n?:a<s>>")); // TODO <s-(sf<s:o>)n?:a<s>>
     staticFrame.bind("join", defineFunction(Functions::join, "<a<s>s?:s>"));
     staticFrame.bind("formatNumber", defineFunction(Functions::formatNumber, "<n-so?:s>"));
     staticFrame.bind("formatBase", defineFunction(Functions::formatBase, "<n-n?:s>"));
     staticFrame.bind("formatInteger", defineFunction(Functions::dateTimeFormatInteger, "<n-s:s>"));
     staticFrame.bind("parseInteger", defineFunction(Functions::dateTimeParseInteger, "<s-s:n>"));
     staticFrame.bind("number", defineFunction(Functions::number, "<(nsb)-:n>"));
     staticFrame.bind("floor", defineFunction(Functions::floor, "<n-:n>"));
     staticFrame.bind("ceil", defineFunction(Functions::ceil, "<n-:n>"));
     staticFrame.bind("round", defineFunction(Functions::round, "<n-n?:n>"));
     staticFrame.bind("abs", defineFunction(Functions::abs, "<n-:n>"));
     staticFrame.bind("sqrt", defineFunction(Functions::sqrt, "<n-:n>"));
     staticFrame.bind("power", defineFunction(Functions::power, "<n-n:n>"));
     staticFrame.bind("random", defineFunction(Functions::random, "<:n>"));
     staticFrame.bind("boolean", defineFunction(Functions::booleanFn, "<x-:b>"));
     staticFrame.bind("not", defineFunction(Functions::not, "<x-:b>"));
     staticFrame.bind("map", defineFunction(Functions::map, "<af>"));
     staticFrame.bind("zip", defineFunction(Functions::zip, "<a+>"));
     staticFrame.bind("filter", defineFunction(Functions::filter, "<af>"));
     staticFrame.bind("single", defineFunction(Functions::single, "<af?>"));
     staticFrame.bind("reduce", defineFunction(Functions::foldLeft, "<afj?:j>")); // TODO <f<jj:j>a<j>j?:j>
     staticFrame.bind("sift", defineFunction(Functions::sift, "<o-f?:o>"));
     staticFrame.bind("keys", defineFunction(Functions::keys, "<x-:a<s>>"));
     staticFrame.bind("lookup", defineFunction(Functions::lookup, "<x-s:x>"));
     staticFrame.bind("append", defineFunction(Functions::append, "<xx:a>"));
     staticFrame.bind("exists", defineFunction(Functions::exists, "<x:b>"));
     staticFrame.bind("spread", defineFunction(Functions::spread, "<x-:a<o>>"));
     staticFrame.bind("merge", defineFunction(Functions::merge, "<a<o>:o>"));
     staticFrame.bind("reverse", defineFunction(Functions::reverse, "<a:a>"));
     staticFrame.bind("each", defineFunction(Functions::each, "<o-f:a>"));
     staticFrame.bind("error", defineFunction(Functions::error, "<s?:x>"));
     staticFrame.bind("assert", defineFunction(Functions::assertFn, "<bs?:x>"));
     staticFrame.bind("type", defineFunction(Functions::type, "<x:s>"));
     staticFrame.bind("sort", defineFunction(Functions::sort, "<af?:a>"));
     staticFrame.bind("shuffle", defineFunction(Functions::shuffle, "<a:a>"));
     staticFrame.bind("distinct", defineFunction(Functions::distinct, "<x:x>"));
     staticFrame.bind("base64encode", defineFunction(Functions::base64encode, "<s-:s>"));
     staticFrame.bind("base64decode", defineFunction(Functions::base64decode, "<s-:s>"));
     staticFrame.bind("encodeUrlComponent", defineFunction(Functions::encodeUrlComponent, "<s-:s>"));
     staticFrame.bind("encodeUrl", defineFunction(Functions::encodeUrl, "<s-:s>"));
     staticFrame.bind("decodeUrlComponent", defineFunction(Functions::decodeUrlComponent, "<s-:s>"));
     staticFrame.bind("decodeUrl", defineFunction(Functions::decodeUrl, "<s-:s>"));
     staticFrame.bind("eval", defineFunction(Functions::functionEval, "<sx?:x>"));
     staticFrame.bind("toMillis", defineFunction(Functions::dataTimeToMillis, "<s-s?:n>"));
     staticFrame.bind("fromMillis", defineFunction(Functions::dateTimeFromMillis, "<n-s?s?:s>"));
     staticFrame.bind("clone", defineFunction(Functions::functionClone, "<(oa)-:o>"));
     */
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
        //  "S0102": "Number out of range: {{token}}",
        //  "S0103": "Unsupported escape sequence: \\{{token}}",
        //  "S0104": "The escape sequence \\u must be followed by 4 hex digits",
        //  "S0105": "Quoted property name must be terminated with a backquote (`)",
        //  "S0106": "Comment has no closing tag",
        //  "S0201": "Syntax error: {{token}}",
        //  "S0202": "Expected {{value}}, got {{token}}",
        //  "S0203": "Expected {{value}} before end of expression",
        //  "S0204": "Unknown operator: {{token}}",
        //  "S0205": "Unexpected token: {{token}}",
        //  "S0206": "Unknown expression type: {{token}}",
        //  "S0207": "Unexpected end of expression",
        //  "S0208": "Parameter {{value}} of Object definition must be a variable name (start with $)",
        //  "S0209": "A predicate cannot follow a grouping expression in a step",
        //  "S0210": "Each step can only have one grouping expression",
        //  "S0211": "The symbol {{token}} cannot be used as a unary operator",
        //  "S0212": "The left side of := must be a variable name (start with $)",
        //  "S0213": "The literal value {{value}} cannot be used as a step within a path expression",
        //  "S0214": "The right side of {{token}} must be a variable name (start with $)",
        //  "S0215": "A context variable binding must precede any predicates on a step",
        //  "S0216": "A context variable binding must precede the "order-by" clause on a step",
        //  "S0217": "The object representing the "parent" cannot be derived from this expression",
        //  "S0301": "Empty regular expressions are not allowed",
        //  "S0302": "No terminating / in regular expression",
        //  "S0402": "Choice groups containing parameterized types are not supported",
        //  "S0401": "Type parameters can only be applied to functions and arrays",
        //  "S0500": "Attempted to evaluate an expression containing syntax error(s)",
        //  "T0410": "Argument {{index}} of Object {{token}} does not match Object signature",
        //  "T0411": "Context value is not a compatible type with argument {{index}} of Object {{token}}",
        //  "T0412": "Argument {{index}} of Object {{token}} must be an array of {{type}}",
        //  "D1001": "Number out of range: {{value}}",
        //  "D1002": "Cannot negate a non-numeric value: {{value}}",
        //  "T1003": "Key in object structure must evaluate to a string; got: {{value}}",
        //  "D1004": "Regular expression matches zero length string",
        //  "T1005": "Attempted to invoke a non-function. Did you mean ${{{token}}}?",
        //  "T1006": "Attempted to invoke a non-function",
        //  "T1007": "Attempted to partially apply a non-function. Did you mean ${{{token}}}?",
        //  "T1008": "Attempted to partially apply a non-function",
        //  "D1009": "Multiple key definitions evaluate to same key: {{value}}",
        //  "T1010": "The matcher Object argument passed to Object {{token}} does not return the correct object structure",
        //  "T2001": "The left side of the {{token}} operator must evaluate to a number",
        //  "T2002": "The right side of the {{token}} operator must evaluate to a number",
        //  "T2003": "The left side of the range operator (..) must evaluate to an integer",
        //  "T2004": "The right side of the range operator (..) must evaluate to an integer",
        //  "D2005": "The left side of := must be a variable name (start with $)",  // defunct - replaced by S0212 parser error
        //  "T2006": "The right side of the Object application operator ~> must be a function",
        //  "T2007": "Type mismatch when comparing values {{value}} and {{value2}} in order-by clause",
        //  "T2008": "The expressions within an order-by clause must evaluate to numeric or string values",
        //  "T2009": "The values {{value}} and {{value2}} either side of operator {{token}} must be of the same data type",
        //  "T2010": "The expressions either side of operator {{token}} must evaluate to numeric or string values",
        //  "T2011": "The insert/update clause of the transform expression must evaluate to an object: {{value}}",
        //  "T2012": "The delete clause of the transform expression must evaluate to a string or array of strings: {{value}}",
        //  "T2013": "The transform expression clones the input object using the $clone() function.  This has been overridden in the current scope by a non-function.",
        //  "D2014": "The size of the sequence allocated by the range operator (..) must not exceed 1e6.  Attempted to allocate {{value}}.",
        //  "D3001": "Attempting to invoke string Object on Infinity or NaN",
        //  "D3010": "Second argument of replace Object cannot be an empty string",
        //  "D3011": "Fourth argument of replace Object must evaluate to a positive number",
        //  "D3012": "Attempted to replace a matched string with a non-string value",
        //  "D3020": "Third argument of split Object must evaluate to a positive number",
        //  "D3030": "Unable to cast value to a number: {{value}}",
        //  "D3040": "Third argument of match Object must evaluate to a positive number",
        //  "D3050": "The second argument of reduce Object must be a Object with at least two arguments",
        //  "D3060": "The sqrt Object cannot be applied to a negative number: {{value}}",
        //  "D3061": "The power Object has resulted in a value that cannot be represented as a JSON number: base={{value}}, exponent={{exp}}",
        //  "D3070": "The single argument form of the sort Object can only be applied to an array of strings or an array of numbers.  Use the second argument to specify a comparison function",
        //  "D3080": "The picture string must only contain a maximum of two sub-pictures",
        //  "D3081": "The sub-picture must not contain more than one instance of the "decimal-separator" character",
        //  "D3082": "The sub-picture must not contain more than one instance of the "percent" character",
        //  "D3083": "The sub-picture must not contain more than one instance of the "per-mille" character",
        //  "D3084": "The sub-picture must not contain both a "percent" and a "per-mille" character",
        //  "D3085": "The mantissa part of a sub-picture must contain at least one character that is either an "optional digit character" or a member of the "decimal digit family"",
        //  "D3086": "The sub-picture must not contain a passive character that is preceded by an active character and that is followed by another active character",
        //  "D3087": "The sub-picture must not contain a "grouping-separator" character that appears adjacent to a "decimal-separator" character",
        //  "D3088": "The sub-picture must not contain a "grouping-separator" at the end of the integer part",
        //  "D3089": "The sub-picture must not contain two adjacent instances of the "grouping-separator" character",
        //  "D3090": "The integer part of the sub-picture must not contain a member of the "decimal digit family" that is followed by an instance of the "optional digit character"",
        //  "D3091": "The fractional part of the sub-picture must not contain an instance of the "optional digit character" that is followed by a member of the "decimal digit family"",
        //  "D3092": "A sub-picture that contains a "percent" or "per-mille" character must not contain a character treated as an "exponent-separator"",
        //  "D3093": "The exponent part of the sub-picture must comprise only of one or more characters that are members of the "decimal digit family"",
        //  "D3100": "The radix of the formatBase Object must be between 2 and 36.  It was given {{value}}",
        //  "D3110": "The argument of the toMillis Object must be an ISO 8601 formatted timestamp. Given {{value}}",
        //  "D3120": "Syntax error in expression passed to Object eval: {{value}}",
        //  "D3121": "Dynamic error evaluating the expression passed to Object eval: {{value}}",
        //  "D3130": "Formatting or parsing an integer as a sequence starting with {{value}} is not supported by this implementation",
        //  "D3131": "In a decimal digit pattern, all digits must be from the same decimal group",
        //  "D3132": "Unknown component specifier {{value}} in date/time picture string",
        //  "D3133": "The "name" modifier can only be applied to months and days in the date/time picture string, not {{value}}",
        //  "D3134": "The timezone integer format specifier cannot have more than four digits",
        //  "D3135": "No matching closing bracket "]" in date/time picture string",
        //  "D3136": "The date/time picture string is missing specifiers required to parse the timestamp",
        //  "D3137": "{{{message}}}",
        //  "D3138": "The $single() Object expected exactly 1 matching result.  Instead it matched more.",
        //  "D3139": "The $single() Object expected exactly 1 matching result.  Instead it matched 0.",
        //  "D3140": "Malformed URL passed to ${{{functionName}}}(): {{value}}",
        //  "D3141": "{{{message}}}"
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

     /**
      * JSONata
      * @param {Object} expr - JSONata expression
      * @param {Object} options
      * @param {boolean} options.recover: attempt to recover on parse error
      * @param {Function} options.RegexEngine: RegEx class constructor to use
      * @returns {{evaluate: evaluate, assign: assign}} Evaluated expression
      */
     public Jsonata(String expr, boolean optionsRecover) throws JException {
         try {
            staticFrame = createFrame(null);
            registerFunctions0();
             ast = parser.parse(expr);//, optionsRecover);
             errors = ast.errors;
             ast.errors = null; //delete ast.errors;
         } catch(JException err) {
             // insert error message into structure
             //populateMessage(err); // possible side-effects on `err`
             throw err;
         }
         environment = createFrame(staticFrame);
 
         var timestamp = new java.util.Date(); // will be overridden on each call to evalute()

        // FIXME bind functions
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
    public Object evaluate(Object input, Frame bindings) throws JException { // FIXME:, callback) {
                 // throw if the expression compiled with syntax errors
                 if(errors != null) {
                    //  var err = {
                    //      code: "S0500",
                    //      position: 0
                    //  };
                    //populateMessage(err); // possible side-effects on `err`
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
                // FIXME array handling
                //  if(Array.isArray(input) && !isSequence(input)) {
                //      input = createSequence(input);
                //      input.outerWrapper = true;
                //  }
 
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
 
    Parser parser = new Parser();
//      jsonata.parser = parser; // TODO remove this in a future release - use ast() instead
 
//      return jsonata;
 
//  })();
 
//  module.exports = jsonata;

    public static void main(String[] args) throws Throwable {

        String s = "$join(['a','b','c'], '#')";
        s = "$count([1..(1e4-1)])";
        //s = "{ 'number': [1..10].$string() }"; // FIXME
        s = "[1].$[]";
        Jsonata jsonata = new Jsonata(s, false);
        Object result = jsonata.evaluate(null, null);
        System.out.println("Result = "+new ObjectMapper().writeValueAsString(result));
    }
}
