package com.aidb.aidb_backend.model.postgres;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "column_metadata")
@Data
public class ColumnMetadata {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    @JsonBackReference
    private TableMetadata tableMetadata;

    private String name;

    private String type; // TEXT, NUMBER, DATE
}

