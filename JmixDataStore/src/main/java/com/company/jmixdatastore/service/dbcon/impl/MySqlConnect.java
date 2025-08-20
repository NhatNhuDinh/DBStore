package com.company.jmixdatastore.service.dbcon.impl;

import com.company.jmixdatastore.entity.*;
import com.company.jmixdatastore.service.dbcon.DbConnect;
import com.company.jmixdatastore.service.dbcon.DbConnectionService;
import io.jmix.core.DataManager;
import io.jmix.core.entity.KeyValueEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class MySqlConnect implements DbConnect {

    @Autowired
    private DbConnectionService connectionService;

    @Autowired
    private DataManager dataManager;

    @Override
    public boolean connect(SourceDb sourceDb) {
        try (Connection connection = connectionService.getConnection(sourceDb)) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public List<TableDb> loadTableList(SourceDb sourceDb) {
        try (Connection connection = connectionService.getConnection(sourceDb)) {
            List<String> tables = new ArrayList<>();
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet rs = metaData.getTables(null, connection.getSchema(), "%", new String[]{"TABLE"});
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
            List<TableDb> tableDbList = tables.stream().map(tableName ->{
                TableDb tableDb = dataManager.create(TableDb.class);
                tableDb.setName(tableName);
                tableDb.setSourceDb(sourceDb);
                tableDb.setStatus(Status.SYNCED);
                tableDb.setDescription("Table: " + tableName);
                return tableDb;
            }).toList();

            dataManager.saveAll(tableDbList);

            return tableDbList;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public List<TableDetail> loadTableFields(SourceDb sourceDb, String tableName, TableDb tableDb) {
        List<TableDetail> tableDetailList = new ArrayList<>();
        try (Connection connection = connectionService.getConnection(sourceDb)) {
            DatabaseMetaData metaData = connection.getMetaData();

            ResultSet rs = metaData.getColumns(null, connection.getSchema(), tableName, "%");
            while (rs.next()) {
                TableDetail tableDetail = dataManager.create(TableDetail.class);

                tableDetail.setName(rs.getString("COLUMN_NAME"));
                tableDetail.setDataType(rs.getString("TYPE_NAME"));
                tableDetail.setIsNull(rs.getBoolean("IS_NULLABLE"));
                tableDetail.setDefaultValue(rs.getString("COLUMN_DEF"));
                tableDetail.setSize(Long.valueOf(rs.getString("COLUMN_SIZE")));
                tableDetail.setStatus(Status.SYNCED);
                tableDetail.setDescription(rs.getString("REMARKS"));
                tableDetail.setTableDb(tableDb);

                tableDetailList.add(tableDetail);
            }
            rs.close();

            dataManager.saveAll(tableDetailList);
            return tableDetailList;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }


    @Override
    public DBType getSupportedDbType() {
        return DBType.MYSQL;
    }
}
