package com.top511.vera.blog.servlet;

import com.top511.vera.blog.exception.BusinessException;
import com.top511.vera.blog.exception.DataAccessException;
import com.top511.vera.blog.model.User;
import com.top511.vera.blog.repository.UserRepository;
import com.top511.vera.blog.util.PasswordUtil;
import com.top511.vera.blog.validator.UserValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Optional;

@WebServlet(name = "AuthServlet", urlPatterns = {"/login", "/register", "/logout"})
public class AuthServlet extends HttpServlet {

    private static final String SERVICE_UNAVAILABLE_MESSAGE =
            "Сервис временно недоступен. Попробуйте позже.";

    private UserRepository userRepository;

    @Override
    public void init() throws ServletException {
        userRepository = new UserRepository();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        switch (request.getServletPath()) {
            case "/login"    -> request.getRequestDispatcher("WEB-INF/views/login.jsp")
                    .forward(request, response);
            case "/register" -> request.getRequestDispatcher("WEB-INF/views/register.jsp")
                    .forward(request, response);
            case "/logout"   -> {
                HttpSession s = request.getSession(false);
                if (s != null) s.invalidate();
                response.sendRedirect(request.getContextPath() + "/login");
            }
            default -> response.sendRedirect(request.getContextPath() + "/login");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        switch (request.getServletPath()) {
            case "/login"    -> handleLogin(request, response);
            case "/register" -> handleRegister(request, response);
            default          -> response.sendRedirect(request.getContextPath() + "/login");
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String login = request.getParameter("login");
        String password = request.getParameter("password");

        if (isBlank(login) || isBlank(password)) {
            fail(request, response, "/login.jsp", "Введите логин и пароль");
            return;
        }

        Optional<User> found;
        try {
            found = userRepository.findByLogin(login);
        } catch (DataAccessException e) {
            getServletContext().log("Ошибка БД при попытке входа", e);
            fail(request, response, "/login.jsp", SERVICE_UNAVAILABLE_MESSAGE);
            return;
        }

        if (found.isEmpty() || !PasswordUtil.matches(password, found.get().passwordHash())) {
            fail(request, response, "/login.jsp", "Неверный логин или пароль");
            return;
        }

        request.getSession(true).setAttribute("user", found.get());
        response.sendRedirect(request.getContextPath() + "/");
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String login     = request.getParameter("login");
        String password  = request.getParameter("password");
        String nickname  = request.getParameter("nickname");
        String blogTitle = request.getParameter("blogTitle");

        if (isBlank(login) || isBlank(password) || isBlank(nickname) || isBlank(blogTitle)) {
            fail(request, response, "/register.jsp", "Заполните все поля");
            return;
        }

        login     = login.trim();
        nickname  = nickname.trim();
        blogTitle = blogTitle.trim();

        if (!UserValidator.isValidLogin(login)) {
            fail(request, response, "/register.jsp", UserValidator.loginHint());
            return;
        }
        if (!UserValidator.isValidPassword(password)) {
            fail(request, response, "/register.jsp", UserValidator.passwordHint());
            return;
        }
        if (!UserValidator.isValidNickname(nickname)) {
            fail(request, response, "/register.jsp", UserValidator.nicknameHint());
            return;
        }

        try {
            if (userRepository.loginExists(login)) {
                fail(request, response, "/register.jsp", "Такой логин уже занят");
                return;
            }
            if (userRepository.nicknameExists(nickname)) {
                fail(request, response, "/register.jsp", "Такой ник уже занят");
                return;
            }

            User user = userRepository.register(login, password, nickname, blogTitle);
            request.getSession(true).setAttribute("user", user);
            response.sendRedirect(request.getContextPath() + "/");
        } catch (BusinessException e) {

            fail(request, response, "/register.jsp", e.getMessage());
        } catch (DataAccessException e) {
            getServletContext().log("Ошибка БД при регистрации", e);
            fail(request, response, "/register.jsp", SERVICE_UNAVAILABLE_MESSAGE);
        }
    }

    private static void fail(HttpServletRequest req, HttpServletResponse resp,
                             String jsp, String message)
            throws ServletException, IOException {
        req.setAttribute("error", message);
        req.getRequestDispatcher("WEB-INF/views" + jsp).forward(req, resp);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}