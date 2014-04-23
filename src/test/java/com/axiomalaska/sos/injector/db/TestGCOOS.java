package com.axiomalaska.sos.injector.db;

import org.junit.BeforeClass;

public class TestGCOOS extends AbstractTestGCOOS {
    @BeforeClass
    public static void initConfig() {
        DatabaseSosInjectorConfig.initialize("gcoos/config.properties");
    }
}
