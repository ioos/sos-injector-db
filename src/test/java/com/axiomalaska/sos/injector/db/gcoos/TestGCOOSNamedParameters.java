package com.axiomalaska.sos.injector.db.gcoos;

import org.junit.BeforeClass;

import com.axiomalaska.sos.injector.db.DatabaseSosInjectorConfig;

public class TestGCOOSNamedParameters extends AbstractTestGCOOS {
    @BeforeClass
    public static void initConfig() {
        DatabaseSosInjectorConfig.initialize("gcoos/config_named_params.properties");
    }
}
