package com.axiomalaska.sos.injector.db.gcoos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axiomalaska.ioos.sos.GeomHelper;
import com.axiomalaska.ioos.sos.exception.UnsupportedGeometryTypeException;
import com.axiomalaska.sos.ObservationRetriever;
import com.axiomalaska.sos.SosInjector;
import com.axiomalaska.sos.StationRetriever;
import com.axiomalaska.sos.data.ObservationCollection;
import com.axiomalaska.sos.data.PublisherInfo;
import com.axiomalaska.sos.data.SosStation;
import com.axiomalaska.sos.exception.InvalidObservationCollectionException;
import com.axiomalaska.sos.exception.ObservationRetrievalException;
import com.axiomalaska.sos.exception.SosCommunicationException;
import com.axiomalaska.sos.exception.SosUpdateException;
import com.axiomalaska.sos.exception.StationCreationException;
import com.axiomalaska.sos.exception.UnsupportedSosAssetTypeException;
import com.axiomalaska.sos.injector.db.DatabaseObservationRetriever;
import com.axiomalaska.sos.injector.db.DatabaseSosInjectorConfig;
import com.axiomalaska.sos.injector.db.DatabaseStationRetriever;
import com.axiomalaska.sos.injector.db.data.DatabasePhenomenon;
import com.axiomalaska.sos.injector.db.data.DatabaseSosSensor;
import com.axiomalaska.sos.injector.db.data.DatabaseSosStation;

public abstract class AbstractTestGCOOS {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTestGCOOS.class);

    @AfterClass
    public static void cleanUp() {
        LOGGER.debug("Cleaning sos-injector-db test config");
        DatabaseSosInjectorConfig.cleanUp();
    }

    @Before
    public void setUp() {
        //only run tests if the gcoos.sqlite database exists
        Assume.assumeTrue(new File("src/test/resources/gcoos/gcoos.sqlite").exists());
    }
    
    @Test
    public void testConfigFileLoading() {
        PublisherInfo publisherInfo = DatabaseSosInjectorConfig.instance().getPublisherInfo();
        assertEquals(publisherInfo.getName(), "Some Publisher");
        assertEquals(publisherInfo.getCode(), "spcode");
        assertEquals(publisherInfo.getCountry(), "Paraguay");
        assertEquals(publisherInfo.getWebAddress(), "http://www.somepublisher.org");
        assertEquals(publisherInfo.getEmail(), "info@somepublisher.org");
    }

    @Test
    public void testStationRetriever() throws StationCreationException {
        StationRetriever stationRetriever = new DatabaseStationRetriever();
        List<SosStation> stations = stationRetriever.getStations();
        assertNotNull(stations);
        assertFalse(stations.isEmpty());
    }

    @Test
    public void testObservationRetriever() {
        ObservationRetriever observationRetriever = new DatabaseObservationRetriever();
        DatabaseSosStation dbStation = new DatabaseSosStation();
        dbStation.setDatabaseId("1");
        DatabaseSosSensor dbSensor = new DatabaseSosSensor();
        dbSensor.setDatabaseId("1");
        dbSensor.setStation(dbStation);
        dbSensor.setLocation(GeomHelper.createLatLngPoint(62.0, -140.0));
        DatabasePhenomenon dbPhenomenon = new DatabasePhenomenon();
        dbPhenomenon.setDatabaseId("9");        
        dbPhenomenon.setTag("air_temperature");
        DateTime startDate = new DateTime(2014, 2, 1, 0, 0, DateTimeZone.UTC);
        List<ObservationCollection> observationCollections = observationRetriever.getObservationCollection(dbSensor, dbPhenomenon,
                new DateTime(2014, 2, 1, 0, 0, DateTimeZone.UTC));
        assertNotNull(observationCollections);
        assertFalse(observationCollections.isEmpty());
        for (ObservationCollection observationCollection : observationCollections) {
            assertEquals(dbSensor, observationCollection.getSensor());
            assertEquals(dbPhenomenon, observationCollection.getPhenomenon());
            for (DateTime obsDate : observationCollection.getObservationValues().keySet()) {
                assertTrue("Start date (" + startDate + ") should be before obsDate (" + obsDate + ")",
                        startDate.isBefore(obsDate));
            }
        }
    }

    @Test
    public void testSosInjector() throws SosUpdateException {
        //run the station and observation retrievers through a mock SosInjector
        SosInjector.mock(
             "mock-sos-injector"
            ,new DatabaseStationRetriever()
            ,new DatabaseObservationRetriever()
            ,false
        ).update();
    }
}
