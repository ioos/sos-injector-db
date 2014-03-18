--PARAMETERS
--station_database_id
--sensor_database_id 
--start_date

SELECT
   Datetime(a.observationDate) as observation_time
  ,a.relHumidity as observation_value
  ,b.verticalPosition as observation_height_meters  
FROM relHumidity a
JOIN sensor b
 ON a.sensorId = b.rowid
WHERE b.platformId = ?
AND ? IS NOT NULL --do nothing with the sensor_database_id
AND Datetime(a.observationDate) IS NOT NULL
AND Datetime(a.observationDate) > Datetime(?)
AND a.relHumidity IS NOT NULL
AND a.relHumidity != -9999.0
ORDER BY a.observationDate;