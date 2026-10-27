package com.top511.vera.blog.validator;

public final class MessageValidator {

    public static final int MAX_CONTENT_LENGTH = 1500;
    public static final int MAX_CAPTION_LENGTH = 300;

    private MessageValidator() {}

    public static boolean isValidContent(String content) {
        return content != null
                && !content.isBlank()
                && content.length() <= MAX_CONTENT_LENGTH;
    }

    public static boolean isValidCaption(String caption) {
        return caption == null || caption.length() <= MAX_CAPTION_LENGTH;
    }
}