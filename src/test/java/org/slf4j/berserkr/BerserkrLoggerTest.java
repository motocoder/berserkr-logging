/**
 * Copyright (c) 2004-2022 QOS.ch Sarl (Switzerland)
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.slf4j.berserkr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class BerserkrLoggerTest {

    String A_KEY = BerserkrLogger.LOG_KEY_PREFIX + "a";
    PrintStream original = System.out;
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PrintStream replacement = new PrintStream(bout);

    @BeforeEach
    public void before() {
        System.setProperty(A_KEY, "info");
    }

    @AfterEach
    public void after() {
        System.clearProperty(A_KEY);
        System.clearProperty(BerserkrLogger.CACHE_OUTPUT_STREAM_STRING_KEY);
        System.clearProperty(BerserkrLogger.SHOW_THREAD_ID_KEY);
        System.clearProperty(BerserkrLogger.SHOW_THREAD_NAME_KEY);
        System.setErr(original);
    }

    @Test
    public void emptyLoggerName() {
        BerserkrLogger berserkrLogger = new BerserkrLogger("a");
        assertEquals("info", berserkrLogger.recursivelyComputeLevelString());
    }

    @Test
    public void offLevel() {
        System.setProperty(A_KEY, "off");
        BerserkrLogger.init();
        BerserkrLogger berserkrLogger = new BerserkrLogger("a");
        assertEquals("off", berserkrLogger.recursivelyComputeLevelString());
        assertFalse(berserkrLogger.isErrorEnabled());
    }

    @Test
    public void loggerNameWithNoDots_WithLevel() {
        BerserkrLogger.init();
        BerserkrLogger berserkrLogger = new BerserkrLogger("a");

        assertEquals("info", berserkrLogger.recursivelyComputeLevelString());
    }

    @Test
    public void loggerNameWithOneDotShouldInheritFromParent() {
        BerserkrLogger berserkrLogger = new BerserkrLogger("a.b");
        assertEquals("info", berserkrLogger.recursivelyComputeLevelString());
    }

    @Test
    public void loggerNameWithNoDots_WithNoSetLevel() {
        BerserkrLogger berserkrLogger = new BerserkrLogger("x");
        assertNull(berserkrLogger.recursivelyComputeLevelString());
    }

    @Test
    public void loggerNameWithOneDot_NoSetLevel() {
        BerserkrLogger berserkrLogger = new BerserkrLogger("x.y");
        assertNull(berserkrLogger.recursivelyComputeLevelString());
    }

    @Test
    public void checkUseOfLastSystemStreamReference() {
        BerserkrLogger.init();
        BerserkrLogger berserkrLogger = new BerserkrLogger(this.getClass().getName());

        System.setErr(replacement);
        berserkrLogger.info("hello");
        replacement.flush();
        assertTrue(bout.toString().contains("INFO " + this.getClass().getName() + " - hello"));
    }

    @Test
    public void checkUseOfCachedOutputStream() {
        System.setErr(replacement);
        System.setProperty(BerserkrLogger.CACHE_OUTPUT_STREAM_STRING_KEY, "true");
        BerserkrLogger.init();
        BerserkrLogger berserkrLogger = new BerserkrLogger(this.getClass().getName());
        // change reference to original before logging
        System.setErr(original);

        berserkrLogger.info("hello");
        replacement.flush();
        assertTrue(bout.toString().contains("INFO " + this.getClass().getName() + " - hello"));
    }

    @Test
    public void testTheadIdWithoutThreadName() {
        System.setProperty(BerserkrLogger.SHOW_THREAD_NAME_KEY, Boolean.FALSE.toString());
        String patternStr = "^tid=\\d{1,12} INFO org.slf4j.berserkr.BerserkrLoggerTest - hello";
        commonTestThreadId(patternStr);
    }

    @Test
    public void testThreadId() {
        String patternStr = "^\\[.*\\] tid=\\d{1,12} INFO org.slf4j.berserkr.BerserkrLoggerTest - hello";
        commonTestThreadId(patternStr);
    }

    private void commonTestThreadId(String patternStr) {
        System.setErr(replacement);
        System.setProperty(BerserkrLogger.SHOW_THREAD_ID_KEY, Boolean.TRUE.toString());
        BerserkrLogger.init();
        BerserkrLogger berserkrLogger = new BerserkrLogger(this.getClass().getName());
        berserkrLogger.info("hello");
        replacement.flush();
        String output = bout.toString();
        System.out.println(patternStr);
        System.out.println(output);
        assertTrue(Pattern.compile(patternStr).matcher(output).lookingAt());
    }
}
