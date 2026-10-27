<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.top511.vera.blog.model.User" %>
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
    User user = (User) request.getAttribute("user");
    String query = (String) request.getAttribute("query");
    List<User> foundBlogs = (List<User>) request.getAttribute("foundBlogs");
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
    <div class="brand">Рай интровертов</div>
    <form class="search" method="GET" action="<%= request.getContextPath() %>/search">
        <input type="text" name="q" placeholder="Поиск блога по названию…"
               value="<%= esc(query) %>">
        <button type="submit">Найти</button>
    </form>
    <div class="user-block">
        <span>Вы вошли как <strong><%= esc(user.nickname()) %></strong></span>
        <a href="<%= request.getContextPath() %>/logout">Выйти</a>
    </div>
</div>

<div class="container">
    <a class="back-link" href="<%= request.getContextPath() %>/">&larr; Вернуться в мой блог</a>

    <h1 class="page-title">
        Результаты поиска
        <% if (query != null && !query.isBlank()) { %>
        по запросу «<span class="q"><%= esc(query) %></span>»
        <% } %>
    </h1>

    <% if (foundBlogs == null || foundBlogs.isEmpty()) { %>
    <div class="empty">Ничего не найдено. Попробуйте другое название.</div>
    <% } else { %>
    <% for (User blog : foundBlogs) { %>
    <a class="result" href="<%= request.getContextPath() %>/blog?id=<%= blog.id() %>">
        <div class="title"><%= esc(blog.blogTitle()) %></div>
        <div class="meta">автор: <%= esc(blog.nickname()) %></div>
    </a>
    <% } %>
    <% } %>
</div>

</body>
</html>