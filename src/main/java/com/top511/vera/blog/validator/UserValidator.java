package com.top511.vera.blog.validator;

import java.util.regex.Pattern;

public final class UserValidator {

    public static final int LOGIN_MIN = 3;
    public static final int LOGIN_MAX = 12;

    public static final int NICKNAME_MIN = 3;
    public static final int NICKNAME_MAX = 12;

    public static final int PASSWORD_MIN = 4;
    public static final int PASSWORD_MAX = 15;

    // Логин:
    private static final Pattern LOGIN_PATTERN =
            Pattern.compile("^[A-Za-zА-Яа-яЁё]{" + LOGIN_MIN + "," + LOGIN_MAX + "}$");

    // Ник:
    private static final Pattern NICKNAME_PATTERN =
            Pattern.compile("^[A-Za-zА-Яа-яЁё0-9]{" + NICKNAME_MIN + "," + NICKNAME_MAX + "}$");

    private UserValidator() {}

    public static boolean isValidLogin(String login) {
        return login != null && LOGIN_PATTERN.matcher(login).matches();
    }

    public static boolean isValidNickname(String nickname) {
        return nickname != null && NICKNAME_PATTERN.matcher(nickname).matches();
    }

    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        int len = password.length();
        return len >= PASSWORD_MIN && len <= PASSWORD_MAX;
    }

    public static String loginHint() {
        return "Логин: 3–12 символов, только буквы (латиница или кириллица), без цифр";
    }

    public static String nicknameHint() {
        return "Ник: 3–12 символов, буквы (латиница/кириллица) и цифры";
    }

    public static String passwordHint() {
        return "Пароль: от " + PASSWORD_MIN + " до " + PASSWORD_MAX + " символов";
    }
}