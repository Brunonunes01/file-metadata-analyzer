package com.metascan.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class MetadataInsightsService {

    public List<String> generateInsights(
            String author,
            boolean hasValidCreationDate,
            int textLength,
            int metadataCount,
            boolean isImageFile,
            boolean hasGps
    ) {
        List<String> insights = new ArrayList<>();

        if (isValidAuthor(author)) {
            insights.add("✔️ Autor identificado no arquivo");
        } else {
            insights.add("⚠️ Este arquivo não possui autor identificado");
        }

        if (hasValidCreationDate) {
            insights.add("✔️ O arquivo possui data de criação registrada");
        } else {
            insights.add("⚠️ O arquivo não possui data de criação");
        }

        if (textLength > 0) {
            insights.add("✔️ O arquivo contém conteúdo textual");
        } else {
            insights.add("⚠️ O arquivo não possui conteúdo textual detectável");
        }

        if (metadataCount >= 10) {
            insights.add("✔️ Arquivo contém diversos metadados");
        } else {
            insights.add("⚠️ Poucos metadados disponíveis neste arquivo");
        }

        if (isImageFile) {
            if (hasGps) {
                insights.add("⚠️ Esta imagem contém localização GPS");
            } else {
                insights.add("✔️ Nenhuma localização GPS foi encontrada na imagem");
            }
        }

        return insights;
    }

    public boolean isValidAuthor(String author) {
        if (author == null || author.isBlank()) {
            return false;
        }

        String normalized = author.trim().toLowerCase(Locale.ROOT);
        return !normalized.equals("un-named") && !normalized.equals("unknown");
    }
}
