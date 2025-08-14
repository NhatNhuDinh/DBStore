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

            tablesDc.setItems(result);

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
                            rs.getString("COLUMN_NAME"),
                            rs.getString("TYPE_NAME"),
                            rs.getString("IS_NULLABLE")
                    ));
                }
            }
            columnsDc.setItems(cols);

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
            loadTables(src);
        }
    }

}
