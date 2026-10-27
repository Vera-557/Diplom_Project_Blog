package com.top511.vera.blog.model;

import java.time.LocalDateTime;

public record Message(Long id, String author, String content, LocalDateTime createdAt,
                      String imagePath, String imageCaption) {

    public Message {
        if (author == null || content == null || author.isBlank() || content.isBlank()) {
            throw new IllegalArgumentException();
        }
    }

    //Новые сообщения, если id ещё не известен.
    public Message(String author, String content, LocalDateTime createdAt,
                   String imagePath, String imageCaption) {
        this(null, author, content, createdAt, imagePath, imageCaption);
    }
}