package com.ems;

import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {
        // Print current directory for debugging
        String cwd = System.getProperty("user.dir");
        System.out.println("📁 Working directory: " + cwd);
        
        // List files in current directory
        File dir = new File(".");
        String[] files = dir.list();
        System.out.println("📁 Files in current directory:");
        if (files != null) {
            for (String f : files) {
                System.out.println("  - " + f);
            }
        }
        
        // Check if frontend folder exists
        File frontendDir = new File("./frontend");
        if (frontendDir.exists() && frontendDir.isDirectory()) {
            System.out.println("✅ frontend folder exists!");
            String[] frontendFiles = frontendDir.list();
            if (frontendFiles != null) {
                for (String f : frontendFiles) {
                    System.out.println("  📄 frontend/" + f);
                }
            }
        } else {
            System.out.println("❌ frontend folder NOT found!");
            // Try /app/frontend
            File appFrontend = new File("/app/frontend");
            if (appFrontend.exists() && appFrontend.isDirectory()) {
                System.out.println("✅ /app/frontend exists!");
                String[] appFiles = appFrontend.list();
                if (appFiles != null) {
                    for (String f : appFiles) {
                        System.out.println("  📄 /app/frontend/" + f);
                    }
                }
            }
        }
        
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        EmployeeController controller = new EmployeeController();
        
        server.createContext("/api", controller);
        server.createContext("/", new StaticFileHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║    🇺🇬 UGANDA EMPLOYEE MANAGEMENT SYSTEM                     ║");
        System.out.println("║                                                              ║");
        System.out.println("║    ✅ Server Started Successfully!                           ║");
        System.out.println("║    📡 API: http://localhost:8080/api/employees              ║");
        System.out.println("║    🌐 Frontend: http://localhost:8080                       ║");
        System.out.println("║    💾 Data: data/employees.json                             ║");
        System.out.println("║    💰 Currency: UGX (Ugandan Shilling)                      ║");
        System.out.println("║    📋 NSSF: 5% Employee, 10% Employer                       ║");
        System.out.println("║                                                              ║");
        System.out.println("║    Press Ctrl+C to stop                                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
    }
}