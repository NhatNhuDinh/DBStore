package com.company.jmixdatastore.service.dbcon;

import com.company.jmixdatastore.entity.*;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public abstract class AbstractDbConnect implements DbConnect {

    @Autowired
    protected DbConnectionService connectionService;

    @Autowired
    protected DataManager dataManager;

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
            DatabaseMetaData metaData = connection.getMetaData();
            String schema = getSchema(connection, sourceDb);
            String[] tableTypes = getTableTypes();

            // Lấy danh sách bảng ở remote
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(null, schema, "%", tableTypes)) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }

            // Lấy danh sách local trong DB app
            List<TableDb> localTables = dataManager.load(TableDb.class)
                    .query("select t from TableDb t where t.sourceDb.id = :srcId")
                    .parameter("srcId", sourceDb.getId())
                    .list();

            // Đồng bộ giữa remote và local
            updateTableDb(tables, localTables, sourceDb);
            dataManager.saveAll(localTables);

            // Đồng bộ field cho từng bảng (chỉ bảng không bị DELETED)
            for (TableDb tableDb : localTables) {
                if (tableDb.getStatus() != Status.DELETED) {
                    loadTableFields(sourceDb, schema, tableDb);
                }
            }

            return localTables;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public List<TableDetail> loadTableFields(SourceDb sourceDb, String tableName, TableDb tableDb) {
        try (Connection connection = connectionService.getConnection(sourceDb)) {
            DatabaseMetaData metaData = connection.getMetaData();
            String schema = getSchema(connection, sourceDb);

            // 1. Lấy field remote: remoteFieldInfos map tên cột -> info
            Map<String, Map<String, Object>> remoteFieldInfos = new LinkedHashMap<>();
            try (ResultSet rs = metaData.getColumns(null, schema, tableName, "%")) {
                while (rs.next()) {
                    String fieldName = rs.getString("COLUMN_NAME");
                    Map<String, Object> info = new HashMap<>();
                    info.put("COLUMN_NAME", fieldName);
                    info.put("TYPE_NAME", rs.getString("TYPE_NAME"));
                    info.put("IS_NULLABLE", rs.getString("IS_NULLABLE"));
                    info.put("COLUMN_DEF", rs.getString("COLUMN_DEF"));
                    info.put("COLUMN_SIZE", rs.getString("COLUMN_SIZE"));
                    info.put("REMARKS", rs.getString("REMARKS"));
                    remoteFieldInfos.put(fieldName.toLowerCase(), info);
                }
            }

            // 2. Lấy field local của tableDb này
            List<TableDetail> localFields = dataManager.load(TableDetail.class)
                    .query("select d from TableDetail d where d.tableDb.id = :tid")
                    .parameter("tid", tableDb.getId())
                    .list();

            // 4. Lưu lại tất cả
            updateTableField(remoteFieldInfos, localFields, tableDb);
            dataManager.saveAll(localFields);

            return localFields;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    protected void updateTableField(
            Map<String, Map<String, Object>> remoteFieldInfos,
            List<TableDetail> localFields,
            TableDb tableDb
    ) {
        Map<String, TableDetail> existByName = new LinkedHashMap<>();
        for (TableDetail field : localFields) {
            if (field.getName() != null)
                existByName.put(field.getName().toLowerCase(), field);
        }

        // Thêm mới hoặc update status SYNCED
        for (String fieldNameLower : remoteFieldInfos.keySet()) {
            TableDetail detail = existByName.get(fieldNameLower);
            Map<String, Object> info = remoteFieldInfos.get(fieldNameLower);

            if (detail == null) {
                detail = dataManager.create(TableDetail.class);
                detail.setTableDb(tableDb);
                detail.setName((String) info.get("COLUMN_NAME"));
                localFields.add(detail);
            }
            // Luôn cập nhật các thông tin mới nhất
            detail.setDataType((String) info.get("TYPE_NAME"));
            String isNullable = (String) info.get("IS_NULLABLE");
            detail.setIsNull("YES".equalsIgnoreCase(isNullable) || "1".equals(isNullable));
            detail.setDefaultValue((String) info.get("COLUMN_DEF"));
            String size = (String) info.get("COLUMN_SIZE");
            detail.setSize(size != null ? Long.valueOf(size) : null);
            detail.setStatus(Status.SYNCED);
            detail.setDescription((String) info.get("REMARKS"));

            existByName.remove(fieldNameLower);
        }

        // Các field còn lại là field local đã bị xóa trên remote
        for (TableDetail orphan : existByName.values()) {
            orphan.setStatus(Status.DELETED);
        }
    }

    protected void updateTableDb(List<String> remoteTableNames, List<TableDb> localTableDbs, SourceDb sourceDb) {
        Map<String, TableDb> existByName = new LinkedHashMap<>();
        for (TableDb t : localTableDbs) {
            if (t.getName() != null) {
                existByName.put(t.getName().toLowerCase(), t);
            }
        }

        // Thêm mới hoặc update trạng thái SYNCED
        for (String name : remoteTableNames) {
            String key = name.toLowerCase();
            TableDb tableDb = existByName.get(key);
            if (tableDb == null) {
                tableDb = dataManager.create(TableDb.class);
                tableDb.setName(name);
                tableDb.setSourceDb(sourceDb);
                tableDb.setStatus(Status.NEW);
                tableDb.setDescription("Table: " + name);
                localTableDbs.add(tableDb);
            } else {
                tableDb.setStatus(Status.SYNCED);
                existByName.remove(key);
            }
        }

        // Còn lại là bảng đã xóa trên remote
        for (TableDb orphan : existByName.values()) {
            orphan.setStatus(Status.DELETED);
        }
    }

    protected String getSchema(Connection connection, SourceDb sourceDb) throws SQLException {
        return connection.getSchema();
    }

    protected String[] getTableTypes() {
        return new String[]{"TABLE"};
    }
}
