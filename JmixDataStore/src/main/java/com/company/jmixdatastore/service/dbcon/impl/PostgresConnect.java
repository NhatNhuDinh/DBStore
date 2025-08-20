package com.company.jmixdatastore.service.dbcon.impl;

import com.company.jmixdatastore.entity.DBType;
import com.company.jmixdatastore.entity.SourceDb;
import com.company.jmixdatastore.entity.Status;
import com.company.jmixdatastore.entity.TableDb;
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
public class PostgresConnect implements DbConnect {

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

            // Ở PostgreSQL thường phải chỉ rõ schema, mặc định là "public"
            ResultSet rs = metaData.getTables(null, "public", "%", new String[]{"TABLE"});
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
    public List<KeyValueEntity> loadTableFields(SourceDb sourceDb, String tableName) {
        try (Connection connection = connectionService.getConnection(sourceDb)) {
            DatabaseMetaData metaData = connection.getMetaData();

            // PostgreSQL schema mặc định là "public"
            ResultSet rs = metaData.getColumns(null, "public", tableName, "%");
            List<KeyValueEntity> fields = new ArrayList<>();

            while (rs.next()) {
                KeyValueEntity field = new KeyValueEntity();
                field.setValue("name", rs.getString("COLUMN_NAME"));
                field.setValue("dataType", rs.getString("TYPE_NAME"));
                field.setValue("size", rs.getInt("COLUMN_SIZE"));
                field.setValue("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                field.setValue("default", rs.getString("COLUMN_DEF"));
                field.setValue("description", rs.getString("REMARKS"));
                fields.add(field);
            }
            return fields;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public DBType getSupportedDbType() {
        return DBType.POSTGRESQL;
    }
}
