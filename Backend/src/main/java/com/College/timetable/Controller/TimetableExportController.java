package com.College.timetable.Controller;

import com.College.timetable.Entity.Semester;
import com.College.timetable.IO.TimetableExportDTO;
import com.College.timetable.Service.TimetableExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for timetable export functionality
 * Provides endpoints for viewing and exporting published timetables for professors and rooms
 * All endpoints require ADMIN role
 */
@RestController
@RequestMapping("/api/timetable-export")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TimetableExportController {

    private final TimetableExportService timetableExportService;

    /**
     * Get published timetable data for display
     * Returns timetable entries for either a professor or room based on viewType
     * 
     * @param academicYearId The ID of the academic year
     * @param semester The semester (SEM_1 or SEM_2)
     * @param viewType The view type: "PROFESSOR" or "ROOM"
     * @param entityId The ID of the professor or room
     * @return TimetableExportDTO containing timetable data
     */
    @GetMapping("/view")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TimetableExportDTO> getPublishedTimetable(
            @RequestParam Long academicYearId,
            @RequestParam String semester,
            @RequestParam String viewType,
            @RequestParam Long entityId) {
        
        try {
            System.out.println("DEBUG: TimetableExportController.getPublishedTimetable called");
            System.out.println("DEBUG: academicYearId=" + academicYearId + ", semester=" + semester + 
                             ", viewType=" + viewType + ", entityId=" + entityId);
            
            Semester sem = Semester.valueOf(semester);
            TimetableExportDTO timetable;
            
            if ("PROFESSOR".equalsIgnoreCase(viewType)) {
                timetable = timetableExportService.getProfessorTimetable(entityId, academicYearId, sem);
            } else if ("ROOM".equalsIgnoreCase(viewType)) {
                timetable = timetableExportService.getRoomTimetable(entityId, academicYearId, sem);
            } else {
                return ResponseEntity.badRequest().build();
            }
            
            System.out.println("DEBUG: Successfully created timetable DTO, returning OK response");
            return ResponseEntity.ok(timetable);
        } catch (RuntimeException e) {
            System.err.println("ERROR: RuntimeException in getPublishedTimetable: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.err.println("ERROR: Exception in getPublishedTimetable: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export professor timetable to PDF
     * Generates PDF in institutional format with header, teaching load, and weekly grid
     * 
     * @param professorId The ID of the professor
     * @param academicYearId The ID of the academic year
     * @param semester The semester (SEM_1 or SEM_2)
     * @return PDF file as byte array
     */
    @GetMapping("/professor/{professorId}/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportProfessorPDF(
            @PathVariable Long professorId,
            @RequestParam Long academicYearId,
            @RequestParam String semester) {
        
        try {
            Semester sem = Semester.valueOf(semester);
            byte[] pdfBytes = timetableExportService.generateProfessorPDF(professorId, academicYearId, sem);
            
            // Generate filename
            String filename = String.format("professor_timetable_%s_%d.pdf", 
                semester.toLowerCase(), System.currentTimeMillis());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export professor timetable to Excel
     * Generates Excel in institutional format with header, teaching load, and weekly grid
     * 
     * @param professorId The ID of the professor
     * @param academicYearId The ID of the academic year
     * @param semester The semester (SEM_1 or SEM_2)
     * @return Excel file as byte array
     */
    @GetMapping("/professor/{professorId}/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportProfessorExcel(
            @PathVariable Long professorId,
            @RequestParam Long academicYearId,
            @RequestParam String semester) {
        
        try {
            Semester sem = Semester.valueOf(semester);
            byte[] excelBytes = timetableExportService.generateProfessorExcel(professorId, academicYearId, sem);
            
            // Generate filename
            String filename = String.format("professor_timetable_%s_%d.xlsx", 
                semester.toLowerCase(), System.currentTimeMillis());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export room timetable to PDF
     * Generates PDF with standard format showing all classes in the room
     * 
     * @param roomId The ID of the room
     * @param academicYearId The ID of the academic year
     * @param semester The semester (SEM_1 or SEM_2)
     * @return PDF file as byte array
     */
    @GetMapping("/room/{roomId}/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportRoomPDF(
            @PathVariable Long roomId,
            @RequestParam Long academicYearId,
            @RequestParam String semester) {
        
        try {
            Semester sem = Semester.valueOf(semester);
            byte[] pdfBytes = timetableExportService.generateRoomPDF(roomId, academicYearId, sem);
            
            // Generate filename
            String filename = String.format("room_timetable_%s_%d.pdf", 
                semester.toLowerCase(), System.currentTimeMillis());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export room timetable to Excel
     * Generates Excel with standard format showing all classes in the room
     * 
     * @param roomId The ID of the room
     * @param academicYearId The ID of the academic year
     * @param semester The semester (SEM_1 or SEM_2)
     * @return Excel file as byte array
     */
    @GetMapping("/room/{roomId}/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportRoomExcel(
            @PathVariable Long roomId,
            @RequestParam Long academicYearId,
            @RequestParam String semester) {
        
        try {
            Semester sem = Semester.valueOf(semester);
            byte[] excelBytes = timetableExportService.generateRoomExcel(roomId, academicYearId, sem);
            
            // Generate filename
            String filename = String.format("room_timetable_%s_%d.xlsx", 
                semester.toLowerCase(), System.currentTimeMillis());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Exception handler for EntityNotFoundException
     * Returns 404 NOT FOUND when entity (professor/room) is not found
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleEntityNotFound(RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Entity not found: " + ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Internal server error");
    }
}
