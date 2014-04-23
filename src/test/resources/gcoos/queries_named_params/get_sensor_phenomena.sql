--PARAMETERS
-- station_database_id 
-- sensor_database_id

-- get sensor phenomena
SELECT
   b.rowid as phenomenon_database_id -- for linking in subsequent queries
  ,c.urn as phenomenon_urn
  ,c.unit as phenomenon_unit
FROM sensor a
JOIN sensorType b
  ON a.sensorTypeId = b.rowid
JOIN (
  SELECT 'airPressure' as sensorType, 'http://mmisw.org/ont/cf/parameter/air_pressure' as urn, 'hPa' as unit --airPressure.airPressure
  UNION ALL SELECT 'airTemperature', 'http://mmisw.org/ont/cf/parameter/air_temperature', 'degC' --airTemperature.airTemperature
  UNION ALL SELECT 'chlorophyll', 'http://mmisw.org/ont/cf/parameter/mass_concentration_of_chlorophyll_in_sea_water', 'ug L-1' --chlorophyll.chlorophyll
  UNION ALL SELECT 'dewTemperature', 'http://mmisw.org/ont/cf/parameter/dew_point_temperature', 'degC' --dewPoint.dewPoint
  UNION ALL SELECT 'dissolvedOxygen', 'http://mmisw.org/ont/cf/parameter/mass_concentration_of_oxygen_in_sea_water', 'ug L-1' --dissovedOxygen.DOConc
  UNION ALL SELECT 'currents', 'http://mmisw.org/ont/cf/parameter/direction_of_sea_water_velocity', 'degree' --oceanCurrents.direction
  UNION ALL SELECT 'currents', 'http://mmisw.org/ont/cf/parameter/sea_water_speed', 'cm s-1' --oceanCurrents.speed
  UNION ALL SELECT 'currents', 'http://mmisw.org/ont/cf/parameter/upward_sea_water_velocity', 'cm s-1' --oceanCurrents.verticalVelocity
  UNION ALL SELECT 'relativeHumidity', 'http://mmisw.org/ont/cf/parameter/relative_humidity', '%' --relHumidity.relHumidity
  UNION ALL SELECT 'salinity', 'http://mmisw.org/ont/cf/parameter/sea_water_practical_salinity', 'PSU' --salinity.salinity (verticalPosition)
  UNION ALL SELECT 'solarRadiation', 'http://mmisw.org/ont/cf/parameter/downwelling_shortwave_flux_in_air', 'W m-2' --solar.shortWave
  UNION ALL SELECT 'solarRadiation', 'http://mmisw.org/ont/cf/parameter/downwelling_longwave_flux_in_air', 'W m-2' --solar.longWave
  UNION ALL SELECT 'turbidity', 'http://mmisw.org/ont/cf/parameter/sea_water_turbidity', 'NTU' --turbidity.turbidity
  UNION ALL SELECT 'waterLevel', 'http://mmisw.org/ont/cf/parameter/sea_surface_height_above_sea_level', 'm' --waterLevel.waterLevel
  UNION ALL SELECT 'waterTemperature', 'http://mmisw.org/ont/cf/parameter/sea_water_temperature', 'degC' --waterTemperature.waterTemperature (verticalPosition)
  UNION ALL SELECT 'winds', 'http://mmisw.org/ont/cf/parameter/wind_speed', 'm s-1' --winds.windSpeed (verticalPosition)
  UNION ALL SELECT 'winds', 'http://mmisw.org/ont/cf/parameter/wind_to_direction', 'degree' --winds.windDirection (verticalPosition)
  UNION ALL SELECT 'winds', 'http://mmisw.org/ont/cf/parameter/wind_speed_of_gust', 'm s-1' --winds.windGust (verticalPosition)
) c
 ON b.shortTypeName = c.sensorType
WHERE a.platformId = :station_database_id
AND a.sensorTypeId = :sensor_database_id
GROUP BY
   b.rowid
  ,c.urn
  ,c.unit;