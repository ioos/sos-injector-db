CREATE TABLE IF NOT EXISTS station (
  id int PRIMARY KEY,
  name varchar,
  lat double,
  lng double,
  height_m double
);

INSERT INTO station SELECT x.* FROM (
SELECT 1 as id, 'station1', 40.3, -120.3, 10.0
UNION SELECT 2, 'station2', 53.1, -116.2, 9.1
UNION SELECT 3, 'station3', 34.2, -80.1, 2.1
) x
LEFT JOIN station a
 ON x.id = a.id
WHERE a.id IS NULL;

CREATE TABLE IF NOT EXISTS sensor (
  id int PRIMARY KEY,
  station_id int,
  standard_name varchar,
  height_m double,
  FOREIGN KEY(station_id) REFERENCES station (id)
);

INSERT INTO sensor SELECT x.* FROM (
SELECT 1 as id, 1, 'air_temperature', 1.0
UNION SELECT 2, 1, 'sea_water_temperature', -2.0
UNION SELECT 3, 2, 'air_temperature', 0.5
UNION SELECT 4, 2, 'sea_water_temperature', -6.7
UNION SELECT 5, 3, 'air_temperature', 2.4
UNION SELECT 6, 3, 'sea_water_temperature', -5.1
) x
LEFT JOIN sensor a
 ON x.id = a.id
WHERE a.id IS NULL;

CREATE TABLE IF NOT EXISTS observation (
  id int PRIMARY KEY AUTO_INCREMENT,
  sensor_id int,
  time datetime,
  value double,
  FOREIGN KEY(sensor_id) REFERENCES sensor(id)
);

INSERT INTO observation (sensor_id, time, value) SELECT x.* FROM (
    SELECT a.id, DATEADD('HOUR', 0 - b.x, NOW()), ROUND(RAND() * 10 + 50, 2)
    FROM sensor a,
    (
      SELECT x FROM system_range(0,19)
    ) b
) x
LEFT JOIN observation a
 ON a.id = x.id
WHERE a.id IS NULL;
