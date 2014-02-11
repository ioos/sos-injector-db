--PARAMETERS
--station_database_id

SELECT
   a.sensorTypeId as sensor_database_id -- for linking in subsequent queries  
  ,b.shortTypeName as sensor_short_name
  ,b.shortTypeName as sensor_long_name
  ,AVG(a.verticalPosition) as sensor_height_meters -- average the sensor heights to obtain a single value
FROM sensor a
JOIN sensorType b
  ON a.sensorTypeId = b.rowid
WHERE a.platformId = ?
GROUP BY
   a.platformId
  ,a.sensorTypeId;