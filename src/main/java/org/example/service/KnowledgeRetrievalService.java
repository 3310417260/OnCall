package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.RerankProperties;
import org.example.dto.RecallCandidate;
import org.example.dto.RetrievedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一的知识检索服务，负责串联召回与重排
 */
@Service
public class KnowledgeRetrievalService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeRetrievalService.class);

    private final VectorSearchService vectorSearchService;
    private final RerankService rerankService;
    private final RerankProperties rerankProperties;
    private final ObjectMapper objectMapper;

    @Autowired
    public KnowledgeRetrievalService(VectorSearchService vectorSearchService,
                                     RerankService rerankService,
                                     RerankProperties rerankProperties,
                                     ObjectMapper objectMapper) {
        this.vectorSearchService = vectorSearchService;
        this.rerankService = rerankService;
        this.rerankProperties = rerankProperties;
        this.objectMapper = objectMapper;
    }

    public List<RetrievedChunk> retrieve(String query, int topK) {
        int recallTopN = Math.max(topK, rerankProperties.getRecallTopN());
        List<VectorSearchService.SearchResult> searchResults =
                vectorSearchService.searchSimilarDocuments(query, recallTopN);

        if (searchResults.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecallCandidate> candidates = searchResults.stream()
                .map(this::toRecallCandidate)
                .toList();

        if (!rerankProperties.isEnabled()) {
            logger.info("RAG 重排关闭，直接返回向量召回结果，query={}, topK={}", query, topK);
            return candidates.stream()
                    .limit(topK)
                    .map(this::toRetrievedChunkWithoutRerank)
                    .toList();
        }

        try {
            return rerankService.rerank(query, candidates, Math.min(topK, rerankProperties.getFinalTopK()));
        } catch (Exception e) {
            logger.warn("RAG 重排失败，回退到纯向量召回结果: {}", e.getMessage());
            return candidates.stream()
                    .limit(topK)
                    .map(this::toRetrievedChunkWithoutRerank)
                    .toList();
        }
    }

    private RecallCandidate toRecallCandidate(VectorSearchService.SearchResult result) {
        RecallCandidate candidate = new RecallCandidate();
        candidate.setId(result.getId());
        candidate.setContent(result.getContent());
        candidate.setVectorDistance(result.getScore());

        Map<String, Object> metadata = parseMetadata(result.getMetadata());
        candidate.setMetadata(metadata);
        candidate.setSource(asString(metadata.get("_source")));
        candidate.setFileName(asString(metadata.get("_file_name")));
        candidate.setExtension(asString(metadata.get("_extension")));
        candidate.setTitle(asString(metadata.get("title")));
        candidate.setChunkIndex(asInteger(metadata.get("chunkIndex")));
        candidate.setTotalChunks(asInteger(metadata.get("totalChunks")));
        return candidate;
    }

    private RetrievedChunk toRetrievedChunkWithoutRerank(RecallCandidate candidate) {
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
        chunk.setSemanticScore(1.0);
        chunk.setKeywordScore(0.0);
        chunk.setTitleScore(0.0);
        chunk.setStructureScore(0.0);
        chunk.setSourceDiversityPenalty(0.0);
        chunk.setFinalScore(1.0);
        return chunk;
    }

    private Map<String, Object> parseMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return new LinkedHashMap<>();
        }

        try {
            return objectMapper.readValue(metadata, new TypeReference<>() {});
        } catch (Exception e) {
            logger.warn("解析 Milvus metadata 失败，使用空 metadata 回退: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
