package com.top511.vera.blog.repository;

import com.top511.vera.blog.exception.BusinessException;
import com.top511.vera.blog.exception.DataAccessException;
import com.top511.vera.blog.model.User;
import com.top511.vera.blog.util.DatabaseConfig;
import com.top511.vera.blog.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    private static final String UNIQUE_VIOLATION = "23505";

    public Optional<User> findByLogin(String login) {
        String sql = "SELECT id, login, password_hash, nickname, blog_title " +
                "FROM users WHERE login = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getLong("id"),
                            rs.getString("login"),
                            rs.getString("password_hash"),
                            rs.getString("nickname"),
                            rs.getString("blog_title")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось найти пользователя по логину", e);
        }
        return Optional.empty();
    }

    public boolean loginExists(String login) {
        return findByLogin(login).isPresent();
    }

    public boolean nicknameExists(String nickname) {
        String sql = "SELECT 1 FROM users WHERE LOWER(nickname) = LOWER(?) LIMIT 1";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось проверить уникальность ника", e);
        }
    }

    public User register(String login, String rawPassword,
                         String nickname, String blogTitle) {
        String sql = "INSERT INTO users (login, password_hash, nickname, blog_title) " +
                "VALUES (?, ?, ?, ?) RETURNING id";
        String hash = PasswordUtil.hash(rawPassword);
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, hash);
            ps.setString(3, nickname);
            ps.setString(4, blogTitle);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new User(rs.getLong("id"), login, hash, nickname, blogTitle);
            }
        } catch (SQLException e) {
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                throw new BusinessException(
                        "Такой логин или ник уже занят, выберите другие", e);
            }
            throw new DataAccessException("Не удалось зарегистрировать пользователя", e);
        }
    }

    public List<User> searchByBlogTitle(String query, int limit) {
        List<User> result = new ArrayList<>();
        String sql = "SELECT id, login, password_hash, nickname, blog_title " +
                "FROM users " +
                "WHERE LOWER(blog_title) LIKE LOWER(?) " +
                "ORDER BY blog_title LIMIT ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + query.trim() + "%");
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new User(
                            rs.getLong("id"),
                            rs.getString("login"),
                            rs.getString("password_hash"),
                            rs.getString("nickname"),
                            rs.getString("blog_title")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось выполнить поиск блогов", e);
        }
        return result;
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT id, login, password_hash, nickname, blog_title " +
                "FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getLong("id"),
                            rs.getString("login"),
                            rs.getString("password_hash"),
                            rs.getString("nickname"),
                            rs.getString("blog_title")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось загрузить пользователя по id", e);
        }
        return Optional.empty();
    }
}