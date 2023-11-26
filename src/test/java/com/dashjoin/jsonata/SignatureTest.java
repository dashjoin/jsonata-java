package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.dashjoin.jsonata.Jsonata.JFunction;
import com.dashjoin.jsonata.Jsonata.JFunctionCallable;

public class SignatureTest {

  @Test
  public void testParametersAreConvertedToArrays() {
    Jsonata expr = jsonata("$greet(1,null,3)");
    expr.registerFunction("greet", new JFunction(new JFunctionCallable() {

      @Override
      public Object call(Object input, @SuppressWarnings("rawtypes") List args) throws Throwable {
        return args.toString();
      }
    }, "<a?a?a?a?:s>"));
    Assertions.assertEquals("[[1], [null], [3], [null]]", expr.evaluate(null));
  }
  
  @Test
  public void testError() {
    Jsonata expr = jsonata("$foo()");
    expr.registerFunction("foo", new JFunction(new JFunctionCallable() {
      
      @Override
      public Object call(Object input, @SuppressWarnings("rawtypes") List args) throws Throwable {
        return null;
      }
    }, "(sao)"));
    
    // null not allowed
    Assertions.assertThrows(JException.class, ()->expr.evaluate(null));
    
    // boolean not allowed
    Assertions.assertThrows(JException.class, ()->expr.evaluate(true));
  }
}
