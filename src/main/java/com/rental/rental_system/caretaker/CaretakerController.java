package com.rental.rental_system.caretaker;

import com.rental.rental_system.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/caretakers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CaretakerController {

    private final CaretakerService caretakerService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        return ResponseEntity.ok(caretakerService.getAllCaretakers());
    }

    @PutMapping("/{caretakerId}/assign/{propertyId}")
    public ResponseEntity<Map<String, Object>> assign(
            @PathVariable Long caretakerId,
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(
                caretakerService.assignCaretaker(caretakerId, propertyId));
    }

    @PutMapping("/{caretakerId}/unassign")
    public ResponseEntity<Map<String, Object>> unassign(
            @PathVariable Long caretakerId) {
        return ResponseEntity.ok(
                caretakerService.unassignCaretaker(caretakerId));
    }

    @GetMapping("/my-property")
    public ResponseEntity<Map<String, Object>> getMyProperty(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                caretakerService.getMyProperty(currentUser.getId()));
    }

    @GetMapping("/my-units")
    public ResponseEntity<List<Map<String, Object>>> getMyUnits(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                caretakerService.getMyUnits(currentUser.getId()));
    }
    @GetMapping("/my-tenants")
    public ResponseEntity<List<Map<String, Object>>> getMyTenants(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                caretakerService.getMyTenants(currentUser.getId()));
    }
}