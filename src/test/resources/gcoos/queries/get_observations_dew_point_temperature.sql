--PARAMETERS
--station_database_id
--sensor_database_id 
--start_date

SELECT
   a.observationDate as observation_time
  ,a.dewPoint as observation_value
  ,b.verticalPosition as observation_height_meters
FROM dewPoint a
JOIN sensor b
 ON a.sensorId = b.rowid
WHERE b.platformId = ?
AND ? IS NOT NULL --do nothing with the sensor_database_id
AND a.observationDate > strftime('%Y-%m-%dT%H:%M:%SZ', ?)
AND a.dewPoint IS NOT NULL
AND a.dewPoint != -9999.0
ORDER BY a.observationDate;