package com.spe.calculator.controller;

import com.spe.calculator.exception.InvalidOperationException;
import com.spe.calculator.model.CalculationResult;
import com.spe.calculator.service.CalculatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CalculatorController {

    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @GetMapping("/add")
    public CalculationResult add(@RequestParam double a, @RequestParam double b) {
        validate(a, b);
        return new CalculationResult("add", a, b, calculatorService.add(a, b));
    }

    @GetMapping("/subtract")
    public CalculationResult subtract(@RequestParam double a, @RequestParam double b) {
        validate(a, b);
        return new CalculationResult("subtract", a, b, calculatorService.subtract(a, b));
    }

    @GetMapping("/multiply")
    public CalculationResult multiply(@RequestParam double a, @RequestParam double b) {
        validate(a, b);
        return new CalculationResult("multiply", a, b, calculatorService.multiply(a, b));
    }

    @GetMapping("/divide")
    public CalculationResult divide(@RequestParam double a, @RequestParam double b) {
        validate(a, b);
        return new CalculationResult("divide", a, b, calculatorService.divide(a, b));
    }

    // Double parsing accepts "NaN" and "Infinity", which are not meaningful inputs
    private void validate(double a, double b) {
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            throw new InvalidOperationException("Parameters 'a' and 'b' must be finite numbers");
        }
    }
}
