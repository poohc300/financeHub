package com.example.financeHub.scheduler.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SchedulerExecutionLogDTO {

    private Long id;
    private String jobName;
    private LocalDateTime executionTime;
    private String status;
    private int recordsProcessed;
    private int recordsInserted;
    private int recordsSkipped;
    private String errorMessage;
    private Long executionDurationMs;
    private LocalDateTime createdAt;
}
