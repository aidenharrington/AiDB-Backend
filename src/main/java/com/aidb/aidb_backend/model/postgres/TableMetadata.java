package com.aidb.aidb_backend.model.postgres;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;


@Entity
@Table(name = "table_metadata")
@Data
public class TableMetadata {
    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "table_name")
    private String tableName;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL)
    private List<ColumnMetadata> columns;
}

