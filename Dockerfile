FROM maven:3
MAINTAINER Shane St Clair<shane@axds.co>

WORKDIR /usr/local/src

ADD pom.xml /usr/local/src
RUN mvn clean verify --fail-never

ADD . /usr/local/src
RUN mvn clean package \
    && mkdir -p /srv/sos-injector-db \
    && mv target/sos-injector-db-*-shaded.jar /srv/sos-injector-db/sos-injector-db.jar \
    && rm -rf /usr/local/src/*

#Add sensor user
RUN useradd --system --home-dir=/srv/sos-injector-db sensor \
      && chown -R sensor:sensor /srv/sos-injector-db

#Run as sensor user
USER sensor

CMD java -jar /srv/sos-injector-db/sos-injector-db.jar
