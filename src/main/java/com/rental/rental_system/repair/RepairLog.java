package com.rental.rental_system.repair;

import com.rental.rental_system.maintenance.MaintenanceRequest;
import com.rental.rental_system.property.Property;
import com.rental.rental_system.property.Unit;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "repair_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_request_id")
    private MaintenanceRequest maintenanceRequest;

    @Column(nullable = false)
    private String description;

    @Column(name = "repaired_by")
    private String repairedBy; // technician/contractor name

    @Column(name = "repair_cost", nullable = false)
    private BigDecimal repairCost;

    @Column(name = "repair_date", nullable = false)
    private LocalDate repairDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepairCategory category;

    private String notes;

    @Column(name = "receipt_number")
    private String receiptNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}