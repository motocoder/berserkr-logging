package org.slf4j.berserkr;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class LogEvent {
    private String name;
    private String threadName;
    private String level;
    private long time;

//    this.message = event.getMessage().getFormattedMessage();
//        this.level = event.getLevel().toString();
//        this.logger = event.getLoggerName();
//        this.threadName = event.getThreadName();
//        this.timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeMillis()), ZoneId.systemDefault()).toString();

    public LogEvent(
        final String name,
        final String level,
        final String threadName,
        final long time
    ) {
        this.name = name;
        this.level = level;
        this.threadName = threadName;
        this.time = time;
    }
}
