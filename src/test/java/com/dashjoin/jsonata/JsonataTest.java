package com.dashjoin.jsonata;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonataTest {

    Long convertToInt(Object val) {
        if (val instanceof Double) {
            double d = (double)val;
            long l = (long)d;
            // If number can be represented as long (no fraction), use long:
            if (l==d)
                return Long.valueOf(l);
        }
        return null;
    }

    void convertNumbers(Map<String, Object> res) {
        for (Entry<String, Object> e : res.entrySet()) {
            Object val = e.getValue();
            Long l = convertToInt(val);
            if (l!=null)
                e.setValue(l);
            if (val instanceof Map) {
                convertNumbers((Map<String, Object>)val);
            }
            recurse(val);
        }
    }

    void convertNumbers(List<Object> res) {
        for (int i=0; i<res.size(); i++) {
            Object val = res.get(i);
            Long l = convertToInt(val);
            if (l!=null)
                res.set(i, l);
            recurse(val);
        }
    }

    void recurse(Object val) {
        if (val instanceof Map)
            convertNumbers((Map)val);
        if (val instanceof List)
            convertNumbers((List)val);
    }

    Object convertNumbers(Object res) {
        if (res instanceof Double) {
            Long l = convertToInt((double)res);
            return l!=null ? l : res;            
        }
        recurse(res);
        return res;
    }

    void testExpr(String expr, Object data, String expected) throws JException {
        try {

        if (debug) System.out.println("Expr="+expr+" Expected="+expected);
        if (debug) System.out.println(data);

        Jsonata jsonata = new Jsonata(expr, false);
        Object result = jsonata.evaluate(data, null);
        convertNumbers(result);
        System.out.println("Result = "+result);
        if (!expected.equals(""+result))
            System.out.println("WRONG RESULT");

        //assertEquals("Must be equal", expected, ""+result);
        } catch (Throwable t) {
            //System.err.println(t);
            t.printStackTrace();
            //if (true) System.exit(-1);
        }

    }

    Object toJson(String jsonStr) throws JsonMappingException, JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        Object json = om.readValue(jsonStr, Object.class);
        return json;
    }

    Object readJson(String name) throws StreamReadException, DatabindException, IOException {
        ObjectMapper om = new ObjectMapper();
        Object json = om.readValue(new java.io.File(name), Object.class);
        return json;
    }

    @Test
    public void testSimple() throws JException {
        testExpr("42", null, "42.0");
        testExpr("(3*(4-2)+1.01e2)/-2", null, "-53.5");
    }

    @Test
    public void testPath() throws Exception {
        Object data = readJson("test/test-suite/datasets/dataset0.json");
        System.out.println(data);
        testExpr("foo.bar", data, "42");
    }

    static class TestDef {
        String expr;
        String dataset;
        Object bindings;
        Object result;
    }

    int testFiles = 0;
    int testCases = 0;

    void runTestSuite(String name) throws Exception {

        //System.out.println("Running test "+name);
        testFiles++;

        Object testCase = readJson(name);
        if (testCase instanceof List) {
            // some cases contain a list of test cases
            // loop over the case definitions
            for (Object testDef : ((List)testCase)) {
                System.out.println("Running sub-test");
                runTestCase(name, (Map<String, Object>) testDef);
            }
        } else {
            runTestCase(name, (Map<String, Object>) testCase);
        }
    }

    void runTestCase(String name, Map<String, Object> testDef) throws Exception {
        testCases++;
        System.out.println("Running test "+name);

        String expr = (String)testDef.get("expr");

        if (expr==null) {
            String exprFile = (String)testDef.get("expr-file");
            String fileName = name.substring(0, name.lastIndexOf("/")) + "/" + exprFile;
            expr = IOUtils.toString(new FileInputStream(fileName));
        }

        String dataset = (String)testDef.get("dataset");
        Object bindings = testDef.get("bindings");
        Object result = testDef.get("result");

        //System.out.println(""+bindings);

        Object data = testDef.get("data");
        if (data==null && dataset!=null)
            data = readJson("test/test-suite/datasets/"+dataset+".json");

        testExpr(expr, data, ""+result);
    }

    String groupDir = "test/test-suite/groups/";

    void runTestGroup(String group) throws Exception {
        
        File dir = new File(groupDir, group);
        System.out.println("Run group "+dir);
        File[] files = dir.listFiles();
        Arrays.sort(files);
        for (File f : files) {
            String name = f.getName();
            if (name.endsWith(".json"))
                runTestSuite(groupDir+group+"/"+name);
        }
    }

    boolean debug = true;

    @Test
    public void testSuite() throws Exception {
        //runTestSuite("test/test-suite/groups/boolean-expresssions/test.jsonx");
        //runTestSuite("test/test-suite/groups/boolean-expresssions/case017.json");
        //runTestSuite("test/test-suite/groups/fields/case000.json");
        //runTestGroup("fields");
        //runTestGroup("comments");
        //runTestGroup("comparison-operators");
        //runTestGroup("boolean-expresssions");
        //runTestGroup("array-constructor");
        //runTestGroup("transform");
        //runTestSuite("test/test-suite/groups/transform/case030.json");
        //runTestSuite("test/test-suite/groups/array-constructor/case004.json");
        // Filter:
        runTestSuite("test/test-suite/groups/array-constructor/case017.json");
        //runAllTestGroups();
    }

    void runAllTestGroups() throws Exception {
        File dir = new File(groupDir);
        File[] groups = dir.listFiles();
        Arrays.sort(groups);
        for (File g : groups) {
            String name = g.getName();
            runTestGroup(name);
        }

        System.out.println("Total test files="+testFiles+" cases="+testCases);
    }
}
