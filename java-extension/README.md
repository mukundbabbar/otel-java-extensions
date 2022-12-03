## Introduction

Extensions add new features and capabilities to the agent without having to create a separate distribution or changing the application code (for examples and ideas, see [Use cases for extensions](#sample-use-cases)).

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

[Sample configuration file](https://github.com/mukundbabbar/otel-java-extensions/blob/main/java-extension/src/main/resources/instlocal.json)

Output

Extracted values are added as span tags to assist with troubleshooting

<img width="292" alt="Screen Shot 2022-12-03 at 6 37 31 pm" src="https://user-images.githubusercontent.com/5012739/205430390-86aec7b6-1c39-4868-b5e2-bb34c820deab.png">

Splunk Observability Cloud to visualize the distribution of error or latency accross different values of these extracted values.

<img width="931" alt="Screen Shot 2022-12-03 at 6 36 54 pm" src="https://user-images.githubusercontent.com/5012739/205430395-1cd40589-4050-4cc6-833e-e82d53acdfef.png">

Histogram metric is created for any configured variable and for each of the extractor configured to count invocations. Tags from the same intercept of class:method are added to the metric if the AddTagToMetric property is set to true in the configuration file.

<img width="1373" alt="Screen Shot 2022-12-03 at 6 11 37 pm" src="https://user-images.githubusercontent.com/5012739/205429555-0cd232df-f7e4-456f-a3c0-a08b7179fb56.png">

Extracted details are also pushed to stdout so that they can be analyzed using Splunk Cloud/Enterprise platform.

## Extensions examples

* Custom `IdGenerator`: [DemoIdGenerator](src/main/java/com/example/javaagent/DemoIdGenerator.java)
* Custom `TextMapPropagator`: [DemoPropagator](src/main/java/com/example/javaagent/DemoPropagator.java)
* Custom `Sampler`: [DemoSampler](src/main/java/com/example/javaagent/DemoSampler.java)
* Custom `SpanProcessor`: [DemoSpanProcessor](src/main/java/com/example/javaagent/DemoSpanProcessor.java)
* Custom `SpanExporter`: [DemoSpanExporter](src/main/java/com/example/javaagent/DemoSpanExporter.java)
* Additional instrumentation: [DemoServlet3InstrumentationModule](src/main/java/com/example/javaagent/instrumentation/DemoServlet3InstrumentationModule.java)

For more examples, see [DemoServlet3InstrumentationModule](src/main/java/com/example/javaagent/instrumentation/DemoServlet3InstrumentationModule.java).

For more information, see the `extendedAgent` task in [build.gradle](build.gradle).

