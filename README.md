# jsonata-java is the JSONata Java reference port
JSONata reference ported to Java

This is a 1:1 Java port of the [JSONata reference implementation](https://github.com/jsonata-js/jsonata)

## Goals
* 100% feature compatibility
    - All JSONata language features supported
* Error messages matching the reference
    - Even stack traces are comparable
* Zero dependency and small
    - Only 160 kB total size
* JSON parser agnostic
    - use with Jackson, GSon, ...
    - comes with integrated vanilla parser
* Performance optimized
* Native tool & native compilation
* Enterprise support
    - [Premium support available from the original developers](https://dashjoin.com)

## History
We needed a high performance and 100% compatible engine for the ETL and data transformations of the Dashjoin Low Code platform. Being a JSON full stack based on Quarkus/Java, JSONata was a very good fit and is even more today.

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
* Java java.util.Map as Javascript object
    - which in turn represents a JSON object
* Java java.util.List as Javascript array
    - which in turn represents JSON lists/JSONata sequences

So no JSON lib like Jackson is being used. This has advantages, but needs careful design w.r.t. how the logic is being ported.
### The big 'null vs undefined' question
Porting Javascript code gets ambiguous as soon as there is a boolean expression that might depend on null and/or undefined.
In Java there are basically these solutions:
* use a Holder class that can disambiguate the null/undefined cases
* use a special value/object for UNDEFINED_VALUE
* use a special value/object for NULL_VALUE

JSON libs will usually use a Holder variant (implemented in some JSONValue implementation).
After review, it turned out that we can stay as near as possible to the original code structure (with as little special code as possible) by using the 3rd variant.

