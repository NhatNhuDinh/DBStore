package com.company.jmixdatastore.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "TABLE_DB", indexes = {
        @Index(name = "IDX_TABLE_DB_SOURCE_DB", columnList = "SOURCE_DB_ID"),
        @Index(name = "IDX_TABLE_DB_TABLE_DETAILS", columnList = "")
})
@Entity
public class TableDb {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @InstanceName
    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "TABLE_STATUS")
    private String status;

    @JoinColumn(name = "SOURCE_DB_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private SourceDb sourceDb;

    @OneToMany(mappedBy = "tableDb")
    private List<TableDetail> tableDetails;

    public void setTableDetails(List<TableDetail> tableDetails) {
        this.tableDetails = tableDetails;
    }

    public List<TableDetail> getTableDetails() {
        return tableDetails;
    }

    public SourceDb getSourceDb() {
        return sourceDb;
    }

    public void setSourceDb(SourceDb sourceDb) {
        this.sourceDb = sourceDb;
    }

    public Status getStatus() {
        return status == null ? null : Status.fromId(status);
    }

    public void setStatus(Status tableStatus) {
        this.status = tableStatus == null ? null : tableStatus.getId();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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