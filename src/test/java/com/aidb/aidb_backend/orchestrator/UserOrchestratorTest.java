package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.model.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserOrchestratorTest {

    @Mock
    private LimitsOrchestrator limitsOrchestrator;

    @InjectMocks
    private UserOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void setupNewUser_callsLimitsSetup_andReturnsUserDTO() throws Exception {
        UserDTO dto = orchestrator.setupNewUser("user-1");
        assertEquals("user-1", dto.getUserId());
        verify(limitsOrchestrator, times(1)).setupLimitsForNewUser("user-1");
    }

    @Test
    void setupNewUser_propagatesException() throws Exception {
        doThrow(new RuntimeException("fail")).when(limitsOrchestrator).setupLimitsForNewUser("user-1");
        assertThrows(RuntimeException.class, () -> orchestrator.setupNewUser("user-1"));
    }
}
