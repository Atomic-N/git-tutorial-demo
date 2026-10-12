package com.aitef.forum.model.dto;

import com.aitef.forum.model.entity.Post;
import com.aitef.forum.model.entity.Tag;

import java.util.ArrayList;
import java.util.List;

public class TopicDetail {
    private TopicCard topic;
    private List<Post> posts = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();

    public TopicCard getTopic() {
        return topic;
    }

    public void setTopic(TopicCard topic) {
        this.topic = topic;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }
}
