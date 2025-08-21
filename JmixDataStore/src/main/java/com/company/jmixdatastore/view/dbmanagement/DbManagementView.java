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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;

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
        isOnline = dbConnect.connect(selectedSourceDb);

        tableDbsDc.getMutableItems().clear();
        tableDetailsDc.getMutableItems().clear();

        if (isOnline) {
            notifications.create("Kết nối thành công! Bắt đầu đồng bộ dữ liệu")
                    .withType(Notifications.Type.SUCCESS)
                    .withPosition(Notification.Position.TOP_END)
                    .show();

            // Query lấy số lượng bảng của SourceDb này
            List<Long> counts = dataManager.loadValue(
                            "select count(e) from TableDb e where e.sourceDb = :sourceDb", Long.class
                    )
                    .parameter("sourceDb", selectedSourceDb)
                    .list();

            long count = (counts.isEmpty() || counts.get(0) == null) ? 0L : counts.get(0);

            // Nếu là lần đầu (count == 0), mới thực hiện lưu bảng và các field
            List<TableDb> tableList = dbConnect.loadTableList(selectedSourceDb);
            tableDbsDc.setItems(tableList);
            if (count == 0 && !tableList.isEmpty()) {
                for (TableDb tableDb : tableList) {
                    dataManager.save(tableDb);
                    // Lấy fields/columns của bảng
                    List<TableDetail> fieldList = dbConnect.loadTableFields(selectedSourceDb, tableDb.getName(), tableDb);
                    for (TableDetail field : fieldList) {
                        field.setTableDb(tableDb); // Gán quan hệ nếu cần
                        dataManager.save(field);
                    }
                }
            }
        } else {
            notifications.create("Kết nối thất bại! Dữ liệu được hiển thị ở trạng thái offline")
                    .withType(Notifications.Type.ERROR)
                    .withPosition(Notification.Position.TOP_END)
                    .show();
            List<TableDb> localTables = dataManager.load(TableDb.class)
                    .query("select e from TableDb e where e.sourceDb = :sourceDb")
                    .parameter("sourceDb", selectedSourceDb)
                    .list();
            for (TableDb table : localTables) {
                table.setStatus(Status.UNAVAILABLE);
            }
            tableDbsDc.setItems(localTables);
        }
    }


    @Subscribe(id = "tableDbsDc", target = Target.DATA_CONTAINER)
    public void onTableDbsDcItemChange(final CollectionContainer.ItemChangeEvent<TableDb> event) {
        TableDb selectedTable = event.getItem();
        tableDetailsDc.getMutableItems().clear();

        if (selectedTable != null) {
            SourceDb selectedSourceDb = dbSourseComboBox.getValue();

            notifications.create("Bảng " + selectedTable.getName() + " được chọn")
                    .withType(Notifications.Type.SUCCESS)
                    .withPosition(Notification.Position.TOP_END)
                    .show();

            List<TableDetail> fieldList;
            if (isOnline) {
                DbConnect dbConnect = dbConnectFactory.get(selectedSourceDb);
                fieldList = dbConnect.loadTableFields(selectedSourceDb, selectedTable.getName(), selectedTable);
            } else {
                fieldList = dataManager.load(TableDetail.class)
                        .query("select e from TableDetail e where e.tableDb.id = :tableDbId")
                        .parameter("tableDbId", selectedTable.getId())
                        .list();
                for (TableDetail field : fieldList) {
                    field.setStatus(Status.UNAVAILABLE);
                }
            }
            tableDetailsDc.setItems(fieldList);
        }
    }
}
