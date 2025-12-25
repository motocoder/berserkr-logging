package org.slf4j.berserkr;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.helpers.Reporter;

public class BerserkrLoggerFactory implements ILoggerFactory {

    ConcurrentMap<String, Logger> loggerMap;

    public BerserkrLoggerFactory() {
        loggerMap = new ConcurrentHashMap<>();
        BerserkrLogger.lazyInit();
    }

    /**
     * Return an appropriate {@link BerserkrLogger} instance by name.
     *
     * This method will call {@link #createLogger(String)} if the logger
     * has not been created yet.
     */
    public Logger getLogger(String name) {
        return loggerMap.computeIfAbsent(name, this::createLogger);
    }

    /**
     * Actually creates the logger for the given name.
     */
    protected Logger createLogger(String name) {
        return new BerserkrLogger(name);
    }

    /**
     * Clear the internal logger cache.
     *
     * This method is intended to be called by classes (in the same package or
     * subclasses) for testing purposes. This method is internal. It can be
     * modified, renamed or removed at any time without notice.
     *
     * You are strongly discouraged from calling this method in production code.
     */
    protected void reset() {
        loggerMap.clear();
    }
}
