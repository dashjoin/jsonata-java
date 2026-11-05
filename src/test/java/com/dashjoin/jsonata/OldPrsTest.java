package com.dashjoin.jsonata;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.dashjoin.jsonata.Jsonata.JFunction;
import com.dashjoin.jsonata.json.Json;

/** tests from PRs #80, #85, #87, adapted to the existing API (partial application of custom functions without signature: see #83) */
public class OldPrsTest {

  static Object res(String name) throws Exception {
    return Json.parseJson(new InputStreamReader(
        OldPrsTest.class.getClassLoader().getResourceAsStream(name), StandardCharsets.UTF_8));
  }

  // #80
  @Test
  public void testEachEmptyArray() {
    Assertions.assertNull(Jsonata.jsonata("[] ~> $each(?, function($v) { $v })").evaluate("{}"));
  }

  @Test
  public void testEachArrayWithData() {
    Assertions.assertEquals(List.of(123, 321),
        Jsonata.jsonata("[123, 321] ~> $each(?, function($v) { $v })").evaluate("{}"));
  }

  @Test
  public void testMapInPipe() throws Exception {
    var expression = Jsonata.jsonata("$sift($.buildings, function($v, $k) { $k ~> /o/ and $v.presentation.*.widget})"
        + "                    ~> $each(?, function($v, $key) {{ $key: $sift($v.presentation, function($v, $k){ $v.widget }) }})"
        + "                    ~> $map(?, function($v){($v.*)})");
    Assertions.assertEquals(res("map_in_pipe/answer.json"), expression.evaluate(res("map_in_pipe/input.json")));
  }

  // #85 (PR adds a name / arity constructor, here: plain constructor)
  @SuppressWarnings("unchecked")
  @Test
  public void testFunctionWithInputData() {
    JFunction addWithFactor = new JFunction((input, args) -> {
      int factor = Integer.parseInt(((Map<String, Object>) input).get("factor").toString());
      return ((Integer) args.get(0) + (Integer) args.get(1)) * factor;
    }, null);
    var expression = Jsonata.jsonata("$addWithFactor(1, 2)");
    expression.registerFunction("addWithFactor", addWithFactor);
    Assertions.assertEquals(9, expression.evaluate(Map.of("factor", "3")));
  }

  // #87
  @Test
  public void testMapWithAppend() {
    Assertions.assertEquals(Json.parseJson("{\"systems\": \"asd\"}"),
        Jsonata.jsonata("{'systems': 'asd'}.$map($, $append(?))").evaluate(null));
  }

  @Test
  public void testMapWithAppend2() {
    Object answer = Json.parseJson("{\"systems\": \"asd\"}");
    Assertions.assertEquals(answer, Jsonata.jsonata("$map($, $append(?))").evaluate(answer));
  }
}
