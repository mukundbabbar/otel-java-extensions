# otel-java-extensions

OpenTelemetry Java agent extension that extracts method parameters, return values, and instance fields as span attributes and metrics. The extension bytecode is Java 8, so it can attach to Java 8+ applications. Pair it with OpenTelemetry Java agent 2.31.x. Other apps only need the extension jar plus a JSON config path (`OTEL_JAVAAGENT_EXTENSIONS` and `INSTRUMENTATION_CONFIG`).

See [java-extension/README.md](java-extension/README.md).

For local Splunk export tests, copy `.env.example` to `.env` and set `SPLUNK_ACCESS_TOKEN`. The sample app does not read `.env`; `./run-sample.sh` exports those values into the process environment before start. In deployment, set the same variables as system environment variables instead.
