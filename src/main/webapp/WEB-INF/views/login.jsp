<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Мир интровертов, вход</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="page-wrap">
    <div class="card">
        <h1>Вход</h1>
        <div class="sub">Мир интровертов, рай для гиков</div>

        <% if (error != null) { %>
        <div class="error"><%= error %></div>
        <% } %>

        <form method="POST" action="<%= request.getContextPath() %>/login">
            <label for="login">Логин</label>
            <input type="text" id="login" name="login" required autofocus>

            <label for="password">Пароль</label>
            <input type="password" id="password" name="password" required>

            <button type="submit" class="btn">Войти</button>
        </form>

        <div class="footer">
            Нет аккаунта?
            <a href="<%= request.getContextPath() %>/register">Зарегистрироваться</a>
        </div>
    </div>
</div>
</body>
</html>