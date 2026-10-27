<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="com.top511.vera.blog.model.Message" %>
<%@ page import="com.top511.vera.blog.model.User" %>
<%@ page import="com.top511.vera.blog.validator.MessageValidator" %>
<%!

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>
<%
    User user       = (User) request.getAttribute("user");
    User blogOwner  = (User) request.getAttribute("blogOwner");
    Boolean ownBox  = (Boolean) request.getAttribute("isOwnBlog");
    boolean isOwnBlog = ownBox != null && ownBox;
    List<Message> latestMessages = (List<Message>) request.getAttribute("latestMessages");
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    String pageError = (String) request.getAttribute("pageError");
    String error     = (String) request.getAttribute("error");
    String editError = (String) request.getAttribute("editError");

    String formContent      = (String) request.getAttribute("formContent");
    String formImageCaption = (String) request.getAttribute("formImageCaption");

    Long editMessageId = (Long) request.getAttribute("editMessageId");
    String editContent = (String) request.getAttribute("editContent");
    String editCaption = (String) request.getAttribute("editCaption");
%>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Мир интровертов</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<div class="topbar">
    <div class="brand">Мир интровертов</div>

    <form class="search" method="GET" action="<%= request.getContextPath() %>/search">
        <input type="text" name="q" placeholder="Поиск блога по названию…"
               value="<%= request.getAttribute("query") != null ? request.getAttribute("query") : "" %>">
        <button type="submit">Найти</button>
    </form>

    <div class="user-block">
        <span>Вы вошли как <strong><%= esc(user.nickname()) %></strong></span>
        <a href="<%= request.getContextPath() %>/logout">Выйти</a>
    </div>
</div>

<div class="container">

    <% if (pageError != null) { %>
    <div class="error">${pageError}</div>
    <% } %>

    <% if (!isOwnBlog) { %>
    <a class="back-link" href="<%= request.getContextPath() %>/">&larr; Вернуться в мой блог</a>
    <% } %>

    <div class="blog-header">
        <h1><%= esc(blogOwner.blogTitle()) %></h1>
        <div class="author">автор: <%= esc(blogOwner.nickname()) %></div>
    </div>

    <% if (isOwnBlog) { %>
    <form class="composer" method="POST"
          action="<%= request.getContextPath() %>/"
          enctype="multipart/form-data">

        <% if (error != null) { %>
        <div class="error">${error}</div>
        <% } %>

        <textarea name="content" placeholder="О чём вы думаете?" required
                  maxlength="<%= MessageValidator.MAX_CONTENT_LENGTH %>"><%= esc(formContent) %></textarea>

        <div class="attach-row">
            <input type="file" name="image" accept="image/*">
            <input type="text" name="imageCaption"
                   placeholder="Подпись к картинке (необязательно)"
                   value="<%= esc(formImageCaption) %>"
                   maxlength="<%= MessageValidator.MAX_CAPTION_LENGTH %>">
        </div>
        <div class="attach-hint">
            Можно приложить одну картинку (JPG/PNG/GIF/WebP, до 5 МБ) и подпись к ней
            (до <%= MessageValidator.MAX_CAPTION_LENGTH %> символов).
            Текст записи — до <%= MessageValidator.MAX_CONTENT_LENGTH %> символов.
        </div>

        <div class="actions">
            <span class="as">Публикуется от имени <strong><%= esc(user.nickname()) %></strong></span>
            <button type="submit">Опубликовать</button>
        </div>
    </form>
    <% } %>

    <% if (latestMessages == null || latestMessages.isEmpty()) { %>
    <div class="empty">
        <%= isOwnBlog ? "Пока нет ни одной записи. Напишите первую!"
                : "В этом блоге пока нет записей." %>
    </div>
    <% } else { %>
    <div class="posts">
        <%
            boolean editErrorShown = false;
            for (Message m : latestMessages) {
                boolean isEditingThis = editMessageId != null && editMessageId.equals(m.id());
                if (isEditingThis && editError != null) editErrorShown = true;
        %>
        <div class="post">
            <div class="meta">
                <span class="author"><%= esc(m.author()) %></span>
                &middot; <%= m.createdAt().format(fmt) %>
            </div>
            <div class="content"><%= esc(m.content()) %></div>

            <% if (m.imagePath() != null && !m.imagePath().isBlank()) { %>
            <div class="post-image">
                <img src="<%= request.getContextPath() %>/uploads/<%= m.imagePath() %>"
                     alt="<%= m.imageCaption() != null ? esc(m.imageCaption()) : "изображение" %>">
                <% if (m.imageCaption() != null && !m.imageCaption().isBlank()) { %>
                <div class="caption"><%= esc(m.imageCaption()) %></div>
                <% } %>
            </div>
            <% } %>

            <% if (isOwnBlog) { %>
            <div class="post-actions">

                <details class="edit-box" <%= isEditingThis ? "open" : "" %>>
                    <summary>Редактировать</summary>

                    <form method="POST" action="<%= request.getContextPath() %>/">
                        <input type="hidden" name="action" value="edit">
                        <input type="hidden" name="id" value="<%= m.id() %>">

                        <% if (isEditingThis && editError != null) { %>
                        <div class="error">${editError}</div>
                        <% } %>

                        <textarea name="content" required
                                  maxlength="<%= MessageValidator.MAX_CONTENT_LENGTH %>"><%= esc(isEditingThis && editContent != null ? editContent : m.content()) %></textarea>

                        <input type="text"
                               name="imageCaption"
                               value="<%= esc(isEditingThis && editCaption != null ? editCaption
                                              : (m.imageCaption() != null ? m.imageCaption() : "")) %>"
                               placeholder="Подпись к картинке (необязательно)"
                               maxlength="<%= MessageValidator.MAX_CAPTION_LENGTH %>">

                        <button type="submit">Сохранить</button>
                    </form>
                </details>

                <form method="POST"
                      action="<%= request.getContextPath() %>/"
                      onsubmit="return confirm('Удалить эту запись?');">
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="id" value="<%= m.id() %>">
                    <button type="submit" class="danger">Удалить</button>
                </form>

            </div>
            <% } %>
        </div>
        <% } %>
    </div>

    <% if (editError != null && !editErrorShown) { %>
    <%-- Пользователь пытался отредактировать пост, которого нет в списке. --%>
    <div class="error" style="margin-top: 14px;">${editError}</div>
    <% } %>
    <% } %>

</div>

</body>
</html>