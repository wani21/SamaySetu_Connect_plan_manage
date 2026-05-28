package com.College.timetable.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.College.timetable.Entity.Batch;
import com.College.timetable.Entity.Division;
import com.College.timetable.Repository.Batch_repo;
import com.College.timetable.Repository.Division_repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class BatchValidationService {

    @Autowired
    private Batch_repo batchRepository;

    @Autowired
    private Division_repo divisionRepository;

    /**
     * Validates batch strengths for a division
     * Returns validation summary with status
     */
    public Map<String, Object> validateBatchStrengths(Long divisionId) {
        Division division = divisionRepository.findById(divisionId)
            .orElseThrow(() -> new EntityNotFoundException("Division not found"));

        List<Batch> batches = batchRepository.findByDivisionId(divisionId);

        int divisionStrength = division.getTotalStudents() != null ? division.getTotalStudents() : 0;
        int totalBatchStrength = batches.stream()
            .mapToInt(b -> b.getStrength() != null ? b.getStrength() : 0)
            .sum();
        int unallocated = divisionStrength - totalBatchStrength;

        Map<String, Object> result = new HashMap<>();
        result.put("divisionId", divisionId);
        result.put("divisionName", division.getName());
        result.put("divisionStrength", divisionStrength);
        result.put("batchCount", batches.size());
        result.put("totalBatchStrength", totalBatchStrength);
        result.put("unallocatedStudents", unallocated);
        result.put("isValid", unallocated == 0);
        result.put("status", getValidationStatus(unallocated, totalBatchStrength, divisionStrength));

        return result;
    }

    /**
     * Calculate suggested strength for a new batch
     */
    public int calculateSuggestedStrength(Long divisionId) {
        Division division = divisionRepository.findById(divisionId)
            .orElseThrow(() -> new EntityNotFoundException("Division not found"));

        List<Batch> batches = batchRepository.findByDivisionId(divisionId);

        int divisionStrength = division.getTotalStudents() != null ? division.getTotalStudents() : 0;
        int totalBatchStrength = batches.stream()
            .mapToInt(b -> b.getStrength() != null ? b.getStrength() : 0)
            .sum();
        int unallocated = divisionStrength - totalBatchStrength;

        // If there's unallocated students, suggest that
        if (unallocated > 0) {
            return unallocated;
        }

        // Otherwise, suggest average batch size
        if (batches.isEmpty()) {
            return divisionStrength > 0 ? divisionStrength / 2 : 0; // Assume 2 batches by default
        }

        int avgBatchSize = batches.stream()
            .mapToInt(b -> b.getStrength() != null ? b.getStrength() : 0)
            .filter(s -> s > 0)
            .sum() / (int) batches.stream().filter(b -> b.getStrength() != null && b.getStrength() > 0).count();

        return avgBatchSize > 0 ? avgBatchSize : 20; // Default to 20 if no data
    }

    /**
     * Auto-adjust last batch strength when division strength changes
     */
    public void autoAdjustLastBatchStrength(Long divisionId) {
        Division division = divisionRepository.findById(divisionId)
            .orElseThrow(() -> new EntityNotFoundException("Division not found"));

        List<Batch> batches = batchRepository.findByDivisionId(divisionId);

        if (batches.isEmpty()) {
            return; // No batches to adjust
        }

        int divisionStrength = division.getTotalStudents() != null ? division.getTotalStudents() : 0;

        // Calculate total strength of all batches except the last one
        Batch lastBatch = batches.get(batches.size() - 1);
        int otherBatchesStrength = batches.stream()
            .filter(b -> !b.getId().equals(lastBatch.getId()))
            .mapToInt(b -> b.getStrength() != null ? b.getStrength() : 0)
            .sum();

        // Set last batch strength to remaining students
        int remainingStrength = divisionStrength - otherBatchesStrength;
        
        // Only adjust if the remaining strength is non-negative
        if (remainingStrength >= 0) {
            lastBatch.setStrength(remainingStrength);
            batchRepository.save(lastBatch);
        }
    }

    private String getValidationStatus(int unallocated, int totalBatchStrength, int divisionStrength) {
        if (unallocated == 0) {
            return "PERFECT"; // Green
        } else if (unallocated > 0) {
            return "UNDER_ALLOCATED"; // Yellow - some students not assigned to batches
        } else {
            return "OVER_ALLOCATED"; // Red - batch total exceeds division strength
        }
    }
}
