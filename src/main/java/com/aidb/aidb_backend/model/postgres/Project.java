package com.aidb.aidb_backend.model.postgres;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "projects")
@Data
public class Project {

    @Id
    private UUID id;

    private String name;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private List<TableMetadata> tables;
}
