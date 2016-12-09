package com.axiomalaska.sos.injector.db.h2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axiomalaska.ioos.sos.GeomHelper;
import com.axiomalaska.sos.ObservationRetriever;
import com.axiomalaska.sos.SosInjector;
import com.axiomalaska.sos.StationRetriever;
import com.axiomalaska.sos.data.ObservationCollection;
import com.axiomalaska.sos.data.PublisherInfo;
import com.axiomalaska.sos.data.SosStation;
import com.axiomalaska.sos.exception.SosUpdateException;
import com.axiomalaska.sos.exception.StationCreationException;
import com.axiomalaska.sos.injector.db.DatabaseObservationRetriever;
import com.axiomalaska.sos.injector.db.DatabaseSosInjectorConfig;
import com.axiomalaska.sos.injector.db.DatabaseStationRetriever;
import com.axiomalaska.sos.injector.db.data.DatabasePhenomenon;
import com.axiomalaska.sos.injector.db.data.DatabaseSosSensor;
import com.axiomalaska.sos.injector.db.data.DatabaseSosStation;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class TestH2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestH2.class);

    @BeforeClass
    public static void initConfig() throws ClassNotFoundException, SQLException, IOException {
        //init h2 database
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:sos;DB_CLOSE_DELAY=-1");
        Statement statement = conn.createStatement();
        statement.execute(Files.toString(new File("src/test/resources/h2/init.sql"), Charsets.UTF_8));

        //show test data
        ResultSet resultSet = statement.executeQuery("SCRIPT;");
        while (resultSet.next()) {
            LOGGER.info(resultSet.getString(1));
        }
        resultSet.close();
        statement.close();

        DatabaseSosInjectorConfig.initialize("h2/config.properties");
    }

    @AfterClass
    public static void cleanUp() {
        LOGGER.debug("Cleaning sos-injector-db test config");
        DatabaseSosInjectorConfig.cleanUp();
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
        dbSensor.setLocation(GeomHelper.createLatLngPoint(62.0, -140.0)); //location isn't checked in equals but must not be null
        DatabasePhenomenon dbPhenomenon = new DatabasePhenomenon();
        dbPhenomenon.setDatabaseId("9");        
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
