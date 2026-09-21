# otel-java-extensions

> **Note:** Splunk already provides supported no-code Java instrumentation for adding custom spans and attributes without changing application source. See [No-code instrumentation](https://help.splunk.com/en/splunk-observability-cloud/manage-data/instrument-back-end-services/instrument-back-end-applications-to-send-spans-to-splunk-apm/instrument-a-java-application/no-code-instrumentation). This extension can extend that already provided and supported capability (for example, extracting values as custom metrics) rather than replace it.

OpenTelemetry Java agent extension that extracts method parameters, return values, and instance fields as span attributes and metrics. Application code does not need to change. The extension bytecode is Java 8, so it can attach to Java 8+ applications. Pair it with OpenTelemetry Java agent 2.31.x. Other apps only need the extension jar plus a JSON config path (`OTEL_JAVAAGENT_EXTENSIONS` and `INSTRUMENTATION_CONFIG`).

It is an [OpenTelemetry Java agent extension](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/examples/extension/README.md). The extractor lives in `java-extension/src/main/java/com/example/javaagent/datacollector`.

## Requirements

- **Instrumented application:** Java 8 or later. The extension is compiled with `--release 8`.
- **OpenTelemetry Java agent:** **2.31.x** (this build uses `2.31.1`). The extension API is alpha, so pair the jar with the same agent line you compiled against.
- **Build JDK:** 17 or later (Gradle 9). The output bytecode is still Java 8.

## Attach to any Java 8+ app

No application code changes. Copy the extension jar and a JSON config, then point the existing OpenTelemetry Java agent at both.

1. Download `otel-java-extension-*-all.jar` from this repository's **Releases** page, or build once from `java-extension/`: `./gradlew assemble` → `build/libs/otel-java-extension-1.0-all.jar`. Push a `v*` tag (or run the **Release** workflow) to publish a new download.
2. Copy [instlocal.json](java-extension/src/main/resources/instlocal.json) and set `class` plus `onMethod` and/or `instance` for the target app
3. Set two settings (env vars or equivalent `-D` flags):

     ```bash
     export OTEL_JAVAAGENT_EXTENSIONS=/path/to/otel-java-extension-1.0-all.jar
     export INSTRUMENTATION_CONFIG=/path/to/instlocal.json
     ```

The process still needs `-javaagent:/path/to/opentelemetry-javaagent.jar` (agent **2.31.x**). That is the upstream agent, not this repo.

`OTEL_JAVAAGENT_EXTENSIONS` is the [standard agent extension setting](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/examples/extension/README.md). `INSTRUMENTATION_CONFIG` (or `-Dinstrumentation.config`) is this extractor’s config path.

If neither config setting is set, the jar loads the bundled sample `instlocal.json` (the sample-app `WebFrontEndController` methods). For any other app you must supply your own JSON.

Note: to load multiple extensions, you can specify a comma-separated list of extension jars or directories for `OTEL_JAVAAGENT_EXTENSIONS`.

For local Splunk export tests, copy `.env.example` to `.env` and set `SPLUNK_ACCESS_TOKEN`. The sample app does not read `.env`; `./run-sample.sh` exports those values into the process environment before start. In deployment, set the same variables as system environment variables instead.

## Embed extensions in the OpenTelemetry Agent

To simplify deployment, you can embed extensions into the OpenTelemetry Java Agent to produce a single jar file. With an integrated extension, you no longer need the `-Dotel.javaagent.extensions` command line option.

For more information, see the `extendedAgent` task in [java-extension/build.gradle](java-extension/build.gradle).

## Sample use case

A configuration file tells the extension which class and method to hook, and which values to extract.

[Sample configuration file](java-extension/src/main/resources/instlocal.json)

Each block needs a `class`. Include **only** the extracts you want — nothing else is required:

- `{ "class", "onMethod", "return": [...] }` — one return value
- `{ "class", "onMethod", "args": [{ "index": 0, "attribute": "..." }] }` — one method parameter
- `{ "class", "onMethod", "args": [...], "instance": [...] }` — a parameter plus an instance field/getter
- `{ "class", "instance": [{ "field" | "call" | "getter" }] }` — instance only; `onMethod` is not required

`id` is optional. `args`, `return`, and `instance` are all optional. A missing section is skipped, not an error.

Use `call` (or `getter`) for a no-arg getter such as `getName`, and `field` for a field such as `classVar`. Optional `path` is a list of `{ "call" }` / `{ "field" }` steps.

Instance-only blocks (no `onMethod`) hook every method of that class on their own. They do not require another method extract to exist. Prefer putting `instance` on the same block as `onMethod` when you already have a method hook, so you do not instrument the whole class.

Extracted values are added as span attributes. A histogram is emitted only when `createMetric` is `true` on that item. Metric names are `extract.{attribute}` (lowercase, dotted), with `extract.class` / `extract.method` (and `extract.id` if set). `addTagToMetric` copies that item onto the metric as a dimension. Invocation counters are not created unless you extract a value with `createMetric`.

<img width="292" alt="Screen Shot 2022-12-03 at 6 37 31 pm" src="https://user-images.githubusercontent.com/5012739/205430390-86aec7b6-1c39-4868-b5e2-bb34c820deab.png">

Splunk Observability Cloud can visualize the distribution of error or latency across different values of these extracted values.

<img width="931" alt="Screen Shot 2022-12-03 at 6 36 54 pm" src="https://user-images.githubusercontent.com/5012739/205430395-1cd40589-4050-4cc6-833e-e82d53acdfef.png">

Tags from the same intercept are added to the metric if `addTagToMetric` is `true` on that extract item.

<img width="1373" alt="Screen Shot 2022-12-03 at 6 11 37 pm" src="https://user-images.githubusercontent.com/5012739/205429555-0cd232df-f7e4-456f-a3c0-a08b7179fb56.png">

Extracted details are also pushed to stdout so that they can be analyzed using Splunk Cloud/Enterprise.
