# Interoperability with JSON libraries

jsonata-java is agnostic and works with any JSON library.

# Internal JSON representation

Consider the following JSON data:
```json
{
  "example":   [
    {
      "value": 4
    },
    {
      "value": 7
    },
    {
      "value": 13
    }
  ]
}
```

The internal JSON representation is based on Map for JSON objects and List for JSON arrays/collections.

This is the format jsonata-java operates on when evaluating expressions.
This format has to be used when JSON is fed as input into the evaluation function.


```Java
var data = Map.of("example",
    List.of(
        Map.of("value", 4),
        Map.of("value", 7),
        Map.of("value", 13)
    )
);
```

# Built-in JSON parsing and stringifying

jsonata-java comes with built-in support for decoding and stringifying JSON:

```Java
import com.dashjoin.jsonata.json.Json;

// jsonInput is a String or Reader:
var data = Json.parseJson(jsonInput);

var expression = jsonata("$sum(example.value)");
var result = expression.evaluate(data);  // returns 24
```

To output JSON, you can use the ```Functions.string``` function:
```Java
boolean beautify = true;
String toString = Functions.string(json, beautify);
```

See [these complete samples](https://github.com/dashjoin/jsonata-java/tree/main/samples/main) for both internal format and JSON parsing cases.

# Jackson Databind

Jackson integration is straightforward, just import the JSON as ```Object.class``` to feed it to jsonata-java:

```Java
var data = new ObjectMapper().readValue(json, Object.class);

var expression = jsonata("$sum(example.value)");
var result = expression.evaluate(data);  // returns 24
```

See [this complete sample](https://github.com/dashjoin/jsonata-java/tree/main/samples/main-jackson).

# Google GSON

GSON integration is straightforward, just import the JSON as ```Object.class``` to feed it to jsonata-java:

```Java
Gson gson = new Gson();
var data = gson.fromJson(json, Object.class); 

var expression = jsonata("$sum(example.value)");
var result = expression.evaluate(data);  // returns 24
```

See [this complete sample](https://github.com/dashjoin/jsonata-java/tree/main/samples/main-gson).
