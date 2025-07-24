package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TypesTest {

  @Test
  public void castTestIn() {
    Assertions.assertFalse((boolean)jsonata("3 in $").evaluate(Arrays.asList(1.0, 2.0)));
    Assertions.assertTrue((boolean)jsonata("1 in $").evaluate(Arrays.asList(1.0, 2.0)));
  }

  @Test
  public void castTestEquals() {
    Assertions.assertTrue((boolean)jsonata("1 = $").evaluate(1.0));
    Assertions.assertFalse((boolean)jsonata("1 = $").evaluate(2.0));
    Assertions.assertTrue( (boolean)jsonata("{'x':1 } = {'x':1 }").evaluate(null)  );
    Assertions.assertFalse( (boolean)jsonata("{'x':1 } = {'x':2 }").evaluate(null)  );
    Assertions.assertTrue( (boolean)jsonata("[1,null] = [1,null]").evaluate(null)  );
    Assertions.assertFalse( (boolean)jsonata("[1,null] = [2,null]").evaluate(null)  );
  }

  @Test
  public void testIllegalTypes() {
    // array
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> jsonata("$").evaluate(new int[] {0, 1, 2, 3}));
    // char
    Assertions.assertThrows(IllegalArgumentException.class, () -> jsonata("$").evaluate('c'));
    // date
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> jsonata("$").evaluate(new Date()));
  }

  @Test
  public void testLegalTypes() {
    // map
    Assertions.assertEquals(1, jsonata("a").evaluate(Map.of("a", 1)));
    // list
    Assertions.assertEquals(1, jsonata("$[0]").evaluate(Arrays.asList(1, 2)));
    // string
    Assertions.assertEquals("string", jsonata("$").evaluate("string"));
    // int
    Assertions.assertEquals(1, jsonata("$").evaluate(1));
    // long
    Assertions.assertEquals(1L, jsonata("$").evaluate(1L));
    // boolean
    Assertions.assertEquals(true, jsonata("$").evaluate(true));
    // double
    Assertions.assertEquals(1.0, jsonata("$").evaluate(1.0));
    // float
    Assertions.assertEquals((float) 1.0, jsonata("$").evaluate((float) 1.0));
    // big decimal
    Assertions.assertEquals(new BigDecimal(3.14), jsonata("$").evaluate(new BigDecimal(3.14)));
  }

  public static class Pojo {
    public char c = 'c';
    public Date d = new Date();
    public int[] arr = new int[] {0, 1, 2, 3};
  }

  @Test
  public void testJacksonConversion() {
    ObjectMapper om = new ObjectMapper();
    Object input = om.convertValue(new Pojo(), Object.class);
    Assertions.assertEquals("c", jsonata("c").evaluate(input));
    Assertions.assertEquals(0, jsonata("arr[0]").evaluate(input));
    Assertions.assertEquals(Long.class, jsonata("d").evaluate(input).getClass());
    
    Object output = jsonata("$").evaluate(input);
    Assertions.assertEquals('c', om.convertValue(output, Pojo.class).c);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCustomFunction() {
    ObjectMapper om = new ObjectMapper();
    Jsonata fn = jsonata("$foo()");
    fn.registerFunction("foo", () -> om.convertValue(new Pojo(), Object.class));
    Map<String, Object> res = (Map<String, Object>) fn.evaluate(null);
    Assertions.assertEquals("c", res.get("c"));
  }

  @Test
  public void testIgnore() {
    Jsonata expr = jsonata("a");
    Date date = new Date();

    // date causes exception
    Assertions.assertThrows(IllegalArgumentException.class, () -> expr.evaluate(Map.of("a", date)));

    // turn off validation, Date is "passed" via $
    expr.setValidateInput(false);
    Assertions.assertEquals(date, expr.evaluate(Map.of("a", date)));

    // change expression to a computation that involves a, we get an error again because concat
    // cannot deal with Date
    Jsonata expr2 = jsonata("a & a");
    expr2.setValidateInput(false);
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> expr2.evaluate(Map.of("a", date)));
  }
}
