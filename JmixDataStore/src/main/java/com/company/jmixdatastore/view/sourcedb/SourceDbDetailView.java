package com.company.jmixdatastore.view.sourcedb;

import com.company.jmixdatastore.entity.SourceDb;
import com.company.jmixdatastore.service.dbcon.DbConnect;
import com.company.jmixdatastore.service.dbcon.DbConnectFactory;
import com.company.jmixdatastore.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "source-dbs/:id", layout = MainView.class)
@ViewController(id = "SourceDb.detail")
@ViewDescriptor(path = "source-db-detail-view.xml")
@EditedEntityContainer("sourceDbDc")
public class SourceDbDetailView extends StandardDetailView<SourceDb> {

    @Autowired
    private DbConnectFactory dbConnectFactory;

    @Autowired
    private Notifications notifications;


    @Subscribe(id = "detailActions", subject = "clickListener")
    public void onDetailActionsClick(final ClickEvent<HorizontalLayout> event) {
        SourceDb currentSourceDb = getEditedEntity();
        DbConnect dbConnect = dbConnectFactory.get(currentSourceDb);
        boolean isConnected = dbConnect.connect(currentSourceDb);

        if (isConnected) {
            notifications.create("Kết nối thành công!")
                    .withType(Notifications.Type.SUCCESS)
                    .withPosition(Notification.Position.TOP_END)
                    .show();
        } else {
            notifications.create("Kết nối thất bại!")
                    .withType(Notifications.Type.ERROR)
                    .withPosition(Notification.Position.TOP_END)
                    .show();
        }
    }


}