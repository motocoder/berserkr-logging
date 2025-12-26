package org.slf4j.berserkr;

import llc.berserkr.common.util.JacksonUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StandardIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(StandardIntegrationTest.class);

    @Test
    public void test() throws IOException, InterruptedException {

       Thread.sleep(2000);
        logger.debug("test");
        logger.info("test");
        logger.error("test");
        logger.warn("test");

        logger.debug("test", new Exception("test"));
        logger.info("test", new Exception("test"));
        logger.error("test", new Exception("test"));
        logger.warn("test", new Exception("test"));

    }
}
