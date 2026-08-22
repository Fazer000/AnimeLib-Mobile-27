package com.example.animelib.util;

import com.example.animelib.models.EpisodesListResponse;
import java.util.Locale;

public class EpisodeUtils {

    public static String getStatusLabel(EpisodesListResponse.EpisodeItem.Status status) {
        if (status == null) return null;

        String id = status.getId();
        String label = status.getLabel();

        if ((id == null || id.trim().isEmpty()) && (label == null || label.trim().isEmpty())) {
            return null;
        }

        if ((id != null && id.equalsIgnoreCase("default")) ||
            (label != null && (label.equalsIgnoreCase("default") || label.equalsIgnoreCase("обычный")))) {
            return null;
        }

        String text = (label != null && !label.trim().isEmpty()) ? label.trim() : id.trim();
        return text.toUpperCase(Locale.getDefault());
    }

    public static String getTransliteratedStatusLabel(EpisodesListResponse.EpisodeItem.Status status) {
        return getStatusLabel(status);
    }
}

