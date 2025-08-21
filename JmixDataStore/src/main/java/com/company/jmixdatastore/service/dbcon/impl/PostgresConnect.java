package com.company.jmixdatastore.service.dbcon.impl;

import com.company.jmixdatastore.entity.DBType;
import com.company.jmixdatastore.entity.SourceDb;
import com.company.jmixdatastore.service.dbcon.AbstractDbConnect;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

@Component
public class PostgresConnect extends AbstractDbConnect {

    @Override
    protected String getSchema(Connection connection) throws SQLException {
        return "public";
    }

    @Override
    public DBType getSupportedDbType() {
        return DBType.POSTGRESQL;
    }
}
