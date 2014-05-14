package com.axiomalaska.sos.injector.db.csv;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axiomalaska.sos.data.PublisherInfo;
import com.axiomalaska.sos.injector.db.DatabaseSosInjectorConfig;

public class TestCSV {
    @BeforeClass
    public static void initConfig() {
        DatabaseSosInjectorConfig.initialize("csv/config.properties");
    }

    @AfterClass
    public static void cleanUp() {
        DatabaseSosInjectorConfig.cleanUp();
    }
    
    @Test
    public void testConfigFileLoading() {
        PublisherInfo publisherInfo = DatabaseSosInjectorConfig.instance().getPublisherInfo();
        assertEquals(publisherInfo.getName(), "Some Publisher");
        assertEquals(publisherInfo.getCode(), "spcode");
        assertEquals(publisherInfo.getCountry(), "Paraguay");
        assertEquals(publisherInfo.getWebAddress(), "http://www.somepublisher.org");
        assertEquals(publisherInfo.getEmail(), "info@somepublisher.org");
    }
}
