package com.helpdesk.scheduler;

import com.helpdesk.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {
    private final RefreshTokenService refreshTokens;

    /** Removes expired refresh tokens every night at 03:00. */
    @Scheduled(cron = "0 0 3 * * *")
    public void purge() { refreshTokens.cleanup(); }
}
