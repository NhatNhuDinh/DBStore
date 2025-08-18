package com.company.jmixdatastore.view.dbmanagement;


import com.company.jmixdatastore.entity.SourceDb;
import com.company.jmixdatastore.service.dbcon.DbConnect;
import com.company.jmixdatastore.service.dbcon.DbConnectFactory;
import com.company.jmixdatastore.view.main.MainView;
import com.company.jmixdatastore.view.sourcedb.SourceDbDetailView;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Route(value = "db-management-view", layout = MainView.class)
@ViewController(id = "DbManagementView")
@ViewDescriptor(path = "db-management-view.xml")
public class DbManagementView extends StandardView {

    @Autowired
    protected DataManager dataManager;
    @Autowired
    private DialogWindows dialogWindows;

    @Autowired
    private DbConnectFactory dbConnectFactory;

    @ViewComponent
    private CollectionLoader<SourceDb> sourceDbsDl;

    @ViewComponent
    private EntityComboBox<SourceDb> dbSourseComboBox;

    @ViewComponent
    private BoxLayout rightbox;

    @ViewComponent
    private Span tableCount;

    @ViewComponent
    private Span fieldCount;

    @ViewComponent
    private Span connectionStatus;

    @ViewComponent
    private KeyValueCollectionContainer tablesDc;

    @ViewComponent
    private KeyValueCollectionContainer fieldsDc;

    @Autowired
    private Notifications notifications;

    @Subscribe(id = "newButton", subject = "clickListener")
    public void onNewButtonClick(final ClickEvent<JmixButton> event) {
        dialogWindows.detail(this, SourceDb.class)
                .newEntity()
                .withAfterCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                        sourceDbsDl.load();
                        SourceDbDetailView sourceDbDetailView = (SourceDbDetailView) closeEvent.getView();
                        SourceDb entity = sourceDbDetailView.getEditedEntity();
                        dbSourseComboBox.setValue(entity);
                    }

                })
                .build()
                .open();

    }

    @Subscribe(id = "tablesDc", target = Target.DATA_CONTAINER)
    public void onTablesDcItemChange(final InstanceContainer.ItemChangeEvent<KeyValueEntity> event) {
        SourceDb selectedSourceDb = dbSourseComboBox.getValue();
        fieldsDc.getMutableItems().clear();
        KeyValueEntity selectedTable = event.getItem();
        if (selectedTable == null) {
            fieldCount.setText("Tổng số cột: 0");
            return;
        }

        String tableName = selectedTable.getValue("name");
        notifications.create("Selected table " + tableName )
                .withType(Notifications.Type.SUCCESS)
                .withPosition(Notification.Position.TOP_END)
                .show();
        DbConnect dbConnect = dbConnectFactory.get(selectedSourceDb);
        List<KeyValueEntity> fieldList = dbConnect.loadTableFields(selectedSourceDb, tableName);
        fieldsDc.setItems(fieldList);
        fieldCount.setText("Tổng số cột: " + fieldList.size());
    }

    @Subscribe("dbSourseComboBox")
    public void onDbSourseComboBoxComponentValueChange(final AbstractField.ComponentValueChangeEvent<EntityComboBox<SourceDb>, SourceDb> event) {
        SourceDb selectedSourceDb = event.getValue();
        if (selectedSourceDb == null) {
            tablesDc.setItems(List.of());
            fieldsDc.setItems(List.of());
            tableCount.setText("Tổng số bảng: 0");
            fieldCount.setText("Tổng số cột: 0");
            connectionStatus.setText("Trạng thái: Chưa kết nối");
            return;
        }

        DbConnect dbConnect = dbConnectFactory.get(selectedSourceDb);
        List<String> tableList = dbConnect.loadTableList(selectedSourceDb);
        fieldsDc.getMutableItems().clear();
        List<KeyValueEntity> tableEntities = new ArrayList<>();
        for (String tableName : tableList) {
            KeyValueEntity newTable = dataManager.create(KeyValueEntity.class);
            newTable.setValue("name", tableName);
            newTable.setValue("description", "Table: " + tableName);
            tableEntities.add(newTable);
        }
        tablesDc.setItems(tableEntities);
        tableCount.setText("Tổng số bảng: " + tableEntities.size());
        fieldsDc.setItems(List.of());
        fieldCount.setText("Tổng số cột: 0");
        connectionStatus.setText("Trạng thái: Đã kết nối");
    }

}