<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.top511.vera.blog.validator.UserValidator" %>
<%
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Мир интровертов</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="page-wrap">
    <div class="card wide">
        <h1>Регистрация</h1>
        <div class="sub">Создайте свой блог</div>

        <% if (error != null) { %>
        <div class="error"><%= error %></div>
        <% } %>

        <form method="POST" action="<%= request.getContextPath() %>/register">
            <label for="login">Логин (для входа)</label>
            <input type="text" id="login" name="login" required
                   minlength="<%= UserValidator.LOGIN_MIN %>"
                   maxlength="<%= UserValidator.LOGIN_MAX %>"
                   pattern="[A-Za-zА-Яа-яЁё]{<%= UserValidator.LOGIN_MIN %>,<%= UserValidator.LOGIN_MAX %>}"
                   title="3–12 символов, только буквы (латиница или кириллица), без цифр">
            <div class="hint"><%= UserValidator.loginHint() %>. Другие его не видят</div>

            <label for="password">Пароль</label>
            <input type="password" id="password" name="password" required
                   minlength="<%= UserValidator.PASSWORD_MIN %>"
                   maxlength="<%= UserValidator.PASSWORD_MAX %>"
                   title="От 4 до 15 символов">
            <div class="hint"><%= UserValidator.passwordHint() %></div>

            <label for="nickname">Никнейм</label>
            <input type="text" id="nickname" name="nickname" required
                   minlength="<%= UserValidator.NICKNAME_MIN %>"
                   maxlength="<%= UserValidator.NICKNAME_MAX %>"
                   pattern="[A-Za-zА-Яа-яЁё0-9]{<%= UserValidator.NICKNAME_MIN %>,<%= UserValidator.NICKNAME_MAX %>}"
                   title="3–12 символов, буквы (латиница/кириллица) и цифры">
            <div class="hint"><%= UserValidator.nicknameHint() %></div>

            <label for="blogTitle">Название блога</label>
            <input type="text" id="blogTitle" name="blogTitle" required
                   maxlength="100"
                   placeholder="Например: «Тихие мысли»">
            <div class="hint">Заголовок вашей страницы — его увидят все</div>

            <button type="submit" class="btn">Создать блог</button>
        </form>

        <div class="footer">
            Уже есть аккаунт?
            <a href="<%= request.getContextPath() %>/login">Войти</a>
        </div>
    </div>
</div>
</body>
</html>