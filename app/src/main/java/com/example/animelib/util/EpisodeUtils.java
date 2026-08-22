package com.example.animelib.util;

import com.example.animelib.models.EpisodesListResponse;

public class EpisodeUtils {

    public static String getTransliteratedStatusLabel(EpisodesListResponse.EpisodeItem.Status status) {
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

        if ((id != null && id.equalsIgnoreCase("recap")) || (label != null && label.equalsIgnoreCase("рекап"))) {
            return "RECAP";
        }
        if ((id != null && id.equalsIgnoreCase("special")) || (label != null && label.equalsIgnoreCase("спешл"))) {
            return "SPECIAL";
        }
        if ((id != null && id.equalsIgnoreCase("ova")) || (label != null && label.equalsIgnoreCase("ова"))) {
            return "OVA";
        }
        if ((id != null && id.equalsIgnoreCase("ona")) || (label != null && label.equalsIgnoreCase("она"))) {
            return "ONA";
        }

        String textToTransliterate = (label != null && !label.trim().isEmpty()) ? label : id;
        return transliterate(textToTransliterate);
    }

    public static String transliterate(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        text = text.trim();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (Character.toLowerCase(c)) {
                case 'а': sb.append("A"); break;
                case 'б': sb.append("B"); break;
                case 'в': sb.append("V"); break;
                case 'г': sb.append("G"); break;
                case 'д': sb.append("D"); break;
                case 'е':
                case 'ё':
                case 'э': sb.append("E"); break;
                case 'ж': sb.append("ZH"); break;
                case 'з': sb.append("Z"); break;
                case 'и':
                case 'й': sb.append("I"); break;
                case 'к': sb.append("K"); break;
                case 'л': sb.append("L"); break;
                case 'м': sb.append("M"); break;
                case 'н': sb.append("N"); break;
                case 'о': sb.append("O"); break;
                case 'п': sb.append("P"); break;
                case 'р': sb.append("R"); break;
                case 'с': sb.append("S"); break;
                case 'т': sb.append("T"); break;
                case 'у': sb.append("U"); break;
                case 'ф': sb.append("F"); break;
                case 'х': sb.append("KH"); break;
                case 'ц': sb.append("TS"); break;
                case 'ч': sb.append("CH"); break;
                case 'ш': sb.append("SH"); break;
                case 'щ': sb.append("SCH"); break;
                case 'ъ':
                case 'ь': break;
                case 'ы': sb.append("Y"); break;
                case 'ю': sb.append("YU"); break;
                case 'я': sb.append("YA"); break;
                default:
                    if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == ' ') {
                        sb.append(Character.toUpperCase(c));
                    }
                    break;
            }
        }
        return sb.toString().trim();
    }
}
