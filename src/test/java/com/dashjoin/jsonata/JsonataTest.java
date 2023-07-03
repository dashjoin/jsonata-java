package com.dashjoin.jsonata;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.dashjoin.jsonata.Jsonata.Frame;
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

    boolean testExpr(String expr, Object data, Map<String,Object> bindings,
        Object expected, String code) throws JException {
        boolean success = true;
        try {

        if (debug) System.out.println("Expr="+expr+" Expected="+expected+" ErrorCode="+code);
        if (debug) System.out.println(data);

        Frame bindingFrame = null;
        if (bindings!=null) {
            // If we have bindings, create a binding env with the settings
            bindingFrame = new Frame(null);
            for (Entry<String,Object> e : bindings.entrySet()) {
                bindingFrame.bind(e.getKey(), e.getValue());
            }
        }

        Jsonata jsonata = new Jsonata(expr, false);
        Object result = jsonata.evaluate(data, bindingFrame);
        if (result==Jsonata.NULL_VALUE)
            result = null;
        //result = convertNumbers(result);
        if (code!=null)
            success = false;
        
        if (expected!=null && !expected.equals(result)) {
            if ((""+expected).equals(""+result))
                System.out.println("Value equals failed, stringified equals = true. Result = "+result);
            else
                success = false;
        }
    
        if (expected==null && result!=null)
            success = false;

        if (!success) {
            System.out.println("--Expr="+expr+" Expected="+expected+" ErrorCode="+code);
            System.out.println("--Data="+data);
            System.out.println("--Result = "+result);
            System.out.println("--Expect = "+expected+" ExpectedError="+code);
            System.out.println("WRONG RESULT");
        }

        //assertEquals("Must be equal", expected, ""+result);
        } catch (Throwable t) {
            if (code==null) {
            System.out.println("--Expr="+expr+" Expected="+expected+" ErrorCode="+code);
            System.out.println("--Data="+data);

                if (t instanceof JException) {
                    JException je = (JException)t;
                    System.out.println("--Exception     = "+je.error+"  --> "+je);
                } else
                    System.out.println("--Exception     = "+t);

                System.out.println("--ExpectedError = "+code+" Expected="+expected);
                System.out.println("WRONG RESULT (exception)");
                success = false;
            }
            if (!success) t.printStackTrace();
            //if (true) System.exit(-1);
        }
        return success;
    }

    Object toJson(String jsonStr) throws JsonMappingException, JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        Object json = om.readValue(jsonStr, Object.class);
        return json;
    }

    Object readJson(String name) throws StreamReadException, DatabindException, IOException {
        ObjectMapper om = new ObjectMapper();
        Object json = om.readValue(new java.io.FileReader(name, Charset.forName("UTF-8")), Object.class);
        return json;
    }

    @Test
    public void testSimple() throws JException {
        testExpr("42", null, null, 42,null);
        testExpr("(3*(4-2)+1.01e2)/-2", null, null, -53.5,null);
    }

    @Test
    public void testPath() throws Exception {
        Object data = readJson("test/test-suite/datasets/dataset0.json");
        System.out.println(data);
        testExpr("foo.bar", data, null, 42,null);
    }

    static class TestDef {
        String expr;
        String dataset;
        Object bindings;
        Object result;
    }

    int testFiles = 0;
    int testCases = 0;

    public void runCase(String name) throws Exception {
      if (!runTestSuite(name))
          throw new Exception();
    }
    
    boolean runTestSuite(String name) throws Exception {

        //System.out.println("Running test "+name);
        testFiles++;

        boolean success = true;

        Object testCase = readJson(name);
        if (testCase instanceof List) {
            // some cases contain a list of test cases
            // loop over the case definitions
            for (Object testDef : ((List)testCase)) {
                System.out.println("Running sub-test");
                success &= runTestCase(name, (Map<String, Object>) testDef);
            }
        } else {
            success &= runTestCase(name, (Map<String, Object>) testCase);
        }
        return success;
    }

    boolean runTestCase(String name, Map<String, Object> testDef) throws Exception {
        testCases++;
        System.out.println("\nRunning test "+name);

        String expr = (String)testDef.get("expr");

        if (expr==null) {
            String exprFile = (String)testDef.get("expr-file");
            String fileName = name.substring(0, name.lastIndexOf("/")) + "/" + exprFile;
            expr = IOUtils.toString(new FileInputStream(fileName));
        }

        String dataset = (String)testDef.get("dataset");
        Map<String,Object> bindings = (Map)testDef.get("bindings");
        Object result = testDef.get("result");
        String code = (String)testDef.get("code");

        //System.out.println(""+bindings);

        Object data = testDef.get("data");
        if (data==null && dataset!=null)
            data = readJson("test/test-suite/datasets/"+dataset+".json");

        return testExpr(expr, data, bindings, result, code);
    }

    String groupDir = "test/test-suite/groups/";

    boolean runTestGroup(String group) throws Exception {
        
        File dir = new File(groupDir, group);
        System.out.println("Run group "+dir);
        File[] files = dir.listFiles();
        Arrays.sort(files);
        boolean success = true;
        int count = 0, good = 0;
        for (File f : files) {
            String name = f.getName();
            if (name.endsWith(".json")) {
                boolean res = runTestSuite(groupDir+group+"/"+name);
                success &= res;

                count++;
                if (res)
                    good++;
            }
        }
        int successPercentage = 100*good/count;
        System.out.println("Success: "+good+" / "+count+" = "+(100*good/count)+"%");
        assertEquals(successPercentage+"% succeeded", count, good);
        //assertEquals("100% test runs must succeed", 100, successPercentage);
        return success;
    }

    boolean debug = false;

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
        //runTestGroup("function-substring");
        //runTestGroup("wildcards");
        //runTestSuite("test/test-suite/groups/function-substring/case012.json");
        //runTestSuite("test/test-suite/groups/transform/case030.json");
        //runTestSuite("test/test-suite/groups/array-constructor/case006.json");
        // Filter:
        //runTestSuite("test/test-suite/groups/array-constructor/case017.json");
        String s = "test/test-suite/groups/wildcards/case003.json";
        s = "test/test-suite/groups/flattening/large.json";
        s = "test/test-suite/groups/function-sum/case006.json";
        s = "test/test-suite/groups/function-substring/case016.json";
        s = "test/test-suite/groups/null/case001.json";
        s = "test/test-suite/groups/context/case003.json";
        s = "test/test-suite/groups/object-constructor/case008.json";
        runTestSuite(s);
        //String g = "function-applications"; // partly
        //String g = "higher-order-functions"; // works!
        String g = "hof-map";
        //String g = "joins"; // TODO
        //String g = "function-join"; // looks good
        //String g = "descendent-operator"; // nearly
        //String g = "object-constructor";
        //String g = "flattening";
        //String g = "parent-operator";
        //String g = "function-substring"; // nearly - unicode encoding issues
        //String g = "function-substringBefore"; // works!
        //String g = "function-substringAfter"; // works!
        //String g = "function-sum"; // works! rounding error delta
        //String g = "function-max"; // nearly - [-1,-5] second unary wrong!!!
        //String g = "function-average"; // nearly - [-1,-5] second unary wrong!!!
        //String g = "function-pad"; // nearly - unicode
        //String g = "function-trim"; // works!
        //String g = "function-contains"; // works NO regexp
        //String g = "function-join"; // works NO regexp
        //runTestGroup(g);

        //runAllTestGroups();
    }

    void runAllTestGroups() throws Exception {
        File dir = new File(groupDir);
        File[] groups = dir.listFiles();
        Arrays.sort(groups);
        for (File g : groups) {
            String name = g.getName();
            System.out.println("@Test");
            System.out.println("public void runTestGroup_"+name.replaceAll("-","_")+"() {");
            System.out.println("\trunTestGroup(\""+name+"\");");
            System.out.println("}");
            //runTestGroup(name);
        }

        System.out.println("Total test files="+testFiles+" cases="+testCases);
    }
}
