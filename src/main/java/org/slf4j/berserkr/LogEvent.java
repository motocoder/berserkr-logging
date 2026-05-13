package org.slf4j.berserkr;

public class LogEvent {

    private String message;
    private String name;
    private String threadName;
    private String level;
    private int levelInt;
    private long time;
    private String throwable;

    public LogEvent() {
    }

    public LogEvent(
        final String name,
        final String message,
        final String level,
        final String threadName,
        final long time
    ) {
        this(name, message, level, 0, threadName, time, null);
    }

    public LogEvent(
        final String name,
        final String message,
        final String level,
        final int levelInt,
        final String threadName,
        final long time,
        final String throwable
    ) {
        this.message = message;
        this.name = name;
        this.level = level;
        this.levelInt = levelInt;
        this.threadName = threadName;
        this.time = time;
        this.throwable = throwable;
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

    public int getLevelInt() {
        return levelInt;
    }

    public void setLevelInt(int levelInt) {
        this.levelInt = levelInt;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getThrowable() {
        return throwable;
    }

    public void setThrowable(String throwable) {
        this.throwable = throwable;
    }

    public static String formatThrowable(Throwable t) {
        if (t == null) return null;
        StringBuilder sb = new StringBuilder(512);
        Throwable current = t;
        while (current != null) {
            if (current != t) sb.append("Caused by: ");
            sb.append(current).append('\n');
            for (StackTraceElement el : current.getStackTrace()) {
                sb.append("\tat ").append(el).append('\n');
            }
            current = current.getCause();
        }
        return sb.toString();
    }
}
