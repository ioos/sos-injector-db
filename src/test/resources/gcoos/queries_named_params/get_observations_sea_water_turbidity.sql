--PARAMETERS
--station_database_id
--sensor_database_id 
--start_date

SELECT
   a.observationDate as observation_time
  ,a.turbidity as observation_value
  ,b.verticalPosition as observation_height_meters
FROM turbidity a
JOIN sensor b
 ON a.sensorId = b.rowid
WHERE b.platformId = :station_database_id
AND :sensor_database_id IS NOT NULL --do nothing with the sensor_database_id
AND a.observationDate > strftime('%Y-%m-%dT%H:%M:%SZ', :start_date)
AND a.turbidity IS NOT NULL
AND a.turbidity != -9999.0
ORDER BY a.observationDate;