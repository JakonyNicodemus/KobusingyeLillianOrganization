package com.ems;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class EmployeeStorage {
    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + "/employees.json";
    private static Long nextId = 1L;

    static {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            if (!Files.exists(Paths.get(FILE_PATH))) {
                Files.write(Paths.get(FILE_PATH), "[]".getBytes());
            }
            
            // Load and clean up on startup
            List<Employee> employees = loadAll();
            List<Employee> cleaned = new ArrayList<>();
            for (Employee emp : employees) {
                if (emp != null && 
                    emp.getId() != null && 
                    emp.getFirstName() != null && 
                    !emp.getFirstName().trim().isEmpty() &&
                    emp.getEmployeeId() != null && 
                    !emp.getEmployeeId().trim().isEmpty()) {
                    cleaned.add(emp);
                }
            }
            
            if (cleaned.size() != employees.size()) {
                saveAll(cleaned);
                System.out.println("🧹 Cleaned " + (employees.size() - cleaned.size()) + " invalid entries on startup");
            }
            
            nextId = cleaned.stream()
                .mapToLong(Employee::getId)
                .max()
                .orElse(0L) + 1;
                
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized List<Employee> loadAll() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(FILE_PATH)));
            if (content.trim().isEmpty() || content.equals("[]")) {
                return new ArrayList<>();
            }
            return parseJsonArray(content);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static synchronized void saveAll(List<Employee> employees) {
        try {
            String json = toJsonArray(employees);
            Files.write(Paths.get(FILE_PATH), json.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized Employee add(Employee employee) {
        // Validate employee before saving
        if (employee == null || 
            employee.getFirstName() == null || 
            employee.getFirstName().trim().isEmpty() ||
            employee.getEmployeeId() == null || 
            employee.getEmployeeId().trim().isEmpty()) {
            System.out.println("⚠️ Skipping invalid employee - missing required fields");
            return null;
        }
        
        List<Employee> employees = loadAll();
        
        // Check for duplicate employeeId
        for (Employee emp : employees) {
            if (emp.getEmployeeId() != null && 
                emp.getEmployeeId().equals(employee.getEmployeeId())) {
                System.out.println("⚠️ Employee with ID " + employee.getEmployeeId() + " already exists");
                return null;
            }
        }
        
        employee.setId(nextId++);
        employees.add(employee);
        saveAll(employees);
        System.out.println("✅ Employee added: " + employee.getFirstName() + " " + employee.getLastName());
        return employee;
    }

    public static synchronized Employee update(Long id, Employee updatedEmployee) {
        // Validate employee before updating
        if (updatedEmployee == null || 
            updatedEmployee.getFirstName() == null || 
            updatedEmployee.getFirstName().trim().isEmpty() ||
            updatedEmployee.getEmployeeId() == null || 
            updatedEmployee.getEmployeeId().trim().isEmpty()) {
            System.out.println("⚠️ Skipping invalid employee update - missing required fields");
            return null;
        }
        
        List<Employee> employees = loadAll();
        for (int i = 0; i < employees.size(); i++) {
            if (employees.get(i).getId().equals(id)) {
                updatedEmployee.setId(id);
                employees.set(i, updatedEmployee);
                saveAll(employees);
                System.out.println("✅ Employee updated: " + updatedEmployee.getFirstName());
                return updatedEmployee;
            }
        }
        return null;
    }

    public static synchronized boolean delete(Long id) {
        List<Employee> employees = loadAll();
        boolean removed = employees.removeIf(e -> e.getId().equals(id));
        if (removed) {
            saveAll(employees);
            System.out.println("🗑️ Deleted employee with ID: " + id);
        }
        return removed;
    }

    public static synchronized Employee findById(Long id) {
        return loadAll().stream()
            .filter(e -> e.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    public static synchronized List<Employee> findByDepartment(String department) {
        List<Employee> result = new ArrayList<>();
        for (Employee e : loadAll()) {
            if (e != null && department != null && department.equals(e.getDepartment())) {
                result.add(e);
            }
        }
        return result;
    }
    
    public static synchronized void cleanNullEntries() {
        List<Employee> employees = loadAll();
        List<Employee> cleaned = new ArrayList<>();
        
        for (Employee emp : employees) {
            if (emp != null && 
                emp.getId() != null && 
                emp.getFirstName() != null && 
                !emp.getFirstName().trim().isEmpty()) {
                cleaned.add(emp);
            }
        }
        
        if (cleaned.size() != employees.size()) {
            saveAll(cleaned);
            System.out.println("🧹 Cleaned " + (employees.size() - cleaned.size()) + " invalid entries");
        }
    }

    private static List<Employee> parseJsonArray(String json) {
        List<Employee> employees = new ArrayList<>();
        if (json.trim().equals("[]")) return employees;
        
        json = json.trim();
        json = json.substring(1, json.length() - 1);
        
        // Handle empty or malformed JSON
        if (json.trim().isEmpty()) return employees;
        
        String[] objects = splitJsonObjects(json);
        for (String obj : objects) {
            if (obj == null || obj.trim().isEmpty()) continue;
            Employee emp = parseJsonObject(obj);
            if (emp != null && emp.getId() != null) {
                employees.add(emp);
            }
        }
        return employees;
    }

    private static String[] splitJsonObjects(String json) {
        List<String> objects = new ArrayList<>();
        int braceCount = 0;
        StringBuilder current = new StringBuilder();
        
        for (char c : json.toCharArray()) {
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;
            current.append(c);
            if (braceCount == 0 && current.length() > 0) {
                String obj = current.toString().trim();
                if (!obj.isEmpty()) {
                    objects.add(obj);
                }
                current = new StringBuilder();
            }
        }
        
        return objects.toArray(new String[0]);
    }

    private static Employee parseJsonObject(String json) {
        try {
            Employee emp = new Employee();
            json = json.replace("{", "").replace("}", "").trim();
            
            if (json.isEmpty()) return null;
            
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                if (pair == null || pair.trim().isEmpty()) continue;
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String value = kv[1].trim().replace("\"", "");
                    
                    switch (key) {
                        case "id": 
                            if (value != null && !value.isEmpty()) {
                                emp.setId(Long.parseLong(value)); 
                            }
                            break;
                        case "firstName": emp.setFirstName(value); break;
                        case "lastName": emp.setLastName(value); break;
                        case "email": emp.setEmail(value); break;
                        case "employeeId": emp.setEmployeeId(value); break;
                        case "department": emp.setDepartment(value); break;
                        case "position": emp.setPosition(value); break;
                        case "hireDate": emp.setHireDate(value); break;
                        case "salary": 
                            if (value != null && !value.isEmpty()) {
                                emp.setSalary(Double.parseDouble(value)); 
                            }
                            break;
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
            
            // Only return if it has a valid ID and name
            if (emp.getId() != null && 
                emp.getFirstName() != null && 
                !emp.getFirstName().trim().isEmpty()) {
                return emp;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String toJsonArray(List<Employee> employees) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < employees.size(); i++) {
            if (i > 0) json.append(",");
            json.append(toJsonObject(employees.get(i)));
        }
        json.append("]");
        return json.toString();
    }

    private static String toJsonObject(Employee emp) {
        if (emp == null) return "{}";
        
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
            emp.getId() != null ? emp.getId() : 0,
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

    private static String escape(String s) {
        return s != null ? s.replace("\"", "\\\"") : "";
    }
}