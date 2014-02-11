package com.axiomalaska.sos.injector.db.data;

import com.axiomalaska.sos.data.SosSensor;

public class DatabaseSosSensor extends SosSensor implements HasDatabaseId {
    private String databaseId;

    @Override
    public String getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(String databaseId) {
        this.databaseId = databaseId;
    }
}
