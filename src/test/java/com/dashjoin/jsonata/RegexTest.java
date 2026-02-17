package com.dashjoin.jsonata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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

    @Test
    public void testEvalRegexCheckAnswerData() {
        var expression = Jsonata.jsonata(
                "(\n" +
                        "    $matcher := $eval('/l/');\n" +
                        "    ('Hello World' ~> $matcher);\n" +
                        ")"
        );
        Map<String, Object> evaluate = (Map<String, Object>)(expression.evaluate(null));
        Assertions.assertEquals("l", evaluate.get("match"));
        Assertions.assertEquals(2, evaluate.get("start"));
        Assertions.assertEquals(3, evaluate.get("end"));
        Assertions.assertEquals(List.of("l"), evaluate.get("groups"));
        Assertions.assertInstanceOf(Jsonata.Fn0.class, evaluate.get("next"));
    }

    @Test
    public void testEvalRegexCallNextAndCheckResult() {
        var expression = Jsonata.jsonata(
                "(\n" +
                        "    $matcher := $eval('/l/');\n" +
                        "    ('Hello World' ~> $matcher).next();\n" +
                        ")"
        );
        Map<String, Object> evaluate = (Map<String, Object>)(expression.evaluate(null));
        Assertions.assertEquals("l", evaluate.get("match"));
        Assertions.assertEquals(3, evaluate.get("start"));
        Assertions.assertEquals(4, evaluate.get("end"));
        Assertions.assertEquals(List.of("l"), evaluate.get("groups"));
        Assertions.assertInstanceOf(Jsonata.Fn0.class, evaluate.get("next"));
    }
}
