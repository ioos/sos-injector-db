package com.axiomalaska.sos.injector.db;

import org.apache.log4j.Logger;

import com.axiomalaska.sos.SosInjector;

public class DatabaseSosInjector {
    private static final Logger LOGGER = Logger.getLogger(DatabaseSosInjector.class);

    public static void main(String[] args){
        if (args.length > 0) {
            String configFile = args[0];
            DatabaseSosInjectorConfig.initialize(configFile);
        }
    
        try {
            new SosInjector("database-sos-injector",
                    DatabaseSosInjectorConfig.instance().getSosUrl(),
                    DatabaseSosInjectorConfig.instance().getSosAuthenticationToken(),
                    DatabaseSosInjectorConfig.instance().getPublisherInfo(),            
                    new DatabaseStationRetriever(),
                    new DatabaseObservationRetriever(),
                    null).update();
        } catch (Exception e) {
            LOGGER.error("Sos injection failed", e);
        }
    }
}