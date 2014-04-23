package com.axiomalaska.sos.injector.db;

import org.junit.BeforeClass;

public class TestGCOOSNamedParameters extends AbstractTestGCOOS {
    @BeforeClass
    public static void initConfig() {
        DatabaseSosInjectorConfig.initialize("gcoos/config_named_params.properties");
    }
}
