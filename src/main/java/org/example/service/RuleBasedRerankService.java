package org.example.service;

import org.example.config.RerankProperties;
import org.example.dto.ChunkScoreBreakdown;
import org.example.dto.RecallCandidate;
import org.example.dto.RecallContext;
import org.example.dto.RetrievedChunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于规则的重排实现
 */
@Service
public class RuleBasedRerankService implements RerankService {

    private final RerankProperties rerankProperties;
    private final KeywordScoringService keywordScoringService;
    private final ChunkScoreCalculator chunkScoreCalculator;

    @Autowired
    public RuleBasedRerankService(RerankProperties rerankProperties,
                                  KeywordScoringService keywordScoringService,
                                  ChunkScoreCalculator chunkScoreCalculator) {
        this.rerankProperties = rerankProperties;
        this.keywordScoringService = keywordScoringService;
        this.chunkScoreCalculator = chunkScoreCalculator;
    }

    @Override
    public List<RetrievedChunk> rerank(String query, List<RecallCandidate> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        RecallContext context = buildContext(query, candidates);
        List<RetrievedChunk> scoredChunks = new ArrayList<>();

        for (RecallCandidate candidate : candidates) {
            ChunkScoreBreakdown breakdown = chunkScoreCalculator.score(candidate, context);
            scoredChunks.add(toRetrievedChunk(candidate, breakdown));
        }

        applySourceDiversityPenalty(scoredChunks);

        return scoredChunks.stream()
                .filter(chunk -> chunk.getFinalScore() >= rerankProperties.getMinFinalScore())
                .sorted(Comparator.comparingDouble(RetrievedChunk::getFinalScore).reversed())
                .collect(Collectors.collectingAndThen(Collectors.toList(), this::limitBySource))
                .stream()
                .limit(topK)
                .toList();
    }

    private RecallContext buildContext(String query, List<RecallCandidate> candidates) {
        RecallContext context = new RecallContext();
        context.setKeywords(keywordScoringService.extractKeywords(query));

        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (RecallCandidate candidate : candidates) {
            float distance = candidate.getVectorDistance();
            min = Math.min(min, distance);
            max = Math.max(max, distance);
        }

        if (min == Float.MAX_VALUE) {
            min = 0f;
        }
        if (max == Float.MIN_VALUE) {
            max = 0f;
        }

        context.setMinVectorDistance(min);
        context.setMaxVectorDistance(max);
        return context;
    }

    private RetrievedChunk toRetrievedChunk(RecallCandidate candidate, ChunkScoreBreakdown breakdown) {
        RetrievedChunk chunk = new RetrievedChunk();
        chunk.setId(candidate.getId());
        chunk.setContent(candidate.getContent());
        chunk.setVectorDistance(candidate.getVectorDistance());
        chunk.setMetadata(candidate.getMetadata());
        chunk.setSource(candidate.getSource());
        chunk.setFileName(candidate.getFileName());
        chunk.setExtension(candidate.getExtension());
        chunk.setTitle(candidate.getTitle());
        chunk.setChunkIndex(candidate.getChunkIndex());
        chunk.setTotalChunks(candidate.getTotalChunks());
        chunk.setSemanticScore(breakdown.getSemanticScore());
        chunk.setKeywordScore(breakdown.getKeywordScore());
        chunk.setTitleScore(breakdown.getTitleScore());
        chunk.setStructureScore(breakdown.getStructureScore());
        chunk.setSourceDiversityPenalty(0.0);
        chunk.setFinalScore(breakdown.getFinalScore());
        return chunk;
    }

    private void applySourceDiversityPenalty(List<RetrievedChunk> chunks) {
        Map<String, List<RetrievedChunk>> grouped = chunks.stream()
                .collect(Collectors.groupingBy(chunk -> normalizeSource(chunk.getSource())));

        for (List<RetrievedChunk> group : grouped.values()) {
            group.sort(Comparator.comparingDouble(RetrievedChunk::getFinalScore).reversed());
            for (int index = 0; index < group.size(); index++) {
                RetrievedChunk chunk = group.get(index);
                double penalty = penaltyForOccurrence(index);
                chunk.setSourceDiversityPenalty(penalty);
                chunk.setFinalScore(chunk.getFinalScore() - penalty);
            }
        }
    }

    private List<RetrievedChunk> limitBySource(List<RetrievedChunk> chunks) {
        Map<String, Integer> sourceCounts = new HashMap<>();
        List<RetrievedChunk> limited = new ArrayList<>();

        for (RetrievedChunk chunk : chunks) {
            String source = normalizeSource(chunk.getSource());
            int count = sourceCounts.getOrDefault(source, 0);
            if (count >= rerankProperties.getMaxChunksPerSource()) {
                continue;
            }
            sourceCounts.put(source, count + 1);
            limited.add(chunk);
        }

        return limited;
    }

    private double penaltyForOccurrence(int occurrenceIndex) {
        if (occurrenceIndex <= 0) {
            return 0.0;
        }
        if (occurrenceIndex == 1) {
            return 0.05;
        }
        return 0.15;
    }

    private String normalizeSource(String source) {
        return source == null ? "" : source;
    }
}
