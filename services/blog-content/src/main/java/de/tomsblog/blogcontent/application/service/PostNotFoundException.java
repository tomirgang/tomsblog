package de.tomsblog.blogcontent.application.service;

import de.tomsblog.blogcontent.domain.model.PostId;

public class PostNotFoundException extends RuntimeException {

    private final PostId postId;

    public PostNotFoundException(PostId postId) {
        super("Post not found: " + postId.asString());
        this.postId = postId;
    }

    public PostId getPostId() {
        return postId;
    }
}
