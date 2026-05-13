package org.slf4j.berserkr;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * In-memory ring buffer of log events for runtime-queryable debugging.
 *
 * Off by default. {@link #enable(int, String)} arms it with an explicit capacity
 * and minimum level — events below that level never enter the buffer.
 * {@link #disable()} drains and turns it off. Calls from logging hot paths short-circuit
 * on a single volatile read when disabled.
 */
public final class LogCache {

    private LogCache() {}

    private static final Object LOCK = new Object();

    private static volatile boolean enabled = false;
    private static volatile int minLevelInt = 0;
    private static volatile int capacity = 0;

    private static ArrayDeque<LogEvent> buffer = new ArrayDeque<>();
    private static long dropped = 0L;

    public static Stats enable(int newCapacity, String minLevel) {
        if (newCapacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive, got " + newCapacity);
        }
        int level = parseLevel(minLevel);
        synchronized (LOCK) {
            capacity = newCapacity;
            minLevelInt = level;
            buffer = new ArrayDeque<>(Math.min(newCapacity, 1024));
            dropped = 0L;
            enabled = true;
            return snapshotStats();
        }
    }

    public static Stats disable() {
        synchronized (LOCK) {
            Stats out = snapshotStats();
            out.enabled = false;
            enabled = false;
            buffer = new ArrayDeque<>();
            dropped = 0L;
            capacity = 0;
            minLevelInt = 0;
            return out;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static Stats clear() {
        synchronized (LOCK) {
            buffer.clear();
            dropped = 0L;
            return snapshotStats();
        }
    }

    public static Stats stats() {
        synchronized (LOCK) {
            return snapshotStats();
        }
    }

    /**
     * Hot-path entry called from {@link BerserkrLogger}. Returns immediately when
     * the cache is disabled or the event is below the configured minimum level.
     */
    static void offer(LogEvent event, int eventLevelInt) {
        if (!enabled) return;
        if (eventLevelInt < minLevelInt) return;
        synchronized (LOCK) {
            if (!enabled) return;
            while (buffer.size() >= capacity) {
                buffer.pollFirst();
                dropped++;
            }
            buffer.addLast(event);
        }
    }

    public static QueryResult query(QueryFilter filter) {
        if (filter == null) throw new IllegalArgumentException("filter required");
        if (filter.limit <= 0) throw new IllegalArgumentException("limit must be positive");
        if (filter.offset < 0) throw new IllegalArgumentException("offset must be non-negative");

        Pattern loggerRegex = null;
        if (filter.loggerPattern != null && !filter.loggerPattern.isEmpty()) {
            try {
                loggerRegex = Pattern.compile(filter.loggerPattern);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("invalid loggerPattern regex: " + e.getMessage());
            }
        }
        Integer minLevelFilter = filter.minLevel == null ? null : parseLevel(filter.minLevel);

        LogEvent[] snapshot;
        synchronized (LOCK) {
            snapshot = buffer.toArray(new LogEvent[0]);
        }

        // Walk newest-first.
        ArrayList<LogEvent> matches = new ArrayList<>();
        int total = 0;
        for (int i = snapshot.length - 1; i >= 0; i--) {
            LogEvent e = snapshot[i];
            if (minLevelFilter != null && e.getLevelInt() < minLevelFilter) continue;
            if (filter.since != null && e.getTime() < filter.since) continue;
            if (filter.until != null && e.getTime() > filter.until) continue;
            if (loggerRegex != null && !loggerRegex.matcher(e.getName() == null ? "" : e.getName()).find()) continue;
            if (filter.messageContains != null && !filter.messageContains.isEmpty()) {
                String msg = e.getMessage();
                if (msg == null || !msg.contains(filter.messageContains)) continue;
            }
            total++;
            if (total > filter.offset && matches.size() < filter.limit) {
                matches.add(e);
            }
        }
        QueryResult r = new QueryResult();
        r.events = matches;
        r.total = total;
        r.returned = matches.size();
        return r;
    }

    private static Stats snapshotStats() {
        Stats s = new Stats();
        s.enabled = enabled;
        s.capacity = capacity;
        s.minLevel = renderLevel(minLevelInt);
        s.dropped = dropped;
        s.size = buffer.size();
        if (!buffer.isEmpty()) {
            Iterator<LogEvent> it = buffer.iterator();
            LogEvent first = it.next();
            s.oldestTs = first.getTime();
            LogEvent last = first;
            while (it.hasNext()) last = it.next();
            s.newestTs = last.getTime();
        }
        return s;
    }

    static int parseLevel(String level) {
        if (level == null) throw new IllegalArgumentException("level required");
        switch (level.trim().toUpperCase()) {
            case "TRACE": return BerserkrLogger.LOG_LEVEL_TRACE;
            case "DEBUG": return BerserkrLogger.LOG_LEVEL_DEBUG;
            case "INFO":  return BerserkrLogger.LOG_LEVEL_INFO;
            case "WARN":  return BerserkrLogger.LOG_LEVEL_WARN;
            case "ERROR": return BerserkrLogger.LOG_LEVEL_ERROR;
            default: throw new IllegalArgumentException(
                "level must be one of TRACE,DEBUG,INFO,WARN,ERROR — got " + level);
        }
    }

    static String renderLevel(int levelInt) {
        if (levelInt == BerserkrLogger.LOG_LEVEL_TRACE) return "TRACE";
        if (levelInt == BerserkrLogger.LOG_LEVEL_DEBUG) return "DEBUG";
        if (levelInt == BerserkrLogger.LOG_LEVEL_INFO)  return "INFO";
        if (levelInt == BerserkrLogger.LOG_LEVEL_WARN)  return "WARN";
        if (levelInt == BerserkrLogger.LOG_LEVEL_ERROR) return "ERROR";
        return null;
    }

    public static final class Stats {
        public boolean enabled;
        public int capacity;
        public int size;
        public String minLevel;
        public Long oldestTs;
        public Long newestTs;
        public long dropped;
    }

    public static final class QueryFilter {
        public String minLevel;
        public String loggerPattern;
        public Long since;
        public Long until;
        public String messageContains;
        public int limit;
        public int offset;
    }

    public static final class QueryResult {
        public List<LogEvent> events;
        public int total;
        public int returned;
    }
}
