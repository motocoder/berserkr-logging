package org.slf4j.berserkr;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class LogEvent {

    private String message;
    private String name;
    private String threadName;
    private String level;
    private long time;

    public LogEvent() {
    }

    public LogEvent(
        final String name,
        final String message,
        final String level,
        final String threadName,
        final long time
    ) {
        this.message = message;
        this.name = name;
        this.level = level;
        this.threadName = threadName;
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
