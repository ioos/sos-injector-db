package com.axiomalaska.sos.injector.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axiomalaska.ioos.sos.GeomHelper;
import com.axiomalaska.ioos.sos.exception.UnsupportedGeometryTypeException;
import com.axiomalaska.phenomena.Phenomenon;
import com.axiomalaska.sos.IObservationSubmitter;
import com.axiomalaska.sos.IProcedureSubmitter;
import com.axiomalaska.sos.ObservationRetriever;
import com.axiomalaska.sos.SosInjector;
import com.axiomalaska.sos.StationRetriever;
import com.axiomalaska.sos.data.AbstractSosAsset;
import com.axiomalaska.sos.data.ObservationCollection;
import com.axiomalaska.sos.data.PublisherInfo;
import com.axiomalaska.sos.data.SosSensor;
import com.axiomalaska.sos.data.SosStation;
import com.axiomalaska.sos.exception.InvalidObservationCollectionException;
import com.axiomalaska.sos.exception.ObservationRetrievalException;
import com.axiomalaska.sos.exception.SosCommunicationException;
import com.axiomalaska.sos.exception.StationCreationException;
import com.axiomalaska.sos.exception.UnsupportedSosAssetTypeException;
import com.axiomalaska.sos.injector.db.data.DatabasePhenomenon;
import com.axiomalaska.sos.injector.db.data.DatabaseSosSensor;
import com.axiomalaska.sos.injector.db.data.DatabaseSosStation;

public class TestGCOOS {
    @BeforeClass
    public static void initConfig() {
        DatabaseSosInjectorConfig.initialize("gcoos/config.properties");
    }
    
    @Before
    public void setUp() {
        //only run tests if the gcoos.sqlite database exists
        //TODO add reduced version of gcoos.sqlite
        Assume.assumeTrue(new File("src/test/resources/gcoos/gcoos.sqlite").exists());
    }
    
    @Test
    public void testConfigFileLoading() {
        assertEquals(DatabaseSosInjectorConfig.instance().getQueryPath(), "src/test/resources/gcoos/queries");
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
    public void testSosInjector() throws InvalidObservationCollectionException, ObservationRetrievalException,
        //run the station and observation retrievers through a mock SosInjector
        UnsupportedSosAssetTypeException, StationCreationException, SosCommunicationException, UnsupportedGeometryTypeException {
        new SosInjector(
             "mock-sos-injector"
            ,new DatabaseStationRetriever()
            ,new DatabaseObservationRetriever()
            ,new IProcedureSubmitter() {
                @Override
                public boolean checkProcedureWithSos(AbstractSosAsset asset) {
                    // NOOP (mock)
                    return true;
                }
            }
            ,new IObservationSubmitter() {
                @Override
                public void update(SosSensor sensor, Phenomenon phenomenon,
                        ObservationRetriever observationRetriever)
                        throws InvalidObservationCollectionException,
                        ObservationRetrievalException,
                        SosCommunicationException,
                        UnsupportedGeometryTypeException {
                    // NOOP (mock)
                }
            }
        ).update();
    }
}
