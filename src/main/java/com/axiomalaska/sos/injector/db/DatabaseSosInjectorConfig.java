package com.axiomalaska.sos.injector.db;


import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.axiomalaska.sos.data.PublisherInfo;

public class DatabaseSosInjectorConfig {
    private static final String CONFIG_FILE = "config.properties";

    private static DatabaseSosInjectorConfig instance;

    //configurable properties
    private static final String SOS_URL = "sos.url";
    private String sosUrl;

    private static final String SOS_AUTHENTICATION_TOKEN = "sos.authenticationToken";
    private String sosAuthenticationToken;

    private static final String JDBC_URL = "jdbc.url";
    private String jdbcUrl;

    private static final String JDBC_USERNAME = "jdbc.username";
    private String jdbcUsername;

    private static final String JDBC_PASSWORD = "jdbc.password";
    private String jdbcPassword;

    public static final String QUERY_PATH = "query.path";    
    private static final String QUERY_PATH_DEFAULT = ".";
    private String queryPath;
    
    private static final String PUBLISHER_NAME = "publisher.name";    
    private static final String PUBLISHER_CODE = "publisher.code";
    private static final String PUBLISHER_COUNTRY = "publisher.country";
    private static final String PUBLISHER_EMAIL = "publisher.email";
    private static final String PUBLISHER_WEB_ADDRESS = "publisher.webAddress";
    private PublisherInfo publisherInfo;
    
    //private constructor
    private DatabaseSosInjectorConfig(){}

    public static void initialize(String configFilePath) {
        if (instance != null) {
            throw new RuntimeException("DatabaseSosInjectorConfig already initialized");
        }
        initializeFromConfigFile(configFilePath);
    }

    public static void cleanUp() {
        if (instance != null) {
            instance = null;
        }
    }

    public static DatabaseSosInjectorConfig instance() {
        if (instance == null) {
            initializeFromConfigFile(CONFIG_FILE);
        }
        return instance;
    }

    private static void initializeFromConfigFile(String configFilePath) {
        instance = new DatabaseSosInjectorConfig();
        Configuration config;
        try {
            config = new PropertiesConfiguration(configFilePath);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }

        instance.sosUrl = getRequiredConfigString(config, SOS_URL);
        instance.sosAuthenticationToken = config.getString(SOS_AUTHENTICATION_TOKEN);
        instance.jdbcUrl = getRequiredConfigString(config, JDBC_URL);
        instance.jdbcUsername = config.getString(JDBC_USERNAME);
        instance.jdbcPassword = config.getString(JDBC_PASSWORD);
 
        if (System.getProperty(QUERY_PATH) != null) {
            instance.queryPath = System.getProperty(QUERY_PATH);
        } else {
            instance.queryPath = config.getString(QUERY_PATH, QUERY_PATH_DEFAULT);    
        }
        File queryPathFile = new File(instance.getQueryPath());
        if (!queryPathFile.exists()) {
            throw new RuntimeException(String.format("Query directory path '%s' doesn't exist", queryPathFile.getAbsolutePath()));
        }
        
        instance.publisherInfo = new PublisherInfo();
        instance.publisherInfo.setName(getRequiredConfigString(config, PUBLISHER_NAME));
        instance.publisherInfo.setCode(getRequiredConfigString(config, PUBLISHER_CODE));
        instance.publisherInfo.setCountry(getRequiredConfigString(config, PUBLISHER_COUNTRY));
        instance.publisherInfo.setEmail(getRequiredConfigString(config, PUBLISHER_EMAIL));
        instance.publisherInfo.setWebAddress(getRequiredConfigString(config, PUBLISHER_WEB_ADDRESS));
    }

    private static String getRequiredConfigString(Configuration config, String propertyName) {
        String value = config.getString(propertyName);
        DatabaseSosInjectorHelper.requireNonNull(propertyName, value);
        return value;        
    }

    public String getSosUrl() {
        return sosUrl;
    }

    public String getSosAuthenticationToken() {
        return sosAuthenticationToken;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public String getQueryPath() {
        return queryPath;
    }

    /**
     * Set queryPath. Normally we don't want setters here but there may be an environment variable override.
     * @param queryPath
     */
    public void setQueryPath(String queryPath) {
        this.queryPath = queryPath;
    }
    
    public PublisherInfo getPublisherInfo() {
        return publisherInfo;
    }
}
