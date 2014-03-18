--PARAMETERS
--station_database_id
--sensor_database_id 
--start_date

SELECT
   strftime('%Y-%m-%dT%H:%M:%fZ', Datetime(a.observationDate)) as observation_time
  ,a.windSpeed as observation_value
  ,a.verticalPosition as observation_height_meters
FROM winds a
JOIN sensor b
 ON a.sensorId = b.rowid
WHERE b.platformId = ?
AND ? IS NOT NULL --do nothing with the sensor_database_id
AND Datetime(a.observationDate) IS NOT NULL
AND Datetime(a.observationDate) > Datetime(?)
AND a.windSpeed IS NOT NULL
AND a.windSpeed != -9999.0
ORDER BY a.observationDate;