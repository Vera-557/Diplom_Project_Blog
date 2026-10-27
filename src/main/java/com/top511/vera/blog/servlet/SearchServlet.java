package com.top511.vera.blog.servlet;

import com.top511.vera.blog.exception.DataAccessException;
import com.top511.vera.blog.model.User;
import com.top511.vera.blog.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "SearchServlet", urlPatterns = "/search")
public class SearchServlet extends HttpServlet {

    private static final int MAX_RESULTS = 30;
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
        request.setCharacterEncoding("UTF-8");

        User current = currentUser(request);
        if (current == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String query = request.getParameter("q");

        List<User> found;
        try {
            found = (query == null || query.isBlank())
                    ? List.of()
                    : userRepository.searchByBlogTitle(query, MAX_RESULTS);
        } catch (DataAccessException e) {
            getServletContext().log("Ошибка БД при поиске блогов", e);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    SERVICE_UNAVAILABLE_MESSAGE);
            return;
        }

        request.setAttribute("user", current);
        request.setAttribute("query", query);
        request.setAttribute("foundBlogs", found);
        request.getRequestDispatcher("WEB-INF/views/search.jsp")
                .forward(request, response);
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }
}