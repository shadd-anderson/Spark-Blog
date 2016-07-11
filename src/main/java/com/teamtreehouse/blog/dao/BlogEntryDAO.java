package com.teamtreehouse.blog.dao;

import com.teamtreehouse.blog.model.Comment;
import spark.Request;

import java.util.ArrayList;
import java.util.List;

public interface BlogEntryDAO {
    String getTitle();
    void setTitle(String title);
    String getAuthor();
    String getDate();
    void setDate();
    List<String> getTags();
    boolean addTag(String tag);
    void setSlug();
    String getSlug();
    String getContent();
    void setContent(String content);
    boolean addComment(Comment comment);
    ArrayList<Comment> getComments();
    boolean isAuthor(Request req);
}
