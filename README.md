# jsonata-java is the JSONata Java reference port
JSONata reference ported to Java

This is a 1:1 Java port of the [JSONata reference implementation](https://github.com/jsonata-js/jsonata)

## Features
* [100% JSONata feature compatibility](https://github.com/dashjoin/jsonata-java/actions/runs/5717119540/job/15490217787)
    - All JSONata language features supported
    - [100% reference test coverage](https://github.com/dashjoin/jsonata-java/actions/runs/5717119540/job/15490217787) [with well justified exceptions](https://github.com/dashjoin/jsonata-java/blob/main/test/test-overrides.json)
* Error messages matching the reference
    - Even stack traces are comparable
* Zero dependency and small
    - Only 160 kB total size
* [JSON parser agnostic](https://github.com/dashjoin/jsonata-java/blob/main/samples/json-interop.md)
    - use with Jackson, GSon, ...
    - comes with integrated vanilla parser
* Performance optimized & thread safe
    - [Native jsonata command line tool](https://github.com/dashjoin/jsonata-cli)
* Enterprise support
    - [Premium support available from the original developers](https://dashjoin.com)

## Quick Start

Add the dependency in pom.xml:
```xml
<dependency>
    <groupId>com.dashjoin</groupId>
    <artifactId>jsonata</artifactId>
    <version>0.9.8</version>
</dependency>
```
Here is the [release change log](changelog.md)

Main.java program:
```Java
import java.util.List; import java.util.Map;
import static com.dashjoin.jsonata.Jsonata.jsonata;

public class Main {
    public static void main(String[] args) {

        var data = Map.of("example",
            List.of(
                Map.of("value", 4),
                Map.of("value", 7),
                Map.of("value", 13)
            )
        );
        
        var expression = jsonata("$sum(example.value)");
        var result = expression.evaluate(data);  // returns 24
        System.out.println(result);
    }
}
```

### Custom Functions

You can define custom functions and declare variables via a JSONata frame or 
the registerFunction method:

```Java
        var expression = jsonata("$sum(example.value) + $sin($PI/2)");

        // Default JSONata has no $sin function and no $PI, so define it
        var env = expression.createFrame();
        env.bind("sin", (Number n) -> Math.sin( n.doubleValue() ) );
        env.bind("PI", Math.PI);

        var result = expression.evaluate(data, env);  // returns 25
```

For more examples, please refer to this [test case](https://github.com/dashjoin/jsonata-java/blob/main/src/test/java/com/dashjoin/jsonata/CustomFunctionTest.java).

### Execution Scope

`Jsonata.Frame` represents the execution scope or environment for a JSONata expression. It serves two primary purposes:

1. Variable Bindings: It holds the symbol table for an expression, mapping variable names (like $name) to their corresponding Java objects.
2. Execution Control: It allows you to set resource constraints on the evaluation to prevent expressions from running for too long or consuming too many resources, which is crucial for security and stability.

When you call `jsonata.evaluate(data, frame)`, the evaluation runs within the context of that frame.

You can set an execution timeout and a maximum recursion depth using the `setRuntimeBounds(long timeout, int maxRecursionDepth)` method on a Frame instance.

* timeout: The maximum time in milliseconds that the evaluation is allowed to run. If it exceeds this time, a JException is thrown.
* maxRecursionDepth: The maximum depth of the function call stack. This prevents infinite recursion (e.g., from a recursive function definition in the expression) and protects against StackOverflowError. If the depth is exceeded, a JException is thrown.

See [JsonataTest.java](https://github.com/dashjoin/jsonata-java/blob/8ebe8d24e35c6e2fac70aa86d3e3820af5adab5b/src/test/java/com/dashjoin/jsonata/JsonataTest.java#L52) for example use.

## History
We needed a high performance and 100% compatible engine for the ETL and data transformations of the [Dashjoin Low Code platform](https://github.com/dashjoin/platform). Being a JSON full stack based on Quarkus/Java, JSONata was a very good fit and is even more today.

In the beginning we used the original Java port, but quickly got lots of issues due to unsupported features and errors that we could not reproduce easily.
The next solution which was running quite well and stable was to use GraalVM's Javascript engine to run the jsonata-js reference implementation in process of the Java backend. This works OK, but there are performance compromises, especially when there are many switches between the Javascript and the Java context (as is the case with ETL and data transformations).

## Design
Working with Java since its inception in 1996, we made an experiment to see what it would take to port the existing reference Javascript into working and performant Java. This experiment went so well that we decided to work on a 100% port of the JSONata reference engine - the result which you can see in this repository.

### No generic Java types
To get a 1:1 readable port, we decided to not use any generic types (yes, so basically this looks like 20 years old Java code...) -
but it has many advantages in this specific case:
* Java code nearly looks the same as Javascript
* Patches and fixes coming into the Javascript reference are easily portable

We lose the type safety and compile time checks Java generics introduced, but since the job is to port Javascript code, we are in an 'un-typed' world anyway.

### No JSON wrapper library
To get as near as possible to the Javascript syntax, decision was made to use
* java.util.Map as Javascript object
    - which in turn represents a JSON object
* java.util.List as Javascript array
    - which in turn represents JSON lists/JSONata sequences
* String, numbers, and boolean can be used as well
* [This test case](https://github.com/dashjoin/jsonata-java/blob/main/src/test/java/com/dashjoin/jsonata/TypesTest.java) documents which types are legal and how you can use a JSON library like Jackson to convert Pojos and other types

No JSON lib like Jackson is being used. This has advantages, but needs careful design w.r.t. how the logic is being ported.
### The big 'null vs undefined' question
Porting Javascript code gets ambiguous as soon as there is a boolean expression that might depend on null and/or undefined.
In Java there are basically these solutions:
* use a Holder class that can disambiguate the null/undefined cases
* Java null means null, use a special value/object for UNDEFINED_VALUE
* Java null means undefined, use a special value/object for NULL_VALUE

JSON libs will usually use a Holder variant (implemented in some JSONValue implementation).
After review, it turned out that we can stay as near as possible to the original code structure (with as little special code as possible) by using the 3rd variant.

### Numeric Values

Compared to Java, Javascript handles numbers quite differently.
Whenever a computation is done, the engine tries to "fit" the result into an int, long, or double (see com.dashjoin.jsonata.Utils.convertNumber(Number)).
This [test case](https://github.com/dashjoin/jsonata-java/blob/main/src/test/java/com/dashjoin/jsonata/NumberTest.java)
shows this behavior.

## Performance
We conducted some experiments to measure performance, but it's not an 'overall benchmark' yet. Your mileage may vary...

|Expression| jsonata-js | JSONata4Java | jsonata-java | speedup factor |
|----------|------------|--------------|---|---|
| function-sift 4 | 26.1 / 109.6 | 36.1 / 144.8 | 140.8 / 348.2 | 3.9 / 2.3 |
| hof-map 0 | 16.4 / 62.2 | 17.7 / 352.8 | 66.2 / 295.2 | 3.7 / 0.8 |
| hof-zip 2 | 15.4 / 59.2 | 16.7 / exception | 64.2 / 312.6 | 3.8 / ? |
| hof-zip-map 0 | 16.0 / 57.3 | 12.6 / 227.7 (wrong) | 58.3 / 323.6 | 4.6 / 1.4 |
| partial-application 2 | 26.1 / 29.1 | parser error | 162.3 / 133.4 | ? / ? |
| [1..500].($*$)~>$sum | 24.4 / 1.8 | 159.6 / exception | 286.4 / 9.0 | 1.8 / ? |

- Expression denotes the test suite name and case.
- First figure = parse operations, second figure = evaluate operations.
- Performance measured in kiloOps/s (thousands of operations per second), higher means faster.
- Speedup factor compared to JSONata4Java (2.0 means "twice as fast").

## Developers: getting started

The project uses the repository of the reference implementation as a submodule.
This allows referencing the current version of the unit tests.
To clone this repository, run:

```
git clone --recurse-submodules https://github.com/dashjoin/jsonata-java
```

To compile, generate / run the unit tests, and create the jar file, run:

```
mvn compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.dashjoin.jsonata.Generate
mvn install
```

## Contribute

We welcome contributions. If you are interested in contributing to Dashjoin, let us know!
You'll get to know an open-minded and motivated team working together to build the next generation platform.

* [Join our Slack](https://join.slack.com/t/dashjoin/shared_invite/zt-1274qbzq9-mwxBq4WwSTJsITjrvYV4pA) and say hello
* [Follow us](https://twitter.com/dashjoin) on Twitter
* [Submit](https://github.com/dashjoin/jsonata-java/issues) your ideas by opening an issue with the enhancement label
* [Help out](https://github.com/dashjoin/jsonata-java/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) by fixing "a good first issue"
