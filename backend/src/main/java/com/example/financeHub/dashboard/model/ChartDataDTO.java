package com.example.financeHub.dashboard.model;

import java.util.List;

import lombok.Data;

@Data
public class ChartDataDTO {
    private List<String> labels;
    private List<String> values;
    private List<String> volumes;
    private List<String> opens;
    private List<String> highs;
    private List<String> lows;
    private String indexName;
}
