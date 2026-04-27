package org.example.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 经召回和重排后的最终 chunk
 */
@Getter
@Setter
public class RetrievedChunk {

    private String id;
    private String content;
    private float vectorDistance;
    private double semanticScore;
    private double keywordScore;
    private double titleScore;
    private double structureScore;
    private double sourceDiversityPenalty;
    private double finalScore;
    private Map<String, Object> metadata;
    private String source;
    private String fileName;
    private String extension;
    private String title;
    private Integer chunkIndex;
    private Integer totalChunks;
}
