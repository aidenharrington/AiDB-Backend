package com.aidb.aidb_backend.scheduled;

import com.aidb.aidb_backend.config.api.GlobalExceptionHandler;
import com.aidb.aidb_backend.service.database.firestore.UserLimitsService;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LimitUsageResetScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LimitUsageResetScheduler.class);

    private final UserLimitsService userLimitsService;

    /**
     * Runs at midnight UTC on the first day of each month.
     */
    @Scheduled(cron = "0 0 0 1 * *", zone = "UTC")
    public void resetMonthlyLimits() {
        try {
            Timestamp now = Timestamp.now();
            userLimitsService.resetUsageForAllUsers(now);
            logger.info("Monthly usage limits reset successfully at {}", now);
        } catch (Exception e) {
            logger.error("Error resetting monthly usage limits", e);
        }
    }
}
