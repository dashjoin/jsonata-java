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
    Assertions.assertEquals("[[1], null, [3], null]", expr.evaluate(null));
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

  @Test
  public void testVarArg() {
    var expression = Jsonata.jsonata("$sumvar(1,2,3)");
    expression.registerFunction("sumvar", new JFunction(new JFunctionCallable() {
      @SuppressWarnings("rawtypes")
      @Override
      public Object call(Object input, List args) throws Throwable {
        int sum = 0;
        for (Object i : args)
          sum += (int) i;
        return sum;
      }
    }, "<n+:n>"));
    Assertions.assertEquals(6, expression.evaluate(null));
  }

  /**
   * Verifies that parameter index tracking works correctly in signature validation.
   * Without the index++ fix in Signature.validate(), the second parameter's capture group
   * is read at the wrong position, causing valid calls to fail with T0410.
   */
  @Test
  public void testArrayFirstArgWithFunctionSecondArg() {
      // $map has signature <af> : first arg = array, second arg = function
      // Without index++ fix, the function arg is validated against group(1) instead of group(2)
      var expression = jsonata("$map([1,2,3], function($v) { $v * 2 })");
      var result = expression.evaluate(null);
      Assertions.assertNotNull(result);
      Assertions.assertEquals(java.util.List.of(2, 4, 6), result);
  }

  @Test
  public void testVarArgMany(){
      Jsonata expr = jsonata("$customArgs('test',[1,2,3,4],3)");
      expr.registerFunction("customArgs", new JFunction(new JFunctionCallable() {

          @Override
          public Object call(Object input, @SuppressWarnings("rawtypes") List args) throws Throwable {
              return args.toString();
          }
      }, "<sa<n>n:s>"));
      Assertions.assertEquals("[test, [1, 2, 3, 4], 3]", expr.evaluate(null));
  }
}
