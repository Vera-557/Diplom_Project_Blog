package com.top511.vera.blog.servlet;

import com.top511.vera.blog.exception.BusinessException;
import com.top511.vera.blog.exception.DataAccessException;
import com.top511.vera.blog.model.Message;
import com.top511.vera.blog.model.User;
import com.top511.vera.blog.repository.MessageRepository;
import com.top511.vera.blog.repository.UserRepository;
import com.top511.vera.blog.util.AppConfig;
import com.top511.vera.blog.validator.MessageValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@WebServlet(name = "BlogServlet", urlPatterns = {"", "/blog"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize       = 5L * 1024 * 1024,
        maxRequestSize    = 6L * 1024 * 1024
)
public class BlogServlet extends HttpServlet {

    private static final int LATEST_MESSAGE_LIMIT = 30;
    private static final String SERVICE_UNAVAILABLE_MESSAGE =
            "Сервис временно недоступен. Попробуйте позже.";

    private MessageRepository messageRepository;
    private UserRepository userRepository;

    @Override
    public void init() throws ServletException {
        messageRepository = new MessageRepository();
        userRepository = new UserRepository();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = currentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        long blogOwnerId = currentUser.id();
        String idParam = request.getParameter("id");
        if (idParam != null && !idParam.isBlank()) {
            try { blogOwnerId = Long.parseLong(idParam); }
            catch (NumberFormatException ignored) { /* оставим свой */ }
        }

        User blogOwner;
        if (blogOwnerId == currentUser.id()) {
            blogOwner = currentUser;
        } else {
            Optional<User> found;
            try {
                found = userRepository.findById(blogOwnerId);
            } catch (DataAccessException e) {
                getServletContext().log("Ошибка БД при загрузке блога", e);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        SERVICE_UNAVAILABLE_MESSAGE);
                return;
            }
            if (found.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Блог не найден");
                return;
            }
            blogOwner = found.get();
        }

        List<Message> latest;
        try {
            latest = messageRepository.getLatestMessages(blogOwner.id(), LATEST_MESSAGE_LIMIT);
        } catch (DataAccessException e) {
            getServletContext().log("Ошибка БД при загрузке сообщений", e);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    SERVICE_UNAVAILABLE_MESSAGE);
            return;
        }

        boolean isOwnBlog = blogOwner.id() == currentUser.id();

        request.setAttribute("user", currentUser);
        request.setAttribute("blogOwner", blogOwner);
        request.setAttribute("isOwnBlog", isOwnBlog);
        request.setAttribute("latestMessages", latest);
        request.getRequestDispatcher("WEB-INF/views/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        User currentUser = currentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        if ("delete".equals(action)) {
            handleDelete(request, response, currentUser);
            return;
        }
        if ("edit".equals(action)) {
            handleEdit(request, response, currentUser);
            return;
        }
        handleCreate(request, response, currentUser);
    }

    //  Создание поста

    private void handleCreate(HttpServletRequest request, HttpServletResponse response,
                              User currentUser) throws ServletException, IOException {
        String content = request.getParameter("content");
        String imageCaption = request.getParameter("imageCaption");

        request.setAttribute("formContent", content);
        request.setAttribute("formImageCaption", imageCaption);

        if (!MessageValidator.isValidContent(content)) {
            forwardBackToOwnBlog(request, response, currentUser, "error",
                    "Текст записи не должен быть пустым и не должен превышать "
                            + MessageValidator.MAX_CONTENT_LENGTH + " символов.");
            return;
        }
        if (!MessageValidator.isValidCaption(imageCaption)) {
            forwardBackToOwnBlog(request, response, currentUser, "error",
                    "Подпись к картинке не должна превышать "
                            + MessageValidator.MAX_CAPTION_LENGTH + " символов.");
            return;
        }

        content = content.trim();
        imageCaption = (imageCaption == null || imageCaption.isBlank())
                ? null : imageCaption.trim();

        String imagePath = null;

        Part filePart;
        try {
            filePart = request.getPart("image");
        } catch (IllegalStateException e) {
            forwardBackToOwnBlog(request, response, currentUser, "error",
                    "Файл слишком большой. Максимальный размер изображения — 5 МБ.");
            return;
        }

        if (filePart != null && filePart.getSize() > 0) {
            String submitted = filePart.getSubmittedFileName();
            if (submitted != null && !submitted.isBlank()) {
                String contentType = filePart.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    forwardBackToOwnBlog(request, response, currentUser, "error",
                            "Можно прикреплять только изображения (JPG, PNG, GIF, WebP).");
                    return;
                }

                String ext = getExtension(submitted);
                String fileName = UUID.randomUUID() + ext;

                Path uploadDir = AppConfig.getUploadDir();
                Files.createDirectories(uploadDir);

                Path target = uploadDir.resolve(fileName);
                try (InputStream in = filePart.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                imagePath = fileName;
            }
        }

        Message message = new Message(
                currentUser.nickname(),
                content,
                LocalDateTime.now(),
                imagePath,
                imageCaption
        );

        try {
            messageRepository.addMessage(currentUser.id(), message);
        } catch (DataAccessException e) {
            getServletContext().log("Ошибка БД при сохранении записи", e);
            forwardBackToOwnBlog(request, response, currentUser, "error",
                    SERVICE_UNAVAILABLE_MESSAGE);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/");
    }

    //  Редактирование

    private void handleEdit(HttpServletRequest request, HttpServletResponse response,
                            User currentUser) throws ServletException, IOException {
        String idParam = request.getParameter("id");
        String content = request.getParameter("content");
        String imageCaption = request.getParameter("imageCaption");

        Long messageId = null;
        if (idParam != null && !idParam.isBlank()) {
            try { messageId = Long.parseLong(idParam); }
            catch (NumberFormatException ignored) { /* покажем ошибку  */ }
        }

        if (messageId == null) {
            request.setAttribute("editContent", content);
            request.setAttribute("editCaption", imageCaption);
            forwardBackToOwnBlog(request, response, currentUser, "pageError",
                    "Не удалось определить запись для редактирования.");
            return;
        }

        request.setAttribute("editMessageId", messageId);
        request.setAttribute("editContent", content);
        request.setAttribute("editCaption", imageCaption);

        if (!MessageValidator.isValidContent(content)) {
            forwardBackToOwnBlog(request, response, currentUser, "editError",
                    "Текст записи не должен быть пустым и не должен превышать "
                            + MessageValidator.MAX_CONTENT_LENGTH + " символов.");
            return;
        }
        if (!MessageValidator.isValidCaption(imageCaption)) {
            forwardBackToOwnBlog(request, response, currentUser, "editError",
                    "Подпись к картинке не должна превышать "
                            + MessageValidator.MAX_CAPTION_LENGTH + " символов.");
            return;
        }

        try {
            messageRepository.updateMessage(
                    currentUser.id(),
                    messageId,
                    content.trim(),
                    (imageCaption == null || imageCaption.isBlank()) ? null : imageCaption.trim()
            );
        } catch (BusinessException e) {
            forwardBackToOwnBlog(request, response, currentUser, "editError", e.getMessage());
            return;
        } catch (DataAccessException e) {
            getServletContext().log("Ошибка БД при обновлении записи", e);
            forwardBackToOwnBlog(request, response, currentUser, "editError",
                    SERVICE_UNAVAILABLE_MESSAGE);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/");
    }

    //  Удаление

    private void handleDelete(HttpServletRequest request, HttpServletResponse response,
                              User currentUser) throws ServletException, IOException {
        String idParam = request.getParameter("id");

        if (idParam == null || idParam.isBlank()) {
            forwardBackToOwnBlog(request, response, currentUser, "pageError",
                    "Не указана запись для удаления.");
            return;
        }

        long messageId;
        try {
            messageId = Long.parseLong(idParam);
        } catch (NumberFormatException e) {
            forwardBackToOwnBlog(request, response, currentUser, "pageError",
                    "Некорректный идентификатор записи.");
            return;
        }

        try {
            messageRepository.deleteMessage(currentUser.id(), messageId);
        } catch (BusinessException e) {
            forwardBackToOwnBlog(request, response, currentUser, "pageError", e.getMessage());
            return;
        } catch (DataAccessException e) {
            getServletContext().log("Ошибка БД при удалении записи", e);
            forwardBackToOwnBlog(request, response, currentUser, "pageError",
                    SERVICE_UNAVAILABLE_MESSAGE);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/");
    }

    private void forwardBackToOwnBlog(HttpServletRequest request,
                                      HttpServletResponse response,
                                      User currentUser,
                                      String errorAttrName,
                                      String errorMessage)
            throws ServletException, IOException {
        List<Message> latest;
        try {
            latest = messageRepository.getLatestMessages(currentUser.id(), LATEST_MESSAGE_LIMIT);
        } catch (DataAccessException e) {
            getServletContext().log("Ошибка БД при загрузке сообщений", e);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    SERVICE_UNAVAILABLE_MESSAGE);
            return;
        }
        request.setAttribute("user", currentUser);
        request.setAttribute("blogOwner", currentUser);
        request.setAttribute("isOwnBlog", true);
        request.setAttribute("latestMessages", latest);
        request.setAttribute(errorAttrName, errorMessage);
        request.getRequestDispatcher("WEB-INF/views/index.jsp").forward(request, response);
    }

    private static String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) return "";
        String ext = name.substring(dot).toLowerCase();
        return switch (ext) {
            case ".jpg", ".jpeg", ".png", ".gif", ".webp" -> ext;
            default -> ".jpg";
        };
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }
}