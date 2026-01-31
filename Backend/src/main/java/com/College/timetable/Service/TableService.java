package com.College.timetable.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.College.timetable.Entity.AcademicYear;
import com.College.timetable.Entity.Batch;
import com.College.timetable.Entity.ClassRoom;
import com.College.timetable.Entity.CourseEntity;
import com.College.timetable.Entity.Division;
import com.College.timetable.Entity.TeacherEntity;
import com.College.timetable.Entity.TimeSlot;
import com.College.timetable.Entity.TimetableEntry;
import com.College.timetable.IO.ManualTimetableRequest;
import com.College.timetable.Repository.Acadamic_repo;
import com.College.timetable.Repository.Batch_repo;
import com.College.timetable.Repository.Course_repo;
import com.College.timetable.Repository.Division_repo;
import com.College.timetable.Repository.Room_repo;
import com.College.timetable.Repository.Teacher_Repo;
import com.College.timetable.Repository.TimeSlot_repo;
import com.College.timetable.Repository.TimetableEntry_repo;

import jakarta.persistence.Entity;

@Service
public class TableService {

		@Autowired
		Teacher_Repo teacherRepo;
		
		@Autowired
		TimeSlot_repo timeSlotRepo;
		
		@Autowired
		Room_repo roomRepo;
		
		@Autowired
		Batch_repo batchRepo;
		
		@Autowired
		Division_repo divisionRepo;
		
		@Autowired
		TimetableEntry_repo timetable;
		
		@Autowired
		Course_repo courseRepo;
		
		@Autowired
		Acadamic_repo academicYearRepo;
		
		
		public TimetableEntry create(ManualTimetableRequest request) {
			TeacherEntity teacher = teacherRepo.findById(request.getTeacherId())
	                .orElseThrow(() -> new RuntimeException("Teacher not found"));

	        TimeSlot timeSlot = timeSlotRepo.findById(request.getTimeSlotId())
	                .orElseThrow(() -> new RuntimeException("TimeSlot not found"));

	        ClassRoom room = roomRepo.findById(request.getRoomId())
	                .orElseThrow(() -> new RuntimeException("Room not found"));

	        Division division = divisionRepo.findById(request.getDivisionId())
	                .orElseThrow(() -> new RuntimeException("Division not found"));

	        CourseEntity course = courseRepo.findById(request.getCourseId())
	                .orElseThrow(() -> new RuntimeException("Course not found"));

	        AcademicYear academicYear = academicYearRepo.findById(request.getAcademicYearId())
	                .orElseThrow(() -> new RuntimeException("Academic year not found"));
			    
	        if (timetable.existsByDayOfWeekAndTimeSlotAndTeacherAndAcademicYear(
	                request.getDayOfWeek(), timeSlot, teacher, academicYear)) {
	            throw new RuntimeException("❌ Teacher already assigned at this time in this academic year");
	        }

	        if (timetable.existsByDayOfWeekAndTimeSlotAndRoomAndAcademicYear(
	                request.getDayOfWeek(), timeSlot, room, academicYear)) {
	            throw new RuntimeException("❌ Room already occupied at this time in this academic year");
	        }

	        if (timetable.existsByDayOfWeekAndTimeSlotAndDivisionAndAcademicYear(
	                request.getDayOfWeek(), timeSlot, division, academicYear)) {
	            throw new RuntimeException("❌ Division already has a lecture at this time in this academic year");
	        }

			    
			    TimetableEntry entry = new TimetableEntry();
		        entry.setDayOfWeek(request.getDayOfWeek());
		        entry.setTimeSlot(timeSlot);
		        entry.setTeacher(teacher);
		        entry.setRoom(room);
		        entry.setDivision(division);
		        entry.setCourse(course);
		        entry.setAcademicYear(academicYear);
		        entry.setWeekNumber(request.getWeekNumber());
		        entry.setIsRecurring(request.getIsRecurring());
		        entry.setNotes(request.getNotes());
			    
				return timetable.save(entry);
			    
		}

}
