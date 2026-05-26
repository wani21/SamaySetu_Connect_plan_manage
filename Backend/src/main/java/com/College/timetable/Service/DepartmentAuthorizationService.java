package com.College.timetable.Service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.College.timetable.Entity.TeacherEntity;
import com.College.timetable.Entity.CourseEntity;
import com.College.timetable.Entity.Division;
import com.College.timetable.Entity.ClassRoom;
import com.College.timetable.Entity.Batch;
import com.College.timetable.Entity.TimetableEntry;
import com.College.timetable.Repository.Teacher_Repo;
import com.College.timetable.Repository.Course_repo;
import com.College.timetable.Repository.Division_repo;
import com.College.timetable.Repository.Room_repo;
import com.College.timetable.Repository.Batch_repo;
import com.College.timetable.Repository.TimetableEntry_repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class DepartmentAuthorizationService {

    @Autowired
    private Teacher_Repo teacherRepository;

    @Autowired
    private Course_repo courseRepository;

    @Autowired
    private Division_repo divisionRepository;

    @Autowired
    private Room_repo classroomRepository;

    @Autowired
    private Batch_repo batchRepository;

    @Autowired
    private TimetableEntry_repo timetableEntryRepository;

    public TeacherEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return teacherRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found in system"));
    }

    public boolean isInstitutionalAdmin() {
        try {
            TeacherEntity user = getCurrentUser();
            String role = user.getRole();
            return "SUPER_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasDepartmentAccess(Long departmentId) {
        if (departmentId == null) {
            return false;
        }
        TeacherEntity user = getCurrentUser();
        String role = user.getRole();
        
        // Institutional/Super admins have access to all departments
        if ("SUPER_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            return true;
        }
        
        // Other roles must belong to the same department
        return user.getDepartment() != null && user.getDepartment().getId().equals(departmentId);
    }

    public void checkDepartmentAccess(Long departmentId) {
        if (!hasDepartmentAccess(departmentId)) {
            throw new AccessDeniedException("Access denied: You do not have permissions for this department");
        }
    }

    public void checkSuperAdminAccess() {
        if (!isInstitutionalAdmin()) {
            throw new AccessDeniedException("Access denied: Institutional administration privileges required");
        }
    }

    public void checkTeacherAccess(Long teacherId) {
        if (isInstitutionalAdmin()) return;
        TeacherEntity target = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found with ID: " + teacherId));
        if (target.getDepartment() == null || !hasDepartmentAccess(target.getDepartment().getId())) {
            throw new AccessDeniedException("Access denied: Teacher does not belong to your department");
        }
    }

    public void checkCourseAccess(Long courseId) {
        if (isInstitutionalAdmin()) return;
        CourseEntity target = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with ID: " + courseId));
        if (target.getDepartment() == null || !hasDepartmentAccess(target.getDepartment().getId())) {
            throw new AccessDeniedException("Access denied: Course does not belong to your department");
        }
    }

    public void checkDivisionAccess(Long divisionId) {
        if (isInstitutionalAdmin()) return;
        Division target = divisionRepository.findById(divisionId)
                .orElseThrow(() -> new EntityNotFoundException("Division not found with ID: " + divisionId));
        if (target.getDepartment() == null || !hasDepartmentAccess(target.getDepartment().getId())) {
            throw new AccessDeniedException("Access denied: Division does not belong to your department");
        }
    }

    public void checkRoomAccess(Long roomId) {
        if (isInstitutionalAdmin()) return;
        ClassRoom target = classroomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found with ID: " + roomId));
        if (target.getDepartment() == null || !hasDepartmentAccess(target.getDepartment().getId())) {
            throw new AccessDeniedException("Access denied: Room does not belong to your department");
        }
    }

    public void checkBatchAccess(Long batchId) {
        if (isInstitutionalAdmin()) return;
        Batch target = batchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found with ID: " + batchId));
        if (target.getDivision() == null || target.getDivision().getDepartment() == null 
                || !hasDepartmentAccess(target.getDivision().getDepartment().getId())) {
            throw new AccessDeniedException("Access denied: Batch does not belong to your department");
        }
    }

    public void checkTimetableEntryAccess(Long entryId) {
        if (isInstitutionalAdmin()) return;
        TimetableEntry target = timetableEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Timetable entry not found with ID: " + entryId));
        if (target.getDivision() == null || target.getDivision().getDepartment() == null 
                || !hasDepartmentAccess(target.getDivision().getDepartment().getId())) {
            throw new AccessDeniedException("Access denied: Timetable entry does not belong to your department");
        }
    }

    public void checkLabSessionGroupAccess(Long groupId) {
        if (isInstitutionalAdmin()) return;
        List<TimetableEntry> entries = timetableEntryRepository.findByLabSessionGroupId(groupId);
        if (!entries.isEmpty()) {
            TimetableEntry target = entries.get(0);
            if (target.getDivision() != null && target.getDivision().getDepartment() != null) {
                checkDepartmentAccess(target.getDivision().getDepartment().getId());
            }
        }
    }
}
