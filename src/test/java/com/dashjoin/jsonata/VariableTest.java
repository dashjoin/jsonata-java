package com.dashjoin.jsonata;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.dashjoin.jsonata.json.Json;

public class VariableTest {

  @Test
  public void testContextVariable() {
    var input = Json.parseJson("{\n"
        + "        \"model\": {\n"
        + "          \"customer\": {\n"
        + "            \"identityDocumentNumber\": \"ABC123456\",\n"
        + "            \"identityDocumentType\": \"ID_CARD\"\n"
        + "          }\n"
        + "        }\n"
        + "      }");
    var e = Jsonata.jsonata("($~>|$|$.model|)@$.\n"
        + "                  ({\n"
        + "                      \"documentIdentityNumber\": customer.identityDocumentNumber,\n"
        + "                      \"documentIdentityType\": customer.identityDocumentType\n"
        + "                  })");
    Assertions.assertEquals("{documentIdentityNumber=ABC123456, documentIdentityType=ID_CARD}", e.evaluate(input) + "");
  }
  
  @Test void testContextVariableSimple() {
    var e = Jsonata.jsonata("model@$");
    Assertions.assertEquals(123, e.evaluate(Map.of("model", 123)));
  }
}
