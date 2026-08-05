package com.ems;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        EmployeeController controller = new EmployeeController();
        
        // FIXED: Just use "/api" and let the controller handle all routes
        server.createContext("/api", controller);
        server.createContext("/", new StaticFileHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║    🇺🇬 UGANDA EMPLOYEE MANAGEMENT SYSTEM                     ║");
        System.out.println("║                                                              ║");
        System.out.println("║    ✅ Server Started Successfully!                           ║");
        System.out.println("║    📡 API: http://localhost:8080/api/employees              ║");
        System.out.println("║    📡 API: http://localhost:8080/api/payroll                ║");
        System.out.println("║    📡 API: http://localhost:8080/api/nssf                   ║");
        System.out.println("║    📡 API: http://localhost:8080/api/ura                    ║");
        System.out.println("║    🌐 Frontend: http://localhost:8080                       ║");
        System.out.println("║    💾 Data: data/employees.json                             ║");
        System.out.println("║    💰 Currency: UGX (Ugandan Shilling)                      ║");
        System.out.println("║    📋 NSSF: 5% Employee, 10% Employer                       ║");
        System.out.println("║                                                              ║");
        System.out.println("║    Press Ctrl+C to stop                                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
    }
}