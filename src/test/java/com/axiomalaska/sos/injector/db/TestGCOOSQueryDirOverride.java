package com.axiomalaska.sos.injector.db;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestGCOOSQueryDirOverride extends AbstractTestGCOOS {
    @BeforeClass
    public static void initConfig() {
        System.setProperty(DatabaseSosInjectorConfig.QUERY_PATH, "src/test/resources/gcoos/queries_named_params");        
        DatabaseSosInjectorConfig.initialize("gcoos/config.properties");
    }

    @Test
    public void testQueryDirOverride() {
        assertTrue(DatabaseSosInjectorConfig.instance().getQueryPath().contains("gcoos/queries_named_params"));
    }
}
