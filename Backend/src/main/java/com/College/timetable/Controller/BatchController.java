package com.College.timetable.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.College.timetable.Entity.Batch;
import com.College.timetable.Entity.Division;
import com.College.timetable.Repository.Batch_repo;
import com.College.timetable.Repository.Division_repo;
import com.College.timetable.Service.DepartmentAuthorizationService;
import com.College.timetable.Service.BatchValidationService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/batches")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
public class BatchController {

    @Autowired
    private Batch_repo batchRepository;

    @Autowired
    private Division_repo divisionRepository;

    @Autowired
    private DepartmentAuthorizationService authService;

    @Autowired
    private BatchValidationService batchValidationService;

    @GetMapping
    public ResponseEntity<List<Batch>> getAll() {
        List<Batch> all = batchRepository.findAll();
        if (authService.isInstitutionalAdmin()) {
            return ResponseEntity.ok(all);
        }
        Long deptId = authService.getCurrentUser().getDepartment().getId();
        List<Batch> filtered = all.stream()
            .filter(b -> b.getDivision() != null && b.getDivision().getDepartment() != null && deptId.equals(b.getDivision().getDepartment().getId()))
            .toList();
        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Batch> getById(@PathVariable Long id) {
        authService.checkBatchAccess(id);
        Batch batch = batchRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Batch not found"));
        return ResponseEntity.ok(batch);
    }

    @GetMapping("/division/{divisionId}")
    public ResponseEntity<List<Batch>> getByDivision(@PathVariable Long divisionId) {
        authService.checkDivisionAccess(divisionId);
        return ResponseEntity.ok(batchRepository.findByDivisionId(divisionId));
    }

    @GetMapping("/division/{divisionId}/validation")
    public ResponseEntity<Map<String, Object>> validateDivisionBatches(@PathVariable Long divisionId) {
        authService.checkDivisionAccess(divisionId);
        return ResponseEntity.ok(batchValidationService.validateBatchStrengths(divisionId));
    }

    @GetMapping("/division/{divisionId}/suggested-strength")
    public ResponseEntity<Map<String, Integer>> getSuggestedStrength(@PathVariable Long divisionId) {
        authService.checkDivisionAccess(divisionId);
        int suggested = batchValidationService.calculateSuggestedStrength(divisionId);
        return ResponseEntity.ok(Map.of("suggestedStrength", suggested));
    }

    @PostMapping
    public ResponseEntity<Batch> create(@Valid @RequestBody Batch batch) {
        if (batch.getDivision() != null && batch.getDivision().getId() != null) {
            authService.checkDivisionAccess(batch.getDivision().getId());
            Division division = divisionRepository.findById(batch.getDivision().getId())
                .orElseThrow(() -> new EntityNotFoundException("Division not found"));
            batch.setDivision(division);
        }
        
        // Validate strength is provided
        if (batch.getStrength() == null || batch.getStrength() < 0) {
            throw new IllegalArgumentException("Batch strength is required and must be non-negative");
        }
        
        return ResponseEntity.ok(batchRepository.save(batch));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Batch> update(@PathVariable Long id, @Valid @RequestBody Batch batch) {
        authService.checkBatchAccess(id);
        Batch existing = batchRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Batch not found"));
        
        existing.setName(batch.getName());
        
        // Update strength if provided
        if (batch.getStrength() != null) {
            if (batch.getStrength() < 0) {
                throw new IllegalArgumentException("Batch strength must be non-negative");
            }
            existing.setStrength(batch.getStrength());
        }
        
        if (batch.getDivision() != null && batch.getDivision().getId() != null) {
            authService.checkDivisionAccess(batch.getDivision().getId());
            Division division = divisionRepository.findById(batch.getDivision().getId())
                .orElseThrow(() -> new EntityNotFoundException("Division not found"));
            existing.setDivision(division);
        }
        
        return ResponseEntity.ok(batchRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authService.checkBatchAccess(id);
        if (!batchRepository.existsById(id)) {
            throw new EntityNotFoundException("Batch not found");
        }
        batchRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
