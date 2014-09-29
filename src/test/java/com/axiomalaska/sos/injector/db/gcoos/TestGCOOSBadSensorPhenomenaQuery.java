package com.axiomalaska.sos.injector.db.gcoos;

import org.junit.BeforeClass;
import org.junit.Test;

import com.axiomalaska.sos.exception.StationCreationException;
import com.axiomalaska.sos.injector.db.DatabaseSosInjectorConfig;

public class TestGCOOSBadSensorPhenomenaQuery extends AbstractTestGCOOS {
    @BeforeClass
    public static void initConfig() {
        DatabaseSosInjectorConfig.initialize("gcoos/config_bad_sensor_phenomena_query.properties");
    }

    @Test(expected=StationCreationException.class)
    @Override    
    public void testStationRetriever() throws StationCreationException {
        super.testStationRetriever();
    }

    @Test
    @Override
    public void testObservationRetriever() {
        //NOOP
    }

    @Test
    @Override
    public void testSosInjector() {
        //NOOP
    }
}
