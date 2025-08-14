package com.company.jmixdatastore.view.dbmanagement;

import io.jmix.core.entity.annotation.JmixId;
import io.jmix.core.metamodel.annotation.JmixEntity;

@JmixEntity
public class ColumnInfo {

    @JmixId
    private String name;

    public ColumnInfo() {
    }

    public ColumnInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
