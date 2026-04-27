package org.example.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 向量召回后的候选 chunk
 */
@Getter
@Setter
public class RecallCandidate {

    private String id;
    private String content;
    private float vectorDistance;
    private Map<String, Object> metadata;
    private String source;
    private String fileName;
    private String extension;
    private String title;
    private Integer chunkIndex;
    private Integer totalChunks;
}
