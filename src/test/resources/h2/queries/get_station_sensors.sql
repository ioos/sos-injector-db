--PARAMETERS
--station_database_id

SELECT
   a.id as sensor_database_id -- for linking in subsequent queries
  ,a.standard_name as sensor_short_name
  ,a.standard_name as sensor_long_name
  ,a.height_m sensor_height_meters
FROM sensor a
WHERE a.station_id = :station_database_id;