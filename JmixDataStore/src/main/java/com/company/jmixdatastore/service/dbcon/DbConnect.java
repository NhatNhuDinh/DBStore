package com.company.jmixdatastore.service.dbcon;

import com.company.jmixdatastore.entity.SourceDb;

import java.util.List;

public interface DbConnect {

    boolean connect(SourceDb sourceDb);
    List<String> loadTableList(SourceDb sourceDb);

    List<String> loadTableFields(SourceDb sourceDb, String tableName);
}
