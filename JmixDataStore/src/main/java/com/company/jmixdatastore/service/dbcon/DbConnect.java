package com.company.jmixdatastore.service.dbcon;

public interface DbConnect {
    boolean connect(String host, String port, String dbName, String userName, String password);
}
