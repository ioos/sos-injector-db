package com.axiomalaska.sos.injector.db;

import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axiomalaska.sos.SosInjector;
import com.axiomalaska.sos.SosInjectorConstants;
import com.google.common.base.Stopwatch;

public class DatabaseSosInjector {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSosInjector.class);
    
    public static void main(String[] args){
        if (args.length > 0) {
            String configFile = args[0];
            DatabaseSosInjectorConfig.initialize(configFile);
        }        
        
        Stopwatch stopwatch = Stopwatch.createStarted();        
        try {            
            SosInjector sosInjector = null;
            if (System.getProperty(DatabaseSosInjectorConstants.ENV_MOCK) != null) {
                //mock
                DateTime startDate = DatabaseSosInjectorConfig.instance().getOverrideStartDate() != null ?
                        DatabaseSosInjectorConfig.instance().getOverrideStartDate() :
                            SosInjectorConstants.DEFAULT_START_DATE;
                sosInjector = SosInjector.mock("mock-database-sos-injector",
                        new DatabaseStationRetriever(),
                        new DatabaseObservationRetriever(),
                        true, startDate);
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
            LOGGER.info("SOS injection finished in {} seconds", stopwatch.elapsed(TimeUnit.SECONDS));
        } catch (Exception e) {
            LOGGER.error("Sos injection failed after {} seconds", stopwatch.elapsed(TimeUnit.SECONDS), e);
        }
    }
}