SELECT
   a.id as station_database_id -- for linking in subsequent queries
  ,'Test' as source_name
  ,'USA' as source_country
  ,NULL as source_email
  ,NULL as source_web_address
  ,NULL as source_operator_sector -- see http://mmisw.org/ont/ioos/sector
  ,NULL as source_address
  ,NULL as source_city
  ,NULL as source_state
  ,NULL as source_zip_code
  ,'test' as station_authority
  ,lower(replace(a.name, ' ', '_')) as station_id
  ,a.name as station_short_name
  ,a.name as station_long_name
  ,a.lat as station_lat
  ,a.lng as station_lng
  ,a.height_m as station_height_meters
  ,a.name as station_feature_of_interest_name
  ,'drifting_buoy' as station_platform_type -- see http://mmisw.org/ont/ioos/platform
  ,NULL as station_wmo_id
  ,NULL as station_sponsor
  ,NULL as station_url
  ,NULL as station_rss_url
  ,NULL as station_image_url
  ,NULL as station_image_mime_type
FROM station a;