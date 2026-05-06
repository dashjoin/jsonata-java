package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import static com.dashjoin.jsonata.Jsonata.NULL_VALUE;
import static com.dashjoin.jsonata.JsonataTest.mapOf;
import static com.dashjoin.jsonata.JsonataTest.evaluateJsonata;
import static com.dashjoin.jsonata.JsonataTest.UNDEFINED;
import com.dashjoin.jsonata.JsonataTest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NullSafetyTest {
    @Test
    public void testNullSafety() {
        Object res;
        res = jsonata("$sift(undefined, $uppercase)").evaluate(null);
        Assertions.assertEquals(null, res);
        
        res = jsonata("$each(undefined, $uppercase)").evaluate(null);
        Assertions.assertEquals(null, res);

        res = jsonata("$keys(null)").evaluate(null);
        Assertions.assertEquals(null, res);

        res = jsonata("$map(null, $uppercase)").evaluate(null);
        Assertions.assertEquals(null, res);
        
        res = jsonata("$filter(null, $uppercase)").evaluate(null);
        Assertions.assertEquals(null, res);

        res = jsonata("$single(null, $uppercase)").evaluate(null);
        Assertions.assertEquals(null, res);

        res = jsonata("$reduce(null, $uppercase)").evaluate(null);
        Assertions.assertEquals(null, res);

        res = jsonata("$lookup(null, 'anykey')").evaluate(null);
        Assertions.assertEquals(null, res);

        res = jsonata("$spread(null)").evaluate(null);
        Assertions.assertEquals(null, res);
    }
    
    @Test
    public void testFilterNull() {
        var x = Jsonata.jsonata("$filter($, function($v, $i, $a){$v})").evaluate(Arrays.asList(1, null));
        Assertions.assertEquals(1, x);
    }
    
    @Test
    public void testNotNull() {
        Assertions.assertNull(Jsonata.jsonata("$not($)").evaluate(null));
    }
    
    @Test
    public void testArrayContainingNull() {
        List<String> list = new ArrayList<>();
        list.add(null);
        Assertions.assertFalse((boolean)jsonata("$ ? true : false").evaluate(list));
        list.add("test");
        Assertions.assertTrue((boolean)jsonata("$ ? true : false").evaluate(list));
    }
    
    @Test
    public void testSingleNull() {
        var x = Jsonata.jsonata("$single($, function($v, $i, $a){ $v })").evaluate(Arrays.asList(null, 1));
        Assertions.assertEquals(1, x);
    }
    
    @Test
    public void testFilterNullLookup() {
      var x = Jsonata.jsonata("$filter($, function($v, $i, $a){$lookup($v, 'content')})").evaluate(
          Arrays.asList(Map.of("content", "some"), Map.of()));
      Assertions.assertEquals(Map.of("content", "some"), x);
    }

    @Test
    public void testOutputConvertNulls() {
        Jsonata j = jsonata("$");
        Jsonata j2 = jsonata("$");
        j2.setOutputConvertNulls(false);

        Assertions.assertTrue(j.isOutputConvertNulls());
        Assertions.assertFalse(j2.isOutputConvertNulls());

        Object res, res2;
        res = j.evaluate(Jsonata.NULL_VALUE);
        res2 = j2.evaluate(Jsonata.NULL_VALUE);

        Assertions.assertEquals(null, res);
        Assertions.assertEquals(Jsonata.NULL_VALUE, res2);

        res = j.evaluate(null);
        res2 = j2.evaluate(null);

        Assertions.assertEquals(null, res);
        Assertions.assertEquals(null, res2);
    }

    /**
     * Executes expression without output null conversion
     */
    Object executeJsonataRaw(String expression, Object input) {
        var expr = jsonata(expression);
        expr.setOutputConvertNulls(false);
        var result = expr.evaluate(input);
        return result;
    }

    @Test
    public void testJavaNullVsUndefined() throws Exception {
        JsonataTest test = new JsonataTest();

        Assertions.assertTrue(test.runTestCase("test-undefined", mapOf(
            "expr", "undefined",
            "undefinedResult", true)));

        Assertions.assertTrue(test.runTestCase("test-null", mapOf(
            "expr", "null",
            "result", null)));

        // Test various evaluations raw vs cooked, returning null or undefined
        var res = executeJsonataRaw("null", null);
        Assertions.assertEquals(Jsonata.NULL_VALUE, res);
        
        res = executeJsonataRaw("$", NULL_VALUE);
        Assertions.assertEquals(Jsonata.NULL_VALUE, res);
        
        res = executeJsonataRaw("$", null);
        Assertions.assertEquals(null, res);

        res = executeJsonataRaw("no_match", null);
        Assertions.assertEquals(null, res);

        res = evaluateJsonata("null", null, null);
        Assertions.assertNull(res);

        res = evaluateJsonata("no_match", null, null);
        Assertions.assertEquals(UNDEFINED, res);

        res = evaluateJsonata("{}.a", null, null);
        Assertions.assertEquals(UNDEFINED, res);

        res = executeJsonataRaw("{\"a\":null}.a", null);
        Assertions.assertEquals(Jsonata.NULL_VALUE, res);

        res = evaluateJsonata("{\"a\":null}.a", null, null);
        Assertions.assertEquals(null, res);

        res = executeJsonataRaw("{\"a\":null}.b", null);
        Assertions.assertNull(res);

        res = evaluateJsonata("{\"a\":null}.b", null, null);
        Assertions.assertEquals(UNDEFINED, res);

        res = evaluateJsonata("[a,null,b][0]", null, null);
        Assertions.assertEquals(null, res);

        res = executeJsonataRaw("$[1]", List.of(42, Jsonata.NULL_VALUE));
        Assertions.assertEquals(NULL_VALUE, res);

        res = executeJsonataRaw("$[2]", List.of(42, Jsonata.NULL_VALUE));
        Assertions.assertEquals(null, res);

        res = evaluateJsonata("$[2]", List.of(42, Jsonata.NULL_VALUE), null);
        Assertions.assertEquals(UNDEFINED, res);

        res = jsonata("$").evaluate(Jsonata.NULL_VALUE);
        Assertions.assertNull(res);

        //
        res = executeJsonataRaw("{'a':$}", Jsonata.NULL_VALUE);
        Assertions.assertEquals(Jsonata.NULL_VALUE, ((Map)res).get("a"));

        res = executeJsonataRaw("{'a':$}", null);
        Assertions.assertEquals("{}", ""+res);

        res = executeJsonataRaw("{'a':{'b':$}}",null);
        Assertions.assertEquals("{a={}}", ""+res);

        res = executeJsonataRaw("[$]", List.of(Jsonata.NULL_VALUE, Jsonata.NULL_VALUE));
        Assertions.assertEquals(List.of(Jsonata.NULL_VALUE, Jsonata.NULL_VALUE), res);

        res = evaluateJsonata("[$]", List.of(Jsonata.NULL_VALUE, Jsonata.NULL_VALUE), null);
        Assertions.assertEquals(Arrays.asList(null, null), res);
    }
}
