package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;

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
}
