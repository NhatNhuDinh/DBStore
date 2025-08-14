package com.company.jmixdatastore.view.sourcedb;

import com.company.jmixdatastore.entity.SourceDb;
import com.company.jmixdatastore.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "source-dbs/:id", layout = MainView.class)
@ViewController(id = "SourceDb.detail")
@ViewDescriptor(path = "source-db-detail-view.xml")
@EditedEntityContainer("sourceDbDc")
public class SourceDbDetailView extends StandardDetailView<SourceDb> {
    @Subscribe(id = "detailActions", subject = "clickListener")
    public void onDetailActionsClick(final ClickEvent<HorizontalLayout> event) {

    }
}