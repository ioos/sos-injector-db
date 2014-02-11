package com.axiomalaska.sos.injector.db.data;

import com.axiomalaska.sos.data.SosStation;

public class DatabaseSosStation extends SosStation implements HasDatabaseId {
    private String databaseId;

    @Override
    public String getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(String databaseId) {
        this.databaseId = databaseId;
    }
}
