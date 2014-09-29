package com.axiomalaska.sos.injector.db.gcoos;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axiomalaska.sos.injector.db.DatabaseSosInjectorConfig;
import com.axiomalaska.sos.injector.db.DatabaseSosInjectorConstants;

public class TestGCOOSQueryDirOverride extends AbstractTestGCOOS {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestGCOOSQueryDirOverride.class);

    @BeforeClass
    public static void initConfig() {
        System.setProperty(DatabaseSosInjectorConstants.ENV_QUERY_PATH, "src/test/resources/gcoos/queries_named_params");        
        DatabaseSosInjectorConfig.initialize("gcoos/config.properties");
    }

    @AfterClass
    public static void cleanUpSystemProperty() {
        LOGGER.debug("Unsetting " + DatabaseSosInjectorConstants.ENV_QUERY_PATH + " environment variable");
        System.clearProperty(DatabaseSosInjectorConstants.ENV_QUERY_PATH);        
    }

    @Test
    public void testQueryDirOverride() {
        assertTrue(DatabaseSosInjectorConfig.instance().getQueryPath().contains("gcoos/queries_named_params"));
    }
}
