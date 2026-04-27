package org.example.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 单个 chunk 的打分明细
 */
@Getter
@Setter
public class ChunkScoreBreakdown {

    private double semanticScore;
    private double keywordScore;
    private double titleScore;
    private double structureScore;
    private double sourceDiversityPenalty;
    private double finalScore;
}
