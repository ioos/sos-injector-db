package com.axiomalaska.sos.injector.db.data;

import com.axiomalaska.phenomena.PhenomenonImp;

public class DatabasePhenomenon extends PhenomenonImp implements HasDatabaseId {
    private String databaseId;

    @Override
    public String getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(String databaseId) {
        this.databaseId = databaseId;
    }
}
