package com.College.timetable.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.Batch;

@Repository
public interface Batch_repo extends JpaRepository<Batch, Long> {
    List<Batch> findByDivisionId(Long divisionId);
    long countByDivisionId(Long divisionId);
}
