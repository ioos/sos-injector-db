--PARAMETERS
--station_database_id
--sensor_database_id 
--start_date

SELECT
   a.observationDate as observation_time
  ,a.windSpeed as observation_value
  ,a.verticalPosition as observation_height_meters
FROM winds a
JOIN sensor b
 ON a.sensorId = b.rowid
WHERE b.platformId = ?
AND ? IS NOT NULL --do nothing with the sensor_database_id
AND a.observationDate > strftime('%Y-%m-%dT%H:%M:%SZ', ?)
AND a.windSpeed IS NOT NULL
AND a.windSpeed != -9999.0
ORDER BY a.observationDate;