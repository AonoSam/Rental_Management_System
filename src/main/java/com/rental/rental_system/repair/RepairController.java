package com.rental.rental_system.repair;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repairs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RepairController {

    private final RepairService repairService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        return ResponseEntity.ok(repairService.getAllRepairs());
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<Map<String, Object>>> getByProperty(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(
                repairService.getRepairsByProperty(propertyId));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(repairService.createRepair(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(repairService.updateRepair(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Long id) {
        repairService.deleteRepair(id);
        return ResponseEntity.ok(Map.of("message", "Repair log deleted"));
    }

    @GetMapping("/expenses/summary")
    public ResponseEntity<Map<String, Object>> getExpenseSummary() {
        return ResponseEntity.ok(repairService.getExpenseSummary());
    }
}