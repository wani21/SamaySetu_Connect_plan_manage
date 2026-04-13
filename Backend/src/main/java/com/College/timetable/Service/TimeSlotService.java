package com.College.timetable.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.College.timetable.Entity.TimeSlot;
import com.College.timetable.Repository.TimeSlot_repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TimeSlotService {

	@Autowired
	private TimeSlot_repo timeSlotRepo;

	@Transactional
	public TimeSlot add(TimeSlot timeSlot) {
		// Check for overlapping slots in the same type
		List<TimeSlot> overlapping = timeSlotRepo.findOverlappingSlotsForNew(
			timeSlot.getType(),
			timeSlot.getStartTime(),
			timeSlot.getEndTime()
		);

		if (!overlapping.isEmpty()) {
			throw new IllegalArgumentException("Time slot overlaps with existing slot: " +
				overlapping.get(0).getSlotName() + " (" +
				overlapping.get(0).getStartTime() + " - " +
				overlapping.get(0).getEndTime() + ")");
		}

		return timeSlotRepo.save(timeSlot);
	}

	@Transactional(readOnly = true)
	public List<TimeSlot> getAll() {
		return timeSlotRepo.findAll();
	}

	@Transactional(readOnly = true)
	public TimeSlot getById(Long id) {
		return timeSlotRepo.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Time slot not found with id: " + id));
	}

	@Transactional
	public TimeSlot update(Long id, TimeSlot timeSlot) {
		TimeSlot existing = getById(id);

		// Check for overlapping slots (excluding current slot)
		List<TimeSlot> overlapping = timeSlotRepo.findOverlappingSlots(
			timeSlot.getType(),
			timeSlot.getStartTime(),
			timeSlot.getEndTime(),
			id
		);

		if (!overlapping.isEmpty()) {
			throw new IllegalArgumentException("Time slot overlaps with existing slot: " +
				overlapping.get(0).getSlotName() + " (" +
				overlapping.get(0).getStartTime() + " - " +
				overlapping.get(0).getEndTime() + ")");
		}

		existing.setStartTime(timeSlot.getStartTime());
		existing.setEndTime(timeSlot.getEndTime());
		existing.setDurationMinutes(timeSlot.getDurationMinutes());
		existing.setSlotName(timeSlot.getSlotName());
		existing.setIsBreak(timeSlot.getIsBreak());
		existing.setIsActive(timeSlot.getIsActive());
		existing.setType(timeSlot.getType());
		return timeSlotRepo.save(existing);
	}

	@Transactional(readOnly = true)
	public List<TimeSlot> getByType(String type) {
		return timeSlotRepo.findByType(type);
	}

	@Transactional
	public void delete(Long id) {
		if (!timeSlotRepo.existsById(id)) {
			throw new EntityNotFoundException("Time slot not found with id: " + id);
		}
		timeSlotRepo.deleteById(id);
	}
}
