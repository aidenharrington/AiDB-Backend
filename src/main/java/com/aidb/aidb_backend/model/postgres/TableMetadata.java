package com.aidb.aidb_backend.model.postgres;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;


@Entity
@Table(name = "table_metadata")
@Data
public class TableMetadata {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonBackReference
    private Project project;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "table_name")
    private String tableName;

    @OneToMany(mappedBy = "tableMetadata", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ColumnMetadata> columns;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;


    // TableMetadata stores tableName but not Table object
    // Tables are fetches using SQL such as: ProjectService.convertTableToTableDtoWithData
}

