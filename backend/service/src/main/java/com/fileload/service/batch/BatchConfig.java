package com.fileload.service.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    @Bean
    public Step processFileStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                FileProcessingTasklet tasklet) {
        return new StepBuilder("processFileStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job fileProcessingJob(JobRepository jobRepository, Step processFileStep) {
        return new JobBuilder("fileProcessingJob", jobRepository)
                .start(processFileStep)
                .build();
    }
}

