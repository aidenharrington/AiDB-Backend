package com.aidb.aidb_backend.model.postgres;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "column_metadata")
@Data
public class ColumnMetadata {
    @Id
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    private String name;

    private String type; // TEXT, NUMBER, DATE
}

