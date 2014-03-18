package com.axiomalaska.sos.injector.db;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

public class DatabaseConnectionHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnectionHelper.class);
    private static BoneCP connectionPool;
    
    private static BoneCP getConnectionPool() throws ClassNotFoundException, SQLException {
        if (connectionPool == null) {
            //only keep the correct driver for the url in the classloader, since otherwise we'll get the error from
            //the first tried driver even if it's not the correct driver for our url.
            Driver correctDriver = DriverManager.getDriver(DatabaseSosInjectorConfig.instance().getJdbcUrl());
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                if (!driver.equals(correctDriver)) {
                    DriverManager.deregisterDriver(driver);
                }
            }
            
            LOGGER.debug("Creating connection pool");
            BoneCPConfig config = new BoneCPConfig();
            config.setJdbcUrl(DatabaseSosInjectorConfig.instance().getJdbcUrl());
            if (!Strings.isNullOrEmpty(DatabaseSosInjectorConfig.instance().getJdbcUsername())) {
                config.setUsername(DatabaseSosInjectorConfig.instance().getJdbcUsername());
            }
            if (!Strings.isNullOrEmpty(DatabaseSosInjectorConfig.instance().getJdbcPassword())) {
                config.setPassword(DatabaseSosInjectorConfig.instance().getJdbcPassword());
            }
            connectionPool = new BoneCP(config);
        }
        return connectionPool;
    }

    public static Connection getConnection() throws SQLException, ClassNotFoundException {        
        return getConnectionPool().getConnection();
    }

    public static void shutdownConnectionPool() {
        if (connectionPool != null) {
            LOGGER.debug("Shutting down connection pool");
            connectionPool.shutdown();
        }
    }
}
