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
        return dataManager.load(TableDb.class)
                .query("select t from TableDb t where t.sourceDb.id = :srcId")
                .parameter("srcId", sourceDb.getId())
                .list();
    }

    @Override
    public List<TableDetail> loadTableFields(TableDb tableDb) {
        return dataManager.load(TableDetail.class)
                .query("select d from TableDetail d where d.tableDb.id = :tid")
                .parameter("tid", tableDb.getId())
                .list();
    }

    @Override
    public List<TableDb> syncTableList(SourceDb sourceDb) {
        try (Connection connection = connectionService.getConnection(sourceDb)) {
            DatabaseMetaData metaData = connection.getMetaData();
            String schema = getSchema(connection);
            String[] tableTypes = getTableTypes();

            // Remote
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(null, schema, "%", tableTypes)) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }

            // Local DB
            List<TableDb> localTables = loadTableList(sourceDb);
            updateTableDb(tables, localTables, sourceDb);

            dataManager.saveAll(localTables);

            for (TableDb tableDb : localTables) {
                if (tableDb.getStatus() != Status.ĐÃ_XÓA) {
                    syncTableFields(sourceDb, tableDb);
                }
            }

            return localTables;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public List<TableDetail> syncTableFields(SourceDb sourceDb, TableDb tableDb) {
        try (Connection connection = connectionService.getConnection(sourceDb)) {
            DatabaseMetaData metaData = connection.getMetaData();
            String schema = getSchema(connection);

            // Lấy remote field
            Map<String, TableDetail> remoteFields = new LinkedHashMap<>();
            try (ResultSet rs = metaData.getColumns(null, schema, tableDb.getName(), "%")) {
                while (rs.next()) {
                    TableDetail td = new TableDetail();
                    td.setName(rs.getString("COLUMN_NAME"));
                    td.setDataType(rs.getString("TYPE_NAME"));
                    String isNullable = rs.getString("IS_NULLABLE");
                    td.setIsNull("YES".equalsIgnoreCase(isNullable) || "1".equals(isNullable));
                    td.setDefaultValue(rs.getString("COLUMN_DEF"));
                    String size = rs.getString("COLUMN_SIZE");
                    td.setSize(size != null ? Long.valueOf(size) : null);
                    td.setDescription(rs.getString("REMARKS"));
                    td.setTableDb(tableDb);

                    remoteFields.put(td.getName().toLowerCase(), td);
                }
            }

            // Local field
            List<TableDetail> localFields = loadTableFields(tableDb);

            updateTableField(remoteFields, localFields, tableDb);
            dataManager.saveAll(localFields);

            return localFields;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    protected void updateTableDb(List<String> remoteTableNames, List<TableDb> localTableDbs, SourceDb sourceDb) {
        Map<String, TableDb> existByName = new LinkedHashMap<>();
        for (TableDb t : localTableDbs) {
            if (t.getName() != null) {
                existByName.put(t.getName().toLowerCase(), t);
            }
        }

        // Thêm mới hoặc update
        for (String name : remoteTableNames) {
            String key = name.toLowerCase();
            TableDb tableDb = existByName.get(key);
            if (tableDb == null) {
                tableDb = dataManager.create(TableDb.class);
                tableDb.setName(name);
                tableDb.setSourceDb(sourceDb);
                tableDb.setStatus(Status.MỚI);
                tableDb.setDescription("Bảng " + name);
                localTableDbs.add(tableDb);
            } else {
                tableDb.setStatus(Status.ĐÃ_ĐỒNG_BỘ);
                existByName.remove(key);
            }
        }

        // bảng đã xóa trên remote
        for (TableDb orphan : existByName.values()) {
            orphan.setStatus(Status.ĐÃ_XÓA);
            List<TableDetail> orphanFields = loadTableFields(orphan);
            for (TableDetail field : orphanFields) {
                field.setStatus(Status.ĐÃ_XÓA);
            }
            dataManager.saveAll(orphanFields);
        }
    }

    protected void updateTableField(
            Map<String, TableDetail> remoteFields,
            List<TableDetail> localFields,
            TableDb tableDb
    ) {
        Map<String, TableDetail> localByName = new LinkedHashMap<>();
        for (TableDetail f : localFields) {
            if (f.getName() != null) {
                localByName.put(f.getName().toLowerCase(), f);
            }
        }

        // Xử lý từng feild remote
        for (Map.Entry<String, TableDetail> entry : remoteFields.entrySet()) {
            String fieldName = entry.getKey();
            TableDetail remoteField = entry.getValue();

            TableDetail localField = localByName.get(fieldName);
            if (localField == null) {
                // Field mới
                TableDetail newField = dataManager.create(TableDetail.class);
                newField.setName(remoteField.getName());
                newField.setDataType(remoteField.getDataType());
                newField.setIsNull(remoteField.getIsNull());
                newField.setDefaultValue(remoteField.getDefaultValue());
                newField.setSize(remoteField.getSize());
                newField.setDescription(remoteField.getDescription());
                newField.setStatus(Status.MỚI);
                newField.setTableDb(tableDb);

                localFields.add(newField);
            } else {
                boolean changed = false;
                if (!Objects.equals(localField.getDataType(), remoteField.getDataType())) changed = true;
                if (!Objects.equals(localField.getIsNull(), remoteField.getIsNull())) changed = true;
                if (!Objects.equals(localField.getDefaultValue(), remoteField.getDefaultValue())) changed = true;
                if (!Objects.equals(localField.getSize(), remoteField.getSize())) changed = true;
                if (!Objects.equals(localField.getDescription(), remoteField.getDescription())) changed = true;

                localField.setDataType(remoteField.getDataType());
                localField.setIsNull(remoteField.getIsNull());
                localField.setDefaultValue(remoteField.getDefaultValue());
                localField.setSize(remoteField.getSize());
                localField.setDescription(remoteField.getDescription());

                if (changed) {
                    localField.setStatus(Status.ĐÃ_THAY_ĐỔI);
                } else {
                    localField.setStatus(Status.ĐÃ_ĐỒNG_BỘ);
                }
                localByName.remove(fieldName);
            }
        }

        // Field đã bị xóa ở remote
        for (TableDetail orphan : localByName.values()) {
            orphan.setStatus(Status.ĐÃ_XÓA);
        }


    }

    protected String getSchema(Connection connection) throws SQLException {
        return connection.getSchema();
    }

    protected String[] getTableTypes() {
        return new String[]{"TABLE"};
    }
}
