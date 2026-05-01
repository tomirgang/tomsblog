package de.tomsblog.blogcontent.application.service;

import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.Slug;

public class PostNotFoundException extends RuntimeException {

    private final PostId postId;

    public PostNotFoundException(PostId postId) {
        super("Post not found: " + postId.asString());
        this.postId = postId;
    }

    public PostNotFoundException(Slug slug) {
        super("Post not found: " + slug.value());
        this.postId = null;
    }

    public PostId getPostId() {
        return postId;
    }
}
