package com.ems;

import java.util.*;

public class PayrollCalculator {
    
    // Uganda Tax Brackets 2024
    private static final double TAX_FREE_THRESHOLD = 235000;
    private static final double[][] TAX_BRACKETS = {
        {235000, 0.0},
        {335000, 0.10},
        {410000, 0.20},
        {10000000, 0.30}
    };
    
    // NSSF Rates
    private static final double NSSF_EMPLOYEE_RATE = 0.05;
    private static final double NSSF_EMPLOYER_RATE = 0.10;
    private static final double NSSF_CAP = 6000000;

    public static PayrollResult calculatePayroll(Employee employee) {
        double salary = employee.getSalary() != null ? employee.getSalary() : 0;
        double nssfEmployee = calculateNSSF(salary, true);
        double nssfEmployer = calculateNSSF(salary, false);
        double taxableIncome = salary - nssfEmployee;
        double paye = calculatePAYE(taxableIncome);
        double netPay = salary - nssfEmployee - paye;
        
        PayrollResult result = new PayrollResult();
        result.employee = employee;
        result.grossSalary = salary;
        result.nssfEmployee = nssfEmployee;
        result.nssfEmployer = nssfEmployer;
        result.paye = paye;
        result.netPay = netPay;
        result.taxableIncome = taxableIncome;
        
        return result;
    }

    private static double calculateNSSF(double salary, boolean isEmployee) {
        double maxSalary = Math.min(salary, NSSF_CAP);
        double rate = isEmployee ? NSSF_EMPLOYEE_RATE : NSSF_EMPLOYER_RATE;
        return Math.round(maxSalary * rate * 100.0) / 100.0;
    }

    private static double calculatePAYE(double taxableIncome) {
        if (taxableIncome <= TAX_FREE_THRESHOLD) {
            return 0;
        }
        
        double remaining = taxableIncome;
        double tax = 0;
        double previousThreshold = TAX_FREE_THRESHOLD;
        
        for (double[] bracket : TAX_BRACKETS) {
            double threshold = bracket[0];
            double rate = bracket[1];
            
            if (remaining > 0) {
                double taxableAmount = Math.min(remaining, threshold - previousThreshold);
                tax += taxableAmount * rate;
                remaining -= taxableAmount;
                previousThreshold = threshold;
            }
        }
        
        return Math.round(tax * 100.0) / 100.0;
    }

    public static class PayrollResult {
        public Employee employee;
        public double grossSalary;
        public double nssfEmployee;
        public double nssfEmployer;
        public double paye;
        public double netPay;
        public double taxableIncome;
    }

    public static Map<String, Object> getPayrollSummary(List<Employee> employees) {
        Map<String, Object> summary = new HashMap<>();
        double totalPayroll = 0;
        double totalNSSF = 0;
        double totalPAYE = 0;
        
        for (Employee emp : employees) {
            PayrollResult result = calculatePayroll(emp);
            totalPayroll += result.netPay;
            totalNSSF += result.nssfEmployee + result.nssfEmployer;
            totalPAYE += result.paye;
        }
        
        summary.put("totalPayroll", totalPayroll);
        summary.put("totalNSSF", totalNSSF);
        summary.put("totalPAYE", totalPAYE);
        summary.put("employeeCount", employees.size());
        summary.put("averageSalary", employees.size() > 0 ? totalPayroll / employees.size() : 0);
        
        return summary;
    }

    public static Map<String, Object> getNSSFSummary(List<Employee> employees) {
        Map<String, Object> summary = new HashMap<>();
        int activeContributors = 0;
        double totalEmployeeContrib = 0;
        double totalEmployerContrib = 0;
        
        for (Employee emp : employees) {
            if ("ACTIVE".equals(emp.getNssfStatus()) && emp.getSalary() != null) {
                activeContributors++;
                PayrollResult result = calculatePayroll(emp);
                totalEmployeeContrib += result.nssfEmployee;
                totalEmployerContrib += result.nssfEmployer;
            }
        }
        
        summary.put("activeContributors", activeContributors);
        summary.put("totalEmployeeContrib", totalEmployeeContrib);
        summary.put("totalEmployerContrib", totalEmployerContrib);
        summary.put("totalNSSF", totalEmployeeContrib + totalEmployerContrib);
        
        return summary;
    }
}
