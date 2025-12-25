package org.slf4j.berserkr;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class to verify configuration parameters of berserkrLogger
 *
 * @since 2.0.18
 */
public class ConfigParamsTest {


    private ListAppendingOutputStream prepareSink(List<String> outputList) {
        return new ListAppendingOutputStream(outputList);
    }

    @BeforeAll
    static public void resetConfigParams() {
        BerserkrLogger.CONFIG_PARAMS.init();
    }

    public Logger createLogger(ListAppendingOutputStream outputStream, Level level, String warnLevelString) {
        BerserkrLogger.CONFIG_PARAMS.outputChoice = new OutputChoice(new PrintStream(outputStream));

        BerserkrLogger.CONFIG_PARAMS.warnLevelString = warnLevelString;

        BerserkrLogger logger = new BerserkrLogger(ConfigParamsTest.class.getName());
        logger.currentLogLevel = BerserkrLoggerConfiguration.stringToLevel(level.toString());
        return logger;
    }

    @Test
    public void berserkrTest(){
        String WARN_LEVEL_STRING = "WXYZ";
        ArrayList<String> outputList = new ArrayList<>();
        Logger configuredLogger = createLogger(prepareSink(outputList), Level.TRACE,  WARN_LEVEL_STRING);

        configuredLogger.warn("This is a test");

        String str0 = outputList.get(0);
        assertTrue(str0.contains(WARN_LEVEL_STRING));

    }
}
