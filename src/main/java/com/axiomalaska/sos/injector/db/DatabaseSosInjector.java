package com.axiomalaska.sos.injector.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axiomalaska.sos.SosInjector;

public class DatabaseSosInjector {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSosInjector.class);
    private static final String MOCK = "mock";
    
    public static void main(String[] args){
        if (args.length > 0) {
            String configFile = args[0];
            DatabaseSosInjectorConfig.initialize(configFile);
        }

        try {        
            SosInjector sosInjector = null;
            if (System.getProperty(MOCK) != null) {
                //mock
                sosInjector = SosInjector.mock("mock-database-sos-injector",
                        new DatabaseStationRetriever(),
                        new DatabaseObservationRetriever(),
                        true);
                LOGGER.info("Mock SosInjector initialized");
            } else {
                sosInjector = new SosInjector("database-sos-injector",
                        DatabaseSosInjectorConfig.instance().getSosUrl(),
                        DatabaseSosInjectorConfig.instance().getSosAuthenticationToken(),
                        DatabaseSosInjectorConfig.instance().getPublisherInfo(),            
                        new DatabaseStationRetriever(),
                        new DatabaseObservationRetriever(),
                        null);
                
            }
            if (sosInjector == null) {
                throw new NullPointerException("sosInjector is null");
            }
            sosInjector.update();
        } catch (Exception e) {
            LOGGER.error("Sos injection failed", e);
        }
    }
}