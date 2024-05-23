package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import static java.util.Arrays.asList;
import static java.util.Map.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ParseIntegerTest {

    @Test
    public void parseIntegerNoError() {
        Jsonata expr = jsonata("$parseInteger('xyz','000')");
        var res = expr.evaluate(null);
        assertEquals(res, null);
    }
    
    @Disabled
    @Test
    public void parseIntegerError() {
        // The following test throws an error in the jsonata-js reference,
        // but DecimalFormat allows this format (plan is not to fix):
        assertThrows(Exception.class, () -> {
            Jsonata expr = jsonata("$parseInteger('000','xyz')");
            var res = expr.evaluate(null);
            assertEquals(res, null);
        } );
    }
}
