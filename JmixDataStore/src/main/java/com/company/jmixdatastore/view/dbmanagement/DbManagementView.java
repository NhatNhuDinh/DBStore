package com.company.jmixdatastore.view.dbmanagement;

import com.company.jmixdatastore.entity.SourceDb;
import com.company.jmixdatastore.view.main.MainView;
import com.company.jmixdatastore.view.sourcedb.SourceDbDetailView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Route(value = "db-management-view", layout = MainView.class)
@ViewController(id = "DbManagementView")
@ViewDescriptor(path = "db-management-view.xml")
public class DbManagementView extends StandardView {

    private final DialogWindows dialogWindows;

    @ViewComponent
    private EntityComboBox<SourceDb> dbSourseComboBox;

    @ViewComponent
    private CollectionLoader<SourceDb> sourceDbsDl;

    @ViewComponent
    private CollectionContainer<TableInfo> tablesDc;

    @ViewComponent
    private CollectionContainer<ColumnInfo> columnsDc;

    @ViewComponent
    private CollectionContainer<LinkedHashMap<String, Object>> rowsDc;

    @ViewComponent
    private io.jmix.flowui.component.grid.DataGrid<LinkedHashMap> rowsGrid;


    @Autowired
    private Notifications notifications;

    public DbManagementView(DialogWindows dialogWindows) {
        this.dialogWindows = dialogWindows;
    }

    @Subscribe(id = "newButton", subject = "clickListener")
    public void onNewButtonClick(final ClickEvent<JmixButton> event) {
        dialogWindows.detail(this, SourceDb.class)
                .newEntity()
                .withAfterCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                        sourceDbsDl.load();
                        SourceDbDetailView created = (SourceDbDetailView) closeEvent.getView();
                        SourceDb sourceDb = created.getEditedEntity();
                        if (sourceDb != null) {
                            dbSourseComboBox.setValue(sourceDb);
                        }
                    }
                })
                .build()
                .open();
    }

    @Subscribe(id = "connectButton", subject = "clickListener")
    public void onConnectClick(ClickEvent<JmixButton> event) {
        SourceDb src = dbSourseComboBox.getValue();
        if (src == null) {
            notifications.create("Please select a data source first.").show();
            return;
        }
        loadTables(src);
    }

    private void loadTables(SourceDb src) {
        List<TableInfo> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(src.getUrl(), src.getUsername(), src.getPassword())) {
            DatabaseMetaData md = conn.getMetaData();
            try (ResultSet rs = md.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    result.add(new TableInfo(rs.getString("TABLE_NAME")));
                }
            }

            System.out.println("DEBUG - Tables loaded from DB: " + result.size());
            result.forEach(t -> System.out.println("DEBUG - Table: " + t.getName()));

            tablesDc.setItems(result);

            // Debug xem container có nhận dữ liệu chưa
            System.out.println("DEBUG - tablesDc item count: " + tablesDc.getItems().size());

            columnsDc.setItems(new ArrayList<>()); // clear columns
            notifications.create("Loaded " + result.size() + " tables.").show();
        } catch (SQLException ex) {
            notifications.create("Connection failed: " + ex.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }

    private void loadColumns(SourceDb src, String tableName) {
        List<ColumnInfo> cols = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(src.getUrl(), src.getUsername(), src.getPassword())) {
            DatabaseMetaData md = conn.getMetaData();
            try (ResultSet rs = md.getColumns(null, null, tableName, "%")) {
                while (rs.next()) {
                    cols.add(new ColumnInfo(
                            rs.getString("COLUMN_NAME")
                    ));
                }
            }

            System.out.println("DEBUG - Columns loaded from DB (" + tableName + "): " + cols.size());
            cols.forEach(c -> System.out.println("DEBUG - Column: " + c.getName()));

            columnsDc.setItems(cols);

            // Debug xem container có nhận dữ liệu chưa
            System.out.println("DEBUG - columnsDc item count: " + columnsDc.getItems().size());

        } catch (SQLException ex) {
            notifications.create("Load columns failed: " + ex.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }

    @Subscribe(id = "tablesGrid", subject = "selection")
    public void onTableSelected() {
        TableInfo selected = tablesDc.getItemOrNull();
        SourceDb src = dbSourseComboBox.getValue();
        if (selected != null && src != null) {
            loadColumns(src, selected.getName());
            loadTableData(src, selected.getName());
        }
    }

    private void loadTableData(SourceDb src, String tableName) {
        List<LinkedHashMap<String, Object>> rows = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(src.getUrl(), src.getUsername(), src.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " LIMIT 100")) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            // Đọc từng dòng dữ liệu
            while (rs.next()) {
                LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }

            // Đẩy data vào container
            rowsDc.setItems(rows);

            // Clear cột cũ và tạo cột mới tương ứng với columnLabel
            rowsGrid.removeAllColumns();
            for (int i = 1; i <= colCount; i++) {
                final String colName = meta.getColumnLabel(i);
                rowsGrid.addColumn(row -> {
                    Object value = row.get(colName);
                    return value != null ? value.toString() : "";
                }).setHeader(colName).setAutoWidth(true);
            }

            System.out.println("DEBUG - TableData rows: " + rows.size());
        } catch (SQLException e) {
            notifications.create("Failed to load data: " + e.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }


}
