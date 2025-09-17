package com.aidb.aidb_backend.service.util.sql;

import com.aidb.aidb_backend.model.dto.ProjectDTO;
import com.aidb.aidb_backend.model.dto.TableDTO;
import com.aidb.aidb_backend.model.postgres.ColumnMetadata;
import com.aidb.aidb_backend.model.postgres.Project;
import com.aidb.aidb_backend.model.postgres.TableMetadata;
import com.aidb.aidb_backend.repository.DynamicTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProjectConversionService {

    @Autowired
    DynamicTableRepository dynamicTableRepository;

    public ProjectDTO convertProjectToDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(String.valueOf(project.getId()));
        dto.setName(project.getName());
        dto.setUserId(project.getUserId());

        if (project.getTables() != null) {
            List<TableDTO> tableDTOS = project.getTables().stream()
                    .map(this::convertTableToTableDtoWithData)
                    .collect(Collectors.toList());
            dto.setTables(tableDTOS);
        }

        return dto;
    }

    private TableDTO convertTableToTableDtoWithData(TableMetadata table) {
        TableDTO dto = new TableDTO();
        dto.setId(String.valueOf(table.getId()));
        dto.setFileName(table.getFileName());
        dto.setDisplayName(table.getDisplayName());
        dto.setTableName(table.getTableName());

        if (table.getColumns() != null) {
            List<TableDTO.ColumnDTO> columnDTOS = table.getColumns().stream()
                    .map(this::convertColumnToTableColumnDTO)
                    .collect(Collectors.toList());
            dto.setColumns(columnDTOS);
        }

        // Fetch actual table data via repository
        List<List<Object>> tableRows = dynamicTableRepository.fetchAllRows(table.getTableName()).stream()
                .map(row -> row.entrySet().stream()
                        .filter(e -> !e.getKey().equalsIgnoreCase("id")) // skip the id column
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList())
                )
                .collect(Collectors.toList());

        dto.setRows(tableRows);
        return dto;
    }

    private TableDTO.ColumnDTO convertColumnToTableColumnDTO(ColumnMetadata column) {
        TableDTO.ColumnDTO dto = new TableDTO.ColumnDTO();
        dto.setName(column.getName());
        dto.setType(TableDTO.ColumnTypeDTO.valueOf(column.getType()));
        return dto;
    }
}

