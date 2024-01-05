package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.dashjoin.jsonata.Jsonata.JFunction;
import com.dashjoin.jsonata.Jsonata.JFunctionCallable;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SerializationTest {

  @Test
  public void testJFunction() throws Exception {
    // return the function and test its serialization
    Jsonata expr = jsonata("$foo");
    expr.registerFunction("foo", new JFunction(new JFunctionCallable() {

      @SuppressWarnings("rawtypes")
      @Override
      public Object call(Object input, List args) throws Throwable {
        return null;
      }
      
    }, null));
    ObjectMapper om = new ObjectMapper();
    System.out.println(expr.evaluate(null).getClass());
    om.writeValueAsString(expr.evaluate(null));
  }
}
