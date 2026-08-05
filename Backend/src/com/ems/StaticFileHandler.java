package com.ems;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.file.*;

public class StaticFileHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        System.out.println("📂 Requested: " + path);
        
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }
        
        if (path.contains("..")) {
            send404(exchange);
            return;
        }
        
        // Get the current working directory
        String cwd = System.getProperty("user.dir");
        System.out.println("📁 Current directory: " + cwd);
        
        // Try ALL possible locations
        String[] possiblePaths = {
            // For Render with our Dockerfile
            "/app/frontend" + path,
            "./frontend" + path,
            "../frontend" + path,
            "frontend" + path,
            "/app" + path,
            "." + path
        };
        
        File file = null;
        for (String filePath : possiblePaths) {
            File f = new File(filePath);
            System.out.println("🔍 Checking: " + filePath);
            if (f.exists() && !f.isDirectory()) {
                file = f;
                System.out.println("✅ FOUND: " + filePath);
                break;
            }
        }
        
        if (file == null) {
            System.out.println("❌ NOT FOUND: " + path);
            // List what IS in the current directory
            File dir = new File(".");
            String[] files = dir.list();
            System.out.println("📁 Files in current directory:");
            if (files != null) {
                for (String f : files) {
                    System.out.println("  - " + f);
                }
            }
            // Also check /app
            File appDir = new File("/app");
            if (appDir.exists()) {
                String[] appFiles = appDir.list();
                System.out.println("📁 Files in /app:");
                if (appFiles != null) {
                    for (String f : appFiles) {
                        System.out.println("  - " + f);
                    }
                }
            }
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