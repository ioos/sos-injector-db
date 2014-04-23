package com.axiomalaska.sos.injector.db;

import static com.axiomalaska.sos.injector.db.DatabaseSosInjectorHelper.getDouble;
import static com.axiomalaska.sos.injector.db.DatabaseSosInjectorHelper.makeDocument;
import static com.axiomalaska.sos.injector.db.DatabaseSosInjectorHelper.requireNonNull;
import static com.axiomalaska.sos.injector.db.DatabaseSosInjectorHelper.requireString;

import java.io.File;
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
import com.axiomalaska.phenomena.UnitResolver;
import com.axiomalaska.sos.StationRetriever;
import com.axiomalaska.sos.data.SosSource;
import com.axiomalaska.sos.data.SosStation;
import com.axiomalaska.sos.exception.StationCreationException;
import com.axiomalaska.sos.injector.db.data.DatabasePhenomenon;
import com.axiomalaska.sos.injector.db.data.DatabaseSosSensor;
import com.axiomalaska.sos.injector.db.data.DatabaseSosStation;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class DatabaseStationRetriever implements StationRetriever {    
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseStationRetriever.class);
    private static final String GET_STATIONS_QUERY = "get_stations.sql";
    private static final String GET_STATION_SENSORS_QUERY = "get_station_sensors.sql";
    private static final String GET_SENSOR_PHENOMENA_QUERY = "get_sensor_phenomena.sql";
    
    private List<Statement> statements = Lists.newArrayList();

    private Statement addStatement(Statement statement){
        statements.add(statement);
        return statement;
    }

    @Override
    public List<SosStation> getStations() throws StationCreationException {
        Connection connection = null;
        Statement stationsStatement = null;
        NamedParameterPreparedStatement stationSensorsStatement = null;
        NamedParameterPreparedStatement sensorPhenomenaStatement = null;

        try {
            List<SosStation> stations = Lists.newArrayList();
            //get the query strings
            String getStationsQuery = Files.toString(new File(DatabaseSosInjectorConfig.instance().getQueryPath(),
                    GET_STATIONS_QUERY), Charsets.UTF_8);
            String getSensorsQuery = Files.toString(new File(DatabaseSosInjectorConfig.instance().getQueryPath(),
                    GET_STATION_SENSORS_QUERY), Charsets.UTF_8);
            String getSensorPhenomena = Files.toString(new File(DatabaseSosInjectorConfig.instance().getQueryPath(),
                    GET_SENSOR_PHENOMENA_QUERY), Charsets.UTF_8);

            //get the connection and statements
            connection = DatabaseConnectionHelper.getConnection();
            stationsStatement = addStatement(connection.createStatement());
            stationSensorsStatement = NamedParameterPreparedStatement.createNamedParameterPreparedStatement(connection, getSensorsQuery);
            addStatement(stationSensorsStatement);            
            sensorPhenomenaStatement = NamedParameterPreparedStatement.createNamedParameterPreparedStatement(connection, getSensorPhenomena);
            addStatement(sensorPhenomenaStatement);

            //execute the stations query
            ResultSet stationsResultSet = stationsStatement.executeQuery(getStationsQuery);

            //loop through the result rest
            while (stationsResultSet.next()) {
                //load all columns to local variables
                String stationDatabaseId = stationsResultSet.getString(DatabaseSosInjectorConstants.STATION_DATABASE_ID);

                String sourceName = stationsResultSet.getString(DatabaseSosInjectorConstants.SOURCE_NAME);
                String sourceCountry = stationsResultSet.getString(DatabaseSosInjectorConstants.SOURCE_COUNTRY);
                String sourceEmail = stationsResultSet.getString(DatabaseSosInjectorConstants.SOURCE_EMAIL);
                String sourceWebAddress = stationsResultSet.getString(DatabaseSosInjectorConstants.SOURCE_WEB_ADDRESS);
                String sourceOperatorSector = stationsResultSet.getString(DatabaseSosInjectorConstants.SOURCE_OPERATOR_SECTOR);
                String sourceAddress = stationsResultSet.getString(DatabaseSosInjectorConstants.SOURCE_ADDRESS);
                String sourceCity = stationsResultSet.getString(DatabaseSosInjectorConstants.SOURCE_CITY);
                String sourceState = stationsResultSet.getString(DatabaseSosInjectorConstants.SOURCE_STATE);
                String sourceZipcode = stationsResultSet.getString(DatabaseSosInjectorConstants.SOURCE_ZIP_CODE);

                String stationAuthority = stationsResultSet.getString(DatabaseSosInjectorConstants.STATION_AUTHORITY);
                String stationId = stationsResultSet.getString(DatabaseSosInjectorConstants.STATION_ID);
                String stationShortName = stationsResultSet.getString(DatabaseSosInjectorConstants.STATION_SHORT_NAME);
                String stationLongName = stationsResultSet.getString(DatabaseSosInjectorConstants.STATION_LONG_NAME);
                Double stationLat = getDouble(stationsResultSet.getObject(DatabaseSosInjectorConstants.STATION_LAT));
                Double stationLng = getDouble(stationsResultSet.getObject(DatabaseSosInjectorConstants.STATION_LNG));
                Double stationHeightMeters = getDouble(stationsResultSet.getObject(DatabaseSosInjectorConstants.STATION_HEIGHT_METERS));
                String stationFeatureOfInterestName = stationsResultSet.getString(DatabaseSosInjectorConstants.STATION_FEATURE_OF_INTEREST_NAME);
                String stationPlatformType = stationsResultSet.getString(DatabaseSosInjectorConstants.STATION_PLATFORM_TYPE);
                String stationWmoId = stationsResultSet.getString(DatabaseSosInjectorConstants.STATION_WMO_ID);
                String stationSponsor = stationsResultSet.getString(DatabaseSosInjectorConstants.STATION_SPONSOR);
                String stationUrl = stationsResultSet.getString(DatabaseSosInjectorConstants.STATION_URL);
                String stationRssUrl = stationsResultSet.getString(DatabaseSosInjectorConstants.STATION_RSS_URL);
                String stationImageUrl = stationsResultSet.getString(DatabaseSosInjectorConstants.STATION_IMAGE_URL);
                String stationImageMimeType = stationsResultSet.getString(DatabaseSosInjectorConstants.STATION_IMAGE_MIME_TYPE);

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
                stationSensorsStatement.clearParameters();
                if (stationSensorsStatement.hasNamedParameters()) {
                    //named parameters, set parameters using named parameters
                    stationSensorsStatement.setString(DatabaseSosInjectorConstants.STATION_DATABASE_ID, station.getDatabaseId());                    
                } else {
                    //no named parameters, set parameters normally
                    stationSensorsStatement.setString(1, station.getDatabaseId());
                }                
                ResultSet stationSensorsResultSet = stationSensorsStatement.executeQuery();
                while (stationSensorsResultSet.next()) {
                    //load all columns to local variables
                    String sensorDatabaseId = stationSensorsResultSet.getString(DatabaseSosInjectorConstants.SENSOR_DATABASE_ID);
                    String sensorShortName = stationSensorsResultSet.getString(DatabaseSosInjectorConstants.SENSOR_SHORT_NAME);
                    String sensorLongName = stationSensorsResultSet.getString(DatabaseSosInjectorConstants.SENSOR_LONG_NAME);
                    Double sensorHeightMeters = getDouble(stationSensorsResultSet.getObject(DatabaseSosInjectorConstants.SENSOR_HEIGHT_METERS));

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

                    sensorPhenomenaStatement.clearParameters();
                    if (sensorPhenomenaStatement.hasNamedParameters()) {
                        //named parameters, set parameters using named parameters
                        sensorPhenomenaStatement.setString(DatabaseSosInjectorConstants.STATION_DATABASE_ID, station.getDatabaseId());
                        sensorPhenomenaStatement.setString(DatabaseSosInjectorConstants.SENSOR_DATABASE_ID, sensor.getDatabaseId());                        
                    } else {
                        //no named parameters, set parameters normally
                        sensorPhenomenaStatement.setString(1, station.getDatabaseId());
                        sensorPhenomenaStatement.setString(2, sensor.getDatabaseId());                        
                    }
                    ResultSet sensorPhenomenaResultSet = sensorPhenomenaStatement.executeQuery();
                    while (sensorPhenomenaResultSet.next()) {
                        //load all columns to local variables
                        String phenomenonDatabaseId = sensorPhenomenaResultSet.getString(DatabaseSosInjectorConstants.PHENOMENON_DATABASE_ID);
                        String phenomenonUrn = sensorPhenomenaResultSet.getString(DatabaseSosInjectorConstants.PHENOMENON_URN);
                        String phenomenonUnit = sensorPhenomenaResultSet.getString(DatabaseSosInjectorConstants.PHENOMENON_UNIT);

                        DatabasePhenomenon phenomenon = new DatabasePhenomenon();
                        phenomenon.setDatabaseId(phenomenonDatabaseId);
                        phenomenon.setId(phenomenonUrn);
                        phenomenon.setTag(IoosSosUtil.getNameFromUri(phenomenonUrn));
                        phenomenon.setName(IoosSosUtil.convertUnderscoredNameToTitleCase(phenomenon.getTag()));
                        phenomenon.setUnit(UnitResolver.parseUnit(phenomenonUnit));

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
        } catch (Exception e) {
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
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.error("Error closing connection.");
            }
        }
    }

    private String cleanUpUrnComponent(String str) {
        return str.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
}
