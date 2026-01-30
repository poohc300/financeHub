package com.example.financeHub.dashboard.model;

import java.util.List;

import lombok.Data;

@Data
public class ChartDataDTO {
    private List<String> labels;
    private List<String> values;
    private String indexName;
}
