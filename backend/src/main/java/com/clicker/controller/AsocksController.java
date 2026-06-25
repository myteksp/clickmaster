package com.clicker.controller;

import com.clicker.service.AsocksService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asocks")
public class AsocksController {

    private final AsocksService asocksService;

    public AsocksController(AsocksService asocksService) {
        this.asocksService = asocksService;
    }

    @GetMapping("/countries")
    public ResponseEntity<List<Map<String, String>>> getCountries() {
        return ResponseEntity.ok(asocksService.getCountries());
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance() {
        return ResponseEntity.ok(Map.of("balance", asocksService.getBalance()));
    }
}
