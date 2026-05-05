package com.fileload.service.batch;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FileLoadBatchEventListener {

    private final BatchJobLauncherService batchJobLauncherService;

    public FileLoadBatchEventListener(BatchJobLauncherService batchJobLauncherService) {
        this.batchJobLauncherService = batchJobLauncherService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onFileLoadQueued(FileLoadQueuedEvent event) {
        batchJobLauncherService.launch(event.fileLoadId());
    }
}

