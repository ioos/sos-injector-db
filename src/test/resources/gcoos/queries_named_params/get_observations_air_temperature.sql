--PARAMETERS
--station_database_id
--sensor_database_id 
--start_date

SELECT
   a.observationDate as observation_time
  ,a.airTemperature as observation_value
  ,b.verticalPosition as observation_height_meters  
FROM airTemperature a
JOIN sensor b
 ON a.sensorId = b.rowid
WHERE b.platformId = :station_database_id
AND :sensor_database_id IS NOT NULL --do nothing with the sensor_database_id
AND a.observationDate > strftime('%Y-%m-%dT%H:%M:%SZ', :start_date)
AND a.airTemperature IS NOT NULL
AND a.airTemperature != -9999.0
ORDER BY a.observationDate;