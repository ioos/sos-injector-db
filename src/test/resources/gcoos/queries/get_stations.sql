SELECT
   a.rowid as station_database_id -- for linking in subsequent queries 
  ,b.name as source_name
  ,c.FullName as source_country
  ,b.contactEmail as source_email
  ,b.url as source_web_address
  ,NULL as source_operator_sector -- see http://mmisw.org/ont/ioos/sector
  ,NULL as source_address
  ,NULL as source_city
  ,NULL as source_state
  ,NULL as source_zip_code
  ,coalesce(lower(b.shortname), 'unknown') as station_authority
  ,a.name as station_id
  ,a.name as station_short_name
  ,a.description as station_long_name
  ,a.loc_lat as station_lat
  ,a.loc_lon as station_lng
  ,NULL as station_height_meters
  ,a.description as station_feature_of_interest_name
  ,CASE
      WHEN d.name = 'Ice Buoy' THEN 'drifting_buoy'
      WHEN d.name = 'Hurricane Drifter' THEN 'drifting_buoy'
      WHEN d.name = 'Climate Reference Station' THEN 'moored_buoy'
      WHEN d.name = 'Tropical Moored Buoy' THEN 'moored_buoy'
      WHEN d.name = 'Tsunami Buoy' THEN 'moored_buoy'
      WHEN d.name = 'Weather Buoy' THEN 'moored_buoy'
      WHEN d.name = 'CMAN Station' THEN 'offshore_tower'
      WHEN d.name = 'Tide Gauge (Real Time Reporting)' THEN 'tide_station'
      WHEN d.name = 'GLOSS' THEN 'tide_station'
      WHEN d.name = 'NWLON' THEN 'tide_station'
      ELSE null
   END as station_platform_type -- see http://mmisw.org/ont/ioos/platform
  ,CASE
      WHEN urn LIKE 'ioos.station.wmo:%' THEN substr(urn, length('ioos.station.wmo:') + 1)
      ELSE NULL
   END as station_wmo_id
  ,NULL as station_sponsor
  ,a.url as station_url
  ,a.rss as station_rss_url
  ,a.image as station_image_url
  ,null as station_image_mime_type
FROM platform a
LEFT JOIN organization b
  ON a.organizationId = b.rowId
LEFT JOIN country c
  ON b.countryId = c.iso_code3
LEFT JOIN platformType d
  ON a.platformTypeId = d.rowid;