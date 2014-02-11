--PARAMETERS
--station_database_id
--sensor_database_id 
--start_date

SELECT
   a.observationDate as observation_time
  ,a.windGust as observation_value
  ,a.verticalPosition as observation_height_meters -- TODO not sensor height?
FROM winds a
JOIN sensor b
 ON a.sensorId = b.rowid
WHERE b.platformId = ?
AND ? IS NOT NULL --do nothing with the sensor_database_id
AND a.observationDate > ?
AND a.windGust IS NOT NULL
AND a.windGust != -9999.0
AND Datetime(a.observationDate) IS NOT NULL
ORDER BY a.observationDate;