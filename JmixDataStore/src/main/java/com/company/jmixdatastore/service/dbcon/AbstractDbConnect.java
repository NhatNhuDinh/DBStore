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

            return localTables;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Đồng bộ bảng giữa remote và local, thêm mới và cập nhật trạng thái.
     */
    protected void updateTableDb(List<String> remoteTableNames, List<TableDb> localTableDbs, SourceDb sourceDb) {
        // Map name -> TableDb (local)
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
                existByName.remove(key); // Đánh dấu đã kiểm tra
            }
        }

        // Còn lại là bảng đã xóa trên remote
        for (TableDb orphan : existByName.values()) {
            orphan.setStatus(Status.DELETED);
        }
    }

    @Override
    public List<TableDetail> loadTableFields(SourceDb sourceDb, String tableName, TableDb tableDb) {
        List<TableDetail> tableDetailList = new ArrayList<>();
        try (Connection connection = connectionService.getConnection(sourceDb)) {
            DatabaseMetaData metaData = connection.getMetaData();
            String schema = getSchema(connection, sourceDb);

            try (ResultSet rs = metaData.getColumns(null, schema, tableName, "%")) {
                while (rs.next()) {
                    TableDetail tableDetail = dataManager.create(TableDetail.class);

                    tableDetail.setName(rs.getString("COLUMN_NAME"));
                    tableDetail.setDataType(rs.getString("TYPE_NAME"));
                    String isNullable = rs.getString("IS_NULLABLE");
                    tableDetail.setIsNull("YES".equalsIgnoreCase(isNullable) || "1".equals(isNullable)); // Chuẩn hóa kiểu boolean
                    tableDetail.setDefaultValue(rs.getString("COLUMN_DEF"));
                    String size = rs.getString("COLUMN_SIZE");
                    tableDetail.setSize(size != null ? Long.valueOf(size) : null);
                    tableDetail.setStatus(Status.SYNCED);
                    tableDetail.setDescription(rs.getString("REMARKS"));
                    tableDetail.setTableDb(tableDb);

                    tableDetailList.add(tableDetail);
                }
            }
            dataManager.saveAll(tableDetailList);
            return tableDetailList;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    protected String getSchema(Connection connection, SourceDb sourceDb) throws SQLException {
        return connection.getSchema();
    }

    protected String[] getTableTypes() {
        return new String[]{"TABLE"};
    }
}
