--PARAMETERS
--station_database_id
--sensor_database_id 
--start_date

SELECT
   a.observationDate as observation_time
  ,a.DOConc as observation_value
  ,b.verticalPosition as observation_height_meters
FROM dissolvedOxygen a
JOIN sensor b
 ON a.sensorId = b.rowid
WHERE b.platformId = ?
AND ? IS NOT NULL --do nothing with the sensor_database_id
AND a.observationDate > ?
AND a.DOConc IS NOT NULL
AND a.DOConc != -9999.0
AND Datetime(a.observationDate) IS NOT NULL
ORDER BY a.observationDate;