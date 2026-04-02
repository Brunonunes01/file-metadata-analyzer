package com.metascan.dto;

import java.util.List;

public record MetadataPrivacyRiskDto(
        String level,
        int score,
        List<String> reasons,
        List<String> sensitiveDataFound
) {
}
