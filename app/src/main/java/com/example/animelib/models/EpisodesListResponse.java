package com.example.animelib.models;

import java.util.List;

public class EpisodesListResponse {
    private List<EpisodeItem> data;

    public EpisodesListResponse() {}

    public List<EpisodeItem> getData() {
        return data;
    }

    public void setData(List<EpisodeItem> data) {
        this.data = data;
    }

    public static class EpisodeItem {
        private int id;
        private String name;
        private String number;
        private String season;

        @com.google.gson.annotations.SerializedName("status")
        private Status status;

        public EpisodeItem() {}

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getSeason() {
            return season;
        }

        public void setSeason(String season) {
            this.season = season;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public static class Status {
            @com.google.gson.annotations.SerializedName("id")
            private String id;

            @com.google.gson.annotations.SerializedName("label")
            private String label;

            public Status() {}

            public Status(String id, String label) {
                this.id = id;
                this.label = label;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getLabel() {
                return label;
            }

            public void setLabel(String label) {
                this.label = label;
            }
        }
    }
}
