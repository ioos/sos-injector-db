package com.axiomalaska.sos.injector.db;

import static com.axiomalaska.sos.injector.db.DatabaseSosInjectorHelper.getDouble;
import static com.axiomalaska.sos.injector.db.DatabaseSosInjectorHelper.makeDocument;
import static com.axiomalaska.sos.injector.db.DatabaseSosInjectorHelper.requireNonNull;
import static com.axiomalaska.sos.injector.db.DatabaseSosInjectorHelper.requireString;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.asset.StationAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axiomalaska.cf4j.CFStandardNameUtil;
import com.axiomalaska.ioos.sos.GeomHelper;
import com.axiomalaska.ioos.sos.IoosSosConstants;
import com.axiomalaska.ioos.sos.IoosSosUtil;
import com.axiomalaska.jdbc.NamedParameterPreparedStatement;
import com.axiomalaska.phenomena.IoosParameterUtil;
import com.axiomalaska.phenomena.Phenomenon;
import com.axiomalaska.phenomena.UnitCreationException;
import com.axiomalaska.phenomena.UnitResolver;
import com.axiomalaska.sos.StationRetriever;
import com.axiomalaska.sos.data.SosSource;
import com.axiomalaska.sos.data.SosStation;
import com.axiomalaska.sos.exception.StationCreationException;
import com.axiomalaska.sos.injector.db.data.DatabasePhenomenon;
import com.axiomalaska.sos.injector.db.data.DatabaseSosSensor;
import com.axiomalaska.sos.injector.db.data.DatabaseSosStation;
import com.axiomalaska.sos.injector.db.exception.DatabaseSosInjectorStationCreationException;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class DatabaseStationRetriever implements StationRetriever {    
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseStationRetriever.class);
    private static final String GET_STATIONS_QUERY = "get_stations.sql";
    private static final String GET_STATION_SENSORS_QUERY = "get_station_sensors.sql";
    private static final String GET_SENSOR_PHENOMENA_QUERY = "get_sensor_phenomena.sql";
    
    private List<Statement> statements;
    private Connection connection;
    private Statement stationsStatement;
    private NamedParameterPreparedStatement stationSensorsStatement;
    private NamedParameterPreparedStatement sensorPhenomenaStatement;

    public DatabaseStationRetriever() {
        super();
        resetInstanceVars();
    }
    
    @Override
    public synchronized List<SosStation> getStations() throws StationCreationException {
        resetInstanceVars();

        try {
            return getStationsInternal();
        } catch (DatabaseSosInjectorStationCreationException e ) {
            throw new StationCreationException(e);
        } catch (SQLException e ) {
            throw new StationCreationException(e);            
        } finally {
            try {                
                for (Statement statement : statements) {
                    if (statement != null && !statement.isClosed()) {
                        statement.close();
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Error closing statement.");
            }
    
            try {
                //don't use lazy accessor getConnection() here, since it shouldn't be created if not already
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.error("Error closing connection.");
            }

            //zero it out
            resetInstanceVars();
        }
    }

    private void resetInstanceVars() {
        statements = Lists.newArrayList();
        connection = null;
        stationsStatement = null;
        stationSensorsStatement = null;
        sensorPhenomenaStatement = null;        
    }
    
    private List<SosStation> getStationsInternal() throws DatabaseSosInjectorStationCreationException, SQLException {
        List<SosStation> stations = Lists.newArrayList();

        //get the query strings
        String getStationsQuery;
        String getSensorsQuery;
        String getSensorPhenomena;            
        try {
            getStationsQuery = Files.toString(new File(DatabaseSosInjectorConfig.instance().getQueryPath(),
                    GET_STATIONS_QUERY), Charsets.UTF_8);
        } catch (IOException e) {
            throw new DatabaseSosInjectorStationCreationException("Error getting " + GET_STATIONS_QUERY, e);
        }

        try {
            getSensorsQuery = Files.toString(new File(DatabaseSosInjectorConfig.instance().getQueryPath(),
                    GET_STATION_SENSORS_QUERY), Charsets.UTF_8);
        } catch (IOException e) {
            throw new DatabaseSosInjectorStationCreationException("Error getting " + GET_STATION_SENSORS_QUERY, e);
        }

        try {
            getSensorPhenomena = Files.toString(new File(DatabaseSosInjectorConfig.instance().getQueryPath(),
                    GET_SENSOR_PHENOMENA_QUERY), Charsets.UTF_8);
        } catch (IOException e) {
            throw new DatabaseSosInjectorStationCreationException("Error getting " + GET_SENSOR_PHENOMENA_QUERY, e);
        }

        //get the connection and statements
        try {
            stationsStatement = addStatement(getConnection().createStatement());
        } catch (Exception e) {
            throw new DatabaseSosInjectorStationCreationException("Error creating stations statement", e);
        }

        try {
            stationSensorsStatement = NamedParameterPreparedStatement.createNamedParameterPreparedStatement(getConnection(), getSensorsQuery);
        } catch (Exception e) {
            throw new DatabaseSosInjectorStationCreationException("Error creating stations sensors statement", e);
        }
        addStatement(stationSensorsStatement);
        if (stationSensorsStatement.getParameterMetaData() != null) {
            LOGGER.debug("Found " + stationSensorsStatement.getParameterMetaData().getParameterCount() +
                    " parameter(s) in stationSensorsStatement");
        }
        
        try {
            sensorPhenomenaStatement = NamedParameterPreparedStatement.createNamedParameterPreparedStatement(getConnection(), getSensorPhenomena);
        } catch (Exception e) {
            throw new DatabaseSosInjectorStationCreationException("Error creating sensor phenomena statement", e);
        }
        addStatement(sensorPhenomenaStatement);
        if (sensorPhenomenaStatement.getParameterMetaData() != null) {
            LOGGER.debug("Found " + sensorPhenomenaStatement.getParameterMetaData().getParameterCount() +
                    " parameter(s) in sensorPhenomenaStatement");
        }

        //execute the stations query
        ResultSet stationsResultSet;
        try {
            stationsResultSet = stationsStatement.executeQuery(getStationsQuery);
        } catch (SQLException e) {
            throw new DatabaseSosInjectorStationCreationException("Error executing stations query", e);
        }

        //loop through the result rest
        while (stationsResultSet.next()) {
            //load all columns to local variables
            String stationDatabaseId = null;
            stationDatabaseId = getStationStringField(stationsResultSet, null, DatabaseSosInjectorConstants.STATION_DATABASE_ID);

            String sourceName = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.SOURCE_NAME);
            String sourceCountry = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.SOURCE_COUNTRY);
            String sourceEmail = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.SOURCE_EMAIL);
            String sourceWebAddress = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.SOURCE_WEB_ADDRESS);
            String sourceOperatorSector = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.SOURCE_OPERATOR_SECTOR);
            String sourceAddress = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.SOURCE_ADDRESS);
            String sourceCity = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.SOURCE_CITY);
            String sourceState = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.SOURCE_STATE);
            String sourceZipcode = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.SOURCE_ZIP_CODE);

            String stationAuthority = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_AUTHORITY);
            String stationId = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_ID);
            String stationShortName = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_SHORT_NAME);
            String stationLongName = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_LONG_NAME);
            Double stationLat = getStationDoubleField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_LAT);
            Double stationLng = getStationDoubleField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_LNG);
            Double stationHeightMeters = getStationDoubleField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_HEIGHT_METERS);
            String stationFeatureOfInterestName = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_FEATURE_OF_INTEREST_NAME);
            String stationPlatformType = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_PLATFORM_TYPE);
            String stationWmoId = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_WMO_ID);
            String stationSponsor = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_SPONSOR);
            String stationUrl = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_URL);
            String stationRssUrl = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_RSS_URL);
            String stationImageUrl = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_IMAGE_URL);
            String stationImageMimeType = getStationStringField(stationsResultSet, stationDatabaseId, DatabaseSosInjectorConstants.STATION_IMAGE_MIME_TYPE);

            //validate the input
            requireString(DatabaseSosInjectorConstants.STATION_DATABASE_ID, stationDatabaseId);
            requireString(DatabaseSosInjectorConstants.STATION_AUTHORITY, stationAuthority);
            requireString(DatabaseSosInjectorConstants.STATION_ID, stationId);
            requireNonNull(DatabaseSosInjectorConstants.STATION_LAT, stationLat);
            requireNonNull(DatabaseSosInjectorConstants.STATION_LNG, stationLng);

            if (Strings.isNullOrEmpty(stationShortName)) {
                stationShortName = stationId;
            }

            if (Strings.isNullOrEmpty(stationLongName)) {
                stationLongName = stationShortName;
            }

            if (Strings.isNullOrEmpty(stationFeatureOfInterestName)) {
                stationFeatureOfInterestName = stationShortName;
            }

            //create objects from queried values
            SosSource source = new SosSource();
            source.setName(sourceName);
            source.setCountry(sourceCountry);
            source.setEmail(sourceEmail);
            source.setWebAddress(sourceWebAddress);
            source.setOperatorSector(sourceOperatorSector);
            source.setAddress(sourceAddress);
            source.setCity(sourceCity);
            source.setState(sourceState);
            source.setZipcode(sourceZipcode);

            DatabaseSosStation station = new DatabaseSosStation();
            stations.add(station);

            station.setDatabaseId(stationDatabaseId);
            station.setSource(source);
            station.setAsset(new StationAsset(cleanUpUrnComponent(stationAuthority), cleanUpUrnComponent(stationId)));
            station.setShortName(stationShortName);
            station.setLongName(stationLongName);
            station.setLocation(GeomHelper.createLatLngPoint(stationLat, stationLng, stationHeightMeters));
            station.setFeatureOfInterestName(stationFeatureOfInterestName);
            station.setPlatformType(stationPlatformType);
            station.setWmoId(stationWmoId);
            station.setSponsor(stationSponsor);

            //TODO history?
            //TODO networks?

            //make documents
            //TODO handle more document types
            //station url
            if (!Strings.isNullOrEmpty(stationUrl)) {
                station.addDocumentMember(makeDocument("webpage", IoosSosConstants.OGC_ROLE_WEB_PAGE,
                        stationUrl, "text/html", null));
            }

            //station rss
            if (!Strings.isNullOrEmpty(stationRssUrl)) {
                station.addDocumentMember(makeDocument("rssfeed", IoosSosConstants.IOOS_ROLE_RSS_FEED,
                        stationRssUrl, "application/rss+xml", null));
            }

            //station image
            if (!Strings.isNullOrEmpty(stationImageUrl)) {
                station.addDocumentMember(makeDocument("image", IoosSosConstants.OGC_ROLE_OBJECT_IMAGE,
                        stationImageUrl, stationImageMimeType, null));
            }

            //sensors
            try {
                stationSensorsStatement.clearParameters();
            } catch (SQLException e) {
                throw new DatabaseSosInjectorStationCreationException("Error clearing stationSensorsStatement parameters", e);
            }
            if (stationSensorsStatement.hasNamedParameters()) {
                //named parameters, set parameters using named parameters
                try {
                    stationSensorsStatement.setString(DatabaseSosInjectorConstants.STATION_DATABASE_ID, station.getDatabaseId());
                } catch (Exception e) {
                    throw new DatabaseSosInjectorStationCreationException("Error setting stationSensorsStatement named parameter: "
                            + DatabaseSosInjectorConstants.STATION_DATABASE_ID, e);
                }                    
            } else {
                //no named parameters, set parameters normally
                try {
                    stationSensorsStatement.setString(1, station.getDatabaseId());
                } catch (Exception e) {
                    throw new DatabaseSosInjectorStationCreationException(
                            "Error setting stationSensorsStatement parameter 1 (stationDatabaseId)", e);
                }
            }                
            ResultSet stationSensorsResultSet;
            try {
                stationSensorsResultSet = stationSensorsStatement.executeQuery();
            } catch (SQLException e) {
                throw new DatabaseSosInjectorStationCreationException("Error executing stationSensors query", e);
            }
            while (stationSensorsResultSet.next()) {
                //load all columns to local variables
                String sensorDatabaseId = getStationSensorStringField(stationSensorsResultSet, stationDatabaseId,
                        null, DatabaseSosInjectorConstants.SENSOR_DATABASE_ID);
                String sensorShortName = getStationSensorStringField(stationSensorsResultSet, stationDatabaseId,
                        sensorDatabaseId, DatabaseSosInjectorConstants.SENSOR_SHORT_NAME);
                String sensorLongName = getStationSensorStringField(stationSensorsResultSet, stationDatabaseId,
                        sensorDatabaseId, DatabaseSosInjectorConstants.SENSOR_LONG_NAME);
                Double sensorHeightMeters = getStationSensorDoubleField(stationSensorsResultSet, stationDatabaseId,
                        sensorDatabaseId, DatabaseSosInjectorConstants.SENSOR_HEIGHT_METERS);

                DatabaseSosSensor sensor = new DatabaseSosSensor();
                station.addSensor(sensor);
                sensor.setDatabaseId(sensorDatabaseId);
                sensor.setStation(station);
                sensor.setAsset(new SensorAsset(station.getAsset(), cleanUpUrnComponent(sensorShortName)));
                sensor.setLocation(GeomHelper.createLatLngPoint(stationLat, stationLng, sensorHeightMeters));
                sensor.setShortName(sensorShortName);
                sensor.setLongName(sensorLongName);

                //sensor phenomena
                List<Phenomenon> sensorPhenomena = Lists.newArrayList();

                try {
                    sensorPhenomenaStatement.clearParameters();
                } catch (SQLException e) {
                    throw new DatabaseSosInjectorStationCreationException("Error clearing sensorPhenomenaStatement parameters", e);
                }
                if (sensorPhenomenaStatement.hasNamedParameters()) {
                    //named parameters, set parameters using named parameters
                    try {
                        sensorPhenomenaStatement.setString(DatabaseSosInjectorConstants.STATION_DATABASE_ID, station.getDatabaseId());
                    } catch (Exception e) {
                        throw new DatabaseSosInjectorStationCreationException("Error setting sensorPhenomenaStatement named parameter: "
                                + DatabaseSosInjectorConstants.STATION_DATABASE_ID, e);
                    }
                    try {
                        sensorPhenomenaStatement.setString(DatabaseSosInjectorConstants.SENSOR_DATABASE_ID, sensor.getDatabaseId());
                    } catch (Exception e) {
                        throw new DatabaseSosInjectorStationCreationException("Error setting sensorPhenomenaStatement named parameter: "
                                + DatabaseSosInjectorConstants.SENSOR_DATABASE_ID, e);
                    }                        
                } else {
                    //no named parameters, set parameters normally
                    try {
                        sensorPhenomenaStatement.setString(1, station.getDatabaseId());
                    } catch (Exception e) {
                        throw new DatabaseSosInjectorStationCreationException(
                                "Error setting sensorPhenomenaStatement parameter 1 (stationDatabaseId)", e);
                    }
                    try {
                        sensorPhenomenaStatement.setString(2, sensor.getDatabaseId());
                    } catch (Exception e) {
                        throw new DatabaseSosInjectorStationCreationException(
                                "Error setting sensorPhenomenaStatement parameter 2 (sensorDatabaseId)", e);
                    }                        
                }
                ResultSet sensorPhenomenaResultSet;
                try {
                    sensorPhenomenaResultSet = sensorPhenomenaStatement.executeQuery();
                } catch (SQLException e) {
                    throw new DatabaseSosInjectorStationCreationException("Error executing sensorPhenomena query", e);
                }
                while (sensorPhenomenaResultSet.next()) {
                    //load all columns to local variables
                    String phenomenonDatabaseId = getSensorPhenomenaStringField(sensorPhenomenaResultSet, stationDatabaseId,
                            sensorDatabaseId, null, DatabaseSosInjectorConstants.PHENOMENON_DATABASE_ID);
                    String phenomenonUrn = getSensorPhenomenaStringField(sensorPhenomenaResultSet, stationDatabaseId,
                            sensorDatabaseId, phenomenonDatabaseId, DatabaseSosInjectorConstants.PHENOMENON_URN);
                    String phenomenonUnit = getSensorPhenomenaStringField(sensorPhenomenaResultSet, stationDatabaseId,
                            sensorDatabaseId, phenomenonDatabaseId, DatabaseSosInjectorConstants.PHENOMENON_UNIT);

                    DatabasePhenomenon phenomenon = new DatabasePhenomenon();
                    phenomenon.setDatabaseId(phenomenonDatabaseId);
                    phenomenon.setId(phenomenonUrn);
                    phenomenon.setTag(IoosSosUtil.getNameFromUri(phenomenonUrn));
                    phenomenon.setName(IoosSosUtil.convertUnderscoredNameToTitleCase(phenomenon.getTag()));
                    try {
                        phenomenon.setUnit(UnitResolver.parseUnit(phenomenonUnit));
                    } catch (UnitCreationException e) {
                        throw new DatabaseSosInjectorStationCreationException("Error creating phenomenon unit: " + phenomenonUnit, e);
                    }

                    if (CFStandardNameUtil.getCFStandardName(phenomenon.getTag()) == null &&
                            !IoosParameterUtil.getInstance().vocabularyContainsParameter(phenomenonUrn)) {
                        LOGGER.warn("{} is not in the CF standard name or IOOS parameter vocabularies!", phenomenonUrn);
                    }

                    sensorPhenomena.add(phenomenon);
                }
                sensor.setPhenomena(sensorPhenomena);
            }
        }

        return stations;
    }
    
    private static String cleanUpUrnComponent(String str) {
        return str.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    private Statement addStatement(Statement statement){
        statements.add(statement);
        return statement;
    }

    private Connection getConnection() throws DatabaseSosInjectorStationCreationException {
        if (connection == null) {
            try {
                connection = DatabaseConnectionHelper.getConnection();
            } catch (Exception e) {
                throw new DatabaseSosInjectorStationCreationException("Couldn't get database connection", e);
            }
        }
        return connection;
    }

    private static String getStationStringField(ResultSet resultSet, String stationDatabaseId, String column)
            throws DatabaseSosInjectorStationCreationException {
        try {
            return resultSet.getString(column);
        } catch (SQLException e) {
            throw new DatabaseSosInjectorStationCreationException(
                    getStationColumnExceptionMessage(stationDatabaseId, column), e);
        }
    }

    private static Double getStationDoubleField(ResultSet resultSet, String stationDatabaseId, String column)
            throws DatabaseSosInjectorStationCreationException {
        try {
            return getDouble(resultSet.getObject(column));
        } catch (SQLException e) {
            throw new DatabaseSosInjectorStationCreationException(
                    getStationColumnExceptionMessage(stationDatabaseId, column), e);
        }
    }

    private static String getStationSensorStringField(ResultSet resultSet, String stationDatabaseId,
            String sensorDatabaseId, String column) throws DatabaseSosInjectorStationCreationException {
        try {
            return resultSet.getString(column);
        } catch (SQLException e) {
            throw new DatabaseSosInjectorStationCreationException(
                    getStationSensorColumnExceptionMessage(stationDatabaseId, sensorDatabaseId, column), e);
        }
    }

    private static Double getStationSensorDoubleField(ResultSet resultSet, String stationDatabaseId,
            String sensorDatabaseId, String column) throws DatabaseSosInjectorStationCreationException {
        try {
            return getDouble(resultSet.getObject(column));
        } catch (SQLException e) {
            throw new DatabaseSosInjectorStationCreationException(
                    getStationSensorColumnExceptionMessage(stationDatabaseId, sensorDatabaseId, column), e);
        }
    }

    private static String getSensorPhenomenaStringField(ResultSet resultSet, String stationDatabaseId,
            String sensorDatabaseId, String phenomenonDatabaseId, String column) throws DatabaseSosInjectorStationCreationException {
        try {
            return resultSet.getString(column);
        } catch (SQLException e) {
            throw new DatabaseSosInjectorStationCreationException(
                    getSensorPhenomenaColumnExceptionMessage(stationDatabaseId, sensorDatabaseId, phenomenonDatabaseId, column), e);
        }
    }

    private static String getStationColumnExceptionMessage(String stationDatabaseId, String column){
        String errorMsg = "Error getting station column: " + column;
        if (stationDatabaseId != null) {
            errorMsg += " [stationDatabaseId: " + stationDatabaseId + "]";
        }
        return errorMsg;
    }

    private static String getStationSensorColumnExceptionMessage(String stationDatabaseId, String sensorDatabaseId, String column){
        String errorMsg = "Error getting stationSensor column: " + column;
        if (stationDatabaseId != null) {
            errorMsg += " [stationDatabaseId: " + stationDatabaseId + "]";
        }
        if (sensorDatabaseId != null) {
            errorMsg += " [sensorDatabaseId: " + sensorDatabaseId + "]";
        }        
        return errorMsg;
    }

    private static String getSensorPhenomenaColumnExceptionMessage(String stationDatabaseId, String sensorDatabaseId, 
            String phenomenonDatabaseId, String column){
        String errorMsg = "Error getting sensorPhenomenon column: " + column;
        if (stationDatabaseId != null) {
            errorMsg += " [stationDatabaseId: " + stationDatabaseId + "]";
        }
        if (sensorDatabaseId != null) {
            errorMsg += " [sensorDatabaseId: " + sensorDatabaseId + "]";
        }
        if (phenomenonDatabaseId != null) {
            errorMsg += " [phenomenonDatabaseId: " + phenomenonDatabaseId + "]";
        }        
        
        return errorMsg;
    }

}
