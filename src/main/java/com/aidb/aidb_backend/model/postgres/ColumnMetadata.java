package com.aidb.aidb_backend.model.postgres;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "column_metadata")
@Data
public class ColumnMetadata {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "table_id", nullable = false)
    private TableMetadata table;

    private String name;

    private String type; // TEXT, NUMBER, DATE
}

