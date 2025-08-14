package com.company.jmixdatastore.service.dbcon;

import com.company.jmixdatastore.entity.SourceDb;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class DbConnectImpl implements DbConnect{

    @Override
    public boolean connect(SourceDb sourceDb) {
        return getConnection(sourceDb) != null;
    }

    @Override
    public List<String> loadTableList(SourceDb sourceDb) {
        Connection connection = getConnection(sourceDb);
        try {
            DatabaseMetaData md = connection.getMetaData();

            String vendor = sourceDb.getDbtype() != null
                    ? sourceDb.getDbtype().name().toLowerCase(Locale.ROOT)
                    : md.getDatabaseProductName().toLowerCase(Locale.ROOT);

            String catalog = null;
            String schemaPattern = null;

            if (vendor.contains("postgres")) {
                catalog = null;
                schemaPattern = (connection.getSchema() == null || connection.getSchema().isBlank())
                        ? "public"
                        : connection.getSchema();
            } else if (vendor.contains("mysql") || vendor.contains("mariadb")) {
                catalog = connection.getCatalog();
                schemaPattern = null;
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
    public List<String> loadTableFields(SourceDb sourceDb, String tableName) {
        List<String> columns = new ArrayList<>();
        try{
            Connection connection = getConnection(sourceDb);
            // Lấy metadata từ connection
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getColumns(null, null, tableName, "%");
            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                columns.add(columnName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return columns;
    }


    private Connection getConnection(SourceDb sourceDb) {
        String url = sourceDb.getUrl();
        String driver = "";
        if(url == null || url.isEmpty()) {
            switch (sourceDb.getDbtype().name().toLowerCase()) {
                case "mysql":
                    driver = "com.mysql.cj.jdbc.Driver";
                    url = String.format("jdbc:mysql://%s:%d/%s", sourceDb.getHost(), sourceDb.getPort(), sourceDb.getDbname());
                    break;
                case "postgresql":
                    driver = "org.postgresql.Driver";
                    url = String.format("jdbc:postgresql://%s:%d/%s", sourceDb.getHost(), sourceDb.getPort(), sourceDb.getDbname());
                    break;
                case "sqlserver":
                    driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                    url = String.format("jdbc:sqlserver://%s:%d;databaseName=%s", sourceDb.getHost(), sourceDb.getPort(), sourceDb.getDbname());
                    break;
                default:
                    throw new IllegalArgumentException("Không hỗ trợ loại DB: " + sourceDb.getDbtype().name());
            }
        }
        try {
            Class.forName(driver);
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
