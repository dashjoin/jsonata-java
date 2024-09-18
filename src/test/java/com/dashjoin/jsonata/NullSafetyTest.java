package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import java.util.Arrays;
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
}
