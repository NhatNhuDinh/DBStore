package com.company.jmixdatastore.view.sourcedb;

import com.company.jmixdatastore.entity.SourceDb;
import com.company.jmixdatastore.service.dbcon.DbConnect;
import com.company.jmixdatastore.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.HasValue;
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
    private DbConnect dbConnect;

    @Autowired
    private Notifications notifications;

    @Subscribe(id = "detailActions", subject = "clickListener")
    public void onDetailActionsClick(final ClickEvent<HorizontalLayout> event) {
        SourceDb currentSourceDb = getEditedEntity();
        boolean isConnected = dbConnect.connect(currentSourceDb);

        if (isConnected) {
            notifications.create("Kết nối thành công!")
                    .withType(Notifications.Type.SUCCESS)
                    .show();
        } else {
            notifications.create("Kết nối thất bại!")
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }


}