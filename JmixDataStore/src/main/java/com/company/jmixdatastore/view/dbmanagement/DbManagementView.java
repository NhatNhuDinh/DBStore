package com.company.jmixdatastore.view.dbmanagement;

import com.company.jmixdatastore.entity.SourceDb;
import com.company.jmixdatastore.entity.Status;
import com.company.jmixdatastore.entity.TableDb;
import com.company.jmixdatastore.entity.TableDetail;
import com.company.jmixdatastore.service.dbcon.DbConnect;
import com.company.jmixdatastore.service.dbcon.DbConnectFactory;
import com.company.jmixdatastore.view.main.MainView;
import com.company.jmixdatastore.view.sourcedb.SourceDbDetailView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.event.EntitySavingEvent;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.checkbox.Switch;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.context.event.EventListener;

import java.awt.*;
import java.util.List;

@Route(value = "db-management-view", layout = MainView.class)
@ViewController(id = "DbManagementView")
@ViewDescriptor(path = "db-management-view.xml")
public class DbManagementView extends StandardView {

    protected final DataManager dataManager;
    private final DialogWindows dialogWindows;
    private final DbConnectFactory dbConnectFactory;
    private final Notifications notifications;

    @ViewComponent
    private CollectionLoader<SourceDb> sourceDbsDl;

    @ViewComponent
    private EntityComboBox<SourceDb> dbSourseComboBox;

    @ViewComponent
    private CollectionContainer<TableDb> tableDbsDc;

    @ViewComponent
    private CollectionContainer<TableDetail> tableDetailsDc;

    @ViewComponent
    private Icon statusIcon;

    private boolean isOnline = false;

    public DbManagementView(DataManager dataManager,
                             DialogWindows dialogWindows,
                             DbConnectFactory dbConnectFactory,
                             Notifications notifications) {
        this.dataManager = dataManager;
        this.dialogWindows = dialogWindows;
        this.dbConnectFactory = dbConnectFactory;
        this.notifications = notifications;
    }

    @Subscribe(id = "newButton", subject = "clickListener")
    public void onNewButtonClick(final ClickEvent<JmixButton> event) {
        dialogWindows.detail(this, SourceDb.class)
                .newEntity()
                .withAfterCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                        sourceDbsDl.load();
                        SourceDbDetailView v = (SourceDbDetailView) closeEvent.getView();
                        dbSourseComboBox.setValue(v.getEditedEntity());
                    }
                })
                .build()
                .open();
    }

    @Subscribe(id = "connectButton", subject = "clickListener")
    public void onConnectButtonClick(final ClickEvent<JmixButton> event) {
        SourceDb selectedSourceDb = dbSourseComboBox.getValue();
        if (selectedSourceDb == null) {
            notifications.create("Chọn cấu hình DB trước")
                    .withType(Notifications.Type.WARNING)
                    .withPosition(Notification.Position.TOP_END)
                    .show();
            return;
        }

        DbConnect dbConnect = dbConnectFactory.get(selectedSourceDb);

        boolean connected = dbConnect.connect(selectedSourceDb);
        isOnline = connected;
        updateStatusIcon(connected);

        tableDbsDc.getMutableItems().clear();
        tableDetailsDc.getMutableItems().clear();

        if (connected) {
            notifications.create("Kết nối thành công!").withType(Notifications.Type.SUCCESS).withPosition(Notification.Position.TOP_END).show();
            List<TableDb> tableList;
            tableList = dbConnect.syncTableList(selectedSourceDb);
            tableDbsDc.setItems(tableList);

            if (!tableList.isEmpty()) {
                tableDbsDc.setItem(tableList.getFirst());
            }

        } else {
            notifications.create("Không kết nối được DB! Hiển thị trạng thái offline.")
                    .withType(Notifications.Type.ERROR)
                    .withPosition(Notification.Position.TOP_END)
                    .show();
            List<TableDb> localTables = dbConnect.loadTableList(selectedSourceDb);
            for (TableDb table : localTables) {
                table.setStatus(Status.NGOẠI_TUYẾN);
            }
            tableDbsDc.setItems(localTables);
            if (!localTables.isEmpty()) {
                tableDbsDc.setItem(localTables.getFirst());
            }
        }
    }

    @Subscribe(id = "tableDbsDc", target = Target.DATA_CONTAINER)
    public void onTableDbsDcItemChange(final CollectionContainer.ItemChangeEvent<TableDb> event) {
        TableDb selectedTable = event.getItem();
        tableDetailsDc.getMutableItems().clear();

        if (selectedTable != null) {
            SourceDb selectedSourceDb = dbSourseComboBox.getValue();
            List<TableDetail> fieldList = dbConnectFactory.get(selectedSourceDb)
                    .loadTableFields(selectedTable);
            if (!isOnline) {
                for (TableDetail field : fieldList) {
                    field.setStatus(Status.NGOẠI_TUYẾN);
                }
            }
            tableDetailsDc.setItems(fieldList);
        }
    }

    public void updateStatusIcon(boolean isOnline) {
        if (isOnline) {
            statusIcon.setColor("green");
        } else {
            statusIcon.setColor("gray");
        }
    }

}
