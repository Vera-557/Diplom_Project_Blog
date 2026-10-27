package com.top511.vera.blog.servlet;

import com.top511.vera.blog.util.AppConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@WebServlet(name = "UploadServlet", urlPatterns = "/uploads/*")
public class UploadServlet extends HttpServlet {

    private Path uploadDir;

    @Override
    public void init() throws ServletException {
        uploadDir = AppConfig.getUploadDir();
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new ServletException(
                    "Не удалось создать каталог загрузок: " + uploadDir, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String fileName = pathInfo.substring(1);


        if (fileName.contains("/") || fileName.contains("\\")
                || fileName.contains("..") || fileName.indexOf('\0') >= 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }


        Path file = uploadDir.resolve(fileName).normalize();
        if (!file.startsWith(uploadDir)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }


        String contentType = contentTypeFor(fileName);
        if (contentType != null) {
            response.setContentType(contentType);
        }


        response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        response.setContentLengthLong(Files.size(file));

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(file, out);
        }
    }

    private static String contentTypeFor(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".png"))  return "image/png";
        if (n.endsWith(".gif"))  return "image/gif";
        if (n.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}