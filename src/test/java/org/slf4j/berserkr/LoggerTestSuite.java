package org.slf4j.berserkr;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public abstract class LoggerTestSuite {

    private ListAppendingOutputStream prepareSink(List<String> outputList) {
        return new ListAppendingOutputStream(outputList);
    }

    @Test
    public void testTrace() {
        ArrayList<String> outputList = new ArrayList<>();
        Logger configuredLogger = createLogger(prepareSink(outputList), Level.TRACE);

        assertTrue(configuredLogger.isTraceEnabled());
        configuredLogger.trace("Simple trace message");

        assertEquals( 1, outputList.size());
        assertTrue( isTraceMessage(outputList.get(0)));
        assertEquals(
                     "Simple trace message",
                     extractMessage(outputList.get(0)));

        outputList.clear();

        configuredLogger.debug("Simple debug message");
        configuredLogger.info("Simple info message");
        configuredLogger.warn("Simple warn message");
        configuredLogger.error("Simple error message");
        assertEquals( 4, outputList.size());

    }

    @Test
    public void testDebug() {
        ArrayList<String> outputList = new ArrayList<>();
        Logger configuredLogger = createLogger(prepareSink(outputList), Level.DEBUG);

        configuredLogger.trace("Simple trace message");
        assertEquals(0, outputList.size());

        assertTrue( configuredLogger.isDebugEnabled());
        configuredLogger.debug("Simple debug message");

        assertEquals(1, outputList.size());
        assertTrue( isDebugMessage(outputList.get(0)));
        assertEquals(
                     "Simple debug message",
                     extractMessage(outputList.get(0)));

        outputList.clear();

        configuredLogger.info("Simple info message");
        configuredLogger.warn("Simple warn message");
        configuredLogger.error("Simple error message");
        assertEquals( 3, outputList.size());
    }


    @Test
    public void testInfo() {
        ArrayList<String> outputList = new ArrayList<>();
        Logger configuredLogger = createLogger(prepareSink(outputList), Level.INFO);

        configuredLogger.trace("Simple trace message");
        configuredLogger.debug("Simple debug message");
        assertEquals(0, outputList.size());

        assertTrue( configuredLogger.isInfoEnabled());
        configuredLogger.info("Simple info message");

        assertEquals( 1, outputList.size());
        assertTrue(isInfoMessage(outputList.get(0)));
        assertEquals(
                     "Simple info message",
                     extractMessage(outputList.get(0)));

        outputList.clear();

        configuredLogger.warn("Simple warn message");
        configuredLogger.error("Simple error message");
        assertEquals(2, outputList.size());
    }

    @Test
    public void testWarn() {
        ArrayList<String> outputList = new ArrayList<>();
        Logger configuredLogger = createLogger(prepareSink(outputList), Level.WARN);

        configuredLogger.trace("Simple trace message");
        configuredLogger.debug("Simple debug message");
        configuredLogger.info("Simple info message");
        assertEquals(0, outputList.size());

        assertTrue( configuredLogger.isWarnEnabled());
        configuredLogger.warn("Simple warn message");

        assertEquals( 1, outputList.size());
        assertTrue( isWarnMessage(outputList.get(0)));
        assertEquals(
                     "Simple warn message",
                     extractMessage(outputList.get(0)));

        outputList.clear();

        configuredLogger.error("Simple error message");
        assertEquals( 1, outputList.size());
    }

    @Test
    public void testError() {
        ArrayList<String> outputList = new ArrayList<>();
        Logger configuredLogger = createLogger(prepareSink(outputList), Level.ERROR);

        configuredLogger.trace("Simple trace message");
        configuredLogger.debug("Simple debug message");
        configuredLogger.info("Simple info message");
        configuredLogger.warn("Simple warn message");
        assertEquals( 0, outputList.size());

        assertTrue( configuredLogger.isErrorEnabled());
        configuredLogger.error("Simple error message");

        assertEquals( 1, outputList.size());
        assertTrue( isErrorMessage(outputList.get(0)));
        assertEquals(
                     "Simple error message",
                     extractMessage(outputList.get(0)));
    }

    @Test
    public void testFormatting() {
        ArrayList<String> outputList = new ArrayList<>();
        Logger configuredLogger = createLogger(prepareSink(outputList), Level.INFO);

        configuredLogger.info("Some {} string", "formatted");
        assertEquals( 1, outputList.size());
        assertEquals( "Some formatted string", extractMessage(outputList.get(0)));
    }

    @Test
    public void testException() {
        ArrayList<String> outputList = new ArrayList<>();
        Logger configuredLogger = createLogger(prepareSink(outputList), Level.INFO);

        Exception exception = new RuntimeException("My error");

        configuredLogger.info("Logging with an exception", exception);
        assertEquals(1, outputList.size());
        assertEquals(
                     "My error",
                     extractExceptionMessage(outputList.get(0)));

        assertEquals(
                     "java.lang.RuntimeException",
                     extractExceptionType(outputList.get(0)));
    }


    /**
     * Allows tests to check whether the log message contains a trace message.
     * Override if needed.
     * @param message String containing the full log message
     * @return whether it is a trace message or not
     */
    protected boolean isTraceMessage(String message) {
        return message.toLowerCase().contains("trace");
    }

    /**
     * Allows tests to check whether the log message contains a debug message.
     * Override if needed.
     * @param message String containing the full log message
     * @return whether it is a debug message or not
     */
    protected boolean isDebugMessage(String message) {
        return message.toLowerCase().contains("debug");
    }

    /**
     * Allows tests to check whether the log message contains an info message.
     * Override if needed.
     * @param message String containing the full log message
     * @return whether it is an info message or not
     */
    protected boolean isInfoMessage(String message) {
        return message.toLowerCase().contains("info");
    }

    /**
     * Allows tests to check whether the log message contains a warn message.
     * Override if needed.
     * @param message String containing the full log message
     * @return whether it is a warn message or not
     */
    protected boolean isWarnMessage(String message) {
        return message.toLowerCase().contains("warn");
    }

    /**
     * Allows tests to check whether the log message contains an error message.
     * Override if needed.
     * @param message String containing the full log message
     * @return whether it is an error message or not
     */
    protected boolean isErrorMessage(String message) {
        return message.toLowerCase().contains("error");
    }

    /**
     * Extracts only the part of the log string that should represent the `message` string.
     * @param message the full log message
     * @return only the supplied message
     */
    public abstract String extractMessage(String message);

    /**
     * Extracts only the part of the log string that should represent the supplied exception message, if any.
     * @param message the full log message
     * @return only the supplied exception message
     */
    public abstract String extractExceptionMessage(String message);

    /**
     * Extracts only the part of the log string that should represent the supplied exception type.
     * @param message the full log message
     * @return only the supplied exception type name
     */
    public abstract String extractExceptionType(String message);

    /**
     * Configures the logger for running the tests.
     * @param outputStream The output stream for logs to be written to
     * @param level The expected level the tests will run for this logger
     * @return a configured logger able to run the tests
     */
    public abstract Logger createLogger(ListAppendingOutputStream outputStream, Level level);

}