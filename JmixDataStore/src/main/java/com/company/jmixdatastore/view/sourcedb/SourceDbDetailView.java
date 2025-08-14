package com.company.jmixdatastore.view.sourcedb;

import com.company.jmixdatastore.entity.DBType;
import com.company.jmixdatastore.entity.SourceDb;
import com.company.jmixdatastore.service.dbcon.DbConnect;
import com.company.jmixdatastore.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.*;

@Route(value = "source-dbs/:id", layout = MainView.class)
@ViewController("SourceDb.detail")
@ViewDescriptor("source-db-detail-view.xml")
@EditedEntityContainer("sourceDbDc")
public class SourceDbDetailView extends StandardDetailView<SourceDb> {

    @Subscribe("datatype")
    public void onDatatypeChange(HasValue.ValueChangeEvent<?> event) {
        updateJdbcUrl();
    }

    @Subscribe("hostField")
    public void onHostChange(HasValue.ValueChangeEvent<?> event) {
        updateJdbcUrl();
    }

    @Subscribe("portField")
    public void onPortChange(HasValue.ValueChangeEvent<?> event) {
        updateJdbcUrl();
    }

    @Subscribe("dbnameField")
    public void onDbnameChange(HasValue.ValueChangeEvent<?> event) {
        updateJdbcUrl();
    }

    @Subscribe("urlField")
    public void onUrlFieldChange(HasValue.ValueChangeEvent<String> event) {
        parseJdbcUrl();
    }

    @Autowired
    private DbConnect dbConnect;

    private void updateJdbcUrl() {
        SourceDb db = getEditedEntity();
        DBType datatype = db.getDbtype();
        String host = db.getHost();
        String port = db.getPort();
        String dbname = db.getDbname();

        if (datatype != null && host != null && port != null && dbname != null) {
            String url = switch (datatype) {
                case MYSQL -> "jdbc:mysql://" + host + ":" + port + "/" + dbname;
                case POSTGRES -> "jdbc:postgresql://" + host + ":" + port + "/" + dbname;
                case ORACLE -> "jdbc:oracle:thin:@" + host + ":" + port + ":" + dbname;
            };
            db.setUrl(url);
            getViewData().loadAll();
        }
    }

    @Subscribe(id = "test_connection", subject = "clickListener")
    public void onTestConnectionClick(ClickEvent<?> event) {
        SourceDb db = getEditedEntity();
        boolean isValid = dbConnect.connect(db);
        if (isValid) {
            showNotification("✅ Connection successful", NotificationVariant.LUMO_SUCCESS);
        } else {
            showNotification("⚠️ Connection failed", NotificationVariant.LUMO_ERROR);
        }
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(variant);
    }

    private void parseJdbcUrl() {
        SourceDb db = getEditedEntity();
        String url = db.getUrl();

        if (url == null || url.isBlank()) return;

        try {
            if (url.startsWith("jdbc:mysql://")) {
                String[] parts = url.substring("jdbc:mysql://".length()).split("/");
                String[] hostPort = parts[0].split(":");
                db.setHost(hostPort[0]);
                db.setPort(hostPort.length > 1 ? hostPort[1] : "");
                db.setDbname(parts.length > 1 ? parts[1] : "");
                db.setDbtype(DBType.MYSQL);
            } else if (url.startsWith("jdbc:postgresql://")) {
                String[] parts = url.substring("jdbc:postgresql://".length()).split("/");
                String[] hostPort = parts[0].split(":");
                db.setHost(hostPort[0]);
                db.setPort(hostPort.length > 1 ? hostPort[1] : "");
                db.setDbname(parts.length > 1 ? parts[1] : "");
                db.setDbtype(DBType.POSTGRES);
            } else if (url.startsWith("jdbc:oracle:thin:@")) {
                String[] parts = url.substring("jdbc:oracle:thin:@".length()).split(":");
                if (parts.length == 3) {
                    db.setHost(parts[0]);
                    db.setPort(parts[1]);
                    db.setDbname(parts[2]);
                    db.setDbtype(DBType.ORACLE);
                }
            }
            getViewData().loadAll();
        } catch (Exception e) {
            System.err.println("Failed to parse JDBC URL: " + e.getMessage());
        }
    }

}