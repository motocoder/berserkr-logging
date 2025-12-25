package org.slf4j.berserkr;

import org.junit.jupiter.api.Test;
import org.slf4j.helpers.Slf4jEnvUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Slf4jVersionTest {


    @Test
    public void slf4jVersionTest() {

        String version = Slf4jEnvUtil.slf4jVersion();
        assertNotNull(version);
        assertTrue(version.startsWith("2"));

    }

}
