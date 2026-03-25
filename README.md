# berserkr-logging

A custom SLF4J binding that forwards log events through an encrypted appender to [Berserkr Chainsaw](https://github.com/motocoder/chainsaw) — a remote log viewer and aggregation tool.

All log data is encrypted from the client using this library to the chainsaw receiver.

---

## Overview

`berserkr-logging` implements the SLF4J provider API, meaning it plugs directly into any SLF4J-compatible application with no code changes. When a log event is emitted, the library:

1. Formats the message locally (optionally printing to console)
2. Serializes it as a JSON `LogEvent` payload containing: logger name, message, level, thread name, and timestamp
3. Transmits the payload asynchronously over an encrypted, authenticated socket connection to the Berserkr Chainsaw server at `www.berserkr.llc`

The connection lifecycle is managed by a `CleanupManager` that automatically restarts the gateway session if the connection is lost.

---

## Architecture

```
Your Application
     │
     │  SLF4J API
     ▼
BerserkrLoggerFactory  ──►  BerserkrLogger
                                  │
                        ┌─────────┴──────────┐
                        │                    │
                   Console output     ExecutorService
                   (optional)               │
                                   AppenderGatewaySession
                                            │
                                   ┌────────┴────────┐
                                   │                 │
                              HTTP (Retrofit)   Encrypted Socket
                              /chainsawchoker/  (AuthenticatingPayloadGateway)
                              channel?...            │
                                   │           Chainsaw Server
                              ChannelResponse        │
                              (port number)    Broadcast payload
```

### Key Classes

| Class | Package | Responsibility |
|---|---|---|
| `BerserkrServiceProvider` | `org.slf4j.berserkr` | SLF4J 2.x service provider entry point |
| `BerserkrLoggerFactory` | `org.slf4j.berserkr` | Creates and caches `BerserkrLogger` instances |
| `BerserkrLogger` | `org.slf4j.berserkr` | Core logger — formats messages and dispatches events |
| `BerserkrLoggerConfiguration` | `org.slf4j.berserkr` | Loads `berserkrlogger.properties` and exposes config |
| `LogEvent` | `org.slf4j.berserkr` | JSON-serializable payload: name, message, level, thread, timestamp |
| `AppenderGatewaySession` | `llc.berserkr.logging` | Manages the HTTP channel launch + authenticated socket connection |
| `LaunchAPI` | `llc.berserkr.logging` | Retrofit interface — calls `GET /chainsawchoker/channel` to obtain a port |
| `ChannelResponse` | `llc.berserkr.logging` | Deserializes the port number returned by the channel launch endpoint |
| `BerserkrMDCAdapter` | `org.slf4j.berserkr` | MDC (Mapped Diagnostic Context) adapter |

### Connection Flow

1. On first log event, `BerserkrLogger.lazyInit()` configures the system and starts a `CleanupManager`.
2. The `CleanupManager` builds an `AppenderGatewaySession` targeting `www.berserkr.llc`.
3. `AppenderGatewaySession.start()` calls the REST endpoint `GET /chainsawchoker/channel?channel=<guid>&password=<password>` via Retrofit/OkHttp to allocate a dynamic port.
4. A `SocketClientConnection` is opened to the returned port, and `AuthenticatingPayloadGateway` authenticates the session using the configured password.
5. Subsequent log events are serialized to JSON and sent as `BROADCAST` (`'B'`) framed byte payloads over this socket.
6. If the connection drops, the `CleanupManager` tears down and rebuilds the session automatically.

---

## Installation

### Gradle

```groovy
implementation("llc.berserkr:logging:1.0.2")
```

### Maven

```xml
<dependency>
    <groupId>llc.berserkr</groupId>
    <artifactId>logging</artifactId>
    <version>1.0.2</version>
</dependency>
```

Because this is an SLF4J binding, **do not** include another SLF4J implementation (e.g. `slf4j-simple`, `logback-classic`) on the classpath alongside it.

---

## Configuration

Add a file named `berserkrlogger.properties` to your `src/main/resources` directory:

```properties
# berserkrLogger configuration file
# Implementation of Logger that sends all enabled log messages, for all defined loggers, to System.err.

# Default logging detail level for all instances of berserkrLogger.
# Must be one of ("trace", "debug", "info", "warn", or "error").
# If not specified, defaults to "info".
org.slf4j.berserkrLogger.defaultLogLevel=trace

# Logging detail level for a berserkrLogger instance named "xxxxx".
# Must be one of ("trace", "debug", "info", "warn", or "error").
# If not specified, the default logging detail level is used.
#org.slf4j.berserkrLogger.log.xxxxx=

# Set to true if you want the current date and time to be included in output messages.
# Default is false, and will output the number of milliseconds elapsed since startup.
org.slf4j.berserkrLogger.showDateTime=true

# The date and time format to be used in the output messages.
# The pattern describing the date and time format is the same that is used in java.text.SimpleDateFormat.
# If the format is not specified or is invalid, the default format is used.
# The default format is yyyy-MM-dd HH:mm:ss:SSS Z.
org.slf4j.berserkrLogger.dateTimeFormat=yyyy-MM-dd HH:mm:ss:SSS Z

# Set to true if you want to output the current thread name.
# Defaults to true.
org.slf4j.berserkrLogger.showThreadName=true

# Set to true if you want the Logger instance name to be included in output messages.
# Defaults to true.
org.slf4j.berserkrLogger.showLogName=true

# Set to true if you want the last component of the name to be included in output messages.
# Defaults to false.
org.slf4j.berserkrLogger.showShortLogName=true

org.slf4j.berserkrLogger.logFile=System.out

# Your Berserkr Chainsaw credentials
org.slf4j.berserkrLogger.password=yourPasswordGoesHere
org.slf4j.berserkrLogger.guid=yourGuidGoesHere

# Tag prepended to all log output (e.g. an app or module name)
org.slf4j.berserkrLogger.tag=logcatTag

# Set to true to also print log output to the local console/stream
org.slf4j.berserkrLogger.console=true
```

### Configuration Properties Reference

| Property | Default | Description |
|---|---|---|
| `defaultLogLevel` | `info` | Minimum level to log. One of `trace`, `debug`, `info`, `warn`, `error`. |
| `log.<name>` | _(unset)_ | Override log level for a specific logger by name (supports hierarchical prefix matching). |
| `showDateTime` | `false` | Prepend formatted date/time to each message. |
| `dateTimeFormat` | `yyyy-MM-dd HH:mm:ss:SSS Z` | `SimpleDateFormat` pattern for the timestamp. |
| `showThreadName` | `true` | Prepend `[thread-name]` to each message. |
| `showThreadId` | `false` | Prepend `tid=<id>` to each message. |
| `showLogName` | `true` | Prepend the fully qualified logger name. |
| `showShortLogName` | `false` | Prepend only the last segment of the logger name (overrides `showLogName`). |
| `levelInBrackets` | `false` | Wrap the level label in brackets, e.g. `[INFO]`. |
| `warnLevelString` | `WARN` | Custom label for WARN-level messages. |
| `logFile` | `System.err` | Output target: `System.err`, `System.out`, or a file path. |
| `cacheOutputStream` | `false` | Cache the reference to `System.err`/`System.out` at startup. |
| `password` | `password` | Authentication password for the Chainsaw gateway. **Set this.** |
| `guid` | `berserkr` | Channel identifier (GUID) used to launch the Chainsaw channel. **Set this.** |
| `tag` | `undefined-tag` | Tag string prepended to log messages. Omitted from output if left as default. |
| `console` | `false` | When `true`, also writes formatted log output to the configured `logFile` stream. |

All properties can alternatively be set as JVM system properties (e.g. `-Dorg.slf4j.berserkrLogger.defaultLogLevel=debug`). System properties take precedence over the properties file.

---

## ProGuard / R8

If you are using ProGuard or R8 (common in Android builds), add these keep rules to prevent the logging classes from being obfuscated or stripped:

```
-keep class org.slf4j.berserkr.** {
    public protected private *;
}
-keep class llc.berserkr.logging.** {
    public protected private *;
}
```

---

## Log Event Payload

Each log event is transmitted as a JSON object with the following fields:

```json
{
  "name": "com.example.MyClass",
  "message": "User logged in successfully",
  "level": "INFO",
  "threadName": "main",
  "time": 1711234567890
}
```

Events are dispatched on a dedicated single-threaded `ExecutorService` to avoid blocking the calling thread.

---

## Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `org.slf4j:slf4j-api` | `2.0.17` | SLF4J provider API |
| `com.squareup.retrofit2:retrofit` | `2.9.0` | HTTP client for channel launch |
| `com.squareup.retrofit2:converter-gson` | `2.9.0` | JSON deserialization of channel response |
| `llc.berserkr:common` | `1.0.2` | Shared payload/socket/auth utilities |

---

## Related Projects

- [Berserkr Chainsaw](https://github.com/motocoder/chainsaw) — the remote log receiver that accepts and displays events forwarded by this library

---

## Credits

Basic logger skeleton inspired by [slf4j-simple](https://github.com/qos-ch/slf4j/tree/master/slf4j-simple).

## License

MIT — see [LICENSE](https://github.com/motocoder/berserkr-logging?tab=MIT-1-ov-file#readme)
