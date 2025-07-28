package com.aidb.aidb_backend.model.postgres;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "table_metadata")
@Data
public class TableMetadata {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    private String name;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL)
    private List<ColumnMetadata> columns;
}

