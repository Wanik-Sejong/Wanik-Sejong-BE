package com.hackathon.project.domain.Roadmap.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WeightHintResponseDTO {

    private List<String> matchedSectors;
    private Map<String, List<String>> sectorKeywords;
    private List<WeightRule> weightRules;
    private int defaultN;
    private String notes;

    @Getter
    @AllArgsConstructor
    public static class WeightRule {

        private String rule;
        private int score;
        private String reason;
    }
}
