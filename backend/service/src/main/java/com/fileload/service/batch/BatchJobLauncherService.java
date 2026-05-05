package com.fileload.service.batch;

import com.fileload.dao.repository.FileLoadRepository;
import com.fileload.model.entity.FileLoad;
import com.fileload.model.entity.FileStatus;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BatchJobLauncherService {

    private static final long PENDING_VISIBLE_DELAY_MS = 10000L;

    private final JobLauncher jobLauncher;
    private final Job fileProcessingJob;
    private final FileLoadRepository fileLoadRepository;
    private final TransactionTemplate noTxTemplate;

    public BatchJobLauncherService(JobLauncher jobLauncher,
                                   Job fileProcessingJob,
                                   FileLoadRepository fileLoadRepository,
                                   PlatformTransactionManager transactionManager) {
        this.jobLauncher = jobLauncher;
        this.fileProcessingJob = fileProcessingJob;
        this.fileLoadRepository = fileLoadRepository;
        this.noTxTemplate = new TransactionTemplate(transactionManager);
        this.noTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    }

    @Async
    public void launch(Long fileLoadId) {
        try {
            // Keep PENDING visible briefly before moving to PROCESSING.
            Thread.sleep(PENDING_VISIBLE_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        launchInternal(fileLoadId, 0);
    }

    private void launchInternal(Long fileLoadId, int attempt) {
        try {
            noTxTemplate.executeWithoutResult(status -> {
                try {
                    JobParameters params = new JobParametersBuilder()
                            .addLong("fileLoadId", fileLoadId)
                            .addLong("uniqueRunId", System.nanoTime())
                            .toJobParameters();
                    jobLauncher.run(fileProcessingJob, params);
                } catch (Exception ex) {
                    throw new IllegalStateException(ex.getMessage(), ex);
                }
            });
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "Batch launch failed" : ex.getMessage();

            // Retry a few times for transient transaction-context cleanup timing.
            if (message.contains("Existing transaction detected in JobRepository") && attempt < 3) {
                try {
                    Thread.sleep(300L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                launchInternal(fileLoadId, attempt + 1);
                return;
            }

            FileLoad entity = fileLoadRepository.findById(fileLoadId)
                    .orElseThrow(() -> new EntityNotFoundException("File load not found: " + fileLoadId));
            entity.setStatus(FileStatus.FAILED);
            entity.setErrors(message);
            fileLoadRepository.save(entity);
        }
    }
}



