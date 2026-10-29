package com.spe.calculator.service;

import com.spe.calculator.exception.InvalidOperationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorServiceTest {

    private final CalculatorService calculator = new CalculatorService();

    @Test
    void addReturnsSum() {
        assertEquals(5, calculator.add(2, 3));
        assertEquals(-1, calculator.add(2, -3));
        assertEquals(0.3, calculator.add(0.1, 0.2), 1e-9);
    }

    @Test
    void subtractReturnsDifference() {
        assertEquals(-1, calculator.subtract(2, 3));
        assertEquals(5, calculator.subtract(2, -3));
        assertEquals(0, calculator.subtract(7.5, 7.5));
    }

    @Test
    void multiplyReturnsProduct() {
        assertEquals(6, calculator.multiply(2, 3));
        assertEquals(-6, calculator.multiply(2, -3));
        assertEquals(0, calculator.multiply(123.45, 0));
    }

    @Test
    void divideReturnsQuotient() {
        assertEquals(2, calculator.divide(6, 3));
        assertEquals(2.5, calculator.divide(5, 2));
        assertEquals(-0.5, calculator.divide(1, -2));
    }

    @Test
    @DisplayName("divide by zero throws InvalidOperationException")
    void divideByZeroThrows() {
        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> calculator.divide(1, 0));
        assertEquals("Division by zero is not allowed", ex.getMessage());
    }

    @Test
    void overflowThrows() {
        assertThrows(InvalidOperationException.class, () -> calculator.multiply(Double.MAX_VALUE, 2));
        assertThrows(InvalidOperationException.class, () -> calculator.add(Double.MAX_VALUE, Double.MAX_VALUE));
    }
}
