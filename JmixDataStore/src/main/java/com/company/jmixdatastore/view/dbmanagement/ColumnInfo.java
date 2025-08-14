package com.company.jmixdatastore.view.dbmanagement;

import io.jmix.core.entity.annotation.JmixId;
import io.jmix.core.metamodel.annotation.JmixEntity;

@JmixEntity
public class ColumnInfo {
    @JmixId
    private String name;
    private String type;
    private String nullable;

    public ColumnInfo() {}

    public ColumnInfo(String name, String type, String nullable) {
        this.name = name;
        this.type = type;
        this.nullable = nullable;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getNullable() { return nullable; }
    public void setNullable(String nullable) { this.nullable = nullable; }
}
