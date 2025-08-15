package com.company.jmixdatastore.service.dbcon;

import com.company.jmixdatastore.entity.SourceDb;
import io.jmix.core.DataManager;
import io.jmix.core.entity.KeyValueEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class DbConnectImpl implements DbConnect{

    @Autowired
    protected DataManager dataManager;

    @Override
    public boolean connect(SourceDb sourceDb) {
        return getConnection(sourceDb) != null;
    }

    @Override
    public List<String> loadTableList(SourceDb sourceDb) {
        Connection connection = getConnection(sourceDb);
        try {
            assert connection != null;
            DatabaseMetaData md = connection.getMetaData();

            String vendor = sourceDb.getDbtype() != null
                    ? sourceDb.getDbtype().name().toLowerCase(Locale.ROOT)
                    : md.getDatabaseProductName().toLowerCase(Locale.ROOT);

            String catalog = null;
            String schemaPattern = null;

            if (vendor.contains("postgres")) {
                schemaPattern = (connection.getSchema() == null || connection.getSchema().isBlank())
                        ? "public"
                        : connection.getSchema();
            } else if (vendor.contains("mysql") || vendor.contains("mariadb")) {
                catalog = connection.getCatalog();
            } else if (vendor.contains("sql server") || vendor.contains("microsoft")) {
                catalog = connection.getCatalog();
                schemaPattern = (connection.getSchema() == null || connection.getSchema().isBlank())
                        ? "dbo"
                        : connection.getSchema();
            }

            List<String> tables = new ArrayList<>();
            try (ResultSet rs = md.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    tables.add(name);
                }
            }

            tables.sort(String.CASE_INSENSITIVE_ORDER);
            connection.close();
            return tables;

        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return List.of();
    }

    @Override
    public List<KeyValueEntity> loadTableFields(SourceDb sourceDb, String tableName) {
        List<KeyValueEntity> fields = new ArrayList<>();
        try{
            Connection connection = getConnection(sourceDb);
            // Lấy metadata từ connection
            assert connection != null;
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getColumns(null, null, tableName, "%");
            while (resultSet.next()) {
                KeyValueEntity field = dataManager.create(KeyValueEntity.class);
                field.setValue("name",resultSet.getString("COLUMN_NAME"));
                field.setValue("dataType", resultSet.getString("TYPE_NAME"));
                field.setValue("size", resultSet.getInt("COLUMN_SIZE"));
                field.setValue("nullable", resultSet.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                field.setValue("default", resultSet.getString("COLUMN_DEF"));
                fields.add(field);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fields;
    }

    private Connection getConnection(SourceDb sourceDb) {
        String url = sourceDb.getUrl();
        if(url == null || url.isEmpty()) {
            url = switch (sourceDb.getDbtype().name().toLowerCase()) {
                case "mysql" ->
                        String.format("jdbc:mysql://%s:%s/%s", sourceDb.getHost(), sourceDb.getPort(), sourceDb.getDbname());
                case "postgresql" ->
                        String.format("jdbc:postgresql://%s:%s/%s", sourceDb.getHost(), sourceDb.getPort(), sourceDb.getDbname());
                case "sqlserver" ->
                        String.format("jdbc:sqlserver://%s:%s;databaseName=%s", sourceDb.getHost(), sourceDb.getPort(), sourceDb.getDbname());
                default -> throw new IllegalArgumentException("Không hỗ trợ loại DB: " + sourceDb.getDbtype().name());
            };
        }
        if(url.contains("sqlserver")) {
            url += ";encrypt=true;trustServerCertificate=true";
        }else if(url.contains("mysql")) {
            url += "?nullCatalogMeansCurrent=true&useInformationSchema=true";
        }
        try {
            Connection connection = DriverManager.getConnection(url, sourceDb.getUsername(), sourceDb.getPassword());
            if (connection != null) {
                return connection;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
