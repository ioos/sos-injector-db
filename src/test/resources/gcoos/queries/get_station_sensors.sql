--PARAMETERS
--station_database_id

SELECT
   a.sensorTypeId as sensor_database_id -- for linking in subsequent queries  
  ,b.shortTypeName as sensor_short_name
  ,b.shortTypeName as sensor_long_name
  ,CASE
    WHEN COUNT(*) = 1 NULL THEN a.verticalPosition
    ELSE NULL
   END as sensor_height_meters -- use sensor.verticalPosition if there is only one sensor, otherwise it's a grouped profile (discard height)
FROM sensor a
JOIN sensorType b
  ON a.sensorTypeId = b.rowid
WHERE a.platformId = ?
GROUP BY
   a.platformId
  ,a.sensorTypeId;