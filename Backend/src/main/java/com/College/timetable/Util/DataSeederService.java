package com.College.timetable.Util;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.College.timetable.Entity.*;
import com.College.timetable.Repository.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DataSeederService {

    private static final Logger logger = LoggerFactory.getLogger(DataSeederService.class);

    @Transactional
    public void seedDatabase(
            Teacher_Repo teacherRepo,
            Dep_repo depRepo,
            AcademicYearRepository academicYearRepo,
            TimeSlot_repo timeSlotRepo,
            Room_repo roomRepo,
            Course_repo courseRepo,
            Division_repo divisionRepo,
            Batch_repo batchRepo,
            TimetableEntry_repo timetableEntryRepo,
            PasswordEncoder passwordEncoder,
            String adminEmail,
            String adminPassword,
            String adminName,
            String adminEmployeeId
    ) {
        try {
            logger.info("[INIT] Checking database state for multi-department test data...");

            // ---------------------------------------------------------------
            // 1. Seed Active Academic Year
            // ---------------------------------------------------------------
            AcademicYear activeYear = academicYearRepo.findAll().stream()
                    .filter(y -> "2026-2027".equals(y.getYearName()))
                    .findFirst().orElse(null);

            if (activeYear == null) {
                activeYear = new AcademicYear();
                activeYear.setYearName("2026-2027");
                activeYear.setStartDate(LocalDate.of(2026, 6, 1));
                activeYear.setEndDate(LocalDate.of(2027, 5, 31));
                activeYear.setIsCurrent(true);
                activeYear = academicYearRepo.save(activeYear);
                logger.info("[INIT] Seeded Academic Year: 2026-2027");
            } else {
                logger.info("[INIT] Reusing Academic Year: {}", activeYear.getYearName());
            }

            // ---------------------------------------------------------------
            // 2. Seed Default Time Slots (TYPE_1)
            // ---------------------------------------------------------------
            List<TimeSlot> slots = timeSlotRepo.findAll().stream()
                    .filter(s -> "TYPE_1".equals(s.getType()))
                    .toList();

            if (slots.isEmpty()) {
                slots = new ArrayList<>();
                // Lecture 1
                TimeSlot s1 = new TimeSlot(null, LocalTime.of(9, 0), LocalTime.of(10, 0), 60, "Lecture 1", false, true, "TYPE_1", null, null);
                // Lecture 2
                TimeSlot s2 = new TimeSlot(null, LocalTime.of(10, 0), LocalTime.of(11, 0), 60, "Lecture 2", false, true, "TYPE_1", null, null);
                // Short Break
                TimeSlot s3 = new TimeSlot(null, LocalTime.of(11, 0), LocalTime.of(11, 15), 15, "Short Break", true, true, "TYPE_1", null, null);
                // Lecture 3
                TimeSlot s4 = new TimeSlot(null, LocalTime.of(11, 15), LocalTime.of(12, 15), 60, "Lecture 3", false, true, "TYPE_1", null, null);
                // Lecture 4
                TimeSlot s5 = new TimeSlot(null, LocalTime.of(12, 15), LocalTime.of(13, 15), 60, "Lecture 4", false, true, "TYPE_1", null, null);
                // Lunch Break
                TimeSlot s6 = new TimeSlot(null, LocalTime.of(13, 15), LocalTime.of(14, 0), 45, "Lunch Break", true, true, "TYPE_1", null, null);
                // Lecture 5
                TimeSlot s7 = new TimeSlot(null, LocalTime.of(14, 0), LocalTime.of(15, 0), 60, "Lecture 5", false, true, "TYPE_1", null, null);
                // Lecture 6
                TimeSlot s8 = new TimeSlot(null, LocalTime.of(15, 0), LocalTime.of(16, 0), 60, "Lecture 6", false, true, "TYPE_1", null, null);

                slots.add(timeSlotRepo.save(s1));
                slots.add(timeSlotRepo.save(s2));
                slots.add(timeSlotRepo.save(s3));
                slots.add(timeSlotRepo.save(s4));
                slots.add(timeSlotRepo.save(s5));
                slots.add(timeSlotRepo.save(s6));
                slots.add(timeSlotRepo.save(s7));
                slots.add(timeSlotRepo.save(s8));
                logger.info("[INIT] Seeded 8 Standard Time Slots (TYPE_1)");
            } else {
                logger.info("[INIT] Reusing {} existing Time Slots", slots.size());
            }

            // ---------------------------------------------------------------
            // 3. Seed Departments (COMP and ENTC)
            // ---------------------------------------------------------------
            DepartmentEntity compDept = depRepo.findAll().stream()
                    .filter(d -> "COMP".equals(d.getCode()))
                    .findFirst().orElse(null);

            if (compDept == null) {
                compDept = new DepartmentEntity();
                compDept.setName("Computer Engineering");
                compDept.setCode("COMP");
                compDept.setHeadOfDepartment("Dr. Nilesh Patil");
                compDept.setYears("1,2,3,4");
                compDept.setAcademicYear(activeYear);
                compDept = depRepo.save(compDept);
                logger.info("[INIT] Seeded Department COMP");
            }

            DepartmentEntity entcDept = depRepo.findAll().stream()
                    .filter(d -> "ENTC".equals(d.getCode()))
                    .findFirst().orElse(null);

            if (entcDept == null) {
                entcDept = new DepartmentEntity();
                entcDept.setName("Electronics & Telecommunication");
                entcDept.setCode("ENTC");
                entcDept.setHeadOfDepartment("Dr. Suhas Pawar");
                entcDept.setYears("1,2,3,4");
                entcDept.setAcademicYear(activeYear);
                entcDept = depRepo.save(entcDept);
                logger.info("[INIT] Seeded Department ENTC");
            }

            // ---------------------------------------------------------------
            // 4. Seed Admin & HOD & Faculty Users
            // ---------------------------------------------------------------
            if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank()) {
                if (teacherRepo.findByEmail(adminEmail).isEmpty()) {
                    TeacherEntity admin = new TeacherEntity();
                    admin.setName(adminName);
                    admin.setEmail(adminEmail);
                    admin.setEmployeeId(adminEmployeeId);
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setRole("SUPER_ADMIN");
                    admin.setIsActive(true);
                    admin.setIsApproved(true);
                    admin.setIsEmailVerified(true);
                    admin.setIsFirstLogin(false);
                    admin.setMinWeeklyHours(10);
                    admin.setMaxWeeklyHours(40);
                    teacherRepo.save(admin);
                    logger.info("[INIT] Seeded SUPER_ADMIN user: {}", adminEmail);
                }
            }

            // HOD: COMP
            TeacherEntity compHOD = teacherRepo.findByEmail("compadmin@mitaoe.ac.in").orElse(null);
            if (compHOD == null) {
                compHOD = new TeacherEntity();
                compHOD.setName("Dr. Nilesh Patil");
                compHOD.setEmail("compadmin@mitaoe.ac.in");
                compHOD.setEmployeeId("COMP_HOD01");
                compHOD.setPassword(passwordEncoder.encode("Admin@123"));
                compHOD.setRole("HOD");
                compHOD.setIsActive(true);
                compHOD.setIsApproved(true);
                compHOD.setIsEmailVerified(true);
                compHOD.setIsFirstLogin(false);
                compHOD.setDepartment(compDept);
                compHOD.setMinWeeklyHours(6);
                compHOD.setMaxWeeklyHours(20);
                compHOD = teacherRepo.save(compHOD);
                logger.info("[INIT] Seeded HOD COMP");
            }

            // Teacher: COMP
            TeacherEntity compTeacher = teacherRepo.findByEmail("compteacher@mitaoe.ac.in").orElse(null);
            if (compTeacher == null) {
                compTeacher = new TeacherEntity();
                compTeacher.setName("Prof. Abhijeet Rane");
                compTeacher.setEmail("compteacher@mitaoe.ac.in");
                compTeacher.setEmployeeId("COMP_TCH01");
                compTeacher.setPassword(passwordEncoder.encode("Teacher@123"));
                compTeacher.setRole("TEACHER");
                compTeacher.setIsActive(true);
                compTeacher.setIsApproved(true);
                compTeacher.setIsEmailVerified(true);
                compTeacher.setIsFirstLogin(false);
                compTeacher.setDepartment(compDept);
                compTeacher.setMinWeeklyHours(12);
                compTeacher.setMaxWeeklyHours(30);
                compTeacher = teacherRepo.save(compTeacher);
                logger.info("[INIT] Seeded Teacher COMP");
            }

            // HOD: ENTC
            TeacherEntity entcHOD = teacherRepo.findByEmail("entcadmin@mitaoe.ac.in").orElse(null);
            if (entcHOD == null) {
                entcHOD = new TeacherEntity();
                entcHOD.setName("Dr. Suhas Pawar");
                entcHOD.setEmail("entcadmin@mitaoe.ac.in");
                entcHOD.setEmployeeId("ENTC_HOD01");
                entcHOD.setPassword(passwordEncoder.encode("Admin@123"));
                entcHOD.setRole("HOD");
                entcHOD.setIsActive(true);
                entcHOD.setIsApproved(true);
                entcHOD.setIsEmailVerified(true);
                entcHOD.setIsFirstLogin(false);
                entcHOD.setDepartment(entcDept);
                entcHOD.setMinWeeklyHours(6);
                entcHOD.setMaxWeeklyHours(20);
                entcHOD = teacherRepo.save(entcHOD);
                logger.info("[INIT] Seeded HOD ENTC");
            }

            // Teacher: ENTC
            TeacherEntity entcTeacher = teacherRepo.findByEmail("entcteacher@mitaoe.ac.in").orElse(null);
            if (entcTeacher == null) {
                entcTeacher = new TeacherEntity();
                entcTeacher.setName("Prof. Smita Joshi");
                entcTeacher.setEmail("entcteacher@mitaoe.ac.in");
                entcTeacher.setEmployeeId("ENTC_TCH01");
                entcTeacher.setPassword(passwordEncoder.encode("Teacher@123"));
                entcTeacher.setRole("TEACHER");
                entcTeacher.setIsActive(true);
                entcTeacher.setIsApproved(true);
                entcTeacher.setIsEmailVerified(true);
                entcTeacher.setIsFirstLogin(false);
                entcTeacher.setDepartment(entcDept);
                entcTeacher.setMinWeeklyHours(12);
                entcTeacher.setMaxWeeklyHours(30);
                entcTeacher = teacherRepo.save(entcTeacher);
                logger.info("[INIT] Seeded Teacher ENTC");
            }

            // ---------------------------------------------------------------
            // 5. Seed Classrooms & Laboratories
            // ---------------------------------------------------------------
            ClassRoom compRoom = roomRepo.findAll().stream()
                    .filter(r -> "CO301".equals(r.getRoomNumber()))
                    .findFirst().orElse(null);

            if (compRoom == null) {
                compRoom = new ClassRoom(null, "Room 301", "CO301", "C-Wing", 60, RoomType.CLASSROOM, true, true, "Projector, AC, Smart Board", true, null, null, compDept, null);
                compRoom = roomRepo.save(compRoom);
                logger.info("[INIT] Seeded Room CO301");
            }

            ClassRoom compLab = roomRepo.findAll().stream()
                    .filter(r -> "CO302".equals(r.getRoomNumber()))
                    .findFirst().orElse(null);

            if (compLab == null) {
                compLab = new ClassRoom(null, "Lab 302", "CO302", "C-Wing", 30, RoomType.LAB, true, false, "30 PCs, LAN, Projector", true, null, null, compDept, null);
                compLab = roomRepo.save(compLab);
                logger.info("[INIT] Seeded Lab CO302");
            }

            ClassRoom entcRoom = roomRepo.findAll().stream()
                    .filter(r -> "EN401".equals(r.getRoomNumber()))
                    .findFirst().orElse(null);

            if (entcRoom == null) {
                entcRoom = new ClassRoom(null, "Room 401", "EN401", "E-Wing", 60, RoomType.CLASSROOM, true, true, "Projector, Smart Board", true, null, null, entcDept, null);
                roomRepo.save(entcRoom);
                logger.info("[INIT] Seeded Room EN401");
            }

            ClassRoom entcLab = roomRepo.findAll().stream()
                    .filter(r -> "EN402".equals(r.getRoomNumber()))
                    .findFirst().orElse(null);

            if (entcLab == null) {
                entcLab = new ClassRoom(null, "Lab 402", "EN402", "E-Wing", 30, RoomType.LAB, true, true, "CRO, DSP Kits, Function Generators", true, null, null, entcDept, null);
                roomRepo.save(entcLab);
                logger.info("[INIT] Seeded Lab EN402");
            }

            // ---------------------------------------------------------------
            // 6. Seed Divisions & Batches
            // ---------------------------------------------------------------
            Division compDiv = divisionRepo.findAll().stream()
                    .filter(d -> "A".equals(d.getName()) && d.getDepartment() != null && "COMP".equals(d.getDepartment().getCode()))
                    .findFirst().orElse(null);

            if (compDiv == null) {
                compDiv = new Division();
                compDiv.setName("A");
                compDiv.setYear(3);
                compDiv.setBranch("Computer Engineering");
                compDiv.setTotalStudents(60);
                compDiv.setIsActive(true);
                compDiv.setTimeSlotType("TYPE_1");
                compDiv.setDepartment(compDept);
                compDiv.setAcademicYear(activeYear);
                compDiv = divisionRepo.save(compDiv);
                logger.info("[INIT] Seeded COMP Division A");
            }

            final Division finalCompDiv = compDiv;

            Batch b1 = batchRepo.findAll().stream()
                    .filter(b -> "B1".equals(b.getName()) && b.getDivision() != null && finalCompDiv.getId().equals(b.getDivision().getId()))
                    .findFirst().orElse(null);
            if (b1 == null) {
                b1 = new Batch(null, "B1", 20, finalCompDiv, null, null);
                batchRepo.save(b1);
                logger.info("[INIT] Seeded COMP Batch B1");
            }

            Batch b2 = batchRepo.findAll().stream()
                    .filter(b -> "B2".equals(b.getName()) && b.getDivision() != null && finalCompDiv.getId().equals(b.getDivision().getId()))
                    .findFirst().orElse(null);
            if (b2 == null) {
                b2 = new Batch(null, "B2", 20, finalCompDiv, null, null);
                batchRepo.save(b2);
                logger.info("[INIT] Seeded COMP Batch B2");
            }

            Division entcDiv = divisionRepo.findAll().stream()
                    .filter(d -> "A".equals(d.getName()) && d.getDepartment() != null && "ENTC".equals(d.getDepartment().getCode()))
                    .findFirst().orElse(null);

            if (entcDiv == null) {
                entcDiv = new Division();
                entcDiv.setName("A");
                entcDiv.setYear(3);
                entcDiv.setBranch("Electronics & Telecommunication");
                entcDiv.setTotalStudents(60);
                entcDiv.setIsActive(true);
                entcDiv.setTimeSlotType("TYPE_1");
                entcDiv.setDepartment(entcDept);
                entcDiv.setAcademicYear(activeYear);
                entcDiv = divisionRepo.save(entcDiv);
                logger.info("[INIT] Seeded ENTC Division A");
            }

            final Division finalEntcDiv = entcDiv;

            Batch eb1 = batchRepo.findAll().stream()
                    .filter(b -> "B1".equals(b.getName()) && b.getDivision() != null && finalEntcDiv.getId().equals(b.getDivision().getId()))
                    .findFirst().orElse(null);
            if (eb1 == null) {
                eb1 = new Batch(null, "B1", 20, finalEntcDiv, null, null);
                batchRepo.save(eb1);
                logger.info("[INIT] Seeded ENTC Batch B1");
            }

            // ---------------------------------------------------------------
            // 7. Seed Courses
            // ---------------------------------------------------------------
            CourseEntity compCourse = courseRepo.findAll().stream()
                    .filter(c -> "CS301".equals(c.getCode()))
                    .findFirst().orElse(null);

            if (compCourse == null) {
                compCourse = new CourseEntity();
                compCourse.setName("Computer Networks");
                compCourse.setCode("CS301");
                compCourse.setCourseType(CourseType.THEORY);
                compCourse.setCredits(3);
                compCourse.setHoursPerWeek(3);
                compCourse.setSemester(Semester.SEM_5);
                compCourse.setYear(3);
                compCourse.setIsActive(true);
                compCourse.setDepartment(compDept);
                compCourse = courseRepo.save(compCourse);
                logger.info("[INIT] Seeded COMP Course CS301");
            }

            CourseEntity compLabCourse = courseRepo.findAll().stream()
                    .filter(c -> "CS301L".equals(c.getCode()))
                    .findFirst().orElse(null);

            if (compLabCourse == null) {
                compLabCourse = new CourseEntity();
                compLabCourse.setName("Computer Networks Lab");
                compLabCourse.setCode("CS301L");
                compLabCourse.setCourseType(CourseType.LAB);
                compLabCourse.setCredits(1);
                compLabCourse.setHoursPerWeek(2);
                compLabCourse.setSemester(Semester.SEM_5);
                compLabCourse.setYear(3);
                compLabCourse.setIsActive(true);
                compLabCourse.setDepartment(compDept);
                compLabCourse = courseRepo.save(compLabCourse);
                logger.info("[INIT] Seeded COMP Course CS301L");
            }

            CourseEntity entcCourse = courseRepo.findAll().stream()
                    .filter(c -> "EN301".equals(c.getCode()))
                    .findFirst().orElse(null);

            if (entcCourse == null) {
                entcCourse = new CourseEntity();
                entcCourse.setName("Embedded Systems");
                entcCourse.setCode("EN301");
                entcCourse.setCourseType(CourseType.THEORY);
                entcCourse.setCredits(3);
                entcCourse.setHoursPerWeek(3);
                entcCourse.setSemester(Semester.SEM_5);
                entcCourse.setYear(3);
                entcCourse.setIsActive(true);
                entcCourse.setDepartment(entcDept);
                entcCourse = courseRepo.save(entcCourse);
                logger.info("[INIT] Seeded ENTC Course EN301");
            }

            // Link COMP courses to COMP Teacher
            if (compTeacher != null && compTeacher.getCourses().isEmpty()) {
                Set<CourseEntity> tCourses = new HashSet<>();
                tCourses.add(compCourse);
                tCourses.add(compLabCourse);
                compTeacher.setCourses(tCourses);
                teacherRepo.save(compTeacher);
                logger.info("[INIT] Assigned COMP courses to Prof. Abhijeet Rane");
            }

            // ---------------------------------------------------------------
            // 8. Seed Timetable Entries
            // ---------------------------------------------------------------
            if (timetableEntryRepo.count() == 0 && slots.size() >= 4) {
                // Lecture 1: Monday 9:00 - 10:00 [PUBLISHED]
                TimetableEntry e1 = new TimetableEntry();
                e1.setDivision(compDiv);
                e1.setCourse(compCourse);
                e1.setTeacher(compTeacher);
                e1.setRoom(compRoom);
                e1.setTimeSlot(slots.get(0));
                e1.setDayOfWeek(DayOfWeek.MONDAY);
                e1.setAcademicYear(activeYear);
                e1.setStatus(TimetableStatus.PUBLISHED);
                e1.setSemester(Semester.SEM_5);
                e1.setIsLabSession(false);
                timetableEntryRepo.save(e1);

                // Lecture 2: Monday 10:00 - 11:00 [PUBLISHED]
                TimetableEntry e2 = new TimetableEntry();
                e2.setDivision(compDiv);
                e2.setCourse(compCourse);
                e2.setTeacher(compTeacher);
                e2.setRoom(compRoom);
                e2.setTimeSlot(slots.get(1));
                e2.setDayOfWeek(DayOfWeek.MONDAY);
                e2.setAcademicYear(activeYear);
                e2.setStatus(TimetableStatus.PUBLISHED);
                e2.setSemester(Semester.SEM_5);
                e2.setIsLabSession(false);
                timetableEntryRepo.save(e2);

                // Lab Session: Tuesday 11:15 - 12:15 [DRAFT]
                TimetableEntry e3 = new TimetableEntry();
                e3.setDivision(compDiv);
                e3.setCourse(compLabCourse);
                e3.setTeacher(compTeacher);
                e3.setRoom(compLab);
                e3.setTimeSlot(slots.get(3)); // Lecture 3 slot
                e3.setDayOfWeek(DayOfWeek.TUESDAY);
                e3.setAcademicYear(activeYear);
                e3.setStatus(TimetableStatus.DRAFT);
                e3.setSemester(Semester.SEM_5);
                e3.setIsLabSession(true);
                e3.setBatch(b1);
                timetableEntryRepo.save(e3);

                logger.info("[INIT] Seeded 3 test Timetable Entries successfully");
            }

            logger.info("[INIT] Database seeding completed successfully with zero conflicts!");

        } catch (Exception e) {
            logger.error("[INIT] Failed to seed development data: " + e.getMessage(), e);
        }
    }
}
