--PARAMETERS
-- station_database_id
-- sensor_database_id

-- get sensor phenomena
SELECT
   a.id as phenomenon_database_id -- for linking in subsequent queries
  ,b.urn as phenomenon_urn
  ,b.unit as phenomenon_unit
FROM sensor a
JOIN (
  SELECT 'air_temperature' as standard_name, 'http://mmisw.org/ont/cf/parameter/air_temperature' as urn,  'degC' as unit
  UNION ALL SELECT 'sea_water_temperature', 'http://mmisw.org/ont/cf/parameter/sea_water_temperature', 'degC'
) b
 ON a.standard_name = b.standard_name
WHERE a.id = :sensor_database_id
AND :station_database_id = :station_database_id;  -- query currently must contain all parameters