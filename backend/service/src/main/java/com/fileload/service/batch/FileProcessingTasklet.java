package com.fileload.service.batch;

import com.fileload.dao.repository.FileLoadRepository;
import com.fileload.model.entity.FileLoad;
import com.fileload.model.entity.FileStatus;
import com.fileload.service.util.RecordCountUtil;
import java.nio.file.Path;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class FileProcessingTasklet implements Tasklet {

    private static final long PROCESSING_VISIBLE_DELAY_MS = 10000L;

    private final FileLoadRepository fileLoadRepository;
    private final RecordCountUtil recordCountUtil;
    private final TransactionTemplate requiresNewTx;

    public FileProcessingTasklet(FileLoadRepository fileLoadRepository,
                                 RecordCountUtil recordCountUtil,
                                 PlatformTransactionManager transactionManager) {
        this.fileLoadRepository = fileLoadRepository;
        this.recordCountUtil = recordCountUtil;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        Long fileLoadId = Long.valueOf(
                chunkContext.getStepContext().getJobParameters().get("fileLoadId").toString()
        );

        FileLoad fileLoad = fileLoadRepository.findById(fileLoadId)
                .orElseThrow(() -> new IllegalArgumentException("File load not found: " + fileLoadId));

        updateStatus(fileLoadId, FileStatus.PROCESSING, null, 0L);

        // Keep PROCESSING visible briefly so UI polling can reflect lifecycle clearly.
        Thread.sleep(PROCESSING_VISIBLE_DELAY_MS);

        try {
            RecordCountUtil.ProcessingResult result = recordCountUtil.analyzeFile(Path.of(fileLoad.getStoragePath()));
            if (result.hasErrors()) {
                updateStatus(fileLoadId, FileStatus.FAILED, result.errorMessage(), result.recordCount());
            } else {
                updateStatus(fileLoadId, FileStatus.SUCCESS, null, result.recordCount());
            }
        } catch (Exception ex) {
            updateStatus(fileLoadId, FileStatus.FAILED, ex.getMessage(), 0L);
        }
        return RepeatStatus.FINISHED;
    }

    private void updateStatus(Long fileLoadId, FileStatus status, String errors, Long recordCount) {
        requiresNewTx.executeWithoutResult(ignored -> {
            FileLoad entity = fileLoadRepository.findById(fileLoadId)
                    .orElseThrow(() -> new IllegalArgumentException("File load not found: " + fileLoadId));
            entity.setStatus(status);
            entity.setErrors(errors);
            if (recordCount != null) {
                entity.setRecordCount(recordCount);
            }
            fileLoadRepository.saveAndFlush(entity);
        });
    }
}

