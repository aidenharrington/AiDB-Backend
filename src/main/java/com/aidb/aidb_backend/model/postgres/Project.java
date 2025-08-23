package com.aidb.aidb_backend.model.postgres;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.List;


@Entity
@Table(name = "projects")
@Data
public class Project {

    @Id
    private Long id;

    private String name;

    @Column(name = "user_id", nullable = false)
    private String userId;


    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<TableMetadata> tables;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

}
