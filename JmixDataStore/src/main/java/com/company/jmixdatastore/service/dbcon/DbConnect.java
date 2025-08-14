package com.company.jmixdatastore.service.dbcon;

import com.company.jmixdatastore.entity.SourceDb;

public interface DbConnect {
    boolean connect(SourceDb db);
}
