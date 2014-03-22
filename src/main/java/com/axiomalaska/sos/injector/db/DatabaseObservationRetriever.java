package com.axiomalaska.sos.injector.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.axiomalaska.ioos.sos.GeomHelper;
import com.axiomalaska.phenomena.Phenomenon;
import com.axiomalaska.sos.ObservationRetriever;
import com.axiomalaska.sos.data.ObservationCollection;
import com.axiomalaska.sos.data.SosSensor;
import com.axiomalaska.sos.injector.db.data.DatabasePhenomenon;
import com.axiomalaska.sos.injector.db.data.DatabaseSosSensor;
import com.axiomalaska.sos.injector.db.data.DatabaseSosStation;
import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

public class DatabaseObservationRetriever implements ObservationRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseObservationRetriever.class);
    private static final String GENERIC_GET_OBS_QUERY = "get_observations.sql";
    private static final String PHEN_SPECIFIC_GET_OBS_QUERY_FORMAT = "get_observations_%s.sql";
    
    @Override
    public List<ObservationCollection> getObservationCollection(
            SosSensor sensor, Phenomenon phenomenon, DateTime startDate) {
        LOGGER.info("Retrieving observations for " + sensor + ", phenomenon " + phenomenon +", start time " + startDate);
        DateTime now = new DateTime(DateTimeZone.UTC);
        
        if (sensor.getLocation() == null) {
            throw new RuntimeException("Sensor must not have a null location!");
        }        
        
        Map<Double,ObservationCollection> observationCollections = Maps.newHashMap();
        
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            //look for a phenomenon specific file first
            boolean phenomenonSpecific = true;
            File phenSpecificQueryFile = new File(DatabaseSosInjectorConfig.instance().getQueryPath(),
                    String.format(PHEN_SPECIFIC_GET_OBS_QUERY_FORMAT, phenomenon.getTag()));
            File genericQueryFile = new File(DatabaseSosInjectorConfig.instance().getQueryPath(),
                    GENERIC_GET_OBS_QUERY);
            File getObsQueryFile = phenSpecificQueryFile;
            if (getObsQueryFile.exists()) {
                LOGGER.debug("Using phenomenon specific getObs query: " + getObsQueryFile.getAbsolutePath());
            } else {
                //fall back to generic get observation query (actually the preferred method)
                phenomenonSpecific = false;
                getObsQueryFile = genericQueryFile;
                LOGGER.debug("Using generic getObs query: " + genericQueryFile.getAbsolutePath());
            }
            if (!getObsQueryFile.exists()) {
                throw new FileNotFoundException("Neither " + phenSpecificQueryFile.getAbsolutePath() + " nor "
                        + genericQueryFile.getAbsolutePath() + " were found.");
            }

            String getObsQuery = Files.toString(getObsQueryFile, Charsets.UTF_8);
            connection = DatabaseConnectionHelper.getConnection();
            statement = connection.prepareStatement(getObsQuery,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            statement.setFetchSize(1000);

            //get special objects with database ids
            if (!(sensor instanceof DatabaseSosSensor)){
                throw new RuntimeException(sensor.getId() + " isn't a DatabaseSosSensor");
            }            
            if (!(sensor.getStation() instanceof DatabaseSosStation)){
                throw new RuntimeException(sensor.getStation().getId() + " isn't a DatabaseSosStation");
            }
            if (!(phenomenon instanceof DatabasePhenomenon)){
                throw new RuntimeException(phenomenon.getId() + " isn't a DatabasePhenomenon");
            }            
            DatabaseSosSensor dbSensor = (DatabaseSosSensor) sensor;
            DatabaseSosStation dbStation = (DatabaseSosStation) sensor.getStation();
            DatabasePhenomenon dbPhenomenon = (DatabasePhenomenon) phenomenon;

            //set the query parameters
            int paramCount = 0;
            statement.setString(++paramCount, dbStation.getDatabaseId());
            statement.setString(++paramCount, dbSensor.getDatabaseId());
            //only the generic get obs query should have the phenomenon database id as a parameter
            if (!phenomenonSpecific) {
                statement.setString(++paramCount, dbPhenomenon.getDatabaseId());
            }
            statement.setString(++paramCount, startDate.toString());

            Stopwatch queryStopwatch = Stopwatch.createStarted();
            ResultSet resultSet = statement.executeQuery();            
            LOGGER.info("Observation query executed in {} ms", queryStopwatch.elapsed(TimeUnit.MILLISECONDS));
            
            Stopwatch processingStopwatch = Stopwatch.createStarted();
            int obsCount = 0;
            while (resultSet.next()) {
                //load all columns to local variables
                DateTime observationTime = null;
                Object dateObj = resultSet.getObject(DatabaseSosInjectorConstants.OBSERVATION_TIME);
                try {
                    if (dateObj instanceof String) {
                        observationTime = DateTime.parse((String) dateObj);
                    } else {
                        observationTime = new DateTime(dateObj, DateTimeZone.UTC);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error parsing date {}. Skipping.", dateObj);
                    continue;           
                }

                if (observationTime == null) {
                    LOGGER.error("Couldn't parse a date from query result ({}). Skipping.", observationTime);
                    continue;                    
                }

                if (!observationTime.isAfter(startDate)) {
                    LOGGER.error("Retrieved a date before the start date! Observation date ({}) is not after start date ({}). Skipping.",
                            observationTime, startDate);
                    continue;
                }

                if (observationTime.isAfter(now)) {
                    LOGGER.error("Observation date ({}) is after now ({}). Skipping.", observationTime, now);
                    continue;
                }

                double observationValue = resultSet.getDouble(DatabaseSosInjectorConstants.OBSERVATION_VALUE);
                double observationHeightMeters = resultSet.getDouble(DatabaseSosInjectorConstants.OBSERVATION_HEIGHT_METERS);

                ObservationCollection obsCollection = observationCollections.get(observationHeightMeters);
                if (obsCollection == null) {
                    LOGGER.debug("Creating a new ObservationCollection for height " + observationHeightMeters);
                    obsCollection = new ObservationCollection();
                    observationCollections.put(observationHeightMeters, obsCollection);
                    obsCollection.setSensor(sensor);
                    obsCollection.setPhenomenon(phenomenon);
                    obsCollection.setGeometry(GeomHelper.createLatLngPoint(sensor.getLocation().getY(),
                            sensor.getLocation().getX(), observationHeightMeters));
                }
                obsCollection.addObservationValue(observationTime, observationValue);
                obsCount++;
            }
            if (obsCount > 0) {
                LOGGER.info("Processed {} observation(s) in {} ms", obsCount, processingStopwatch.elapsed(TimeUnit.MILLISECONDS));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting observations for sensor " + sensor + ", phenomenon " + phenomenon
                    + ", start date " + startDate, e);
        } finally {
            try {
                if (statement != null && !statement.isClosed()) {
                    statement.close();
                }                    
            } catch (SQLException e) {
                LOGGER.error("Error closing statement.");
            }

            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.error("Error closing connection.");
            }
        }
        return new ArrayList<ObservationCollection>(observationCollections.values());
    }

}
