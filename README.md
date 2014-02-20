# sos-injector-db

[![Build Status](https://api.travis-ci.org/ioos/sos-injector-db.png)](https://travis-ci.org/ioos/sos-injector-db)

**Note: view the [README on GitHub](https://github.com/ioos/sos-injector-db) for a better reading experience!**

This project uses the [sos-injector](https://github.com/axiomalaska/sos-injector) Java library
to read station information and observation values from an arbitrary database and inject
them into an IOOS 52 North SOS.

All deployment specific information (SOS URL, provider information, source database connection url,
queries to retrieve data from source database) is external to the compiled jar, so users should be able
to download a release jar, edit the configuration and query files, and run the application without
having to edit or compile Java.

## Running the application

 * Download and unzip the [sos-injector-db distribution](https://github.com/ioos/sos-injector-db/releases)
   (or clone the source code and use Maven to compile, mvn clean install)
 
 * Write configuration and SQL query files (see **Configuration/SQL files** section below)
 
 * Make sure your target SOS is running, that it has transactional support, and that the computer you
   are running the sos-injector-db application from has transactional operation authorization
   
 * Run the application
 
    * Specify your config file as the only argument:
 
      ```
      java -jar sos-injector-db.jar myconfig.properties
      ```
    
    * Or name your config file config.properties and run the jar in the same directory with no arguments:
 
      ```
      java -jar sos-injector-db.jar 
      ```
 * To do a trial run (test your queries against your database without injecting data into an SOS), set the "mock" VM argument:

      ``` 
      java -Dmock -jar sos-injector-db.jar myconfig.properties
      ```


## Configuration/SQL files

To run the injector, you need to set up a configuration file and set of query files.

To view examples of these files, see the GCOOS test files.

The query files fall into four categories (*italic fields* = optionally null, data type text unless otherwise noted):

### [get_stations.sql](src/test/resources/gcoos/queries/get_stations.sql)

Queries all stations and associated metadata (provider, URLs, etc).

**No parameters.**
  
**Expected result columns:**
  
  * station_database_id - database key (for linking in subsequent queries) 
  * source_name - station operator name
  * source_country
  * source_email
  * source_web_address
  * *source_operator_sector* - see http://mmisw.org/ont/ioos/sector
  * *source_address*
  * *source_city*
  * *source_state*
  * *source_zip_code*
  * station_authority - station naming authority, see [IOOS convention](https://geo-ide.noaa.gov/wiki/index.php?title=IOOS_Conventions_for_Observing_Asset_Identifiers)
  * station_id
  * station_short_name
  * station_long_name
  * station_lat (numeric)
  * station_lng (numeric)
  * *station_height_meters* (numeric) 
  * station_feature_of_interest_name - the name of the object being sampled (often the same as the station name or description)
  * station_platform_type - see http://mmisw.org/ont/ioos/platform
  * *station_wmo_id*
  * *station_sponsor* - name of station funding sponsor 
  * *station_url* - web accessible station webpage
  * *station_rss_url* 
  * *station_image_url*
  * *station_image_mime_type*  

### [get_station_sensors.sql](src/test/resources/gcoos/queries/get_station_sensors.sql)

Queries all sensors for a station.
  
**Parameters:**
  
   * station_database_id - from get_stations.sql
   
**Expected result columns:**
  
  * sensor_database_id - database key (for linking in subsequent queries)  
  * sensor_short_name
  * sensor_long_name
  * *sensor_height_meters* (numeric)
  
### [get_sensor_phenomena.sql](src/test/resources/gcoos/queries/get_sensor_phenomena.sql)

Queries all phenomena for a sensor.
  
**Parameters:**

  * station_database_id - from get_stations.sql
  * sensor_database_id - from get_station_sensors.sql
  
**Expected result columns:**
  
  * phenomenon_database_id - database key (for linking in subsequent queries)
  * phenomenon_urn - full CF standard name mmisw urn (e.g. http://mmisw.org/ont/cf/parameter/air_temperature, see [http://mmisw.org/ont/cf/parameter])
  * phenomenon_unit - UDUNITS compatible units string (e.g. degC)

### get_observations*.sql

Queries observations for a sensor phenomenon. 
  
For get_observations queries, there are two options: 
    
#### get_observations.sql (generic/all phenomena)

If your observations for all phenomena can be queried using a single query
(usually if all your observations are in a single table), you can use a single query.

**Parameters:**

  * station_database_id - from get_stations.sql
  * sensor_database_id - from get_station_sensors.sql
  * phenomenon_database_id - from get_sensor_phenomena.sql
  * start_date - queried from target SOS

#### [get_observations_{cf_standard_name}.sql](src/test/resources/gcoos/queries/get_observations_air_temperature.sql) (phenomenon specific queries)

Alternatively, if you need to write a separate query for each phenomenon (usually if you store observations for each
phenomenon in a separate table), you can create a query for each phenomenon using the filename format
get_observations_{cf_standard_name}.sql (e.g. get_observations_air_temperature.sql).

**Parameters:**

  * station_database_id - from get_stations.sql
  * sensor_database_id - from get_station_sensors.sql
  * start_date - queried from target SOS

**Expected result columns:**

  * observation_time (date or text)
  * observation_value (numeric)
  * *observation_height_meters* (numeric)

The application will first look for a phenomenon specific get_observations_{cf_standard_name}.sql file, and then fall back
to the generic get_observations.sql.

## Database Drivers

The project currently includes drivers for PostgreSQL, MySQL, and SQLite. If you need another database
driver, you can:

 * Create a GitHub issue requesting the new driver
 * Or fork the project, add the new driver dependency to pom.xml, and create a pull request
 * Or download the driver java and specify it while executing the java (java -jar -cp yourdriver.jar sos-injector-db.jar)
