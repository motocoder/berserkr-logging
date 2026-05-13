package org.slf4j.berserkr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the runtime controls added for remote logging management:
 *   - {@link BerserkrLogger#setShowConsole}/{@link BerserkrLogger#isShowConsole}
 *   - {@link BerserkrLogger#setLogLevel}/{@link BerserkrLogger#getLogLevel}
 *   - The runtime override path in {@link BerserkrLogger#isLevelEnabled}.
 */
class RuntimeLogControlTest {

    @BeforeEach
    void setUp() {
        BerserkrLogger.clearRuntimeLogLevel();
        BerserkrLogger.CONFIG_PARAMS.init();
    }

    @AfterEach
    void tearDown() {
        BerserkrLogger.clearRuntimeLogLevel();
        BerserkrLogger.setShowConsole(false);
    }

    // ----------------------------------------------------------------------
    // console toggle
    // ----------------------------------------------------------------------

    @Test
    void setShowConsole_roundTrip() {
        BerserkrLogger.setShowConsole(true);
        assertTrue(BerserkrLogger.isShowConsole());

        BerserkrLogger.setShowConsole(false);
        assertFalse(BerserkrLogger.isShowConsole());
    }

    // ----------------------------------------------------------------------
    // log-level override
    // ----------------------------------------------------------------------

    @Test
    void setLogLevel_roundTrip_allLevels() {
        BerserkrLogger.setLogLevel("TRACE"); assertEquals("TRACE", BerserkrLogger.getLogLevel());
        BerserkrLogger.setLogLevel("DEBUG"); assertEquals("DEBUG", BerserkrLogger.getLogLevel());
        BerserkrLogger.setLogLevel("INFO");  assertEquals("INFO",  BerserkrLogger.getLogLevel());
        BerserkrLogger.setLogLevel("WARN");  assertEquals("WARN",  BerserkrLogger.getLogLevel());
        BerserkrLogger.setLogLevel("ERROR"); assertEquals("ERROR", BerserkrLogger.getLogLevel());
        BerserkrLogger.setLogLevel("OFF");   assertEquals("OFF",   BerserkrLogger.getLogLevel());
    }

    @Test
    void setLogLevel_caseInsensitive() {
        BerserkrLogger.setLogLevel("warn");
        assertEquals("WARN", BerserkrLogger.getLogLevel());

        BerserkrLogger.setLogLevel("Debug");
        assertEquals("DEBUG", BerserkrLogger.getLogLevel());
    }

    @Test
    void getLogLevel_unset_readsConfigDefault() {
        // Without a runtime override, getLogLevel reflects CONFIG_PARAMS.defaultLogLevel.
        // Verify by mutating the config default and watching getLogLevel follow.
        final int original = BerserkrLogger.CONFIG_PARAMS.defaultLogLevel;
        try {
            BerserkrLogger.CONFIG_PARAMS.defaultLogLevel = BerserkrLogger.LOG_LEVEL_WARN;
            assertEquals("WARN", BerserkrLogger.getLogLevel());

            BerserkrLogger.CONFIG_PARAMS.defaultLogLevel = BerserkrLogger.LOG_LEVEL_ERROR;
            assertEquals("ERROR", BerserkrLogger.getLogLevel());
        } finally {
            BerserkrLogger.CONFIG_PARAMS.defaultLogLevel = original;
        }
    }

    @Test
    void runtimeOverride_winsOverPerInstanceLevel() {
        final BerserkrLogger logger = new BerserkrLogger("test.override");
        // Default config level is INFO, so info is enabled.
        assertTrue(logger.isInfoEnabled());

        BerserkrLogger.setLogLevel("ERROR");
        assertFalse(logger.isInfoEnabled());
        assertTrue(logger.isErrorEnabled());

        BerserkrLogger.setLogLevel("OFF");
        assertFalse(logger.isErrorEnabled());
    }

    @Test
    void runtimeOverride_affectsAlreadyConstructedLoggers() {
        // The whole point of the override is that loggers built before
        // setLogLevel still see the new threshold.
        final BerserkrLogger pre = new BerserkrLogger("test.pre");
        assertTrue(pre.isInfoEnabled());

        BerserkrLogger.setLogLevel("ERROR");

        final BerserkrLogger post = new BerserkrLogger("test.post");

        assertFalse(pre.isInfoEnabled());
        assertFalse(post.isInfoEnabled());
        assertTrue(pre.isErrorEnabled());
        assertTrue(post.isErrorEnabled());
    }

    @Test
    void clearRuntimeOverride_restoresPerInstanceBehavior() {
        final BerserkrLogger logger = new BerserkrLogger("test.clear");

        BerserkrLogger.setLogLevel("OFF");
        assertFalse(logger.isErrorEnabled());

        BerserkrLogger.clearRuntimeLogLevel();
        assertTrue(logger.isErrorEnabled());
    }

    @Test
    void setLogLevel_isIndependentOfConsole() {
        // Toggling one must not toggle the other.
        BerserkrLogger.setShowConsole(true);
        BerserkrLogger.setLogLevel("WARN");

        assertTrue(BerserkrLogger.isShowConsole());
        assertEquals("WARN", BerserkrLogger.getLogLevel());

        BerserkrLogger.setShowConsole(false);
        assertEquals("WARN", BerserkrLogger.getLogLevel());

        BerserkrLogger.setLogLevel("INFO");
        assertFalse(BerserkrLogger.isShowConsole());
    }
}
