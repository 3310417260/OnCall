package org.example.service;

import org.example.dto.RecallCandidate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关键词提取与命中打分服务
 */
@Service
public class KeywordScoringService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{IsHan}]{2,}|[a-zA-Z0-9_\\-]{2,}");

    /**
     * 从 query 中提取适合用于简单匹配的关键词
     */
    public List<String> extractKeywords(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        Set<String> keywords = new LinkedHashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(query);
        while (matcher.find()) {
            String token = normalize(matcher.group());
            if (!token.isBlank()) {
                keywords.add(token);
            }
        }

        if (keywords.isEmpty()) {
            keywords.add(normalize(query));
        }

        return new ArrayList<>(keywords);
    }

    /**
     * 计算内容层面的关键词命中分
     */
    public double calculateKeywordScore(List<String> keywords, RecallCandidate candidate) {
        if (keywords == null || keywords.isEmpty() || candidate == null) {
            return 0.0;
        }

        String content = normalize(candidate.getContent());
        if (content.isBlank()) {
            return 0.0;
        }

        int matched = 0;
        for (String keyword : keywords) {
            if (!keyword.isBlank() && content.contains(keyword)) {
                matched++;
            }
        }

        return (double) matched / keywords.size();
    }

    /**
     * 计算标题/文件名层面的匹配分
     */
    public double calculateTitleScore(List<String> keywords, RecallCandidate candidate) {
        if (keywords == null || keywords.isEmpty() || candidate == null) {
            return 0.0;
        }

        double titleMatchRatio = calculateMatchRatio(keywords, candidate.getTitle());
        double fileNameMatchRatio = calculateMatchRatio(keywords, candidate.getFileName());

        return Math.max(titleMatchRatio, fileNameMatchRatio * 0.6);
    }

    private double calculateMatchRatio(List<String> keywords, String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return 0.0;
        }

        int matched = 0;
        for (String keyword : keywords) {
            if (!keyword.isBlank() && normalized.contains(keyword)) {
                matched++;
            }
        }

        return (double) matched / keywords.size();
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }
}
