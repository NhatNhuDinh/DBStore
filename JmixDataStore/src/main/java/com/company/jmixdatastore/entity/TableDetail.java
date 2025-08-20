package com.company.jmixdatastore.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.UUID;

@JmixEntity
@Table(name = "TABLE_DETAIL", indexes = {
        @Index(name = "IDX_TABLE_DETAIL_TABLE_DB", columnList = "TABLE_DB_ID")
})
@Entity
public class TableDetail {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @InstanceName
    @Column(name = "NAME")
    private String name;

    @Column(name = "DATA_TYPE")
    private String dataType;

    @Column(name = "SIZE_")
    private Long size;

    @Column(name = "DEFAULT_VALUE")
    private String defaultValue;

    @Column(name = "IS_NULL")
    private Boolean isNull;

    @Column(name = "DESCRIPTION")
    @Lob
    private String description;

    @JoinColumn(name = "TABLE_DB_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private TableDb tableDb;

    @Column(name = "STATUS")
    private String status;

    public Status getStatus() {
        return status == null ? null : Status.fromId(status);
    }

    public void setStatus(Status status) {
        this.status = status == null ? null : status.getId();
    }

    public TableDb getTableDb() {
        return tableDb;
    }

    public void setTableDb(TableDb tableDb) {
        this.tableDb = tableDb;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsNull() {
        return isNull;
    }

    public void setIsNull(Boolean isNull) {
        this.isNull = isNull;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getSize() {
        return size;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

}