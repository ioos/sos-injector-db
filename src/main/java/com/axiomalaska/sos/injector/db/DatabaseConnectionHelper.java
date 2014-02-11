package com.axiomalaska.sos.injector.db;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.google.common.base.Strings;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

public class DatabaseConnectionHelper {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnectionHelper.class);
    private static BoneCP connectionPool;
    
    private static BoneCP getConnectionPool() throws ClassNotFoundException, SQLException {
        if (connectionPool == null) {
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
