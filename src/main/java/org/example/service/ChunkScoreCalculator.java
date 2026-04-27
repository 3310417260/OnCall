package org.example.service;

import org.example.dto.ChunkScoreBreakdown;
import org.example.dto.RecallCandidate;
import org.example.dto.RecallContext;

/**
 * chunk 打分计算器
 */
public interface ChunkScoreCalculator {

    ChunkScoreBreakdown score(RecallCandidate candidate, RecallContext context);
}
