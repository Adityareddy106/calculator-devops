package com.spe.calculator.service;

import com.spe.calculator.exception.InvalidOperationException;
import org.springframework.stereotype.Service;

@Service
public class CalculatorService {

    public double add(double a, double b) {
        return checkFinite(a + b);
    }

    public double subtract(double a, double b) {
        return checkFinite(a - b);
    }

    public double multiply(double a, double b) {
        return checkFinite(a * b);
    }

    public double divide(double a, double b) {
        if (b == 0) {
            throw new InvalidOperationException("Division by zero is not allowed");
        }
        return checkFinite(a / b);
    }

    // Overflow produces Infinity, which cannot be represented in valid JSON
    private double checkFinite(double result) {
        if (!Double.isFinite(result)) {
            throw new InvalidOperationException("Result is out of range");
        }
        return result;
    }
}
