package com.ems;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.file.*;

public class StaticFileHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        System.out.println("📂 Requested path: " + path);
        
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }
        
        if (path.contains("..")) {
            send404(exchange);
            return;
        }
        
        // For Render, the files are in /app/frontend/
        // For local, they are in ../frontend/
        String[] possiblePaths = {
            "./frontend" + path,      // Render: /app/frontend/index.html
            "../frontend" + path,     // Local: from Backend folder
            "frontend" + path,        // Alternative
            "." + path                // Fallback
        };
        
        File file = null;
        for (String filePath : possiblePaths) {
            File f = new File(filePath);
            if (f.exists() && !f.isDirectory()) {
                file = f;
                System.out.println("✅ Found file at: " + filePath);
                break;
            } else {
                System.out.println("❌ Not found: " + filePath);
            }
        }
        
        if (file == null) {
            System.out.println("🚫 File not found: " + path);
            send404(exchange);
            return;
        }
        
        String contentType = getContentType(path);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, file.length());
        
        try (OutputStream os = exchange.getResponseBody();
             FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }
    
    private void send404(HttpExchange exchange) throws IOException {
        String response = "404 Not Found - File not found";
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(404, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    
    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        if (path.endsWith(".json")) return "application/json";
        return "text/plain";
    }
}