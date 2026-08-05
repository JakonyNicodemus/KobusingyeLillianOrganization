package com.ems;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EmployeeController implements HttpHandler {
    
    // FIXED: Changed from handleRequest to handle
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String response = "";
        int statusCode = 200;
        
        // Handle CORS preflight
        if (method.equals("OPTIONS")) {
            sendResponse(exchange, 204, "");
            return;
        }
        
        try {
            // Check if it's a payroll, nssf, or ura request
            if (path.startsWith("/api/payroll")) {
                handlePayroll(exchange);
                return;
            } else if (path.startsWith("/api/nssf")) {
                handleNSSF(exchange);
                return;
            } else if (path.startsWith("/api/ura")) {
                handleURA(exchange);
                return;
            }
            
            // Parse ID from path for employee operations
            Long id = null;
            String[] parts = path.split("/");
            if (parts.length > 3) {
                try {
                    id = Long.parseLong(parts[3]);
                } catch (NumberFormatException e) {
                    // Not an ID, ignore
                }
            }
            
            switch (method) {
                case "GET":
                    if (id != null) {
                        response = getEmployee(id);
                    } else {
                        response = getAllEmployees();
                    }
                    break;
                    
                case "POST":
                    String body = getRequestBody(exchange);
                    response = createEmployee(body);
                    statusCode = 201;
                    break;
                    
                case "PUT":
                    if (id != null) {
                        body = getRequestBody(exchange);
                        response = updateEmployee(id, body);
                    } else {
                        statusCode = 400;
                        response = "{\"error\":\"ID required\"}";
                    }
                    break;
                    
                case "DELETE":
                    if (id != null) {
                        response = deleteEmployee(id);
                    } else {
                        statusCode = 400;
                        response = "{\"error\":\"ID required\"}";
                    }
                    break;
                    
                default:
                    statusCode = 405;
                    response = "{\"error\":\"Method not allowed\"}";
            }
        } catch (Exception e) {
            statusCode = 500;
            response = "{\"error\":\"" + e.getMessage() + "\"}";
        }
        
        sendResponse(exchange, statusCode, response);
    }

    // Payroll Handler
    private void handlePayroll(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        
        List<Employee> employees = EmployeeStorage.loadAll();
        Map<String, Object> summary = PayrollCalculator.getPayrollSummary(employees);
        List<Map<String, Object>> payrollDetails = new ArrayList<>();
        
        for (Employee emp : employees) {
            PayrollCalculator.PayrollResult result = PayrollCalculator.calculatePayroll(emp);
            Map<String, Object> detail = new HashMap<>();
            detail.put("employee", emp.getFirstName() + " " + emp.getLastName());
            detail.put("department", emp.getDepartment());
            detail.put("grossSalary", result.grossSalary);
            detail.put("nssfEmployee", result.nssfEmployee);
            detail.put("nssfEmployer", result.nssfEmployer);
            detail.put("paye", result.paye);
            detail.put("netPay", result.netPay);
            payrollDetails.add(detail);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("summary", summary);
        response.put("details", payrollDetails);
        
        sendResponse(exchange, 200, toJson(response));
    }

    // NSSF Handler
    private void handleNSSF(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        
        List<Employee> employees = EmployeeStorage.loadAll();
        Map<String, Object> nssfData = PayrollCalculator.getNSSFSummary(employees);
        List<Map<String, Object>> nssfDetails = new ArrayList<>();
        
        for (Employee emp : employees) {
            if ("ACTIVE".equals(emp.getNssfStatus()) && emp.getSalary() != null) {
                PayrollCalculator.PayrollResult result = PayrollCalculator.calculatePayroll(emp);
                Map<String, Object> detail = new HashMap<>();
                detail.put("employee", emp.getFirstName() + " " + emp.getLastName());
                detail.put("nssfNumber", emp.getNssfNumber());
                detail.put("salary", result.grossSalary);
                detail.put("employeeContrib", result.nssfEmployee);
                detail.put("employerContrib", result.nssfEmployer);
                detail.put("totalContrib", result.nssfEmployee + result.nssfEmployer);
                nssfDetails.add(detail);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("summary", nssfData);
        response.put("details", nssfDetails);
        
        sendResponse(exchange, 200, toJson(response));
    }

    // URA Handler
    private void handleURA(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        
        List<Employee> employees = EmployeeStorage.loadAll();
        List<Map<String, Object>> taxDetails = new ArrayList<>();
        double totalPAYE = 0;
        
        for (Employee emp : employees) {
            PayrollCalculator.PayrollResult result = PayrollCalculator.calculatePayroll(emp);
            Map<String, Object> detail = new HashMap<>();
            detail.put("employee", emp.getFirstName() + " " + emp.getLastName());
            detail.put("tinNumber", emp.getTinNumber());
            detail.put("grossSalary", result.grossSalary);
            detail.put("nssfDeduction", result.nssfEmployee);
            detail.put("taxableIncome", result.taxableIncome);
            detail.put("paye", result.paye);
            detail.put("netPay", result.netPay);
            taxDetails.add(detail);
            totalPAYE += result.paye;
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("details", taxDetails);
        response.put("totalPAYE", totalPAYE);
        
        sendResponse(exchange, 200, toJson(response));
    }

    // CRUD Operations
    private String getAllEmployees() {
        List<Employee> employees = EmployeeStorage.loadAll();
        return toJsonArray(employees);
    }
    
    private String getEmployee(Long id) {
        Employee emp = EmployeeStorage.findById(id);
        if (emp == null) {
            return "{\"error\":\"Employee not found\"}";
        }
        return toJsonObject(emp);
    }
    
    private String createEmployee(String jsonBody) {
        Employee emp = parseEmployee(jsonBody);
        if (emp == null) {
            return "{\"error\":\"Invalid employee data\"}";
        }
        Employee saved = EmployeeStorage.add(emp);
        return toJsonObject(saved);
    }
    
    private String updateEmployee(Long id, String jsonBody) {
        Employee emp = parseEmployee(jsonBody);
        if (emp == null) {
            return "{\"error\":\"Invalid employee data\"}";
        }
        Employee updated = EmployeeStorage.update(id, emp);
        if (updated == null) {
            return "{\"error\":\"Employee not found\"}";
        }
        return toJsonObject(updated);
    }
    
    private String deleteEmployee(Long id) {
        boolean deleted = EmployeeStorage.delete(id);
        if (!deleted) {
            return "{\"error\":\"Employee not found\"}";
        }
        return "{\"message\":\"Employee deleted successfully\"}";
    }

    private Employee parseEmployee(String json) {
        try {
            Employee emp = new Employee();
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1);
                String[] pairs = json.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().replace("\"", "");
                        String value = kv[1].trim().replace("\"", "");
                        
                        switch (key) {
                            case "firstName": emp.setFirstName(value); break;
                            case "lastName": emp.setLastName(value); break;
                            case "email": emp.setEmail(value); break;
                            case "employeeId": emp.setEmployeeId(value); break;
                            case "department": emp.setDepartment(value); break;
                            case "position": emp.setPosition(value); break;
                            case "hireDate": emp.setHireDate(value); break;
                            case "salary": emp.setSalary(Double.parseDouble(value)); break;
                            case "status": emp.setStatus(value); break;
                            case "phoneNumber": emp.setPhoneNumber(value); break;
                            case "address": emp.setAddress(value); break;
                            case "dateOfBirth": emp.setDateOfBirth(value); break;
                            case "gender": emp.setGender(value); break;
                            case "nssfNumber": emp.setNssfNumber(value); break;
                            case "tinNumber": emp.setTinNumber(value); break;
                            case "nationalId": emp.setNationalId(value); break;
                            case "district": emp.setDistrict(value); break;
                            case "nationality": emp.setNationality(value); break;
                            case "nssfStatus": emp.setNssfStatus(value); break;
                            case "emergencyContact": emp.setEmergencyContact(value); break;
                        }
                    }
                }
            }
            return emp;
        } catch (Exception e) {
            return null;
        }
    }

    private String toJsonObject(Employee emp) {
        return String.format(
            "{"
            + "\"id\":%d,"
            + "\"firstName\":\"%s\","
            + "\"lastName\":\"%s\","
            + "\"email\":\"%s\","
            + "\"employeeId\":\"%s\","
            + "\"department\":\"%s\","
            + "\"position\":\"%s\","
            + "\"hireDate\":\"%s\","
            + "\"salary\":%f,"
            + "\"status\":\"%s\","
            + "\"phoneNumber\":\"%s\","
            + "\"address\":\"%s\","
            + "\"dateOfBirth\":\"%s\","
            + "\"gender\":\"%s\","
            + "\"nssfNumber\":\"%s\","
            + "\"tinNumber\":\"%s\","
            + "\"nationalId\":\"%s\","
            + "\"district\":\"%s\","
            + "\"nationality\":\"%s\","
            + "\"nssfStatus\":\"%s\","
            + "\"emergencyContact\":\"%s\""
            + "}",
            emp.getId(),
            escape(emp.getFirstName()),
            escape(emp.getLastName()),
            escape(emp.getEmail()),
            escape(emp.getEmployeeId()),
            escape(emp.getDepartment()),
            escape(emp.getPosition()),
            escape(emp.getHireDate()),
            emp.getSalary() != null ? emp.getSalary() : 0.0,
            escape(emp.getStatus()),
            escape(emp.getPhoneNumber()),
            escape(emp.getAddress()),
            escape(emp.getDateOfBirth()),
            escape(emp.getGender()),
            escape(emp.getNssfNumber()),
            escape(emp.getTinNumber()),
            escape(emp.getNationalId()),
            escape(emp.getDistrict()),
            escape(emp.getNationality()),
            escape(emp.getNssfStatus()),
            escape(emp.getEmergencyContact())
        );
    }

    private String toJsonArray(List<Employee> employees) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < employees.size(); i++) {
            if (i > 0) json.append(",");
            json.append(toJsonObject(employees.get(i)));
        }
        json.append("]");
        return json.toString();
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (count > 0) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escape((String)value)).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else if (value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof List) {
                json.append("[]");
            } else {
                json.append("\"").append(value).append("\"");
            }
            count++;
        }
        json.append("}");
        return json.toString();
    }

    private String escape(String s) {
        return s != null ? s.replace("\"", "\\\"") : "";
    }

    private String getRequestBody(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        // CORS headers
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}