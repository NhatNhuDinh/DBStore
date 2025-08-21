package com.company.jmixdatastore.service.dbcon;

import com.company.jmixdatastore.entity.DBType;
import com.company.jmixdatastore.entity.SourceDb;
import com.company.jmixdatastore.entity.TableDb;
import com.company.jmixdatastore.entity.TableDetail;

import java.util.List;

public interface DbConnect {

    boolean connect(SourceDb sourceDb);

    List<TableDb> loadTableList(SourceDb sourceDb);
    List<TableDetail> loadTableFields(TableDb tableDb);

    List<TableDb> syncTableList(SourceDb sourceDb);
    List<TableDetail> syncTableFields(SourceDb sourceDb, TableDb tableDb);

    DBType getSupportedDbType();
}
