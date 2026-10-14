package com.devops.paylite.config;

import com.devops.paylite.service.PaynetCatalogSyncService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SyncRecipientCatalog {

    private static final Logger LOG = LoggerFactory.getLogger(SyncRecipientCatalog.class);

    private final PaynetCatalogSyncService paynetCatalogSyncService;

    @Scheduled(cron = "${paynet.catalog-sync.cron:0 0 2 * * *}", zone = "${paynet.catalog-sync.zone:Asia/Tashkent}")
    public void syncRecipientCatalog() {
        LOG.info("Starting scheduled Paynet catalog synchronization");
        try {
            paynetCatalogSyncService.sync();
            LOG.info("Scheduled Paynet catalog synchronization completed");
        } catch (RuntimeException exception) {
            LOG.error("Scheduled Paynet catalog synchronization failed", exception);
        }
    }
}
