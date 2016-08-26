--PARAMETERS
--station_database_id
--sensor_database_id 
--sensor_database_id
--phenomenon_database_id
--start_date

SELECT
   a.time as observation_time
  ,a.value as observation_value
  ,NULL as observation_height_meters
FROM observation a
WHERE a.sensor_id = :sensor_database_id
AND a.time > :start_date
AND :station_database_id = :station_database_id        -- query currently must contain all parameters
AND :phenomenon_database_id = :phenomenon_database_id  -- query currently must contain all parameters
ORDER BY a.time;