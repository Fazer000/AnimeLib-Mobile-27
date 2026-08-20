package com.example.animelib.models;

import java.util.List;

public class StickyCommentsResponse {
    private List<CommentsResponse.CommentItem> data;

    public List<CommentsResponse.CommentItem> getData() {
        return data;
    }

    public void setData(List<CommentsResponse.CommentItem> data) {
        this.data = data;
    }
}
