package com.company.jmixdatastore.service.dbcon.impl;

import com.company.jmixdatastore.entity.DBType;
import com.company.jmixdatastore.service.dbcon.AbstractDbConnect;
import org.springframework.stereotype.Service;

@Service
public class SqlServerConnect extends AbstractDbConnect {

    @Override
    public DBType getSupportedDbType() {
        return DBType.SQLSERVER;
    }
}