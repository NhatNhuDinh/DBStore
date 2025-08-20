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
import java.util.*;

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

            List<TableDb> existing = dataManager.load(TableDb.class)
                    .query("select t from TableDb t where t.sourceDb.id = :srcId")
                    .parameter("srcId", sourceDb.getId())
                    .list();

            if (existing.isEmpty()) {
                existing = tables.stream().map(tableName -> {
                    TableDb tableDb = dataManager.create(TableDb.class);
                    tableDb.setName(tableName);
                    tableDb.setSourceDb(sourceDb);
                    tableDb.setStatus(Status.SYNCED);
                    tableDb.setDescription("Table: " + tableName);
                    return tableDb;
                }).toList();
            } else {
                updateTableDb(tables, existing);
            }
            dataManager.saveAll(existing);
            return existing;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }


    private List<TableDb> updateTableDb(List<String> tableName, List<TableDb> tableDbList) {

        // Tạo map để kiểm tra sự tồn tại của tên bảng
        Map<String, TableDb> existByName = new LinkedHashMap<>();
        for (TableDb t : tableDbList) {
            if (t.getName() != null) {
                existByName.put(t.getName().toLowerCase(), t);
            }
        }

        // Trường hợp có thêm bảng mới
        for (String name : tableName) {
            String key = name.toLowerCase();
            TableDb tableDb = existByName.get(key);
            if (tableDb == null) {
                tableDb = dataManager.create(TableDb.class);
                tableDb.setName(name);
                tableDb.setSourceDb(tableDbList.getFirst().getSourceDb()); // Giả sử tất cả bảng đều thuộc cùng một SourceDb
                tableDb.setStatus(Status.NEW);
                tableDb.setDescription("Table: " + name);
                tableDbList.add(tableDb);
            }
            // Xóa đi nhưng cái nào đã duyệt qua ở remote
            existByName.remove(key);
        }

        // Những cái nào còn lại thì đánh dấu là deleted
        for (TableDb orphan : existByName.values()) {
            TableDb tableDb = tableDbList.get(tableDbList.indexOf(orphan));
            tableDb.setStatus(Status.DELETED);
        }
        return tableDbList;
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
