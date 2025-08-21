package com.aidb.aidb_backend.orchestrator;

import com.aidb.aidb_backend.exception.UserLimitExceededException;
import com.aidb.aidb_backend.exception.UserNotFoundException;
import com.aidb.aidb_backend.model.api.TierInfo;
import com.aidb.aidb_backend.model.firestore.Tier;
import com.aidb.aidb_backend.model.firestore.UserLimitsUsage;
import com.aidb.aidb_backend.model.firestore.util.LimitedOperation;
import com.aidb.aidb_backend.model.firestore.util.TierId;
import com.aidb.aidb_backend.service.database.firestore.TierService;
import com.aidb.aidb_backend.service.database.firestore.UserLimitsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LimitsOrchestratorTest {

    @Mock
    private TierService tierService;

    @Mock
    private UserLimitsService userLimitsService;

    @InjectMocks
    private LimitsOrchestrator limitsOrchestrator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getUserTierInfo_success_buildsTierInfoFromUsageAndTier() throws Exception {
        String userId = "user-123";

        UserLimitsUsage usage = new UserLimitsUsage();
        usage.setId(userId);
        usage.setTierId(TierId.FREE);
        usage.setQueryLimitUsage(3L);
        usage.setTranslationLimitUsage(4L);
        usage.setDataRowLimitUsage(100L);
        usage.setProjectLimitUsage(1L);

        Tier tier = new Tier();
        tier.setName("FREE");
        tier.setQueryLimit(10L);
        tier.setTranslationLimit(5L);
        tier.setDataRowLimit(1000L);
        tier.setProjectLimit(2L);
        tier.setMaxFileSize(123L);

        when(userLimitsService.getUserLimitsById(userId)).thenReturn(usage);
        when(tierService.getTier(TierId.FREE)).thenReturn(tier);

        TierInfo info = limitsOrchestrator.getUserTierInfo(userId);

        assertNotNull(info);
        assertEquals("FREE", info.getName());
        assertEquals(userId, info.getUserId());
        assertEquals(10L, info.getQueryLimit());
        assertEquals(3L, info.getQueryLimitUsage());
        assertEquals(5L, info.getTranslationLimit());
        assertEquals(4L, info.getTranslationLimitUsage());
        assertEquals(1000L, info.getDataRowLimit());
        assertEquals(100L, info.getDataRowLimitUsage());
        assertEquals(2L, info.getProjectLimit());
        assertEquals(1L, info.getProjectLimitUsage());
        assertEquals(123L, info.getMaxFileSize());
    }

    @Test
    void getUserTierInfo_userNotFound_propagatesException() throws Exception {
        String missingUserId = "missing";
        when(userLimitsService.getUserLimitsById(missingUserId))
                .thenThrow(new UserNotFoundException("not found"));

        assertThrows(UserNotFoundException.class, () ->
                limitsOrchestrator.getUserTierInfo(missingUserId)
        );
    }

    @Test
    void verifyLimit_allowsWhenWithinLimit() {
        TierInfo info = TierInfo.builder()
                .name("FREE")
                .userId("user-1")
                .queryLimit(10L)
                .queryLimitUsage(9L)
                .translationLimit(0L)
                .translationLimitUsage(0L)
                .dataRowLimit(0L)
                .dataRowLimitUsage(0L)
                .projectLimit(0L)
                .projectLimitUsage(0L)
                .maxFileSize(0L)
                .build();

        assertDoesNotThrow(() -> limitsOrchestrator.verifyLimit(info, LimitedOperation.QUERY, 1));
    }

    @Test
    void verifyLimit_allowsForUnlimitedTier() {
        TierInfo info = TierInfo.builder()
                .name("UNLIMITED")
                .userId("user-1")
                .queryLimit(-1L) // unlimited token
                .queryLimitUsage(10_000L)
                .translationLimit(0L)
                .translationLimitUsage(0L)
                .dataRowLimit(0L)
                .dataRowLimitUsage(0L)
                .projectLimit(0L)
                .projectLimitUsage(0L)
                .maxFileSize(0L)
                .build();

        assertDoesNotThrow(() -> limitsOrchestrator.verifyLimit(info, LimitedOperation.QUERY, Integer.MAX_VALUE));
    }

    @Test
    void verifyLimit_throwsWhenExceedsLimit() {
        TierInfo info = TierInfo.builder()
                .name("FREE")
                .userId("user-1")
                .queryLimit(10L)
                .queryLimitUsage(10L)
                .translationLimit(0L)
                .translationLimitUsage(0L)
                .dataRowLimit(0L)
                .dataRowLimitUsage(0L)
                .projectLimit(0L)
                .projectLimitUsage(0L)
                .maxFileSize(0L)
                .build();

        assertThrows(UserLimitExceededException.class, () ->
                limitsOrchestrator.verifyLimit(info, LimitedOperation.QUERY, 1)
        );
    }

    @Test
    void updateLimit_updatesUsageOnTierInfo_whenServiceReturnsValue() {
        TierInfo info = TierInfo.builder()
                .name("FREE")
                .userId("user-1")
                .queryLimit(10L)
                .queryLimitUsage(1L)
                .translationLimit(0L)
                .translationLimitUsage(0L)
                .dataRowLimit(0L)
                .dataRowLimitUsage(0L)
                .projectLimit(0L)
                .projectLimitUsage(0L)
                .maxFileSize(0L)
                .build();

        when(userLimitsService.updateLimitUsage("user-1", LimitedOperation.QUERY, 3))
                .thenReturn(4L);

        TierInfo returned = limitsOrchestrator.updateLimit(info, LimitedOperation.QUERY, 3);

        assertSame(info, returned);
        assertEquals(4L, returned.getQueryLimitUsage());
        verify(userLimitsService, times(1)).updateLimitUsage("user-1", LimitedOperation.QUERY, 3);
    }

    @Test
    void updateLimit_setsNullUsage_whenServiceReturnsNullGracefully() {
        TierInfo info = TierInfo.builder()
                .name("FREE")
                .userId("user-1")
                .queryLimit(10L)
                .queryLimitUsage(1L)
                .translationLimit(0L)
                .translationLimitUsage(0L)
                .dataRowLimit(0L)
                .dataRowLimitUsage(0L)
                .projectLimit(0L)
                .projectLimitUsage(0L)
                .maxFileSize(0L)
                .build();

        when(userLimitsService.updateLimitUsage("user-1", LimitedOperation.QUERY, 5))
                .thenReturn(null);

        TierInfo returned = limitsOrchestrator.updateLimit(info, LimitedOperation.QUERY, 5);

        assertNull(returned.getQueryLimitUsage());
        verify(userLimitsService, times(1)).updateLimitUsage("user-1", LimitedOperation.QUERY, 5);
    }

    @Test
    void setupLimitsForNewUser_callsServiceWithNewUserLimits() throws Exception {
        String newUserId = "new-user";

        when(userLimitsService.addUserLimits(any(UserLimitsUsage.class))).thenReturn(newUserId);

        limitsOrchestrator.setupLimitsForNewUser(newUserId);

        ArgumentCaptor<UserLimitsUsage> captor = ArgumentCaptor.forClass(UserLimitsUsage.class);
        verify(userLimitsService, times(1)).addUserLimits(captor.capture());

        UserLimitsUsage saved = captor.getValue();
        assertEquals(newUserId, saved.getId());
        assertEquals(TierId.FREE, saved.getTierId());
        assertEquals(0L, saved.getQueryLimitUsage());
        assertEquals(0L, saved.getTranslationLimitUsage());
        assertEquals(0L, saved.getDataRowLimitUsage());
        assertEquals(0L, saved.getProjectLimitUsage());
        assertNull(saved.getCreatedAt());
        assertNull(saved.getLastUpdated());
    }

    @Test
    void setupLimitsForNewUser_propagatesServiceException() throws Exception {
        String newUserId = "new-user";
        when(userLimitsService.addUserLimits(any(UserLimitsUsage.class)))
                .thenThrow(new ExecutionException(new Throwable("write failed")));

        assertThrows(ExecutionException.class, () ->
                limitsOrchestrator.setupLimitsForNewUser(newUserId)
        );
    }
}
