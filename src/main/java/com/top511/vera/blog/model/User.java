package com.top511.vera.blog.model;

public record User(Long id, String login, String passwordHash,
                   String nickname, String blogTitle) {
}