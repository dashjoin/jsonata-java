package com.dashjoin.jsonata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RegexTest {
    @Test
    public void testRegex() {
        var expression = Jsonata.jsonata("/^test.*$/");
        Object evaluate = expression.evaluate(null);
        String expected = "^test.*$";
        Assertions.assertEquals(expected, evaluate.toString());
    }

    @Test
    public void testEvalRegex() {
        var expression = Jsonata.jsonata("$eval('/^test.*$/')");
        Object evaluate = expression.evaluate(null);
        String expected = "^test.*$";
        Assertions.assertEquals(expected, evaluate.toString());
    }
}
