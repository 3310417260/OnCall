package org.example.service;

import org.example.dto.IndexTaskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DocumentIndexTaskService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIndexTaskService.class);

    private final VectorIndexService vectorIndexService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Map<String, IndexTaskRecord> tasks = new ConcurrentHashMap<>();

    public DocumentIndexTaskService(VectorIndexService vectorIndexService) {
        this.vectorIndexService = vectorIndexService;
    }

    public IndexTaskResponse submitTask(String fileName, String filePath) {
        String taskId = UUID.randomUUID().toString();
        IndexTaskRecord record = new IndexTaskRecord(taskId, fileName, filePath);
        tasks.put(taskId, record);
        executorService.submit(() -> runTask(record));
        return record.toResponse();
    }

    public Optional<IndexTaskResponse> getTask(String taskId) {
        IndexTaskRecord record = tasks.get(taskId);
        if (record == null) {
            return Optional.empty();
        }
        return Optional.of(record.toResponse());
    }

    private void runTask(IndexTaskRecord record) {
        record.markRunning();
        logger.info("后台索引任务开始 - taskId: {}, file: {}", record.taskId, record.filePath);

        try {
            vectorIndexService.indexSingleFile(record.filePath);
            record.markSucceeded();
            logger.info("后台索引任务完成 - taskId: {}, file: {}", record.taskId, record.filePath);
        } catch (Exception e) {
            record.markFailed(e.getMessage());
            logger.error("后台索引任务失败 - taskId: {}, file: {}", record.taskId, record.filePath, e);
        }
    }

    private static class IndexTaskRecord {
        private final String taskId;
        private final String fileName;
        private final String filePath;
        private final long createdAt;
        private volatile String status;
        private volatile String message;
        private volatile String errorMessage;
        private volatile Long startedAt;
        private volatile Long finishedAt;

        private IndexTaskRecord(String taskId, String fileName, String filePath) {
            this.taskId = taskId;
            this.fileName = fileName;
            this.filePath = filePath;
            this.createdAt = System.currentTimeMillis();
            this.status = "PENDING";
            this.message = "任务已创建，等待后台索引";
        }

        private void markRunning() {
            this.status = "RUNNING";
            this.startedAt = System.currentTimeMillis();
            this.message = "后台索引进行中";
        }

        private void markSucceeded() {
            this.status = "SUCCEEDED";
            this.finishedAt = System.currentTimeMillis();
            this.message = "后台索引已完成";
        }

        private void markFailed(String errorMessage) {
            this.status = "FAILED";
            this.finishedAt = System.currentTimeMillis();
            this.errorMessage = errorMessage;
            this.message = "后台索引失败";
        }

        private IndexTaskResponse toResponse() {
            IndexTaskResponse response = new IndexTaskResponse();
            response.setTaskId(taskId);
            response.setFileName(fileName);
            response.setFilePath(filePath);
            response.setStatus(status);
            response.setMessage(message);
            response.setErrorMessage(errorMessage);
            response.setCreatedAt(createdAt);
            response.setStartedAt(startedAt);
            response.setFinishedAt(finishedAt);
            return response;
        }
    }
}
