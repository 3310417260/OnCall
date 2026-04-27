package org.example.service;

import org.example.dto.RecallCandidate;
import org.example.dto.RetrievedChunk;

import java.util.List;

/**
 * 重排服务
 */
public interface RerankService {

    List<RetrievedChunk> rerank(String query, List<RecallCandidate> candidates, int topK);
}
