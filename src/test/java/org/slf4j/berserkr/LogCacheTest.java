package org.slf4j.berserkr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LogCacheTest {

    @BeforeEach
    void setUp() {
        if (LogCache.isEnabled()) LogCache.disable();
    }

    @AfterEach
    void tearDown() {
        if (LogCache.isEnabled()) LogCache.disable();
    }

    private static LogEvent ev(String logger, String msg, String level, long t) {
        return new LogEvent(logger, msg, level, BerserkrLoggerConfiguration.stringToLevel(level),
                "test-thread", t, null);
    }

    @Test
    void disabledByDefault() {
        assertFalse(LogCache.isEnabled());
        LogCache.offer(ev("a", "m", "INFO", 1L), Level.INFO.toInt());
        // Re-enabling should reveal nothing was buffered.
        LogCache.enable(10, "INFO");
        LogCache.QueryFilter f = new LogCache.QueryFilter(); f.limit = 100;
        assertEquals(0, LogCache.query(f).total);
    }

    @Test
    void enableRequiresPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> LogCache.enable(0, "INFO"));
        assertThrows(IllegalArgumentException.class, () -> LogCache.enable(-1, "INFO"));
    }

    @Test
    void enableRequiresValidLevel() {
        assertThrows(IllegalArgumentException.class, () -> LogCache.enable(10, "BANANA"));
        assertThrows(IllegalArgumentException.class, () -> LogCache.enable(10, null));
    }

    @Test
    void enableIsFreshStart() {
        LogCache.enable(10, "INFO");
        LogCache.offer(ev("a", "first", "INFO", 1L), Level.INFO.toInt());
        LogCache.QueryFilter f = new LogCache.QueryFilter(); f.limit = 100;
        assertEquals(1, LogCache.query(f).total);

        LogCache.enable(10, "INFO");
        assertEquals(0, LogCache.query(f).total, "re-enable should reset buffer");
    }

    @Test
    void insertTimeLevelGating() {
        LogCache.enable(10, "WARN");
        LogCache.offer(ev("a", "trace",  "TRACE", 1L), Level.TRACE.toInt());
        LogCache.offer(ev("a", "debug",  "DEBUG", 2L), Level.DEBUG.toInt());
        LogCache.offer(ev("a", "info",   "INFO",  3L), Level.INFO.toInt());
        LogCache.offer(ev("a", "warn",   "WARN",  4L), Level.WARN.toInt());
        LogCache.offer(ev("a", "error",  "ERROR", 5L), Level.ERROR.toInt());

        LogCache.QueryFilter f = new LogCache.QueryFilter(); f.limit = 100;
        LogCache.QueryResult r = LogCache.query(f);
        assertEquals(2, r.total);
        // newest-first ordering
        assertEquals("error", r.events.get(0).getMessage());
        assertEquals("warn",  r.events.get(1).getMessage());
    }

    @Test
    void ringOverflowDropsOldest() {
        LogCache.enable(3, "TRACE");
        for (int i = 0; i < 5; i++) {
            LogCache.offer(ev("a", "msg" + i, "INFO", i), Level.INFO.toInt());
        }
        LogCache.Stats s = LogCache.stats();
        assertEquals(3, s.size);
        assertEquals(2, s.dropped);
        assertEquals(2L, (long) s.oldestTs);
        assertEquals(4L, (long) s.newestTs);
    }

    @Test
    void queryFiltersByLoggerPattern() {
        LogCache.enable(100, "TRACE");
        LogCache.offer(ev("com.foo.A", "a", "INFO", 1L), Level.INFO.toInt());
        LogCache.offer(ev("com.bar.B", "b", "INFO", 2L), Level.INFO.toInt());
        LogCache.offer(ev("com.foo.C", "c", "INFO", 3L), Level.INFO.toInt());

        LogCache.QueryFilter f = new LogCache.QueryFilter();
        f.limit = 100;
        f.loggerPattern = "^com\\.foo\\..*";
        LogCache.QueryResult r = LogCache.query(f);
        assertEquals(2, r.total);
    }

    @Test
    void queryFiltersByTimeRangeAndMessageContains() {
        LogCache.enable(100, "TRACE");
        LogCache.offer(ev("a", "alpha apple",  "INFO", 100L), Level.INFO.toInt());
        LogCache.offer(ev("a", "beta apple",   "INFO", 200L), Level.INFO.toInt());
        LogCache.offer(ev("a", "gamma cherry", "INFO", 300L), Level.INFO.toInt());

        LogCache.QueryFilter f = new LogCache.QueryFilter();
        f.limit = 100;
        f.since = 150L;
        f.until = 250L;
        f.messageContains = "apple";
        LogCache.QueryResult r = LogCache.query(f);
        assertEquals(1, r.total);
        assertEquals("beta apple", r.events.get(0).getMessage());
    }

    @Test
    void queryLimitAndOffsetPaginateNewestFirst() {
        LogCache.enable(100, "TRACE");
        for (int i = 0; i < 5; i++) {
            LogCache.offer(ev("a", "m" + i, "INFO", i), Level.INFO.toInt());
        }
        LogCache.QueryFilter f = new LogCache.QueryFilter();
        f.limit = 2;
        LogCache.QueryResult page1 = LogCache.query(f);
        assertEquals(5, page1.total);
        assertEquals(2, page1.returned);
        assertEquals("m4", page1.events.get(0).getMessage());
        assertEquals("m3", page1.events.get(1).getMessage());

        f.offset = 2;
        LogCache.QueryResult page2 = LogCache.query(f);
        assertEquals(5, page2.total);
        assertEquals(2, page2.returned);
        assertEquals("m2", page2.events.get(0).getMessage());
        assertEquals("m1", page2.events.get(1).getMessage());
    }

    @Test
    void queryInvalidRegexThrows() {
        LogCache.enable(10, "TRACE");
        LogCache.QueryFilter f = new LogCache.QueryFilter();
        f.limit = 1;
        f.loggerPattern = "[unclosed";
        assertThrows(IllegalArgumentException.class, () -> LogCache.query(f));
    }

    @Test
    void disableReturnsFinalStatsAndDrains() {
        LogCache.enable(10, "TRACE");
        LogCache.offer(ev("a", "x", "INFO", 1L), Level.INFO.toInt());
        LogCache.Stats s = LogCache.disable();
        assertFalse(s.enabled);
        assertFalse(LogCache.isEnabled());
        // After disable, offer is a no-op.
        LogCache.offer(ev("a", "y", "INFO", 2L), Level.INFO.toInt());
    }

    @Test
    void clearEmptiesBufferAndDroppedCount() {
        LogCache.enable(2, "TRACE");
        for (int i = 0; i < 4; i++) {
            LogCache.offer(ev("a", "m" + i, "INFO", i), Level.INFO.toInt());
        }
        assertEquals(2, LogCache.stats().dropped);
        LogCache.clear();
        LogCache.Stats s = LogCache.stats();
        assertEquals(0, s.size);
        assertEquals(0, s.dropped);
    }

    @Test
    void concurrentAppendsThreadSafe() throws Exception {
        LogCache.enable(10_000, "TRACE");
        final int threads = 8;
        final int per = 1000;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int tid = t;
            Thread w = new Thread(() -> {
                ready.countDown();
                try { go.await(); } catch (InterruptedException ignored) {}
                for (int i = 0; i < per; i++) {
                    LogCache.offer(ev("t" + tid, "m" + i, "INFO", i), Level.INFO.toInt());
                }
            });
            workers.add(w);
            w.start();
        }
        ready.await(5, TimeUnit.SECONDS);
        go.countDown();
        for (Thread w : workers) w.join(10_000);

        LogCache.Stats s = LogCache.stats();
        assertEquals(threads * per, s.size + s.dropped);
    }
}
