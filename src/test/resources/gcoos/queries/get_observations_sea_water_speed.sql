--PARAMETERS
--station_database_id
--sensor_database_id 
--start_date

SELECT
   a.observationDate as observation_time
  ,a.speed as observation_value
  ,0 - a.verticalDatum as observation_height_meters
FROM oceanCurrents a
JOIN sensor b
 ON a.sensorId = b.rowid
WHERE b.platformId = ?
AND ? IS NOT NULL --do nothing with the sensor_database_id
AND a.observationDate > ?
AND a.speed IS NOT NULL
AND a.speed != -9999.0
AND Datetime(a.observationDate) IS NOT NULL
ORDER BY a.observationDate;