package org.example.service;

import org.example.config.RerankProperties;
import org.example.dto.ChunkScoreBreakdown;
import org.example.dto.RecallCandidate;
import org.example.dto.RecallContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 规则型 chunk 打分器
 */
@Service
public class RuleBasedChunkScoreCalculator implements ChunkScoreCalculator {

    private final RerankProperties rerankProperties;
    private final KeywordScoringService keywordScoringService;

    @Autowired
    public RuleBasedChunkScoreCalculator(RerankProperties rerankProperties,
                                         KeywordScoringService keywordScoringService) {
        this.rerankProperties = rerankProperties;
        this.keywordScoringService = keywordScoringService;
    }

    @Override
    public ChunkScoreBreakdown score(RecallCandidate candidate, RecallContext context) {
        double semanticScore = calculateSemanticScore(candidate, context);
        double keywordScore = keywordScoringService.calculateKeywordScore(context.getKeywords(), candidate);
        double titleScore = keywordScoringService.calculateTitleScore(context.getKeywords(), candidate);
        double structureScore = calculateStructureScore(candidate);

        double finalScore =
                rerankProperties.getSemanticWeight() * semanticScore +
                rerankProperties.getKeywordWeight() * keywordScore +
                rerankProperties.getTitleWeight() * titleScore +
                rerankProperties.getStructureWeight() * structureScore;

        ChunkScoreBreakdown breakdown = new ChunkScoreBreakdown();
        breakdown.setSemanticScore(semanticScore);
        breakdown.setKeywordScore(keywordScore);
        breakdown.setTitleScore(titleScore);
        breakdown.setStructureScore(structureScore);
        breakdown.setSourceDiversityPenalty(0.0);
        breakdown.setFinalScore(finalScore);
        return breakdown;
    }

    private double calculateSemanticScore(RecallCandidate candidate, RecallContext context) {
        float min = context.getMinVectorDistance();
        float max = context.getMaxVectorDistance();
        float distance = candidate.getVectorDistance();

        if (Float.compare(max, min) == 0) {
            return 1.0;
        }

        double normalizedDistance = (distance - min) / (max - min + 1e-6);
        return clamp(1.0 - normalizedDistance);
    }

    private double calculateStructureScore(RecallCandidate candidate) {
        String content = candidate.getContent();
        int length = content == null ? 0 : content.length();

        double score;
        if (length >= 80 && length <= 500) {
            score = 1.0;
        } else if ((length >= 40 && length < 80) || (length > 500 && length <= 1000)) {
            score = 0.7;
        } else if ((length >= 20 && length < 40) || (length > 1000 && length <= 1500)) {
            score = 0.4;
        } else {
            score = 0.2;
        }

        if (candidate.getTitle() != null && !candidate.getTitle().isBlank()) {
            score += 0.1;
        }

        if (candidate.getChunkIndex() != null && candidate.getChunkIndex() == 0) {
            score += 0.05;
        }

        return clamp(score);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
