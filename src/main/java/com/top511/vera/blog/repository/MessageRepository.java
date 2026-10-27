package com.top511.vera.blog.repository;

import com.top511.vera.blog.exception.BusinessException;
import com.top511.vera.blog.exception.DataAccessException;
import com.top511.vera.blog.model.Message;
import com.top511.vera.blog.util.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public List<Message> getLatestMessages(long userId, int limit) {
        List<Message> result = new ArrayList<>();
        String sql = "SELECT id, author, content, created_at, image_path, image_caption " +
                "FROM messages WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    result.add(new Message(
                            rs.getLong("id"),
                            rs.getString("author"),
                            rs.getString("content"),
                            ts.toLocalDateTime(),
                            rs.getString("image_path"),
                            rs.getString("image_caption")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось загрузить сообщения", e);
        }
        return result;
    }

    public void addMessage(long userId, Message message) {
        String sql = "INSERT INTO messages " +
                "(user_id, author, content, created_at, image_path, image_caption) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, message.author());
            ps.setString(3, message.content());
            ps.setTimestamp(4, Timestamp.valueOf(message.createdAt()));
            ps.setString(5, message.imagePath());
            ps.setString(6, message.imageCaption());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось сохранить сообщение", e);
        }
    }

    public void updateMessage(long userId, long messageId, String content, String imageCaption) {
        String sql = "UPDATE messages SET content = ?, image_caption = ? " +
                "WHERE id = ? AND user_id = ?";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, content);
            ps.setString(2, imageCaption);
            ps.setLong(3, messageId);
            ps.setLong(4, userId);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                // Если вдруг запись удалили в другой вкладке,
                // или пытаются редактировать чужой пост.
                throw new BusinessException(
                        "Сообщение не найдено или нет прав на редактирование");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось обновить сообщение", e);
        }
    }

    public void deleteMessage(long userId, long messageId) {
        String sql = "DELETE FROM messages WHERE id = ? AND user_id = ?";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, messageId);
            ps.setLong(2, userId);

            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                throw new BusinessException(
                        "Сообщение не найдено или нет прав на удаление");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось удалить сообщение", e);
        }
    }
}