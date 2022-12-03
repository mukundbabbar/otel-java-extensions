## Introduction

Extensions add new features and capabilities to the agent without having to create a separate distribution (for examples and ideas, see [Use cases for extensions](#sample-use-cases)).

The contents in this folder demonstrate how to create an extension for the OpenTelemetry Java instrumentation agent, with examples for every extension point. 

> Read both the source code and the Gradle build script, as they contain documentation that explains the purpose of all the major components.

## Build and add extensions

To build this extension project, run `./gradlew build`. You can find the resulting jar file in `build/libs/`. 

To add the extension to the instrumentation agent:

1. Copy the jar file to a host that is running an application to which you've attached the OpenTelemetry Java instrumentation.
2. Modify the startup command to add the full path to the extension file. For example:

     ```bash
     java -javaagent:path/to/opentelemetry-javaagent.jar \
          -Dotel.javaagent.extensions=build/libs/otel-java-extension-1.0-all.jar
          -Dinstrumentation.config=/home/ubuntu/inst2.json
          -jar myapp.jar
     ```
Note: to load multiple extensions, you can specify a comma-separated list of extension jars or directories (that
contain extension jars) for the `otel.javaagent.extensions` value.

## Embed extensions in the OpenTelemetry Agent

To simplify deployment, you can embed extensions into the OpenTelemetry Java Agent to produce a single jar file. With an integrated extension, you no longer need the `-Dotel.javaagent.extensions` command line option.

## Sample use case - Data extractor

Extensions are designed to override or customize the instrumentation provided by the upstream agent without having to create a new OpenTelemetry distribution or alter the agent code in any way.

Consider a scenario where we want to extract transaction data from within the code eg. method parameters or return value of a method and use them as tags or metrics.

In this example, a configuration file is used by the extension to instrument specific class:method and extract values when these are invoked.








For more examples, see [DemoServlet3InstrumentationModule](src/main/java/com/example/javaagent/instrumentation/DemoServlet3InstrumentationModule.java).


For more information, see the `extendedAgent` task in [build.gradle](build.gradle).
## Extensions examples

* Custom `IdGenerator`: [DemoIdGenerator](src/main/java/com/example/javaagent/DemoIdGenerator.java)
* Custom `TextMapPropagator`: [DemoPropagator](src/main/java/com/example/javaagent/DemoPropagator.java)
* Custom `Sampler`: [DemoSampler](src/main/java/com/example/javaagent/DemoSampler.java)
* Custom `SpanProcessor`: [DemoSpanProcessor](src/main/java/com/example/javaagent/DemoSpanProcessor.java)
* Custom `SpanExporter`: [DemoSpanExporter](src/main/java/com/example/javaagent/DemoSpanExporter.java)
* Additional instrumentation: [DemoServlet3InstrumentationModule](src/main/java/com/example/javaagent/instrumentation/DemoServlet3InstrumentationModule.java)

