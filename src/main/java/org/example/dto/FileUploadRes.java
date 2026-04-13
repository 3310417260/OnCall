package org.example.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FileUploadRes {

    private String fileName;
    private String filePath;
    private Long fileSize;
    private String taskId;
    private String indexStatus;
    private String message;

    public FileUploadRes() {
    }

    public FileUploadRes(String fileName, String filePath, Long fileSize) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }

    public FileUploadRes(String fileName, String filePath, Long fileSize, String taskId, String indexStatus, String message) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.taskId = taskId;
        this.indexStatus = indexStatus;
        this.message = message;
    }
}
