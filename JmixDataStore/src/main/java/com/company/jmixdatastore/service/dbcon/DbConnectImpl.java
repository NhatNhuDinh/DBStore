package com.company.jmixdatastore.service.dbcon;

import com.company.jmixdatastore.entity.SourceDb;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Service
public class DbConnectImpl implements DbConnect{

    @Override
    public boolean connect(SourceDb db) {
        try (Connection conn = DriverManager.getConnection(db.getUrl(), db.getUsername(), db.getPassword())) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
