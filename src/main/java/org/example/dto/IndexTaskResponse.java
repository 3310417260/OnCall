package org.example.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndexTaskResponse {

    private String taskId;
    private String fileName;
    private String filePath;
    private String status;
    private String message;
    private String errorMessage;
    private long createdAt;
    private Long startedAt;
    private Long finishedAt;
}
