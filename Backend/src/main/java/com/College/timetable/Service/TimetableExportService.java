package com.College.timetable.Service;

import com.College.timetable.Entity.*;
import com.College.timetable.IO.TimetableEntryDTO;
import com.College.timetable.IO.TimetableExportDTO;
import com.College.timetable.IO.TimeSlotDTO;
import com.College.timetable.Repository.*;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Chunk;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableExportService {

    private final TimetableService timetableService;
    private final TimeSlot_repo timeSlotRepo;
    private final Division_repo divisionRepo;
    private final Teacher_Repo teacherRepo;
    private final AcademicYearRepository academicYearRepo;
    private final TimetableEntry_repo timetableEntryRepo;
    private final Room_repo roomRepo;

    private static final String[] DAY_NAMES = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
    private static final String[] DAY_LABELS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    // ═══════════════════════════════════════════════════════════════
    // TIMETABLE DATA RETRIEVAL FOR EXPORT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get timetable entries (DRAFT and PUBLISHED) for a professor in a specific academic year and semester
     * Includes all divisions, branches, and years
     * Maps entities to TimetableExportDTO for frontend display
     * 
     * @param professorId The ID of the professor
     * @param academicYearId The ID of the academic year
     * @param semester The semester (SEM_1 or SEM_2)
     * @return TimetableExportDTO containing professor info, time slots, and entries
     */
    public TimetableExportDTO getProfessorTimetable(Long professorId, Long academicYearId, Semester semester) {
        // Debug logging
        System.out.println("DEBUG: Fetching timetable for professor ID: " + professorId + 
                          ", Academic Year ID: " + academicYearId + 
                          ", Semester: " + semester);
        
        // Fetch professor entity
        TeacherEntity professor = teacherRepo.findById(professorId)
            .orElseThrow(() -> {
                System.err.println("ERROR: Professor not found with id: " + professorId);
                System.err.println("ERROR: This is likely a Redis cache issue. Please clear Redis cache.");
                return new RuntimeException("Professor not found with id: " + professorId);
            });
        
        System.out.println("DEBUG: Found professor: " + professor.getName());
        
        // Fetch academic year entity
        AcademicYear academicYear = academicYearRepo.findById(academicYearId)
            .orElseThrow(() -> new RuntimeException("Academic year not found with id: " + academicYearId));
        
        System.out.println("DEBUG: Found academic year: " + academicYear.getYearName());
        
        // Query entries (DRAFT and PUBLISHED) for professor using custom repository method
        List<TimetableEntry> allEntries = timetableEntryRepo.findPublishedByProfessorAndSemester(
            professorId, 
            academicYearId, 
            semester
        );
        
        // Filter entries by semester series (odd or even)
        List<TimetableEntry> entries = allEntries.stream()
            .filter(entry -> entry.getSemester() != null && entry.getSemester().isSameSeries(semester))
            .collect(Collectors.toList());
        
        System.out.println("DEBUG: Found " + allEntries.size() + " total entries, " + entries.size() + " in same semester series");
        
        if (entries.isEmpty()) {
            System.out.println("DEBUG: No entries found - returning empty timetable");
        }
        
        // Get all active time slots (use TYPE_1 as default for professor view)
        System.out.println("DEBUG: Fetching time slots...");
        List<TimeSlot> timeSlots = timeSlotRepo.findAll().stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
            .filter(s -> "TYPE_1".equals(s.getType()))
            .sorted(Comparator.comparing(TimeSlot::getStartTime))
            .collect(Collectors.toList());
        
        System.out.println("DEBUG: Found " + timeSlots.size() + " time slots");
        
        // Map time slots to DTOs
        System.out.println("DEBUG: Mapping time slots to DTOs...");
        List<TimeSlotDTO> timeSlotDTOs = timeSlots.stream()
            .map(slot -> new TimeSlotDTO(
                slot.getId(),
                slot.getSlotName(),
                slot.getStartTime() != null ? slot.getStartTime().toString().substring(0, 5) : "??:??",
                slot.getEndTime() != null ? slot.getEndTime().toString().substring(0, 5) : "??:??",
                slot.getIsBreak()
            ))
            .collect(Collectors.toList());
        
        System.out.println("DEBUG: Mapping timetable entries to DTOs...");
        
        // Map timetable entries to DTOs
        List<TimetableEntryDTO> entryDTOs = entries.stream()
            .map(entry -> {
                // Build division name in format "branch year division" (e.g., "CS 3 A")
                String divisionName = "";
                Integer divisionYear = null;
                if (entry.getDivision() != null) {
                    String branch = entry.getDivision().getDepartment() != null 
                        ? entry.getDivision().getDepartment().getCode() 
                        : "";
                    String year = String.valueOf(entry.getDivision().getYear());
                    String div = entry.getDivision().getName();
                    divisionName = String.format("%s %s %s", branch, year, div).trim();
                    divisionYear = entry.getDivision().getYear();
                }
                
                // Get batch name for lab sessions
                String batchName = null;
                boolean isLabSession = entry.getCourse() != null 
                    && entry.getCourse().getCourseType() == CourseType.LAB;
                if (isLabSession && entry.getBatch() != null) {
                    batchName = entry.getBatch().getName();
                }
                
                // Get room information
                String roomName = entry.getRoom() != null ? entry.getRoom().getName() : null;
                String roomNumber = entry.getRoom() != null ? entry.getRoom().getRoomNumber() : null;
                
                return new TimetableEntryDTO(
                    entry.getId(),
                    entry.getCourse() != null ? entry.getCourse().getName() : "-",
                    divisionName,
                    divisionYear,
                    batchName,
                    null, // professorName is null for professor view
                    roomName,
                    roomNumber,
                    entry.getDayOfWeek() != null ? entry.getDayOfWeek().name() : null,
                    entry.getTimeSlot() != null ? entry.getTimeSlot().getId() : null,
                    isLabSession
                );
            })
            .collect(Collectors.toList());
        
        // Build semester label
        String semesterLabel = semester != null ? semester.name().replace("_", " ") : "";
        
        // Build and return TimetableExportDTO
        return new TimetableExportDTO(
            professor.getName(),
            professor.getEmployeeId(),
            academicYear.getYearName(),
            semesterLabel,
            timeSlotDTOs,
            entryDTOs
        );
    }

    /**
     * Get timetable entries (DRAFT and PUBLISHED) for a room in a specific academic year and semester
     * Includes all divisions, branches, and years
     * Maps entities to TimetableExportDTO for frontend display
     * 
     * @param roomId The ID of the room
     * @param academicYearId The ID of the academic year
     * @param semester The semester (SEM_1 or SEM_2)
     * @return TimetableExportDTO containing room info, time slots, and entries
     */
    public TimetableExportDTO getRoomTimetable(Long roomId, Long academicYearId, Semester semester) {
        // Fetch room entity
        ClassRoom room = roomRepo.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));
        
        // Fetch academic year entity
        AcademicYear academicYear = academicYearRepo.findById(academicYearId)
            .orElseThrow(() -> new RuntimeException("Academic year not found with id: " + academicYearId));
        
        // Query entries (DRAFT and PUBLISHED) for room using custom repository method
        List<TimetableEntry> allEntries = timetableEntryRepo.findPublishedByRoomAndSemester(
            roomId, 
            academicYearId, 
            semester
        );
        
        // Filter entries by semester series (odd or even)
        List<TimetableEntry> entries = allEntries.stream()
            .filter(entry -> entry.getSemester() != null && entry.getSemester().isSameSeries(semester))
            .collect(Collectors.toList());
        
        // Get all active time slots (use TYPE_1 as default for room view)
        List<TimeSlot> timeSlots = timeSlotRepo.findAll().stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
            .filter(s -> "TYPE_1".equals(s.getType()))
            .sorted(Comparator.comparing(TimeSlot::getStartTime))
            .collect(Collectors.toList());
        
        // Map time slots to DTOs
        List<TimeSlotDTO> timeSlotDTOs = timeSlots.stream()
            .map(slot -> new TimeSlotDTO(
                slot.getId(),
                slot.getSlotName(),
                slot.getStartTime() != null ? slot.getStartTime().toString().substring(0, 5) : "??:??",
                slot.getEndTime() != null ? slot.getEndTime().toString().substring(0, 5) : "??:??",
                slot.getIsBreak()
            ))
            .collect(Collectors.toList());
        
        // Map timetable entries to DTOs
        List<TimetableEntryDTO> entryDTOs = entries.stream()
            .map(entry -> {
                // Build division name in format "branch year division" (e.g., "CS 3 A")
                String divisionName = "";
                Integer divisionYear = null;
                if (entry.getDivision() != null) {
                    String branch = entry.getDivision().getDepartment() != null 
                        ? entry.getDivision().getDepartment().getCode() 
                        : "";
                    String year = String.valueOf(entry.getDivision().getYear());
                    String div = entry.getDivision().getName();
                    divisionName = String.format("%s %s %s", branch, year, div).trim();
                    divisionYear = entry.getDivision().getYear();
                }
                
                // Get batch name for lab sessions
                String batchName = null;
                boolean isLabSession = entry.getCourse() != null 
                    && entry.getCourse().getCourseType() == CourseType.LAB;
                if (isLabSession && entry.getBatch() != null) {
                    batchName = entry.getBatch().getName();
                }
                
                // Get professor information (for room view)
                String professorName = entry.getTeacher() != null ? entry.getTeacher().getName() : null;
                
                return new TimetableEntryDTO(
                    entry.getId(),
                    entry.getCourse() != null ? entry.getCourse().getName() : "-",
                    divisionName,
                    divisionYear,
                    batchName,
                    professorName, // professorName is populated for room view
                    null, // roomName is null for room view
                    null, // roomNumber is null for room view
                    entry.getDayOfWeek() != null ? entry.getDayOfWeek().name() : null,
                    entry.getTimeSlot() != null ? entry.getTimeSlot().getId() : null,
                    isLabSession
                );
            })
            .collect(Collectors.toList());
        
        // Build semester label
        String semesterLabel = semester != null ? semester.name().replace("_", " ") : "";
        
        // Build and return TimetableExportDTO
        return new TimetableExportDTO(
            room.getName(),
            room.getRoomNumber(),
            academicYear.getYearName(),
            semesterLabel,
            timeSlotDTOs,
            entryDTOs
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // PROFESSOR/ROOM PDF & EXCEL GENERATION (Institutional Format)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generate professor initials from full name or use short name if available
     * Example: "Abhijeet Purushottam Rane" -> "APR" (auto-generated)
     * Or use teacher.getShortName() if available (e.g., "APR" from database)
     */
    private String getProfessorInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        
        String[] nameParts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        
        for (String part : nameParts) {
            if (!part.isEmpty()) {
                initials.append(part.charAt(0));
            }
        }
        
        return initials.toString().toUpperCase();
    }
    
    /**
     * Get professor short name (preferred) or generate initials from full name
     */
    private String getProfessorShortName(TeacherEntity teacher) {
        if (teacher == null) {
            return "-";
        }
        
        // Use short name if available, otherwise generate initials
        if (teacher.getShortName() != null && !teacher.getShortName().trim().isEmpty()) {
            return teacher.getShortName();
        }
        
        return getProfessorInitials(teacher.getName());
    }
    
    /**
     * Get course short name (preferred) or use full course name as fallback
     */
    private String getCourseShortName(CourseEntity course) {
        if (course == null) {
            return "-";
        }
        
        // Use short name if available, otherwise use full name
        if (course.getShortName() != null && !course.getShortName().trim().isEmpty()) {
            return course.getShortName();
        }
        
        return course.getName();
    }

    /**
     * Get year label from year number
     * 1 -> FY, 2 -> SY, 3 -> TY, 4 -> B.Tech
     */
    private String getYearLabel(Integer year) {
        if (year == null) {
            return "";
        }
        switch (year) {
            case 1: return "FY";
            case 2: return "SY";
            case 3: return "TY";
            case 4: return "B.Tech";
            default: return "Year " + year;
        }
    }

    /**
     * Generate PDF for professor timetable in institutional format (landscape)
     * Follows the exact format: header with institution details, teaching load, time slots as columns, days as rows
     */
    public byte[] generateProfessorPDF(Long professorId, Long academicYearId, Semester semester) throws Exception {
        // Fetch data
        TeacherEntity professor = teacherRepo.findById(professorId)
            .orElseThrow(() -> new RuntimeException("Professor not found"));
        AcademicYear academicYear = academicYearRepo.findById(academicYearId)
            .orElseThrow(() -> new RuntimeException("Academic year not found"));
        List<TimetableEntry> entries = timetableEntryRepo.findPublishedByProfessorAndSemester(
            professorId, academicYearId, semester
        );
        
        // Get time slots
        List<TimeSlot> slots = timeSlotRepo.findAll().stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
            .filter(s -> "TYPE_1".equals(s.getType()))
            .sorted(Comparator.comparing(TimeSlot::getStartTime))
            .collect(Collectors.toList());
        
        // Calculate teaching load
        long thCount = entries.stream().filter(e -> e.getCourse() != null && e.getCourse().getCourseType() == CourseType.THEORY).count();
        long prCount = entries.stream().filter(e -> e.getCourse() != null && e.getCourse().getCourseType() == CourseType.LAB).count();
        long totalCount = thCount + prCount;
        
        // Build lookup map
        Map<String, TimetableEntry> lookup = new HashMap<>();
        for (TimetableEntry e : entries) {
            if (e.getDayOfWeek() != null && e.getTimeSlot() != null) {
                String key = e.getDayOfWeek().name() + ":" + e.getTimeSlot().getId();
                lookup.put(key, e);
            }
        }
        
        // Create PDF
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 15, 15, 15, 15);
        PdfWriter.getInstance(doc, out);
        doc.open();
        
        // Fonts
        Font titleFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 8, Font.NORMAL);
        Font smallFont = new Font(Font.HELVETICA, 7, Font.NORMAL);
        Font headerFont = new Font(Font.HELVETICA, 7, Font.BOLD, Color.WHITE);
        Font breakFont = new Font(Font.HELVETICA, 6, Font.NORMAL, Color.GRAY);
        
        // Header section (4 rows as per institutional format)
        PdfPTable headerTable = new PdfPTable(6);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{25, 25, 25, 15, 5, 10});
        
        // Row 1: Institution | Title | Faculty Name | Professor Name
        PdfPCell instCell = new PdfPCell(new Phrase("MIT Academy of Engineering\n(An Autonomous Institute)\nAlandi (D), Pune - 412 105", smallFont));
        instCell.setBorder(PdfPCell.NO_BORDER);
        instCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        headerTable.addCell(instCell);
        
        PdfPCell titleCell = new PdfPCell(new Phrase("FACULTY WISE TIME TABLE", titleFont));
        titleCell.setBorder(PdfPCell.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerTable.addCell(titleCell);
        
        PdfPCell fnLabelCell = new PdfPCell(new Phrase("FACULTY NAME", normalFont));
        fnLabelCell.setBorder(PdfPCell.NO_BORDER);
        fnLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTable.addCell(fnLabelCell);
        
        PdfPCell profNameCell = new PdfPCell(new Phrase(professor.getName() != null ? professor.getName().toUpperCase() : "", normalFont));
        profNameCell.setBorder(PdfPCell.NO_BORDER);
        profNameCell.setColspan(3);
        headerTable.addCell(profNameCell);
        
        // Row 2: Department | Academic Year | Teaching Load | TH | Total
        PdfPCell deptCell = new PdfPCell(new Phrase("SCHOOL OF COMPUTER ENGINEERING", smallFont));
        deptCell.setBorder(PdfPCell.NO_BORDER);
        headerTable.addCell(deptCell);
        
        PdfPCell ayLabelCell = new PdfPCell(new Phrase("ACADEMIC YEAR :", smallFont));
        ayLabelCell.setBorder(PdfPCell.NO_BORDER);
        ayLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTable.addCell(ayLabelCell);
        
        PdfPCell ayValueCell = new PdfPCell(new Phrase(academicYear.getYearName(), smallFont));
        ayValueCell.setBorder(PdfPCell.NO_BORDER);
        headerTable.addCell(ayValueCell);
        
        PdfPCell tlLabelCell = new PdfPCell(new Phrase("TEACHING LOAD", smallFont));
        tlLabelCell.setBorder(PdfPCell.NO_BORDER);
        tlLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTable.addCell(tlLabelCell);
        
        PdfPCell thCell = new PdfPCell(new Phrase("TH (" + thCount + ")", smallFont));
        thCell.setBorder(PdfPCell.NO_BORDER);
        headerTable.addCell(thCell);
        
        PdfPCell totalCell = new PdfPCell(new Phrase("TOTAL : " + totalCount, smallFont));
        totalCell.setBorder(PdfPCell.NO_BORDER);
        headerTable.addCell(totalCell);
        
        // Row 3: Empty | Semester | Empty | PR/TU/Proj
        PdfPCell emptyCell1 = new PdfPCell(new Phrase("", smallFont));
        emptyCell1.setBorder(PdfPCell.NO_BORDER);
        headerTable.addCell(emptyCell1);
        
        PdfPCell semLabelCell = new PdfPCell(new Phrase("SEMESTER :", smallFont));
        semLabelCell.setBorder(PdfPCell.NO_BORDER);
        semLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTable.addCell(semLabelCell);
        
        PdfPCell semValueCell = new PdfPCell(new Phrase(semester != null ? semester.name().replace("_", " ") : "", smallFont));
        semValueCell.setBorder(PdfPCell.NO_BORDER);
        headerTable.addCell(semValueCell);
        
        PdfPCell emptyCell2 = new PdfPCell(new Phrase("", smallFont));
        emptyCell2.setBorder(PdfPCell.NO_BORDER);
        headerTable.addCell(emptyCell2);
        
        PdfPCell prCell = new PdfPCell(new Phrase("PR (" + prCount + ") / TU\n/ Proj (0)", smallFont));
        prCell.setBorder(PdfPCell.NO_BORDER);
        prCell.setColspan(2);
        headerTable.addCell(prCell);
        
        // Row 4: Empty | W.E.F | Date
        PdfPCell emptyCell3 = new PdfPCell(new Phrase("", smallFont));
        emptyCell3.setBorder(PdfPCell.NO_BORDER);
        headerTable.addCell(emptyCell3);
        
        PdfPCell wefLabelCell = new PdfPCell(new Phrase("W.E.F :", smallFont));
        wefLabelCell.setBorder(PdfPCell.NO_BORDER);
        wefLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTable.addCell(wefLabelCell);
        
        PdfPCell wefValueCell = new PdfPCell(new Phrase(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), smallFont));
        wefValueCell.setBorder(PdfPCell.NO_BORDER);
        wefValueCell.setColspan(4);
        headerTable.addCell(wefValueCell);
        
        doc.add(headerTable);
        doc.add(new Paragraph(" ", smallFont));
        
        // Timetable grid: Day/Time column + time slot columns
        int numSlots = slots.size();
        PdfPTable table = new PdfPTable(numSlots + 1);
        table.setWidthPercentage(100);
        float[] widths = new float[numSlots + 1];
        widths[0] = 8f; // Day/Time column
        for (int i = 1; i <= numSlots; i++) {
            widths[i] = 6f; // Time slot columns
        }
        table.setWidths(widths);
        
        // Header row: Day/Time + slot numbers with time ranges
        PdfPCell dayTimeCell = new PdfPCell(new Phrase("Day / Time", headerFont));
        dayTimeCell.setBackgroundColor(new Color(27, 42, 78));
        dayTimeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        dayTimeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        dayTimeCell.setPadding(3);
        table.addCell(dayTimeCell);
        
        int slotNumber = 1;
        for (TimeSlot slot : slots) {
            String timeRange = formatTime(slot);
            PdfPCell slotCell = new PdfPCell(new Phrase(timeRange + "\n" + slotNumber, headerFont));
            slotCell.setBackgroundColor(new Color(27, 42, 78));
            slotCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            slotCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            slotCell.setPadding(2);
            table.addCell(slotCell);
            slotNumber++;
        }
        
        // Day rows
        for (String day : new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT"}) {
            PdfPCell dayCell = new PdfPCell(new Phrase(day, normalFont));
            dayCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            dayCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dayCell.setPadding(3);
            table.addCell(dayCell);
            
            for (TimeSlot slot : slots) {
                if (Boolean.TRUE.equals(slot.getIsBreak())) {
                    // Break cell with vertical text
                    PdfPCell breakCell = new PdfPCell();
                    breakCell.setBackgroundColor(new Color(240, 240, 240));
                    breakCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    breakCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    breakCell.setPadding(2);
                    
                    // Vertical text for breaks
                    String breakText = slot.getSlotName().contains("LUNCH") ? "L\nU\nN\nC\nH\n\nB\nR\nE\nA\nK" : "S\nH\nO\nR\nT\n\nB\nR\nE\nA\nK";
                    breakCell.addElement(new Phrase(breakText, breakFont));
                    table.addCell(breakCell);
                } else {
                    // Regular entry cell
                    String dayName = day.equals("MON") ? "MONDAY" : 
                                    day.equals("TUE") ? "TUESDAY" :
                                    day.equals("WED") ? "WEDNESDAY" :
                                    day.equals("THU") ? "THURSDAY" :
                                    day.equals("FRI") ? "FRIDAY" : "SATURDAY";
                    String key = dayName + ":" + slot.getId();
                    TimetableEntry entry = lookup.get(key);
                    
                    PdfPCell entryCell = new PdfPCell();
                    entryCell.setPadding(2);
                    entryCell.setMinimumHeight(30);
                    
                    if (entry != null) {
                        String courseName = getCourseShortName(entry.getCourse());
                        String profInitials = getProfessorShortName(professor);
                        String roomLocation = entry.getRoom() != null ? entry.getRoom().getRoomNumber() : "";
                        boolean isLab = entry.getCourse() != null && entry.getCourse().getCourseType() == CourseType.LAB;
                        
                        // Get year label
                        String yearLabel = "";
                        if (entry.getDivision() != null && entry.getDivision().getYear() != null) {
                            yearLabel = getYearLabel(entry.getDivision().getYear());
                        }
                        
                        if (isLab && entry.getBatch() != null) {
                            // Lab format: "FY B1 - Course Name - H306B"
                            String batchName = entry.getBatch().getName();
                            String displayText = yearLabel + " " + batchName + " - " + courseName + " - " + roomLocation;
                            entryCell.addElement(new Phrase(displayText, smallFont));
                        } else {
                            // Theory format: "SY A - Course Name - H301"
                            String divisionName = entry.getDivision() != null ? entry.getDivision().getName() : "";
                            String displayText = yearLabel + " " + divisionName + " - " + courseName + " - " + roomLocation;
                            entryCell.addElement(new Phrase(displayText, smallFont));
                        }
                    }
                    
                    table.addCell(entryCell);
                }
            }
        }
        
        doc.add(table);
        
        // Footer
        doc.add(new Paragraph(" ", smallFont));
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        PdfPCell footerLeft = new PdfPCell(new Phrase("Time Table Coordinator", smallFont));
        footerLeft.setBorder(PdfPCell.NO_BORDER);
        footerLeft.setHorizontalAlignment(Element.ALIGN_LEFT);
        footerTable.addCell(footerLeft);
        
        PdfPCell footerRight = new PdfPCell(new Phrase("HOD Comp Engg Dept", smallFont));
        footerRight.setBorder(PdfPCell.NO_BORDER);
        footerRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        footerTable.addCell(footerRight);
        doc.add(footerTable);
        
        doc.close();
        return out.toByteArray();
    }

    /**
     * Generate Excel for professor timetable in institutional format
     * Follows the same layout as PDF with proper cell merging and formatting
     */
    public byte[] generateProfessorExcel(Long professorId, Long academicYearId, Semester semester) throws Exception {
        // Fetch data
        TeacherEntity professor = teacherRepo.findById(professorId)
            .orElseThrow(() -> new RuntimeException("Professor not found"));
        AcademicYear academicYear = academicYearRepo.findById(academicYearId)
            .orElseThrow(() -> new RuntimeException("Academic year not found"));
        List<TimetableEntry> entries = timetableEntryRepo.findPublishedByProfessorAndSemester(
            professorId, academicYearId, semester
        );
        
        // Get time slots
        List<TimeSlot> slots = timeSlotRepo.findAll().stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
            .filter(s -> "TYPE_1".equals(s.getType()))
            .sorted(Comparator.comparing(TimeSlot::getStartTime))
            .collect(Collectors.toList());
        
        // Calculate teaching load
        long thCount = entries.stream().filter(e -> e.getCourse() != null && e.getCourse().getCourseType() == CourseType.THEORY).count();
        long prCount = entries.stream().filter(e -> e.getCourse() != null && e.getCourse().getCourseType() == CourseType.LAB).count();
        long totalCount = thCount + prCount;
        
        // Build lookup map
        Map<String, TimetableEntry> lookup = new HashMap<>();
        for (TimetableEntry e : entries) {
            if (e.getDayOfWeek() != null && e.getTimeSlot() != null) {
                String key = e.getDayOfWeek().name() + ":" + e.getTimeSlot().getId();
                lookup.put(key, e);
            }
        }
        
        // Create Excel workbook
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("Faculty Timetable");
        
        // Styles
        XSSFCellStyle titleStyle = wb.createCellStyle();
        XSSFFont titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 10);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.LEFT);
        
        XSSFCellStyle normalStyle = wb.createCellStyle();
        XSSFFont normalFont = wb.createFont();
        normalFont.setFontHeightInPoints((short) 8);
        normalStyle.setFont(normalFont);
        
        XSSFCellStyle headerStyle = wb.createCellStyle();
        XSSFFont headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 8);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{27, 42, 78}, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setWrapText(true);
        
        XSSFCellStyle breakStyle = wb.createCellStyle();
        breakStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 240, (byte) 240, (byte) 240}, null));
        breakStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        breakStyle.setAlignment(HorizontalAlignment.CENTER);
        breakStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        breakStyle.setBorderBottom(BorderStyle.THIN);
        breakStyle.setBorderTop(BorderStyle.THIN);
        breakStyle.setBorderLeft(BorderStyle.THIN);
        breakStyle.setBorderRight(BorderStyle.THIN);
        XSSFFont breakFont = wb.createFont();
        breakFont.setFontHeightInPoints((short) 7);
        breakFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        breakStyle.setFont(breakFont);
        
        XSSFCellStyle cellStyle = wb.createCellStyle();
        cellStyle.setWrapText(true);
        cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        XSSFFont cellFont = wb.createFont();
        cellFont.setFontHeightInPoints((short) 7);
        cellStyle.setFont(cellFont);
        
        int rowIdx = 0;
        int numSlots = slots.size();
        
        // Header section (4 rows)
        // Row 1: Institution | Title | Faculty Name | Professor Name
        Row row1 = sheet.createRow(rowIdx++);
        org.apache.poi.ss.usermodel.Cell instCell = row1.createCell(0);
        instCell.setCellValue("MIT Academy of Engineering\n(An Autonomous Institute)\nAlandi (D), Pune - 412 105");
        instCell.setCellStyle(normalStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 2));
        
        org.apache.poi.ss.usermodel.Cell titleCell = row1.createCell(3);
        titleCell.setCellValue("FACULTY WISE TIME TABLE");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 3, numSlots - 3));
        
        org.apache.poi.ss.usermodel.Cell fnLabelCell = row1.createCell(numSlots - 2);
        fnLabelCell.setCellValue("FACULTY NAME");
        fnLabelCell.setCellStyle(normalStyle);
        
        org.apache.poi.ss.usermodel.Cell profNameCell = row1.createCell(numSlots - 1);
        profNameCell.setCellValue(professor.getName() != null ? professor.getName().toUpperCase() : "");
        profNameCell.setCellStyle(normalStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, numSlots - 1, numSlots));
        
        // Row 2: Department | Academic Year | Teaching Load | TH | Total
        Row row2 = sheet.createRow(rowIdx++);
        org.apache.poi.ss.usermodel.Cell deptCell = row2.createCell(0);
        deptCell.setCellValue("SCHOOL OF COMPUTER ENGINEERING");
        deptCell.setCellStyle(normalStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 2));
        
        org.apache.poi.ss.usermodel.Cell ayLabelCell = row2.createCell(3);
        ayLabelCell.setCellValue("ACADEMIC YEAR :");
        ayLabelCell.setCellStyle(normalStyle);
        
        org.apache.poi.ss.usermodel.Cell ayValueCell = row2.createCell(4);
        ayValueCell.setCellValue(academicYear.getYearName());
        ayValueCell.setCellStyle(normalStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 4, numSlots - 5));
        
        org.apache.poi.ss.usermodel.Cell tlLabelCell = row2.createCell(numSlots - 4);
        tlLabelCell.setCellValue("TEACHING LOAD");
        tlLabelCell.setCellStyle(normalStyle);
        
        org.apache.poi.ss.usermodel.Cell thCell = row2.createCell(numSlots - 3);
        thCell.setCellValue("TH (" + thCount + ")");
        thCell.setCellStyle(normalStyle);
        
        org.apache.poi.ss.usermodel.Cell totalCell = row2.createCell(numSlots - 2);
        totalCell.setCellValue("TOTAL : " + totalCount);
        totalCell.setCellStyle(normalStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, numSlots - 2, numSlots));
        
        // Row 3: Empty | Semester | Empty | PR/TU/Proj
        Row row3 = sheet.createRow(rowIdx++);
        org.apache.poi.ss.usermodel.Cell semLabelCell = row3.createCell(3);
        semLabelCell.setCellValue("SEMESTER :");
        semLabelCell.setCellStyle(normalStyle);
        
        org.apache.poi.ss.usermodel.Cell semValueCell = row3.createCell(4);
        semValueCell.setCellValue(semester != null ? semester.name().replace("_", " ") : "");
        semValueCell.setCellStyle(normalStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 4, numSlots - 5));
        
        org.apache.poi.ss.usermodel.Cell prCell = row3.createCell(numSlots - 3);
        prCell.setCellValue("PR (" + prCount + ") / TU\n/ Proj (0)");
        prCell.setCellStyle(normalStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, numSlots - 3, numSlots));
        
        // Row 4: Empty | W.E.F | Date
        Row row4 = sheet.createRow(rowIdx++);
        org.apache.poi.ss.usermodel.Cell wefLabelCell = row4.createCell(3);
        wefLabelCell.setCellValue("W.E.F :");
        wefLabelCell.setCellStyle(normalStyle);
        
        org.apache.poi.ss.usermodel.Cell wefValueCell = row4.createCell(4);
        wefValueCell.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        wefValueCell.setCellStyle(normalStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 4, numSlots));
        
        // Empty row
        rowIdx++;
        
        // Timetable header row: Day/Time + slot numbers with time ranges
        Row headerRow = sheet.createRow(rowIdx++);
        headerRow.setHeightInPoints(30);
        org.apache.poi.ss.usermodel.Cell dayTimeCell = headerRow.createCell(0);
        dayTimeCell.setCellValue("Day / Time");
        dayTimeCell.setCellStyle(headerStyle);
        
        int slotNumber = 1;
        for (int i = 0; i < slots.size(); i++) {
            TimeSlot slot = slots.get(i);
            String timeRange = formatTime(slot);
            org.apache.poi.ss.usermodel.Cell slotCell = headerRow.createCell(i + 1);
            slotCell.setCellValue(timeRange + "\n" + slotNumber);
            slotCell.setCellStyle(headerStyle);
            slotNumber++;
        }
        
        // Day rows
        for (String day : new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT"}) {
            Row dayRow = sheet.createRow(rowIdx++);
            dayRow.setHeightInPoints(40);
            
            org.apache.poi.ss.usermodel.Cell dayCell = dayRow.createCell(0);
            dayCell.setCellValue(day);
            dayCell.setCellStyle(headerStyle);
            
            for (int i = 0; i < slots.size(); i++) {
                TimeSlot slot = slots.get(i);
                org.apache.poi.ss.usermodel.Cell entryCell = dayRow.createCell(i + 1);
                
                if (Boolean.TRUE.equals(slot.getIsBreak())) {
                    // Break cell
                    String breakText = slot.getSlotName().contains("LUNCH") ? "L\nU\nN\nC\nH\n\nB\nR\nE\nA\nK" : "S\nH\nO\nR\nT\n\nB\nR\nE\nA\nK";
                    entryCell.setCellValue(breakText);
                    entryCell.setCellStyle(breakStyle);
                } else {
                    // Regular entry cell
                    String dayName = day.equals("MON") ? "MONDAY" : 
                                    day.equals("TUE") ? "TUESDAY" :
                                    day.equals("WED") ? "WEDNESDAY" :
                                    day.equals("THU") ? "THURSDAY" :
                                    day.equals("FRI") ? "FRIDAY" : "SATURDAY";
                    String key = dayName + ":" + slot.getId();
                    TimetableEntry entry = lookup.get(key);
                    
                    if (entry != null) {
                        String courseName = getCourseShortName(entry.getCourse());
                        String profInitials = getProfessorShortName(professor);
                        String roomLocation = entry.getRoom() != null ? entry.getRoom().getRoomNumber() : "";
                        boolean isLab = entry.getCourse() != null && entry.getCourse().getCourseType() == CourseType.LAB;
                        
                        // Get year label
                        String yearLabel = "";
                        if (entry.getDivision() != null && entry.getDivision().getYear() != null) {
                            yearLabel = getYearLabel(entry.getDivision().getYear());
                        }
                        
                        if (isLab && entry.getBatch() != null) {
                            // Lab format: "FY B1 - Course Name - H306B"
                            String batchName = entry.getBatch().getName();
                            entryCell.setCellValue(yearLabel + " " + batchName + " - " + courseName + " - " + roomLocation);
                        } else {
                            // Theory format: "SY A - Course Name - H301"
                            String divisionName = entry.getDivision() != null ? entry.getDivision().getName() : "";
                            entryCell.setCellValue(yearLabel + " " + divisionName + " - " + courseName + " - " + roomLocation);
                        }
                    }
                    entryCell.setCellStyle(cellStyle);
                }
            }
        }
        
        // Footer row
        rowIdx++;
        Row footerRow = sheet.createRow(rowIdx);
        org.apache.poi.ss.usermodel.Cell footerLeft = footerRow.createCell(0);
        footerLeft.setCellValue("Time Table Coordinator");
        footerLeft.setCellStyle(normalStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, numSlots / 2));
        
        org.apache.poi.ss.usermodel.Cell footerRight = footerRow.createCell(numSlots / 2 + 1);
        footerRight.setCellValue("HOD Comp Engg Dept");
        footerRight.setCellStyle(normalStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, numSlots / 2 + 1, numSlots));
        
        // Auto-size columns
        sheet.setColumnWidth(0, 3000);
        for (int i = 1; i <= numSlots; i++) {
            sheet.setColumnWidth(i, 3500);
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    /**
     * Generate PDF for room timetable (standard format)
     * Shows all classes scheduled in the room across all divisions
     */
    public byte[] generateRoomPDF(Long roomId, Long academicYearId, Semester semester) throws Exception {
        // Fetch data
        ClassRoom room = roomRepo.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));
        AcademicYear academicYear = academicYearRepo.findById(academicYearId)
            .orElseThrow(() -> new RuntimeException("Academic year not found"));
        List<TimetableEntry> entries = timetableEntryRepo.findPublishedByRoomAndSemester(
            roomId, academicYearId, semester
        );
        
        // Get time slots — detect type from entries
        List<TimeSlot> slots = getSortedSlotsForEntries(entries);
        
        // Build lookup map
        Map<String, TimetableEntry> lookup = new HashMap<>();
        for (TimetableEntry e : entries) {
            if (e.getDayOfWeek() != null && e.getTimeSlot() != null) {
                String key = e.getDayOfWeek().name() + ":" + e.getTimeSlot().getId();
                lookup.put(key, e);
            }
        }
        
        // Create PDF
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 20, 20, 30, 20);
        PdfWriter.getInstance(doc, out);
        doc.open();
        
        // Fonts
        Font headerFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(27, 42, 78));
        Font subFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
        Font thFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
        Font cellFont = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.BLACK);
        Font cellBold = new Font(Font.HELVETICA, 7, Font.BOLD, new Color(27, 42, 78));
        Font breakFont = new Font(Font.HELVETICA, 7, Font.ITALIC, Color.GRAY);
        
        // Header
        doc.add(new Paragraph("MIT Academy of Engineering", new Font(Font.HELVETICA, 10, Font.BOLD, Color.DARK_GRAY)));
        String title = String.format("Room Timetable — %s (%s) — %s — %s",
            room.getName(), room.getRoomNumber(),
            academicYear.getYearName(),
            semester != null ? semester.name().replace("_", " ") : "");
        doc.add(new Paragraph(title, headerFont));
        doc.add(new Paragraph("Generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), subFont));
        doc.add(new Paragraph(" "));
        
        // Table: 1 time column + 6 day columns
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{12, 15, 15, 15, 15, 15, 13});
        
        // Header row
        Color headerBg = new Color(27, 42, 78);
        PdfPCell timeHeaderCell = new PdfPCell(new Phrase("Time", thFont));
        timeHeaderCell.setBackgroundColor(headerBg);
        timeHeaderCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        timeHeaderCell.setPadding(4);
        table.addCell(timeHeaderCell);
        
        for (String day : DAY_LABELS) {
            PdfPCell dayHeaderCell = new PdfPCell(new Phrase(day, thFont));
            dayHeaderCell.setBackgroundColor(headerBg);
            dayHeaderCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            dayHeaderCell.setPadding(4);
            table.addCell(dayHeaderCell);
        }
        
        // Data rows
        for (TimeSlot slot : slots) {
            if (Boolean.TRUE.equals(slot.getIsBreak())) {
                // Break row
                PdfPCell timeCell = new PdfPCell(new Phrase(formatTime(slot) + "\n" + slot.getSlotName(), breakFont));
                timeCell.setBackgroundColor(new Color(240, 240, 240));
                timeCell.setPadding(4);
                table.addCell(timeCell);
                
                PdfPCell breakCell = new PdfPCell(new Phrase(slot.getSlotName(), breakFont));
                breakCell.setColspan(6);
                breakCell.setBackgroundColor(new Color(240, 240, 240));
                breakCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                breakCell.setPadding(4);
                table.addCell(breakCell);
                continue;
            }
            
            // Time cell
            PdfPCell timeCell = new PdfPCell();
            timeCell.addElement(new Phrase(formatTime(slot), cellBold));
            timeCell.addElement(new Phrase(slot.getSlotName(), subFont));
            timeCell.setBackgroundColor(new Color(248, 249, 250));
            timeCell.setPadding(4);
            table.addCell(timeCell);
            
            // Day cells
            for (String day : DAY_NAMES) {
                String key = day + ":" + slot.getId();
                TimetableEntry entry = lookup.get(key);
                
                PdfPCell cell = new PdfPCell();
                cell.setPadding(3);
                cell.setMinimumHeight(35);
                
                if (entry != null) {
                    String courseName = getCourseShortName(entry.getCourse());
                    String profInitials = entry.getTeacher() != null ? getProfessorShortName(entry.getTeacher()) : "-";
                    String divisionName = "";
                    String yearLabel = "";
                    if (entry.getDivision() != null) {
                        String branch = entry.getDivision().getDepartment() != null ? entry.getDivision().getDepartment().getCode() : "";
                        String year = String.valueOf(entry.getDivision().getYear());
                        String div = entry.getDivision().getName();
                        divisionName = String.format("%s %s %s", branch, year, div).trim();
                        yearLabel = getYearLabel(entry.getDivision().getYear());
                    }
                    boolean isLab = entry.getCourse() != null && entry.getCourse().getCourseType() == CourseType.LAB;
                    
                    if (isLab && entry.getBatch() != null) {
                        // Lab format: "FY B1 - Course Name - Division"
                        String batchName = entry.getBatch().getName();
                        String displayText = yearLabel + " " + batchName + " - " + courseName + " - " + divisionName;
                        cell.addElement(new Phrase(displayText, cellFont));
                    } else {
                        // Theory format: "SY A - Course Name - Division"
                        String divName = entry.getDivision() != null ? entry.getDivision().getName() : "";
                        String displayText = yearLabel + " " + divName + " - " + courseName + " - " + divisionName;
                        cell.addElement(new Phrase(displayText, cellFont));
                    }
                    cell.setBackgroundColor(isLab ? new Color(243, 232, 255) : new Color(232, 240, 254));
                } else {
                    cell.addElement(new Phrase("-", breakFont));
                }
                table.addCell(cell);
            }
        }
        
        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    /**
     * Generate Excel for room timetable (standard format)
     * Shows all classes scheduled in the room across all divisions
     */
    public byte[] generateRoomExcel(Long roomId, Long academicYearId, Semester semester) throws Exception {
        // Fetch data
        ClassRoom room = roomRepo.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));
        AcademicYear academicYear = academicYearRepo.findById(academicYearId)
            .orElseThrow(() -> new RuntimeException("Academic year not found"));
        List<TimetableEntry> entries = timetableEntryRepo.findPublishedByRoomAndSemester(
            roomId, academicYearId, semester
        );
        
        // Get time slots — detect type from entries
        List<TimeSlot> slots = getSortedSlotsForEntries(entries);
        
        // Build lookup map
        Map<String, TimetableEntry> lookup = new HashMap<>();
        for (TimetableEntry e : entries) {
            if (e.getDayOfWeek() != null && e.getTimeSlot() != null) {
                String key = e.getDayOfWeek().name() + ":" + e.getTimeSlot().getId();
                lookup.put(key, e);
            }
        }
        
        // Create Excel workbook
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("Room Timetable");
        
        // Styles
        XSSFCellStyle titleStyle = wb.createCellStyle();
        XSSFFont titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        
        XSSFCellStyle headerStyle = wb.createCellStyle();
        XSSFFont headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 10);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{27, 42, 78}, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        
        XSSFCellStyle cellStyle = wb.createCellStyle();
        cellStyle.setWrapText(true);
        cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        
        XSSFCellStyle breakStyle = wb.createCellStyle();
        breakStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 240, (byte) 240, (byte) 240}, null));
        breakStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        breakStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFFont breakFont = wb.createFont();
        breakFont.setItalic(true);
        breakFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        breakStyle.setFont(breakFont);
        
        XSSFCellStyle theoryStyle = wb.createCellStyle();
        theoryStyle.cloneStyleFrom(cellStyle);
        theoryStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 232, (byte) 240, (byte) 254}, null));
        theoryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        XSSFCellStyle labStyle = wb.createCellStyle();
        labStyle.cloneStyleFrom(cellStyle);
        labStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 243, (byte) 232, (byte) 255}, null));
        labStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // Title rows
        Row titleRow = sheet.createRow(0);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("MIT Academy of Engineering");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
        
        Row subtitleRow = sheet.createRow(1);
        String title = String.format("Room Timetable — %s (%s) — %s — %s",
            room.getName(), room.getRoomNumber(),
            academicYear.getYearName(),
            semester != null ? semester.name().replace("_", " ") : "");
        subtitleRow.createCell(0).setCellValue(title);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));
        
        Row dateRow = sheet.createRow(2);
        dateRow.createCell(0).setCellValue("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        
        // Header row
        int rowIdx = 4;
        Row headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"Time", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }
        
        // Data rows
        for (TimeSlot slot : slots) {
            Row row = sheet.createRow(rowIdx++);
            
            if (Boolean.TRUE.equals(slot.getIsBreak())) {
                org.apache.poi.ss.usermodel.Cell timeC = row.createCell(0);
                timeC.setCellValue(formatTime(slot));
                timeC.setCellStyle(breakStyle);
                
                org.apache.poi.ss.usermodel.Cell breakC = row.createCell(1);
                breakC.setCellValue(slot.getSlotName());
                breakC.setCellStyle(breakStyle);
                for (int i = 2; i <= 6; i++) {
                    row.createCell(i).setCellStyle(breakStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 6));
                continue;
            }
            
            // Time cell
            org.apache.poi.ss.usermodel.Cell timeCell = row.createCell(0);
            timeCell.setCellValue(formatTime(slot) + "\n" + slot.getSlotName());
            timeCell.setCellStyle(cellStyle);
            row.setHeightInPoints(45);
            
            // Day cells
            for (int d = 0; d < DAY_NAMES.length; d++) {
                String key = DAY_NAMES[d] + ":" + slot.getId();
                TimetableEntry entry = lookup.get(key);
                org.apache.poi.ss.usermodel.Cell cell = row.createCell(d + 1);
                
                if (entry != null) {
                    String courseName = getCourseShortName(entry.getCourse());
                    String profInitials = entry.getTeacher() != null ? getProfessorShortName(entry.getTeacher()) : "-";
                    String divisionName = "";
                    String yearLabel = "";
                    if (entry.getDivision() != null) {
                        String branch = entry.getDivision().getDepartment() != null ? entry.getDivision().getDepartment().getCode() : "";
                        String year = String.valueOf(entry.getDivision().getYear());
                        String div = entry.getDivision().getName();
                        divisionName = String.format("%s %s %s", branch, year, div).trim();
                        yearLabel = getYearLabel(entry.getDivision().getYear());
                    }
                    boolean isLab = entry.getCourse() != null && entry.getCourse().getCourseType() == CourseType.LAB;
                    
                    String displayText;
                    if (isLab && entry.getBatch() != null) {
                        // Lab format: "FY B1 - Course Name - Division"
                        String batchName = entry.getBatch().getName();
                        displayText = yearLabel + " " + batchName + " - " + courseName + " - " + divisionName;
                    } else {
                        // Theory format: "SY A - Course Name - Division"
                        String divName = entry.getDivision() != null ? entry.getDivision().getName() : "";
                        displayText = yearLabel + " " + divName + " - " + courseName + " - " + divisionName;
                    }
                    
                    cell.setCellValue(displayText);
                    cell.setCellStyle(isLab ? labStyle : theoryStyle);
                } else {
                    cell.setCellValue("-");
                    cell.setCellStyle(cellStyle);
                }
            }
        }
        
        // Auto-size columns
        for (int i = 0; i < 7; i++) {
            sheet.setColumnWidth(i, i == 0 ? 4500 : 5000);
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════
    // PDF GENERATION (Semester-Specific) - EXISTING METHODS
    // ═══════════════════════════════════════════════════════════════

    public byte[] generateDivisionPDF(Long divisionId, Long academicYearId, Semester semester) throws Exception {
        // Get all entries for division and filter by semester
        List<TimetableEntry> allEntries = timetableService.getDivisionTimetable(divisionId, academicYearId);
        List<TimetableEntry> entries = allEntries.stream()
            .filter(e -> e.getSemester() == semester)
            .collect(Collectors.toList());
            
        Division division = divisionRepo.findById(divisionId).orElse(null);
        AcademicYear year = academicYearRepo.findById(academicYearId).orElse(null);
        List<TimeSlot> slots = getSortedSlots(division);

        return buildInstitutionalDivisionPDF(entries, slots, division, year, semester);
    }

    public byte[] generateTeacherPDF(Long teacherId, Long academicYearId) throws Exception {
        List<TimetableEntry> entries = timetableService.getTeacherTimetable(teacherId, academicYearId);
        TeacherEntity teacher = teacherRepo.findById(teacherId).orElse(null);
        AcademicYear year = academicYearRepo.findById(academicYearId).orElse(null);
        List<TimeSlot> slots = getSortedSlotsForEntries(entries);

        return buildInstitutionalTeacherPDF(entries, slots, teacher, year);
    }

    // ═══════════════════════════════════════════════════════════════
    // INSTITUTIONAL TEACHER PDF (Official MITAOE Faculty Format)
    // ═══════════════════════════════════════════════════════════════

    private byte[] buildInstitutionalTeacherPDF(
            List<TimetableEntry> entries,
            List<TimeSlot> allSlots,
            TeacherEntity teacher,
            AcademicYear academicYear) throws Exception {

        // Separate break slots and teaching slots
        List<TimeSlot> teachingSlots = new ArrayList<>();
        List<Integer> breakIndices = new ArrayList<>(); // indices into allSlots that are breaks
        for (int i = 0; i < allSlots.size(); i++) {
            if (Boolean.TRUE.equals(allSlots.get(i).getIsBreak())) {
                breakIndices.add(i);
            } else {
                teachingSlots.add(allSlots.get(i));
            }
        }

        // Build multi-value lookup for entries
        Map<String, List<TimetableEntry>> multiLookup = buildMultiLookup(entries);

        // Teaching load calculation
        long theoryCount = entries.stream()
            .filter(e -> e.getCourse() != null && e.getCourse().getCourseType() == CourseType.THEORY)
            .count();
        long labCount = entries.stream()
            .filter(e -> e.getCourse() != null && e.getCourse().getCourseType() == CourseType.LAB)
            .count();
        long totalLoad = theoryCount + labCount;

        // Detect semester(s) from entries
        Set<Semester> semesters = entries.stream()
            .map(TimetableEntry::getSemester)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        String semesterDisplay = semesters.stream()
            .map(this::toRomanNumeral)
            .collect(Collectors.joining(", "));
        if (semesterDisplay.isEmpty()) semesterDisplay = "-";

        // Teacher info
        String facultyName = teacher != null ? teacher.getName().toUpperCase() : "UNKNOWN";
        String deptName = teacher != null && teacher.getDepartment() != null
            ? "SCHOOL OF " + teacher.getDepartment().getName().toUpperCase()
            : "SCHOOL OF COMPUTER ENGINEERING";
        String yearName = academicYear != null ? academicYear.getYearName() : "-";

        // ── Create PDF — Landscape A4 ──
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 10, 10, 8, 8);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // ── Font definitions ──
        Font titleFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
        Font metaLabelFont = new Font(Font.HELVETICA, 7, Font.BOLD, Color.BLACK);
        Font metaValueFont = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.BLACK);
        Font smallFont = new Font(Font.HELVETICA, 6, Font.NORMAL, Color.DARK_GRAY);
        Font deptFont = new Font(Font.HELVETICA, 7, Font.BOLD, Color.BLACK);
        Font facultyNameFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);

        Color headerBgColor = new Color(214, 228, 240);
        Font gridHeaderFont = new Font(Font.HELVETICA, 6, Font.BOLD, new Color(27, 42, 78));
        Font dayFont = new Font(Font.HELVETICA, 7, Font.BOLD, Color.BLACK);
        Font cellCourseFont = new Font(Font.HELVETICA, 6.5f, Font.BOLD, Color.BLACK);
        Font cellDetailFont = new Font(Font.HELVETICA, 6, Font.NORMAL, Color.BLACK);
        Font breakVertFont = new Font(Font.HELVETICA, 6, Font.BOLD, Color.GRAY);
        Font sigFont = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.BLACK);

        float hdrBorderWidth = 0.5f;

        // ═══════════════════════════════════════════════════════════
        // HEADER TABLE: 3 columns — Institution | Meta Labels | Meta Values
        // ═══════════════════════════════════════════════════════════
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{40f, 30f, 30f});

        // ── ROW 1: Logo+Address | "FACULTY WISE TIME TABLE" | FACULTY NAME ──
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorderWidth(hdrBorderWidth);
        logoCell.setPadding(3);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setRowspan(2);
        try {
            Image logo = Image.getInstance(getClass().getResource("/static/mitaoe_logo.png"));
            logo.scaleToFit(120, 28);
            Paragraph logoPara = new Paragraph();
            logoPara.add(new Chunk(logo, 0, 0, true));
            logoPara.add(new Chunk("  Alandi (D), Pune - 412 105", smallFont));
            logoCell.addElement(logoPara);
        } catch (Exception e) {
            logoCell.setPhrase(new Phrase("MIT Academy of Engineering, Alandi (D), Pune - 412 105", metaLabelFont));
        }
        headerTable.addCell(logoCell);

        PdfPCell titleCell = new PdfPCell(new Phrase("FACULTY WISE TIME TABLE", titleFont));
        titleCell.setBorderWidth(hdrBorderWidth);
        titleCell.setPadding(3);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleCell.setRowspan(2);
        headerTable.addCell(titleCell);

        PdfPCell fnLabel = new PdfPCell(new Phrase("FACULTY NAME", metaLabelFont));
        fnLabel.setBorderWidth(hdrBorderWidth);
        fnLabel.setPadding(3);
        fnLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
        fnLabel.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(fnLabel);

        // ── ROW 2: (logo continues) | (title continues) | Faculty Name Value ──
        PdfPCell fnValue = new PdfPCell(new Phrase(facultyName, facultyNameFont));
        fnValue.setBorderWidth(hdrBorderWidth);
        fnValue.setPadding(3);
        fnValue.setHorizontalAlignment(Element.ALIGN_CENTER);
        fnValue.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(fnValue);

        // ── ROW 3: Department | ACADEMIC YEAR : value | Teaching Load Header ──
        PdfPCell deptCell = new PdfPCell(new Phrase(deptName, deptFont));
        deptCell.setBorderWidth(hdrBorderWidth);
        deptCell.setPadding(3);
        deptCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        deptCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(deptCell);

        PdfPCell ayCell = new PdfPCell();
        ayCell.setBorderWidth(hdrBorderWidth);
        ayCell.setPadding(3);
        ayCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph ayPara = new Paragraph();
        ayPara.add(new Chunk("ACADEMIC YEAR : ", metaLabelFont));
        ayPara.add(new Chunk(yearName, metaValueFont));
        ayCell.addElement(ayPara);
        headerTable.addCell(ayCell);

        // Teaching load block: TH / PR/TU / TOTAL
        PdfPCell loadCell = new PdfPCell();
        loadCell.setBorderWidth(hdrBorderWidth);
        loadCell.setPadding(3);
        loadCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        loadCell.setRowspan(3);
        loadCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph loadPara = new Paragraph();
        loadPara.setAlignment(Element.ALIGN_CENTER);
        loadPara.add(new Chunk("TEACHING LOAD\n", metaLabelFont));
        loadPara.add(new Chunk("TH (" + theoryCount + ")\n", metaValueFont));
        loadPara.add(new Chunk("PR / TU (" + labCount + ")\n", metaValueFont));
        loadPara.add(new Chunk("TOTAL : " + totalLoad, new Font(Font.HELVETICA, 7, Font.BOLD, Color.BLACK)));
        loadCell.addElement(loadPara);
        headerTable.addCell(loadCell);

        // ── ROW 4: (empty) | SEMESTER : value | (load continues) ──
        PdfPCell emptyCell4 = new PdfPCell(new Phrase("", metaValueFont));
        emptyCell4.setBorderWidth(hdrBorderWidth);
        emptyCell4.setPadding(3);
        headerTable.addCell(emptyCell4);

        PdfPCell semCell = new PdfPCell();
        semCell.setBorderWidth(hdrBorderWidth);
        semCell.setPadding(3);
        semCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph semPara = new Paragraph();
        semPara.add(new Chunk("SEMESTER : ", metaLabelFont));
        semPara.add(new Chunk(semesterDisplay, metaValueFont));
        semCell.addElement(semPara);
        headerTable.addCell(semCell);

        // ── ROW 5: (empty) | W.E.F : date | (load continues) ──
        PdfPCell emptyCell5 = new PdfPCell(new Phrase("", metaValueFont));
        emptyCell5.setBorderWidth(hdrBorderWidth);
        emptyCell5.setPadding(3);
        headerTable.addCell(emptyCell5);

        PdfPCell wefCell = new PdfPCell();
        wefCell.setBorderWidth(hdrBorderWidth);
        wefCell.setPadding(3);
        wefCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph wefPara = new Paragraph();
        wefPara.add(new Chunk("W.E.F : ", metaLabelFont));
        wefPara.add(new Chunk(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), metaValueFont));
        wefCell.addElement(wefPara);
        headerTable.addCell(wefCell);

        doc.add(headerTable);
        doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 2)));

        // ═══════════════════════════════════════════════════════════
        // TIMETABLE GRID: Days = Rows, Slots = Columns (inverted grid)
        // With break columns rendered as vertical merged text
        // ═══════════════════════════════════════════════════════════

        // Build column structure: Day label + all slots (teaching + breaks interleaved)
        // allSlots is already sorted by time; some are breaks, some are teaching
        int totalCols = 1 + allSlots.size(); // 1 for Day column + all slots
        PdfPTable grid = new PdfPTable(totalCols);
        grid.setWidthPercentage(100);

        // Column widths: day=8%, each slot gets equal share of 92%
        float[] colWidths = new float[totalCols];
        colWidths[0] = 7f;
        float slotWidth = 93f / allSlots.size();
        for (int i = 1; i < totalCols; i++) {
            // Break columns narrower
            colWidths[i] = Boolean.TRUE.equals(allSlots.get(i - 1).getIsBreak()) ? slotWidth * 0.5f : slotWidth;
        }
        grid.setWidths(colWidths);

        // ── Header Row 1: "Time" + time ranges ──
        PdfPCell timeHeader = new PdfPCell(new Phrase("Time", gridHeaderFont));
        timeHeader.setBackgroundColor(headerBgColor);
        timeHeader.setBorderWidth(0.5f);
        timeHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        timeHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
        timeHeader.setPadding(2);
        timeHeader.setRowspan(2);
        grid.addCell(timeHeader);

        for (int i = 0; i < allSlots.size(); i++) {
            TimeSlot slot = allSlots.get(i);
            if (Boolean.TRUE.equals(slot.getIsBreak())) {
                // Break column header — vertical text, merged with row 2
                PdfPCell breakHdr = new PdfPCell();
                breakHdr.setBackgroundColor(new Color(240, 240, 240));
                breakHdr.setBorderWidth(0.5f);
                breakHdr.setHorizontalAlignment(Element.ALIGN_CENTER);
                breakHdr.setVerticalAlignment(Element.ALIGN_MIDDLE);
                breakHdr.setPadding(1);
                breakHdr.setRowspan(2 + DAY_NAMES.length); // span header rows + all day rows

                // Vertical text for break name
                String breakName = slot.getSlotName() != null ? slot.getSlotName() : "BREAK";
                StringBuilder vertText = new StringBuilder();
                for (char c : breakName.toCharArray()) {
                    vertText.append(c).append("\n");
                }
                breakHdr.setPhrase(new Phrase(vertText.toString().trim(), breakVertFont));
                grid.addCell(breakHdr);
            } else {
                // Teaching slot header — time range
                String timeRange = formatTime(slot);
                PdfPCell slotHdr = new PdfPCell(new Phrase(timeRange, gridHeaderFont));
                slotHdr.setBackgroundColor(headerBgColor);
                slotHdr.setBorderWidth(0.5f);
                slotHdr.setHorizontalAlignment(Element.ALIGN_CENTER);
                slotHdr.setVerticalAlignment(Element.ALIGN_MIDDLE);
                slotHdr.setPadding(2);
                grid.addCell(slotHdr);
            }
        }

        // ── Header Row 2: "Day" + period numbers ──
        // Note: "Day" header is already part of the rowspan from Row 1
        int periodNum = 1;
        for (int i = 0; i < allSlots.size(); i++) {
            TimeSlot slot = allSlots.get(i);
            if (Boolean.TRUE.equals(slot.getIsBreak())) {
                // Already merged via rowspan above — skip
                continue;
            }
            PdfPCell numCell = new PdfPCell(new Phrase(String.valueOf(periodNum++), gridHeaderFont));
            numCell.setBackgroundColor(headerBgColor);
            numCell.setBorderWidth(0.5f);
            numCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            numCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            numCell.setPadding(2);
            grid.addCell(numCell);
        }

        // Track merged cells for lab sessions
        Set<String> mergedCells = new HashSet<>();

        // ── Data Rows: one per day ──
        for (int d = 0; d < DAY_NAMES.length; d++) {
            String day = DAY_NAMES[d];
            String dayLabel = DAY_LABELS[d].substring(0, 3).toUpperCase();

            PdfPCell dayCell = new PdfPCell(new Phrase(dayLabel, dayFont));
            dayCell.setBorderWidth(0.5f);
            dayCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            dayCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dayCell.setPadding(3);
            dayCell.setMinimumHeight(45);
            grid.addCell(dayCell);

            for (int slotIdx = 0; slotIdx < allSlots.size(); slotIdx++) {
                TimeSlot slot = allSlots.get(slotIdx);

                if (Boolean.TRUE.equals(slot.getIsBreak())) {
                    // Break columns already handled by rowspan above
                    continue;
                }

                String cellKey = day + ":" + slotIdx;
                if (mergedCells.contains(cellKey)) {
                    // This cell is part of a colspan merge — skip
                    continue;
                }

                String key = day + ":" + slot.getId();
                List<TimetableEntry> cellEntries = multiLookup.getOrDefault(key, Collections.emptyList());

                PdfPCell cell = new PdfPCell();
                cell.setBorderWidth(0.5f);
                cell.setPadding(2);
                cell.setMinimumHeight(45);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);

                if (!cellEntries.isEmpty()) {
                    TimetableEntry entry = cellEntries.get(0);
                    boolean isLab = entry.getCourse() != null && entry.getCourse().getCourseType() == CourseType.LAB;
                    String courseName = getCourseShortName(entry.getCourse());
                    String roomNum = entry.getRoom() != null ? entry.getRoom().getRoomNumber() : "-";

                    // Division/batch info
                    String divInfo = "";
                    if (entry.getDivision() != null) {
                        String yearLabel = entry.getDivision().getYear() != null ? getYearLabel(entry.getDivision().getYear()) : "";
                        String divName = entry.getDivision().getName() != null ? entry.getDivision().getName() : "";
                        divInfo = yearLabel + " " + divName;
                    }
                    String batchName = entry.getBatch() != null ? entry.getBatch().getName() : null;

                    // Check for colspan merge (consecutive lab slots)
                    boolean shouldMerge = false;
                    if (isLab && slotIdx + 1 < allSlots.size()) {
                        // Find next non-break slot
                        int nextTeachingIdx = -1;
                        for (int ni = slotIdx + 1; ni < allSlots.size(); ni++) {
                            if (!Boolean.TRUE.equals(allSlots.get(ni).getIsBreak())) {
                                nextTeachingIdx = ni;
                                break;
                            }
                        }
                        if (nextTeachingIdx >= 0) {
                            TimeSlot nextSlot = allSlots.get(nextTeachingIdx);
                            String nextKey = day + ":" + nextSlot.getId();
                            List<TimetableEntry> nextEntries = multiLookup.getOrDefault(nextKey, Collections.emptyList());
                            if (!nextEntries.isEmpty()) {
                                TimetableEntry nextEntry = nextEntries.get(0);
                                if (nextEntry.getCourse() != null &&
                                    nextEntry.getCourse().getCourseType() == CourseType.LAB &&
                                    entry.getCourse().getId().equals(nextEntry.getCourse().getId()) &&
                                    Objects.equals(entry.getTeacher() != null ? entry.getTeacher().getId() : null,
                                                   nextEntry.getTeacher() != null ? nextEntry.getTeacher().getId() : null)) {
                                    shouldMerge = true;
                                    // Calculate colspan (skip over any breaks between)
                                    int colspan = nextTeachingIdx - slotIdx + 1;
                                    // Count how many break cells are in between (they're already rowspanned)
                                    int breaksBetween = 0;
                                    for (int bi = slotIdx + 1; bi < nextTeachingIdx; bi++) {
                                        if (Boolean.TRUE.equals(allSlots.get(bi).getIsBreak())) {
                                            breaksBetween++;
                                        }
                                    }
                                    cell.setColspan(colspan - breaksBetween);
                                    mergedCells.add(day + ":" + nextTeachingIdx);
                                }
                            }
                        }
                    }

                    // Build cell content — NO teacher short name (it's in the header)
                    Paragraph cellPara = new Paragraph();
                    cellPara.setAlignment(Element.ALIGN_CENTER);
                    cellPara.setLeading(8f);

                    if (isLab && batchName != null) {
                        // Lab format: "B2-DAA Lab\nCNL II (H204B)"
                        cellPara.add(new Chunk(divInfo, cellDetailFont));
                        cellPara.add(new Chunk("\n", cellDetailFont));
                        cellPara.add(new Chunk(batchName + "-" + courseName + " Lab", cellCourseFont));
                        cellPara.add(new Chunk("\n", cellDetailFont));
                        cellPara.add(new Chunk("(" + roomNum + ")", cellDetailFont));
                    } else if (isLab) {
                        // Lab without batch
                        cellPara.add(new Chunk(courseName + " Lab", cellCourseFont));
                        cellPara.add(new Chunk("\n", cellDetailFont));
                        cellPara.add(new Chunk(divInfo + " (" + roomNum + ")", cellDetailFont));
                    } else {
                        // Theory format: "DAA\nTY A\nH301"
                        cellPara.add(new Chunk(courseName, cellCourseFont));
                        cellPara.add(new Chunk("\n", cellDetailFont));
                        cellPara.add(new Chunk(divInfo, cellDetailFont));
                        cellPara.add(new Chunk("\n", cellDetailFont));
                        cellPara.add(new Chunk(roomNum, cellDetailFont));
                    }

                    // Handle multiple batches in same slot
                    if (cellEntries.size() > 1) {
                        for (int ei = 1; ei < cellEntries.size(); ei++) {
                            TimetableEntry extraEntry = cellEntries.get(ei);
                            String extraBatch = extraEntry.getBatch() != null ? extraEntry.getBatch().getName() : "";
                            String extraRoom = extraEntry.getRoom() != null ? extraEntry.getRoom().getRoomNumber() : "";
                            cellPara.add(new Chunk("\n" + extraBatch + " (" + extraRoom + ")", cellDetailFont));
                        }
                    }

                    cell.addElement(cellPara);
                    cell.setBackgroundColor(isLab ? new Color(248, 244, 255) : Color.WHITE);
                }

                grid.addCell(cell);
            }
        }

        doc.add(grid);
        doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 4)));

        // ═══════════════════════════════════════════════════════════
        // SIGNATURE SECTION
        // ═══════════════════════════════════════════════════════════
        PdfPTable sigTable = new PdfPTable(2);
        sigTable.setWidthPercentage(100);
        sigTable.setWidths(new float[]{50f, 50f});

        PdfPCell sigLeft = new PdfPCell(new Phrase("Time Table Coordinator", sigFont));
        sigLeft.setBorder(0);
        sigLeft.setPaddingTop(12);
        sigLeft.setHorizontalAlignment(Element.ALIGN_LEFT);
        sigTable.addCell(sigLeft);

        PdfPCell sigRight = new PdfPCell(new Phrase("HOD " + deptName.replace("SCHOOL OF ", ""), sigFont));
        sigRight.setBorder(0);
        sigRight.setPaddingTop(12);
        sigRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sigTable.addCell(sigRight);

        doc.add(sigTable);
        doc.close();
        return out.toByteArray();
    }

    private byte[] buildPDF(List<TimetableEntry> entries, List<TimeSlot> slots, String title) throws Exception {
        Map<String, TimetableEntry> lookup = buildLookup(entries);
        
        // Track which cells have been merged (to skip rendering duplicate content)
        java.util.Set<String> mergedCells = new java.util.HashSet<>();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 20, 20, 30, 20);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // Fonts
        Font headerFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(27, 42, 78));
        Font subFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
        Font thFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
        Font cellFont = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.BLACK);
        Font cellBold = new Font(Font.HELVETICA, 7, Font.BOLD, new Color(27, 42, 78));
        Font breakFont = new Font(Font.HELVETICA, 7, Font.ITALIC, Color.GRAY);

        // Header
        doc.add(new Paragraph("MIT Academy of Engineering", new Font(Font.HELVETICA, 10, Font.BOLD, Color.DARK_GRAY)));
        doc.add(new Paragraph(title, headerFont));
        doc.add(new Paragraph("Generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), subFont));
        doc.add(new Paragraph(" "));

        // Table: 1 time column + 6 day columns
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{12, 15, 15, 15, 15, 15, 13});

        // Header row
        Color headerBg = new Color(27, 42, 78);
        addHeaderCell(table, "Time", thFont, headerBg);
        for (String day : DAY_LABELS) {
            addHeaderCell(table, day, thFont, headerBg);
        }

        // Data rows
        for (int slotIdx = 0; slotIdx < slots.size(); slotIdx++) {
            TimeSlot slot = slots.get(slotIdx);
            
            if (Boolean.TRUE.equals(slot.getIsBreak())) {
                // Break row — grey merged
                PdfPCell timeCell = new PdfPCell(new Phrase(formatTime(slot) + "\n" + slot.getSlotName(), breakFont));
                timeCell.setBackgroundColor(new Color(240, 240, 240));
                timeCell.setPadding(4);
                table.addCell(timeCell);

                PdfPCell breakCell = new PdfPCell(new Phrase(slot.getSlotName(), breakFont));
                breakCell.setColspan(6);
                breakCell.setBackgroundColor(new Color(240, 240, 240));
                breakCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                breakCell.setPadding(4);
                table.addCell(breakCell);
                continue;
            }

            // Time cell
            PdfPCell timeCell = new PdfPCell();
            timeCell.addElement(new Phrase(formatTime(slot), cellBold));
            timeCell.addElement(new Phrase(slot.getSlotName(), subFont));
            timeCell.setBackgroundColor(new Color(248, 249, 250));
            timeCell.setPadding(4);
            table.addCell(timeCell);

            // Day cells
            for (int d = 0; d < DAY_NAMES.length; d++) {
                String day = DAY_NAMES[d];
                String key = day + ":" + slot.getId();
                String cellKey = day + ":" + slotIdx;
                
                // Skip if this cell was already merged
                if (mergedCells.contains(cellKey)) {
                    PdfPCell cell = new PdfPCell();
                    cell.setPadding(3);
                    cell.setMinimumHeight(35);
                    table.addCell(cell);
                    continue;
                }
                
                TimetableEntry entry = lookup.get(key);

                PdfPCell cell = new PdfPCell();
                cell.setPadding(3);
                cell.setMinimumHeight(35);

                if (entry != null) {
                    String courseName = getCourseShortName(entry.getCourse());
                    String profInitials = entry.getTeacher() != null ? getProfessorShortName(entry.getTeacher()) : "-";
                    String roomLocation = entry.getRoom() != null ? entry.getRoom().getRoomNumber() : "-";
                    boolean isLab = entry.getCourse() != null && entry.getCourse().getCourseType() == CourseType.LAB;

                    // Get year label
                    String yearLabel = "";
                    if (entry.getDivision() != null && entry.getDivision().getYear() != null) {
                        yearLabel = getYearLabel(entry.getDivision().getYear());
                    }

                    // Check if this is a lab entry with consecutive slot
                    boolean shouldMerge = false;
                    if (isLab && slotIdx + 1 < slots.size()) {
                        TimeSlot nextSlot = slots.get(slotIdx + 1);
                        if (!Boolean.TRUE.equals(nextSlot.getIsBreak())) {
                            String nextKey = day + ":" + nextSlot.getId();
                            TimetableEntry nextEntry = lookup.get(nextKey);
                            
                            // Check if next entry is same lab session
                            // Match by: same course, teacher, room, day
                            if (nextEntry != null && 
                                nextEntry.getCourse() != null && 
                                nextEntry.getCourse().getCourseType() == CourseType.LAB &&
                                entry.getCourse().getId().equals(nextEntry.getCourse().getId()) &&
                                entry.getTeacher().getId().equals(nextEntry.getTeacher().getId()) &&
                                entry.getRoom().getId().equals(nextEntry.getRoom().getId()) &&
                                entry.getDayOfWeek() == nextEntry.getDayOfWeek()) {
                                
                                // If both have labSessionGroup, they must match
                                if (entry.getLabSessionGroup() != null && nextEntry.getLabSessionGroup() != null) {
                                    shouldMerge = entry.getLabSessionGroup().getId().equals(nextEntry.getLabSessionGroup().getId());
                                } else if (entry.getBatch() != null && nextEntry.getBatch() != null) {
                                    // If both have batch, they must match
                                    shouldMerge = entry.getBatch().getId().equals(nextEntry.getBatch().getId());
                                } else {
                                    // Otherwise, match by course/teacher/room is enough
                                    shouldMerge = true;
                                }
                            }
                        }
                    }

                    String displayText;
                    if (isLab && entry.getBatch() != null) {
                        // Lab format: "FY B1 - Course Name - H306B"
                        String batchName = entry.getBatch().getName();
                        displayText = yearLabel + " " + batchName + " - " + courseName + " - " + roomLocation;
                    } else {
                        // Theory format: "SY A - Course Name - H301"
                        String divisionName = entry.getDivision() != null ? entry.getDivision().getName() : "";
                        displayText = yearLabel + " " + divisionName + " - " + courseName + " - " + roomLocation;
                    }

                    if (shouldMerge) {
                        // For PDF, use rowspan to merge cells vertically
                        cell.setRowspan(2);
                        cell.addElement(new Phrase(displayText, cellFont));
                        cell.setBackgroundColor(new Color(243, 232, 255));
                        // Mark next cell as merged
                        mergedCells.add(day + ":" + (slotIdx + 1));
                    } else {
                        cell.addElement(new Phrase(displayText, cellFont));
                        // Apply consistent color: lab=purple, theory=blue
                        cell.setBackgroundColor(isLab ? new Color(243, 232, 255) : new Color(232, 240, 254));
                    }
                } else {
                    cell.addElement(new Phrase("-", breakFont));
                }
                table.addCell(cell);
            }
        }

        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════
    // INSTITUTIONAL DIVISION PDF (Official MITAOE Format)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Convert semester enum to Roman numeral for institutional display.
     * SEM_1 → I, SEM_2 → II, ..., SEM_8 → VIII
     */
    private String toRomanNumeral(Semester semester) {
        if (semester == null) return "";
        switch (semester) {
            case SEM_1: return "I";
            case SEM_2: return "II";
            case SEM_3: return "III";
            case SEM_4: return "IV";
            case SEM_5: return "V";
            case SEM_6: return "VI";
            case SEM_7: return "VII";
            case SEM_8: return "VIII";
            default: return semester.name();
        }
    }

    /**
     * Build a multi-value lookup map for timetable entries.
     * Key: "DAYNAME:slotId", Value: list of all entries at that day+slot.
     * This handles lab sessions where multiple batches occupy the same slot.
     */
    private Map<String, List<TimetableEntry>> buildMultiLookup(List<TimetableEntry> entries) {
        Map<String, List<TimetableEntry>> multiLookup = new HashMap<>();
        for (TimetableEntry e : entries) {
            if (e.getDayOfWeek() != null && e.getTimeSlot() != null) {
                String key = e.getDayOfWeek().name() + ":" + e.getTimeSlot().getId();
                multiLookup.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
            }
        }
        return multiLookup;
    }

    /**
     * Generate division timetable PDF in the official MITAOE institutional format.
     *
     * Layout: Landscape A4, single page.
     * Grid axes: Days = rows (MON-SAT), Time slots = columns.
     * Includes: institutional header with logo, metadata panel, teaching load,
     * course/teacher reference tables, and signature section.
     */
    private byte[] buildInstitutionalDivisionPDF(
            List<TimetableEntry> entries,
            List<TimeSlot> slots,
            Division division,
            AcademicYear academicYear,
            Semester semester) throws Exception {

        // ── Multi-value lookup (supports multiple lab batches per slot) ──
        Map<String, List<TimetableEntry>> multiLookup = buildMultiLookup(entries);

        // ── Calculate teaching load ──
        long theoryCount = entries.stream()
            .filter(e -> e.getCourse() != null && e.getCourse().getCourseType() == CourseType.THEORY)
            .count();
        long labCount = entries.stream()
            .filter(e -> e.getCourse() != null && e.getCourse().getCourseType() == CourseType.LAB)
            .count();
        long totalCount = theoryCount + labCount;

        // ── Collect distinct courses and teachers for reference tables ──
        Map<Long, CourseEntity> distinctCourses = new LinkedHashMap<>();
        Map<Long, TeacherEntity> distinctTeachers = new LinkedHashMap<>();
        Map<Long, Set<String>> courseRooms = new LinkedHashMap<>();
        Map<Long, Set<String>> teacherCourses = new LinkedHashMap<>();

        for (TimetableEntry e : entries) {
            if (e.getCourse() != null) {
                distinctCourses.putIfAbsent(e.getCourse().getId(), e.getCourse());
                String roomNum = e.getRoom() != null ? e.getRoom().getRoomNumber() : "";
                courseRooms.computeIfAbsent(e.getCourse().getId(), k -> new LinkedHashSet<>()).add(roomNum);
            }
            if (e.getTeacher() != null) {
                distinctTeachers.putIfAbsent(e.getTeacher().getId(), e.getTeacher());
                String courseShort = getCourseShortName(e.getCourse());
                teacherCourses.computeIfAbsent(e.getTeacher().getId(), k -> new LinkedHashSet<>()).add(courseShort);
            }
        }

        // ── Create PDF document — Landscape A4, tight margins ──
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 10, 10, 8, 8);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // ── Font definitions ──
        Font titleFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
        Font metaLabelFont = new Font(Font.HELVETICA, 7, Font.BOLD, Color.BLACK);
        Font metaValueFont = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.BLACK);
        Font smallFont = new Font(Font.HELVETICA, 6, Font.NORMAL, Color.DARK_GRAY);
        Font deptFont = new Font(Font.HELVETICA, 7, Font.BOLD, Color.BLACK);

        Color headerBgColor = new Color(214, 228, 240); // Light steel blue
        Font gridHeaderFont = new Font(Font.HELVETICA, 6, Font.BOLD, new Color(27, 42, 78)); // Dark navy text
        Font dayFont = new Font(Font.HELVETICA, 7, Font.BOLD, Color.BLACK);
        Font cellCourseFont = new Font(Font.HELVETICA, 6.5f, Font.BOLD, Color.BLACK);
        Font cellDetailFont = new Font(Font.HELVETICA, 6, Font.NORMAL, Color.BLACK);
        Font labBatchFont = new Font(Font.HELVETICA, 5.5f, Font.NORMAL, Color.BLACK);
        Font breakFont = new Font(Font.HELVETICA, 5.5f, Font.ITALIC, Color.GRAY);
        Font refHeaderFont = new Font(Font.HELVETICA, 6, Font.BOLD, Color.BLACK);
        Font refCellFont = new Font(Font.HELVETICA, 5.5f, Font.NORMAL, Color.BLACK);
        Font sigFont = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.BLACK);

        // ═══════════════════════════════════════════════════════════
        // SECTION 1: INSTITUTIONAL HEADER (3-col × 6-row bordered table)
        // ═══════════════════════════════════════════════════════════

        // Precompute values
        String classLabel = (division != null ? getYearLabel(division.getYear()) : "") + " " +
            (division != null && division.getDepartment() != null ? division.getDepartment().getName() : "");
        String coordinator = division != null && division.getClassTeacher() != null
            ? division.getClassTeacher() : "";
        String divName = division != null ? division.getName() : "";
        String deptName = division != null && division.getDepartment() != null
            ? "DEPARTMENT OF " + division.getDepartment().getName().toUpperCase()
            : "DEPARTMENT OF COMPUTER ENGINEERING";

        // Table: 3 columns × 6 rows, all bordered
        //  Col 0 (50%): Institution info  |  Col 1 (22%): Meta Label  |  Col 2 (28%): Meta Value
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{50f, 22f, 28f});

        float hdrBorderWidth = 0.5f;

        // ──────── ROW 1: Logo + Address | CLASS | value ────────
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorderWidth(hdrBorderWidth);
        logoCell.setPadding(3);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            Image logo = Image.getInstance(getClass().getResource("/static/mitaoe_logo.png"));
            logo.scaleToFit(120, 28);
            Paragraph logoPara = new Paragraph();
            logoPara.add(new Chunk(logo, 0, 0, true));
            logoPara.add(new Chunk("  Alandi (D), Pune - 412 105", smallFont));
            logoCell.addElement(logoPara);
        } catch (Exception e) {
            logoCell.setPhrase(new Phrase("MIT Academy of Engineering, Alandi (D), Pune - 412 105", metaLabelFont));
        }
        headerTable.addCell(logoCell);

        PdfPCell r1Lbl = new PdfPCell(new Phrase("CLASS", metaLabelFont));
        r1Lbl.setBorderWidth(hdrBorderWidth); r1Lbl.setPadding(3); r1Lbl.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(r1Lbl);
        PdfPCell r1Val = new PdfPCell(new Phrase(classLabel.trim(), metaValueFont));
        r1Val.setBorderWidth(hdrBorderWidth); r1Val.setPadding(3); r1Val.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(r1Val);

        // ──────── ROW 2: Department | CLASS COORDINATOR | value ────────
        PdfPCell deptCell = new PdfPCell(new Phrase(deptName, deptFont));
        deptCell.setBorderWidth(hdrBorderWidth); deptCell.setPadding(3); deptCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(deptCell);

        PdfPCell r2Lbl = new PdfPCell(new Phrase("CLASS COORDINATOR", metaLabelFont));
        r2Lbl.setBorderWidth(hdrBorderWidth); r2Lbl.setPadding(3); r2Lbl.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(r2Lbl);
        PdfPCell r2Val = new PdfPCell(new Phrase(coordinator, metaValueFont));
        r2Val.setBorderWidth(hdrBorderWidth); r2Val.setPadding(3); r2Val.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(r2Val);

        // ──────── ROW 3: CLASS WISE TIME TABLE | DIVISION | value ────────
        PdfPCell titleCell = new PdfPCell(new Phrase("CLASS WISE TIME TABLE", titleFont));
        titleCell.setBorderWidth(hdrBorderWidth); titleCell.setPadding(3);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(titleCell);

        PdfPCell r3Lbl = new PdfPCell(new Phrase("DIVISION", metaLabelFont));
        r3Lbl.setBorderWidth(hdrBorderWidth); r3Lbl.setPadding(3); r3Lbl.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(r3Lbl);
        PdfPCell r3Val = new PdfPCell(new Phrase(divName, metaValueFont));
        r3Val.setBorderWidth(hdrBorderWidth); r3Val.setPadding(3); r3Val.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(r3Val);

        // ──────── ROW 4: ACADEMIC YEAR | THEORY | value ────────
        PdfPCell ayCell = new PdfPCell(new Phrase("ACADEMIC YEAR : " +
            (academicYear != null ? academicYear.getYearName() : ""), metaValueFont));
        ayCell.setBorderWidth(hdrBorderWidth); ayCell.setPadding(3);
        ayCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        ayCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(ayCell);

        PdfPCell r4Lbl = new PdfPCell(new Phrase("THEORY", metaLabelFont));
        r4Lbl.setBorderWidth(hdrBorderWidth); r4Lbl.setPadding(3); r4Lbl.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(r4Lbl);
        PdfPCell r4Val = new PdfPCell(new Phrase(String.valueOf(theoryCount), metaValueFont));
        r4Val.setBorderWidth(hdrBorderWidth); r4Val.setPadding(3); r4Val.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(r4Val);

        // ──────── ROW 5: SEMESTER | PRACTICAL/TUTORIAL | value ────────
        PdfPCell semCell = new PdfPCell(new Phrase("SEMESTER : " + toRomanNumeral(semester), metaValueFont));
        semCell.setBorderWidth(hdrBorderWidth); semCell.setPadding(3);
        semCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        semCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(semCell);

        PdfPCell r5Lbl = new PdfPCell(new Phrase("PRACTICAL/TUTORIAL", metaLabelFont));
        r5Lbl.setBorderWidth(hdrBorderWidth); r5Lbl.setPadding(3); r5Lbl.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(r5Lbl);
        PdfPCell r5Val = new PdfPCell(new Phrase(String.valueOf(labCount), metaValueFont));
        r5Val.setBorderWidth(hdrBorderWidth); r5Val.setPadding(3); r5Val.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(r5Val);

        // ──────── ROW 6: W.E.F. + Rev No. | TOTAL | value ────────
        PdfPCell wefCell = new PdfPCell();
        wefCell.setBorderWidth(hdrBorderWidth); wefCell.setPadding(3);
        wefCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph wefPara = new Paragraph("W.E.F. : " +
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), metaValueFont);
        wefPara.setAlignment(Element.ALIGN_CENTER);
        wefCell.addElement(wefPara);
        Paragraph revPara = new Paragraph("Rev. No. : 01", smallFont);
        revPara.setAlignment(Element.ALIGN_RIGHT);
        wefCell.addElement(revPara);
        headerTable.addCell(wefCell);

        PdfPCell r6Lbl = new PdfPCell(new Phrase("TOTAL", metaLabelFont));
        r6Lbl.setBorderWidth(hdrBorderWidth); r6Lbl.setPadding(3); r6Lbl.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(r6Lbl);
        PdfPCell r6Val = new PdfPCell(new Phrase(String.valueOf(totalCount), metaLabelFont));
        r6Val.setBorderWidth(hdrBorderWidth); r6Val.setPadding(3); r6Val.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(r6Val);

        doc.add(headerTable);

        // ═══════════════════════════════════════════════════════════
        // SECTION 2: TIMETABLE GRID (Days = Rows, Time = Columns)
        // ═══════════════════════════════════════════════════════════

        int numSlots = slots.size();
        PdfPTable grid = new PdfPTable(numSlots + 1); // 1 day column + N slot columns
        grid.setWidthPercentage(100);

        // Calculate column widths: day col narrow, breaks narrow, regular slots equal
        float[] colWidths = new float[numSlots + 1];
        colWidths[0] = 4f; // Day label column
        float totalSlotWidth = 96f;
        int breakCount = 0;
        int regularCount = 0;
        for (TimeSlot s : slots) {
            if (Boolean.TRUE.equals(s.getIsBreak())) breakCount++;
            else regularCount++;
        }
        float breakWidth = 1.8f;
        float regularWidth = regularCount > 0
            ? (totalSlotWidth - (breakCount * breakWidth)) / regularCount
            : totalSlotWidth / numSlots;

        for (int i = 0; i < numSlots; i++) {
            colWidths[i + 1] = Boolean.TRUE.equals(slots.get(i).getIsBreak()) ? breakWidth : regularWidth;
        }
        grid.setWidths(colWidths);

        // ── Header row: "Day/Time" + slot time ranges ──
        PdfPCell dayTimeHeader = new PdfPCell(new Phrase("Day/\nTime", gridHeaderFont));
        dayTimeHeader.setBackgroundColor(headerBgColor);
        dayTimeHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        dayTimeHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
        dayTimeHeader.setPadding(2);
        dayTimeHeader.setBorderWidth(0.5f);
        grid.addCell(dayTimeHeader);

        int periodNumber = 1;
        for (TimeSlot slot : slots) {
            PdfPCell slotHeader = new PdfPCell();
            slotHeader.setBackgroundColor(headerBgColor);
            slotHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            slotHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            slotHeader.setPadding(1);
            slotHeader.setBorderWidth(0.5f);

            if (Boolean.TRUE.equals(slot.getIsBreak())) {
                // Break header: just a narrow column
                slotHeader.setPhrase(new Phrase("", gridHeaderFont));
            } else {
                // Regular slot: time range + period number
                String timeStr = slot.getStartTime() != null
                    ? slot.getStartTime().toString().substring(0, 5) : "??:??";
                String endStr = slot.getEndTime() != null
                    ? slot.getEndTime().toString().substring(0, 5) : "??:??";
                Paragraph timePara = new Paragraph(timeStr + "\nto\n" + endStr, new Font(Font.HELVETICA, 5, Font.BOLD, new Color(27, 42, 78)));
                timePara.setAlignment(Element.ALIGN_CENTER);
                slotHeader.addElement(timePara);
                Paragraph numPara = new Paragraph(String.valueOf(periodNumber), new Font(Font.HELVETICA, 7, Font.BOLD, new Color(27, 42, 78)));
                numPara.setAlignment(Element.ALIGN_CENTER);
                slotHeader.addElement(numPara);
                periodNumber++;
            }
            grid.addCell(slotHeader);
        }

        // ── Day rows (6 rows: MON through SAT) ──
        String[] dayKeys = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
        String[] dayLabels = {"MON", "TUE", "WED", "THU", "FRI", "SAT"};

        for (int d = 0; d < dayKeys.length; d++) {
            String dayKey = dayKeys[d];
            String dayLabel = dayLabels[d];

            // Day label cell
            PdfPCell dayCell = new PdfPCell(new Phrase(dayLabel, dayFont));
            dayCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            dayCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dayCell.setPadding(2);
            dayCell.setBorderWidth(0.5f);
            dayCell.setMinimumHeight(38);
            grid.addCell(dayCell);

            // Slot cells for this day
            for (TimeSlot slot : slots) {
                if (Boolean.TRUE.equals(slot.getIsBreak())) {
                    if (d == 0) {
                        // First day row: create merged break cell spanning all 6 day rows
                        PdfPCell brkCell = new PdfPCell();
                        brkCell.setRowspan(dayKeys.length); // Span all day rows
                        brkCell.setBackgroundColor(new Color(245, 245, 245));
                        brkCell.setBorderWidth(0.5f);
                        brkCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        brkCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        brkCell.setPadding(1);

                        // Stacked vertical letter text
                        String breakText;
                        String slotName = slot.getSlotName() != null ? slot.getSlotName().toLowerCase() : "";
                        if (slotName.contains("lunch")) {
                            breakText = "L\nU\nN\nC\nH\n\nB\nR\nE\nA\nK";
                        } else {
                            breakText = "S\nH\nO\nR\nT\n\nB\nR\nE\nA\nK";
                        }
                        Font brkVertFont = new Font(Font.HELVETICA, 5.5f, Font.BOLD, Color.GRAY);
                        Paragraph brkPara = new Paragraph(breakText, brkVertFont);
                        brkPara.setAlignment(Element.ALIGN_CENTER);
                        brkPara.setLeading(6.5f);
                        brkCell.addElement(brkPara);
                        grid.addCell(brkCell);
                    }
                    // For subsequent day rows (d > 0), do NOT add a cell — rowspan covers it
                } else {
                    // Regular entry cell
                    String key = dayKey + ":" + slot.getId();
                    List<TimetableEntry> slotEntries = multiLookup.getOrDefault(key, Collections.emptyList());

                    PdfPCell entryCell = new PdfPCell();
                    entryCell.setPadding(2);
                    entryCell.setBorderWidth(0.5f);
                    entryCell.setMinimumHeight(38);
                    entryCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

                    if (!slotEntries.isEmpty()) {
                        // Check if this is a multi-batch lab slot
                        boolean hasMultipleBatches = slotEntries.size() > 1;
                        boolean allLabs = slotEntries.stream()
                            .allMatch(e -> e.getCourse() != null && e.getCourse().getCourseType() == CourseType.LAB);

                        if (hasMultipleBatches && allLabs) {
                            // Lab cell: list all batches, one line each
                            // Sort by batch name for consistent ordering
                            slotEntries.sort((a, b) -> {
                                String batchA = a.getBatch() != null ? a.getBatch().getName() : "";
                                String batchB = b.getBatch() != null ? b.getBatch().getName() : "";
                                return batchA.compareTo(batchB);
                            });
                            for (TimetableEntry entry : slotEntries) {
                                String batchName = entry.getBatch() != null ? entry.getBatch().getName() : "";
                                String courseName = getCourseShortName(entry.getCourse());
                                String teacherName = getProfessorShortName(entry.getTeacher());
                                String room = entry.getRoom() != null ? entry.getRoom().getRoomNumber() : "";
                                String line = batchName + "-" + courseName + "-" + teacherName + " (" + room + ")";
                                Paragraph linePara = new Paragraph(line, labBatchFont);
                                linePara.setAlignment(Element.ALIGN_CENTER);
                                linePara.setLeading(6.5f);
                                entryCell.addElement(linePara);
                            }
                        } else {
                            // Single entry (theory or single lab)
                            TimetableEntry entry = slotEntries.get(0);
                            String courseName = getCourseShortName(entry.getCourse());
                            String teacherName = getProfessorShortName(entry.getTeacher());
                            String room = entry.getRoom() != null ? entry.getRoom().getRoomNumber() : "";

                            boolean isLab = entry.getCourse() != null
                                && entry.getCourse().getCourseType() == CourseType.LAB;

                            if (isLab && entry.getBatch() != null) {
                                // Single lab batch
                                String batchName = entry.getBatch().getName();
                                Paragraph p1 = new Paragraph(batchName + "-" + courseName, cellCourseFont);
                                p1.setAlignment(Element.ALIGN_CENTER);
                                p1.setLeading(7f);
                                entryCell.addElement(p1);

                                Paragraph p2 = new Paragraph(teacherName, cellDetailFont);
                                p2.setAlignment(Element.ALIGN_CENTER);
                                p2.setLeading(6.5f);
                                entryCell.addElement(p2);

                                Paragraph p3 = new Paragraph(room, cellDetailFont);
                                p3.setAlignment(Element.ALIGN_CENTER);
                                p3.setLeading(6.5f);
                                entryCell.addElement(p3);
                            } else {
                                // Theory: 3 lines stacked centered
                                Paragraph p1 = new Paragraph(courseName, cellCourseFont);
                                p1.setAlignment(Element.ALIGN_CENTER);
                                p1.setLeading(7f);
                                entryCell.addElement(p1);

                                Paragraph p2 = new Paragraph(teacherName, cellDetailFont);
                                p2.setAlignment(Element.ALIGN_CENTER);
                                p2.setLeading(6.5f);
                                entryCell.addElement(p2);

                                Paragraph p3 = new Paragraph(room, cellDetailFont);
                                p3.setAlignment(Element.ALIGN_CENTER);
                                p3.setLeading(6.5f);
                                entryCell.addElement(p3);
                            }
                        }
                    }
                    // Empty cells stay blank (no dash)
                    grid.addCell(entryCell);
                }
            }
        }

        doc.add(grid);

        // ═══════════════════════════════════════════════════════════
        // SECTION 3: REFERENCE TABLES
        // ═══════════════════════════════════════════════════════════

        doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 2)));

        // Wrapper table: 2 columns for side-by-side reference tables
        PdfPTable refWrapper = new PdfPTable(2);
        refWrapper.setWidthPercentage(100);
        refWrapper.setWidths(new float[]{55f, 45f});

        // ── Left: Course Reference Table ──
        PdfPTable courseRefTable = new PdfPTable(4);
        courseRefTable.setWidthPercentage(100);
        courseRefTable.setWidths(new float[]{20f, 35f, 15f, 30f});

        // Course table header
        String[] courseHeaders = {"Course Code", "Course Name (Short)", "Type", "Room No."};
        for (String h : courseHeaders) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, refHeaderFont));
            hCell.setBackgroundColor(new Color(230, 230, 230));
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            hCell.setPadding(2);
            hCell.setBorderWidth(0.5f);
            courseRefTable.addCell(hCell);
        }

        // Course table data
        for (Map.Entry<Long, CourseEntity> ce : distinctCourses.entrySet()) {
            CourseEntity course = ce.getValue();
            Set<String> rooms = courseRooms.getOrDefault(ce.getKey(), Collections.emptySet());

            PdfPCell codeCell = new PdfPCell(new Phrase(course.getCode() != null ? course.getCode() : "", refCellFont));
            codeCell.setPadding(1);
            codeCell.setBorderWidth(0.5f);
            courseRefTable.addCell(codeCell);

            String nameWithShort = course.getName();
            if (course.getShortName() != null && !course.getShortName().isEmpty()) {
                nameWithShort = course.getName() + " (" + course.getShortName() + ")";
            }
            PdfPCell nameCell = new PdfPCell(new Phrase(nameWithShort, refCellFont));
            nameCell.setPadding(1);
            nameCell.setBorderWidth(0.5f);
            courseRefTable.addCell(nameCell);

            String typeLabel = course.getCourseType() == CourseType.LAB ? "Lab" : "Theory";
            PdfPCell typeCell = new PdfPCell(new Phrase(typeLabel, refCellFont));
            typeCell.setPadding(1);
            typeCell.setBorderWidth(0.5f);
            typeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            courseRefTable.addCell(typeCell);

            PdfPCell roomCell = new PdfPCell(new Phrase(String.join(", ", rooms), refCellFont));
            roomCell.setPadding(1);
            roomCell.setBorderWidth(0.5f);
            courseRefTable.addCell(roomCell);
        }

        PdfPCell courseRefWrapperCell = new PdfPCell(courseRefTable);
        courseRefWrapperCell.setBorder(PdfPCell.NO_BORDER);
        courseRefWrapperCell.setPaddingRight(5);
        refWrapper.addCell(courseRefWrapperCell);

        // ── Right: Faculty Reference Table ──
        PdfPTable teacherRefTable = new PdfPTable(3);
        teacherRefTable.setWidthPercentage(100);
        teacherRefTable.setWidths(new float[]{40f, 20f, 40f});

        // Teacher table header
        String[] teacherHeaders = {"Faculty Name", "Short Name", "Courses Taught"};
        for (String h : teacherHeaders) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, refHeaderFont));
            hCell.setBackgroundColor(new Color(230, 230, 230));
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            hCell.setPadding(2);
            hCell.setBorderWidth(0.5f);
            teacherRefTable.addCell(hCell);
        }

        // Teacher table data
        for (Map.Entry<Long, TeacherEntity> te : distinctTeachers.entrySet()) {
            TeacherEntity teacher = te.getValue();
            Set<String> courses = teacherCourses.getOrDefault(te.getKey(), Collections.emptySet());

            PdfPCell nameCell = new PdfPCell(new Phrase(teacher.getName() != null ? teacher.getName() : "", refCellFont));
            nameCell.setPadding(1);
            nameCell.setBorderWidth(0.5f);
            teacherRefTable.addCell(nameCell);

            PdfPCell shortCell = new PdfPCell(new Phrase(getProfessorShortName(teacher), refCellFont));
            shortCell.setPadding(1);
            shortCell.setBorderWidth(0.5f);
            shortCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            teacherRefTable.addCell(shortCell);

            PdfPCell coursesCell = new PdfPCell(new Phrase(String.join(", ", courses), refCellFont));
            coursesCell.setPadding(1);
            coursesCell.setBorderWidth(0.5f);
            teacherRefTable.addCell(coursesCell);
        }

        PdfPCell teacherRefWrapperCell = new PdfPCell(teacherRefTable);
        teacherRefWrapperCell.setBorder(PdfPCell.NO_BORDER);
        teacherRefWrapperCell.setPaddingLeft(5);
        refWrapper.addCell(teacherRefWrapperCell);

        doc.add(refWrapper);

        // ═══════════════════════════════════════════════════════════
        // SECTION 4: SIGNATURE SECTION
        // ═══════════════════════════════════════════════════════════

        doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 3)));

        PdfPTable sigTable = new PdfPTable(3);
        sigTable.setWidthPercentage(90);
        sigTable.setWidths(new float[]{33f, 34f, 33f});

        PdfPCell sigLeft = new PdfPCell(new Phrase("TIME TABLE COORDINATOR", sigFont));
        sigLeft.setBorder(PdfPCell.NO_BORDER);
        sigLeft.setHorizontalAlignment(Element.ALIGN_LEFT);
        sigLeft.setPaddingTop(12);
        sigTable.addCell(sigLeft);

        PdfPCell sigCenter = new PdfPCell(new Phrase("HOD", sigFont));
        sigCenter.setBorder(PdfPCell.NO_BORDER);
        sigCenter.setHorizontalAlignment(Element.ALIGN_CENTER);
        sigCenter.setPaddingTop(12);
        sigTable.addCell(sigCenter);

        PdfPCell sigRight = new PdfPCell(new Phrase("DY. DIRECTOR / DIRECTOR", sigFont));
        sigRight.setBorder(PdfPCell.NO_BORDER);
        sigRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sigRight.setPaddingTop(12);
        sigTable.addCell(sigRight);

        doc.add(sigTable);

        doc.close();
        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════
    // EXCEL GENERATION (Semester-Specific)
    // ═══════════════════════════════════════════════════════════════

    public byte[] generateDivisionExcel(Long divisionId, Long academicYearId, Semester semester) throws Exception {
        // Get all entries for division and filter by semester
        List<TimetableEntry> allEntries = timetableService.getDivisionTimetable(divisionId, academicYearId);
        List<TimetableEntry> entries = allEntries.stream()
            .filter(e -> e.getSemester() == semester)
            .collect(Collectors.toList());
            
        Division division = divisionRepo.findById(divisionId).orElse(null);
        AcademicYear year = academicYearRepo.findById(academicYearId).orElse(null);
        List<TimeSlot> slots = getSortedSlots(division);

        return buildInstitutionalDivisionExcel(entries, slots, division, year, semester);
    }

    public byte[] generateTeacherExcel(Long teacherId, Long academicYearId) throws Exception {
        List<TimetableEntry> entries = timetableService.getTeacherTimetable(teacherId, academicYearId);
        TeacherEntity teacher = teacherRepo.findById(teacherId).orElse(null);
        AcademicYear year = academicYearRepo.findById(academicYearId).orElse(null);
        List<TimeSlot> slots = getSortedSlotsForEntries(entries);

        String sheetName = teacher != null ? teacher.getName() : "Timetable";
        String title = String.format("Timetable — %s — %s",
            teacher != null ? teacher.getName() : "Unknown",
            year != null ? year.getYearName() : "");

        return buildExcel(entries, slots, sheetName, title);
    }

    private byte[] buildExcel(List<TimetableEntry> entries, List<TimeSlot> slots, String sheetName, String title) throws Exception {
        Map<String, TimetableEntry> lookup = buildLookup(entries);
        
        // Track which cells have been merged (to skip rendering duplicate content)
        java.util.Set<String> mergedCells = new java.util.HashSet<>();

        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet(sheetName.length() > 31 ? sheetName.substring(0, 31) : sheetName);

        // Styles
        XSSFCellStyle headerStyle = wb.createCellStyle();
        XSSFFont headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 10);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{27, 42, 78}, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);

        XSSFCellStyle cellStyle = wb.createCellStyle();
        cellStyle.setWrapText(true);
        cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);

        XSSFCellStyle breakStyle = wb.createCellStyle();
        breakStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 240, (byte) 240, (byte) 240}, null));
        breakStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        breakStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFFont breakFont = wb.createFont();
        breakFont.setItalic(true);
        breakFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        breakStyle.setFont(breakFont);

        // Theory style - consistent blue color
        XSSFCellStyle theoryStyle = wb.createCellStyle();
        theoryStyle.cloneStyleFrom(cellStyle);
        theoryStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 232, (byte) 240, (byte) 254}, null)); // Light blue
        theoryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Lab style - consistent purple color
        XSSFCellStyle labStyle = wb.createCellStyle();
        labStyle.cloneStyleFrom(cellStyle);
        labStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 243, (byte) 232, (byte) 255}, null)); // Light purple
        labStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFCellStyle titleStyle = wb.createCellStyle();
        XSSFFont titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        // Title rows
        Row titleRow = sheet.createRow(0);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("MIT Academy of Engineering");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        Row subtitleRow = sheet.createRow(1);
        subtitleRow.createCell(0).setCellValue(title);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

        Row dateRow = sheet.createRow(2);
        dateRow.createCell(0).setCellValue("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));

        // Header row
        int rowIdx = 4;
        Row headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"Time", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        // Data rows
        for (int slotIdx = 0; slotIdx < slots.size(); slotIdx++) {
            TimeSlot slot = slots.get(slotIdx);
            Row row = sheet.createRow(rowIdx++);

            if (Boolean.TRUE.equals(slot.getIsBreak())) {
                org.apache.poi.ss.usermodel.Cell timeC = row.createCell(0);
                timeC.setCellValue(formatTime(slot));
                timeC.setCellStyle(breakStyle);
                // Merge day columns for break
                org.apache.poi.ss.usermodel.Cell breakC = row.createCell(1);
                breakC.setCellValue(slot.getSlotName());
                breakC.setCellStyle(breakStyle);
                for (int i = 2; i <= 6; i++) {
                    row.createCell(i).setCellStyle(breakStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 6));
                continue;
            }

            // Time cell
            org.apache.poi.ss.usermodel.Cell timeCell = row.createCell(0);
            timeCell.setCellValue(formatTime(slot) + "\n" + slot.getSlotName());
            timeCell.setCellStyle(cellStyle);
            row.setHeightInPoints(45);

            // Day cells
            for (int d = 0; d < DAY_NAMES.length; d++) {
                String key = DAY_NAMES[d] + ":" + slot.getId();
                String cellKey = DAY_NAMES[d] + ":" + slotIdx;
                
                // Skip if this cell was already merged
                if (mergedCells.contains(cellKey)) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(d + 1);
                    cell.setCellStyle(cellStyle);
                    continue;
                }
                
                TimetableEntry entry = lookup.get(key);
                org.apache.poi.ss.usermodel.Cell cell = row.createCell(d + 1);

                if (entry != null) {
                    String courseName = getCourseShortName(entry.getCourse());
                    String profInitials = entry.getTeacher() != null ? getProfessorShortName(entry.getTeacher()) : "-";
                    String roomLocation = entry.getRoom() != null ? entry.getRoom().getRoomNumber() : "-";
                    boolean isLab = entry.getCourse() != null && entry.getCourse().getCourseType() == CourseType.LAB;

                    // Get year label
                    String yearLabel = "";
                    if (entry.getDivision() != null && entry.getDivision().getYear() != null) {
                        yearLabel = getYearLabel(entry.getDivision().getYear());
                    }

                    // Check if this is a lab entry with consecutive slot
                    boolean shouldMerge = false;
                    if (isLab && slotIdx + 1 < slots.size()) {
                        TimeSlot nextSlot = slots.get(slotIdx + 1);
                        if (!Boolean.TRUE.equals(nextSlot.getIsBreak())) {
                            String nextKey = DAY_NAMES[d] + ":" + nextSlot.getId();
                            TimetableEntry nextEntry = lookup.get(nextKey);
                            
                            // Check if next entry is same lab session
                            // Match by: same course, teacher, room, day
                            if (nextEntry != null && 
                                nextEntry.getCourse() != null && 
                                nextEntry.getCourse().getCourseType() == CourseType.LAB &&
                                entry.getCourse().getId().equals(nextEntry.getCourse().getId()) &&
                                entry.getTeacher().getId().equals(nextEntry.getTeacher().getId()) &&
                                entry.getRoom().getId().equals(nextEntry.getRoom().getId()) &&
                                entry.getDayOfWeek() == nextEntry.getDayOfWeek()) {
                                
                                // If both have labSessionGroup, they must match
                                if (entry.getLabSessionGroup() != null && nextEntry.getLabSessionGroup() != null) {
                                    shouldMerge = entry.getLabSessionGroup().getId().equals(nextEntry.getLabSessionGroup().getId());
                                } else if (entry.getBatch() != null && nextEntry.getBatch() != null) {
                                    // If both have batch, they must match
                                    shouldMerge = entry.getBatch().getId().equals(nextEntry.getBatch().getId());
                                } else {
                                    // Otherwise, match by course/teacher/room is enough
                                    shouldMerge = true;
                                }
                            }
                        }
                    }

                    String displayText;
                    if (isLab && entry.getBatch() != null) {
                        // Lab format: "FY B1 - Course Name - H306B"
                        String batchName = entry.getBatch().getName();
                        displayText = yearLabel + " " + batchName + " - " + courseName + " - " + roomLocation;
                    } else {
                        // Theory format: "SY A - Course Name - H301"
                        String divisionName = entry.getDivision() != null ? entry.getDivision().getName() : "";
                        displayText = yearLabel + " " + divisionName + " - " + courseName + " - " + roomLocation;
                    }

                    if (shouldMerge) {
                        // Merge this cell with the next row
                        cell.setCellValue(displayText);
                        cell.setCellStyle(labStyle);
                        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx, d + 1, d + 1));
                        // Mark next cell as merged
                        mergedCells.add(DAY_NAMES[d] + ":" + (slotIdx + 1));
                    } else {
                        cell.setCellValue(displayText);
                        cell.setCellStyle(isLab ? labStyle : theoryStyle);
                    }
                } else {
                    cell.setCellValue("-");
                    cell.setCellStyle(cellStyle);
                }
            }
        }

        // Auto-size columns
        for (int i = 0; i < 7; i++) {
            sheet.setColumnWidth(i, i == 0 ? 4500 : 5000);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════
    // INSTITUTIONAL DIVISION EXCEL (Official MITAOE Format)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generate division timetable Excel in the official MITAOE institutional format.
     *
     * Layout mirrors the PDF institutional format:
     * - Institutional header with metadata table
     * - Grid: Days = rows (MON-SAT), Time slots = columns
     * - Break columns merged across all day rows
     * - Multi-batch lab cells aggregated
     * - Course and Teacher reference tables
     * - Signature row
     */
    private byte[] buildInstitutionalDivisionExcel(
            List<TimetableEntry> entries,
            List<TimeSlot> slots,
            Division division,
            AcademicYear academicYear,
            Semester semester) throws Exception {

        // ── Multi-value lookup ──
        Map<String, List<TimetableEntry>> multiLookup = buildMultiLookup(entries);

        // ── Teaching load ──
        long theoryCount = entries.stream()
            .filter(e -> e.getCourse() != null && e.getCourse().getCourseType() == CourseType.THEORY)
            .count();
        long labCount = entries.stream()
            .filter(e -> e.getCourse() != null && e.getCourse().getCourseType() == CourseType.LAB)
            .count();
        long totalCount = theoryCount + labCount;

        // ── Collect distinct courses and teachers ──
        Map<Long, CourseEntity> distinctCourses = new LinkedHashMap<>();
        Map<Long, TeacherEntity> distinctTeachers = new LinkedHashMap<>();
        Map<Long, Set<String>> courseRooms = new LinkedHashMap<>();
        Map<Long, Set<String>> teacherCourses = new LinkedHashMap<>();

        for (TimetableEntry e : entries) {
            if (e.getCourse() != null) {
                distinctCourses.putIfAbsent(e.getCourse().getId(), e.getCourse());
                String roomNum = e.getRoom() != null ? e.getRoom().getRoomNumber() : "";
                courseRooms.computeIfAbsent(e.getCourse().getId(), k -> new LinkedHashSet<>()).add(roomNum);
            }
            if (e.getTeacher() != null) {
                distinctTeachers.putIfAbsent(e.getTeacher().getId(), e.getTeacher());
                String courseShort = getCourseShortName(e.getCourse());
                teacherCourses.computeIfAbsent(e.getTeacher().getId(), k -> new LinkedHashSet<>()).add(courseShort);
            }
        }

        // ── Workbook setup ──
        String sheetName = "Division " + (division != null ? division.getName() : "Timetable");
        if (sheetName.length() > 31) sheetName = sheetName.substring(0, 31);
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet(sheetName);
        sheet.setDisplayGridlines(false);

        // ═══════════════════════════════════════════════════════════
        // STYLES
        // ═══════════════════════════════════════════════════════════

        // Title style (large bold)
        XSSFCellStyle titleStyle = wb.createCellStyle();
        XSSFFont titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 13);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Meta label style (bold, bordered)
        XSSFCellStyle metaLabelStyle = wb.createCellStyle();
        XSSFFont metaLabelFont = wb.createFont();
        metaLabelFont.setBold(true);
        metaLabelFont.setFontHeightInPoints((short) 8);
        metaLabelStyle.setFont(metaLabelFont);
        metaLabelStyle.setBorderBottom(BorderStyle.THIN);
        metaLabelStyle.setBorderTop(BorderStyle.THIN);
        metaLabelStyle.setBorderLeft(BorderStyle.THIN);
        metaLabelStyle.setBorderRight(BorderStyle.THIN);
        metaLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Meta value style (normal, bordered)
        XSSFCellStyle metaValueStyle = wb.createCellStyle();
        XSSFFont metaValFont = wb.createFont();
        metaValFont.setFontHeightInPoints((short) 8);
        metaValueStyle.setFont(metaValFont);
        metaValueStyle.setBorderBottom(BorderStyle.THIN);
        metaValueStyle.setBorderTop(BorderStyle.THIN);
        metaValueStyle.setBorderLeft(BorderStyle.THIN);
        metaValueStyle.setBorderRight(BorderStyle.THIN);
        metaValueStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Small label style (no border)
        XSSFCellStyle smallStyle = wb.createCellStyle();
        XSSFFont smallFont = wb.createFont();
        smallFont.setFontHeightInPoints((short) 7);
        smallFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        smallStyle.setFont(smallFont);

        // Grid header style (dark bg, white bold text)
        XSSFCellStyle gridHeaderStyle = wb.createCellStyle();
        XSSFFont gridHdrFont = wb.createFont();
        gridHdrFont.setBold(true);
        gridHdrFont.setFontHeightInPoints((short) 8);
        gridHdrFont.setColor(IndexedColors.WHITE.getIndex());
        gridHeaderStyle.setFont(gridHdrFont);
        gridHeaderStyle.setFillForegroundColor(new XSSFColor(new byte[]{27, 42, 78}, null));
        gridHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        gridHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        gridHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        gridHeaderStyle.setWrapText(true);
        gridHeaderStyle.setBorderBottom(BorderStyle.THIN);
        gridHeaderStyle.setBorderTop(BorderStyle.THIN);
        gridHeaderStyle.setBorderLeft(BorderStyle.THIN);
        gridHeaderStyle.setBorderRight(BorderStyle.THIN);

        // Day label style (bold, centered, bordered)
        XSSFCellStyle dayStyle = wb.createCellStyle();
        XSSFFont dayFont = wb.createFont();
        dayFont.setBold(true);
        dayFont.setFontHeightInPoints((short) 9);
        dayStyle.setFont(dayFont);
        dayStyle.setAlignment(HorizontalAlignment.CENTER);
        dayStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dayStyle.setBorderBottom(BorderStyle.THIN);
        dayStyle.setBorderTop(BorderStyle.THIN);
        dayStyle.setBorderLeft(BorderStyle.THIN);
        dayStyle.setBorderRight(BorderStyle.THIN);

        // Cell style (entry content, centered, bordered, wrap)
        XSSFCellStyle cellStyle = wb.createCellStyle();
        XSSFFont cellFont = wb.createFont();
        cellFont.setFontHeightInPoints((short) 8);
        cellStyle.setFont(cellFont);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setWrapText(true);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);

        // Break style (light grey bg, italic, centered, bordered)
        XSSFCellStyle breakStyle = wb.createCellStyle();
        XSSFFont brkFont = wb.createFont();
        brkFont.setItalic(true);
        brkFont.setBold(true);
        brkFont.setFontHeightInPoints((short) 7);
        brkFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        breakStyle.setFont(brkFont);
        breakStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 245, (byte) 245, (byte) 245}, null));
        breakStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        breakStyle.setAlignment(HorizontalAlignment.CENTER);
        breakStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        breakStyle.setWrapText(true);
        breakStyle.setBorderBottom(BorderStyle.THIN);
        breakStyle.setBorderTop(BorderStyle.THIN);
        breakStyle.setBorderLeft(BorderStyle.THIN);
        breakStyle.setBorderRight(BorderStyle.THIN);
        breakStyle.setRotation((short) 90);

        // Reference table header style
        XSSFCellStyle refHeaderStyle = wb.createCellStyle();
        XSSFFont refHdrFont = wb.createFont();
        refHdrFont.setBold(true);
        refHdrFont.setFontHeightInPoints((short) 8);
        refHeaderStyle.setFont(refHdrFont);
        refHeaderStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 230, (byte) 230, (byte) 230}, null));
        refHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        refHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        refHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        refHeaderStyle.setBorderBottom(BorderStyle.THIN);
        refHeaderStyle.setBorderTop(BorderStyle.THIN);
        refHeaderStyle.setBorderLeft(BorderStyle.THIN);
        refHeaderStyle.setBorderRight(BorderStyle.THIN);

        // Reference table cell style
        XSSFCellStyle refCellStyle = wb.createCellStyle();
        XSSFFont refFont = wb.createFont();
        refFont.setFontHeightInPoints((short) 7);
        refCellStyle.setFont(refFont);
        refCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        refCellStyle.setWrapText(true);
        refCellStyle.setBorderBottom(BorderStyle.THIN);
        refCellStyle.setBorderTop(BorderStyle.THIN);
        refCellStyle.setBorderLeft(BorderStyle.THIN);
        refCellStyle.setBorderRight(BorderStyle.THIN);

        // Signature style
        XSSFCellStyle sigStyle = wb.createCellStyle();
        XSSFFont sigFont = wb.createFont();
        sigFont.setFontHeightInPoints((short) 9);
        sigStyle.setFont(sigFont);
        sigStyle.setVerticalAlignment(VerticalAlignment.BOTTOM);

        XSSFCellStyle sigCenterStyle = wb.createCellStyle();
        sigCenterStyle.cloneStyleFrom(sigStyle);
        sigCenterStyle.setAlignment(HorizontalAlignment.CENTER);

        XSSFCellStyle sigRightStyle = wb.createCellStyle();
        sigRightStyle.cloneStyleFrom(sigStyle);
        sigRightStyle.setAlignment(HorizontalAlignment.RIGHT);

        // ═══════════════════════════════════════════════════════════
        // SECTION 1: INSTITUTIONAL HEADER
        // ═══════════════════════════════════════════════════════════

        int numSlots = slots.size();
        int totalCols = numSlots + 1; // 1 day col + N slot cols

        int rowIdx = 0;

        // Row 0: Institution name
        Row instRow = sheet.createRow(rowIdx);
        instRow.setHeightInPoints(18);
        org.apache.poi.ss.usermodel.Cell instCell = instRow.createCell(0);
        instCell.setCellValue("MIT Academy of Engineering, Alandi (D), Pune - 412 105");
        XSSFCellStyle instStyle = wb.createCellStyle();
        XSSFFont instFont = wb.createFont();
        instFont.setBold(true);
        instFont.setFontHeightInPoints((short) 10);
        instStyle.setFont(instFont);
        instCell.setCellStyle(instStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, Math.min(totalCols - 1, 6)));
        // Rev No. at right
        if (totalCols > 7) {
            org.apache.poi.ss.usermodel.Cell revCell = instRow.createCell(totalCols - 2);
            revCell.setCellValue("Rev. No. : 01");
            revCell.setCellStyle(smallStyle);
        }
        rowIdx++;

        // Row 1: Department
        String deptName = division != null && division.getDepartment() != null
            ? "DEPARTMENT OF " + division.getDepartment().getName().toUpperCase() : "";
        Row deptRow = sheet.createRow(rowIdx);
        deptRow.setHeightInPoints(15);
        org.apache.poi.ss.usermodel.Cell deptCell = deptRow.createCell(0);
        deptCell.setCellValue(deptName);
        XSSFCellStyle deptStyle = wb.createCellStyle();
        XSSFFont deptFont = wb.createFont();
        deptFont.setBold(true);
        deptFont.setFontHeightInPoints((short) 9);
        deptStyle.setFont(deptFont);
        deptCell.setCellStyle(deptStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, Math.min(totalCols - 1, 6)));
        rowIdx++;

        // Row 2: "CLASS WISE TIME TABLE" centered
        Row ttRow = sheet.createRow(rowIdx);
        ttRow.setHeightInPoints(18);
        org.apache.poi.ss.usermodel.Cell ttCell = ttRow.createCell(0);
        ttCell.setCellValue("CLASS WISE TIME TABLE");
        ttCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, Math.min(totalCols - 1, 6)));
        rowIdx++;

        // Row 3: Academic details + metadata table side by side
        // Left side: academic year, semester, wef
        Row acadRow = sheet.createRow(rowIdx);
        acadRow.setHeightInPoints(13);
        org.apache.poi.ss.usermodel.Cell ayCell = acadRow.createCell(0);
        ayCell.setCellValue("ACADEMIC YEAR : " + (academicYear != null ? academicYear.getYearName() : ""));
        ayCell.setCellStyle(smallStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 3));
        rowIdx++;

        Row semRow = sheet.createRow(rowIdx);
        semRow.setHeightInPoints(13);
        org.apache.poi.ss.usermodel.Cell semCell = semRow.createCell(0);
        semCell.setCellValue("SEMESTER : " + toRomanNumeral(semester));
        semCell.setCellStyle(smallStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 3));
        rowIdx++;

        Row wefRow = sheet.createRow(rowIdx);
        wefRow.setHeightInPoints(13);
        org.apache.poi.ss.usermodel.Cell wefCell = wefRow.createCell(0);
        wefCell.setCellValue("W.E.F. : " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        wefCell.setCellStyle(smallStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 3));

        // Metadata table on right side (rows 3-8, columns ~7 onward)
        int metaCol = Math.max(7, totalCols - 5);
        int metaStartRow = rowIdx - 2; // row 3

        String classLabel = (division != null ? getYearLabel(division.getYear()) : "") + " " +
            (division != null && division.getDepartment() != null ? division.getDepartment().getName() : "");
        String coordinator = division != null && division.getClassTeacher() != null
            ? division.getClassTeacher() : "";
        String divName = division != null ? division.getName() : "";

        String[][] metaData = {
            {"CLASS", classLabel.trim()},
            {"CLASS COORDINATOR", coordinator},
            {"DIVISION", divName},
            {"THEORY", String.valueOf(theoryCount)},
            {"PRACTICAL/TUTORIAL", String.valueOf(labCount)},
            {"TOTAL", String.valueOf(totalCount)}
        };

        for (int m = 0; m < metaData.length; m++) {
            int mRow = metaStartRow + m;
            Row existingRow = sheet.getRow(mRow);
            if (existingRow == null) existingRow = sheet.createRow(mRow);

            org.apache.poi.ss.usermodel.Cell lbl = existingRow.createCell(metaCol);
            lbl.setCellValue(metaData[m][0]);
            lbl.setCellStyle(metaLabelStyle);

            org.apache.poi.ss.usermodel.Cell val = existingRow.createCell(metaCol + 1);
            val.setCellValue(metaData[m][1]);
            val.setCellStyle(metaValueStyle);
        }

        rowIdx++;

        // Gap row
        rowIdx++;

        // ═══════════════════════════════════════════════════════════
        // SECTION 2: TIMETABLE GRID (Days = Rows, Time = Columns)
        // ═══════════════════════════════════════════════════════════

        int gridStartRow = rowIdx;

        // ── Header row ──
        Row headerRow = sheet.createRow(rowIdx);
        headerRow.setHeightInPoints(40);

        org.apache.poi.ss.usermodel.Cell dayTimeHdr = headerRow.createCell(0);
        dayTimeHdr.setCellValue("Day / Time");
        dayTimeHdr.setCellStyle(gridHeaderStyle);

        int periodNum = 1;
        for (int s = 0; s < numSlots; s++) {
            TimeSlot slot = slots.get(s);
            org.apache.poi.ss.usermodel.Cell hdrCell = headerRow.createCell(s + 1);

            if (Boolean.TRUE.equals(slot.getIsBreak())) {
                hdrCell.setCellValue("");
                hdrCell.setCellStyle(gridHeaderStyle);
            } else {
                String timeStr = slot.getStartTime() != null
                    ? slot.getStartTime().toString().substring(0, 5) : "??:??";
                String endStr = slot.getEndTime() != null
                    ? slot.getEndTime().toString().substring(0, 5) : "??:??";
                hdrCell.setCellValue(timeStr + "\nto\n" + endStr + "\n" + periodNum);
                hdrCell.setCellStyle(gridHeaderStyle);
                periodNum++;
            }
        }
        rowIdx++;

        // ── Day rows (MON-SAT) ──
        String[] dayLabels = {"MON", "TUE", "WED", "THU", "FRI", "SAT"};

        // Track break column indices for merging after all rows are created
        List<Integer> breakColIndices = new ArrayList<>();
        for (int s = 0; s < numSlots; s++) {
            if (Boolean.TRUE.equals(slots.get(s).getIsBreak())) {
                breakColIndices.add(s + 1); // +1 because col 0 is day label
            }
        }

        for (int d = 0; d < DAY_NAMES.length; d++) {
            String dayKey = DAY_NAMES[d];
            Row dayRow = sheet.createRow(rowIdx);
            dayRow.setHeightInPoints(50);

            // Day label
            org.apache.poi.ss.usermodel.Cell dayCl = dayRow.createCell(0);
            dayCl.setCellValue(dayLabels[d]);
            dayCl.setCellStyle(dayStyle);

            // Slot cells
            for (int s = 0; s < numSlots; s++) {
                TimeSlot slot = slots.get(s);
                org.apache.poi.ss.usermodel.Cell cl = dayRow.createCell(s + 1);

                if (Boolean.TRUE.equals(slot.getIsBreak())) {
                    // Break cell — only set content on first day row; merge later
                    if (d == 0) {
                        String slotName = slot.getSlotName() != null ? slot.getSlotName().toLowerCase() : "";
                        if (slotName.contains("lunch")) {
                            cl.setCellValue("LUNCH BREAK");
                        } else {
                            cl.setCellValue("SHORT BREAK");
                        }
                    }
                    cl.setCellStyle(breakStyle);
                } else {
                    // Regular entry cell
                    String key = dayKey + ":" + slot.getId();
                    List<TimetableEntry> slotEntries = multiLookup.getOrDefault(key, Collections.emptyList());

                    if (!slotEntries.isEmpty()) {
                        boolean hasMultiple = slotEntries.size() > 1;
                        boolean allLabs = slotEntries.stream()
                            .allMatch(e -> e.getCourse() != null && e.getCourse().getCourseType() == CourseType.LAB);

                        if (hasMultiple && allLabs) {
                            // Multi-batch lab
                            slotEntries.sort((a, b) -> {
                                String bA = a.getBatch() != null ? a.getBatch().getName() : "";
                                String bB = b.getBatch() != null ? b.getBatch().getName() : "";
                                return bA.compareTo(bB);
                            });
                            StringBuilder sb = new StringBuilder();
                            for (TimetableEntry entry : slotEntries) {
                                String batch = entry.getBatch() != null ? entry.getBatch().getName() : "";
                                String course = getCourseShortName(entry.getCourse());
                                String teacher = getProfessorShortName(entry.getTeacher());
                                String room = entry.getRoom() != null ? entry.getRoom().getRoomNumber() : "";
                                if (sb.length() > 0) sb.append("\n");
                                sb.append(batch).append("-").append(course).append("-").append(teacher).append(" (").append(room).append(")");
                            }
                            cl.setCellValue(sb.toString());
                        } else {
                            // Single entry (theory or single lab)
                            TimetableEntry entry = slotEntries.get(0);
                            String course = getCourseShortName(entry.getCourse());
                            String teacher = getProfessorShortName(entry.getTeacher());
                            String room = entry.getRoom() != null ? entry.getRoom().getRoomNumber() : "";
                            boolean isLab = entry.getCourse() != null
                                && entry.getCourse().getCourseType() == CourseType.LAB;

                            if (isLab && entry.getBatch() != null) {
                                cl.setCellValue(entry.getBatch().getName() + "-" + course + "\n" + teacher + "\n" + room);
                            } else {
                                cl.setCellValue(course + "\n" + teacher + "\n" + room);
                            }
                        }
                    }
                    cl.setCellStyle(cellStyle);
                }
            }
            rowIdx++;
        }

        // Merge break columns across all day rows
        int dayRowStart = gridStartRow + 1; // first day row
        int dayRowEnd = dayRowStart + DAY_NAMES.length - 1; // last day row
        for (int brkCol : breakColIndices) {
            sheet.addMergedRegion(new CellRangeAddress(dayRowStart, dayRowEnd, brkCol, brkCol));
        }

        // ═══════════════════════════════════════════════════════════
        // SECTION 3: REFERENCE TABLES
        // ═══════════════════════════════════════════════════════════

        rowIdx++; // gap

        // ── Course Reference Table ──
        Row courseHdrRow = sheet.createRow(rowIdx);
        courseHdrRow.setHeightInPoints(15);
        String[] courseHeaders = {"Course Code", "Course Name (Short)", "Type", "Room No."};
        for (int i = 0; i < courseHeaders.length; i++) {
            org.apache.poi.ss.usermodel.Cell ch = courseHdrRow.createCell(i);
            ch.setCellValue(courseHeaders[i]);
            ch.setCellStyle(refHeaderStyle);
        }

        // Faculty ref headers on the right side
        String[] facultyHeaders = {"Faculty Name", "Short Name", "Courses Taught"};
        int facStartCol = 5; // start after a gap column
        for (int i = 0; i < facultyHeaders.length; i++) {
            org.apache.poi.ss.usermodel.Cell fh = courseHdrRow.createCell(facStartCol + i);
            fh.setCellValue(facultyHeaders[i]);
            fh.setCellStyle(refHeaderStyle);
        }
        rowIdx++;

        // Course and Teacher data rows (side by side)
        List<Map.Entry<Long, CourseEntity>> courseList = new ArrayList<>(distinctCourses.entrySet());
        List<Map.Entry<Long, TeacherEntity>> teacherList = new ArrayList<>(distinctTeachers.entrySet());
        int maxRows = Math.max(courseList.size(), teacherList.size());

        for (int r = 0; r < maxRows; r++) {
            Row refRow = sheet.createRow(rowIdx);
            refRow.setHeightInPoints(14);

            // Course data (left side)
            if (r < courseList.size()) {
                Map.Entry<Long, CourseEntity> ce = courseList.get(r);
                CourseEntity course = ce.getValue();
                Set<String> rooms = courseRooms.getOrDefault(ce.getKey(), Collections.emptySet());

                org.apache.poi.ss.usermodel.Cell c0 = refRow.createCell(0);
                c0.setCellValue(course.getCode() != null ? course.getCode() : "");
                c0.setCellStyle(refCellStyle);

                String nameShort = course.getName();
                if (course.getShortName() != null && !course.getShortName().isEmpty()) {
                    nameShort = course.getName() + " (" + course.getShortName() + ")";
                }
                org.apache.poi.ss.usermodel.Cell c1 = refRow.createCell(1);
                c1.setCellValue(nameShort);
                c1.setCellStyle(refCellStyle);

                org.apache.poi.ss.usermodel.Cell c2 = refRow.createCell(2);
                c2.setCellValue(course.getCourseType() == CourseType.LAB ? "Lab" : "Theory");
                c2.setCellStyle(refCellStyle);

                org.apache.poi.ss.usermodel.Cell c3 = refRow.createCell(3);
                c3.setCellValue(String.join(", ", rooms));
                c3.setCellStyle(refCellStyle);
            }

            // Teacher data (right side)
            if (r < teacherList.size()) {
                Map.Entry<Long, TeacherEntity> te = teacherList.get(r);
                TeacherEntity teacher = te.getValue();
                Set<String> courses = teacherCourses.getOrDefault(te.getKey(), Collections.emptySet());

                org.apache.poi.ss.usermodel.Cell f0 = refRow.createCell(facStartCol);
                f0.setCellValue(teacher.getName() != null ? teacher.getName() : "");
                f0.setCellStyle(refCellStyle);

                org.apache.poi.ss.usermodel.Cell f1 = refRow.createCell(facStartCol + 1);
                f1.setCellValue(getProfessorShortName(teacher));
                f1.setCellStyle(refCellStyle);

                org.apache.poi.ss.usermodel.Cell f2 = refRow.createCell(facStartCol + 2);
                f2.setCellValue(String.join(", ", courses));
                f2.setCellStyle(refCellStyle);
            }
            rowIdx++;
        }

        // ═══════════════════════════════════════════════════════════
        // SECTION 4: SIGNATURE ROW
        // ═══════════════════════════════════════════════════════════

        rowIdx += 2; // gap for signature space
        Row sigRow = sheet.createRow(rowIdx);
        sigRow.setHeightInPoints(15);

        org.apache.poi.ss.usermodel.Cell sigLeft = sigRow.createCell(0);
        sigLeft.setCellValue("TIME TABLE COORDINATOR");
        sigLeft.setCellStyle(sigStyle);

        int midCol = totalCols / 2;
        org.apache.poi.ss.usermodel.Cell sigCenter = sigRow.createCell(midCol);
        sigCenter.setCellValue("HOD");
        sigCenter.setCellStyle(sigCenterStyle);

        org.apache.poi.ss.usermodel.Cell sigRight = sigRow.createCell(totalCols - 1);
        sigRight.setCellValue("DY. DIRECTOR / DIRECTOR");
        sigRight.setCellStyle(sigRightStyle);

        // ═══════════════════════════════════════════════════════════
        // COLUMN WIDTHS
        // ═══════════════════════════════════════════════════════════

        sheet.setColumnWidth(0, 3200); // Day column
        for (int s = 0; s < numSlots; s++) {
            if (Boolean.TRUE.equals(slots.get(s).getIsBreak())) {
                sheet.setColumnWidth(s + 1, 1200); // Narrow break column
            } else {
                sheet.setColumnWidth(s + 1, 4800); // Regular slot column
            }
        }

        // Print setup: landscape
        sheet.getPrintSetup().setLandscape(true);
        sheet.getPrintSetup().setPaperSize(org.apache.poi.ss.usermodel.PrintSetup.A4_PAPERSIZE);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 1);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private List<TimeSlot> getSortedSlots(Division division) {
        String slotType = division != null && division.getTimeSlotType() != null ? division.getTimeSlotType() : "TYPE_1";
        return timeSlotRepo.findAll().stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
            .filter(s -> slotType.equals(s.getType()))
            .sorted(Comparator.comparing(TimeSlot::getStartTime))
            .collect(Collectors.toList());
    }

    /**
     * Detect the correct time slot type from the entries themselves.
     * Used for teacher/room exports where no single division is available.
     */
    private List<TimeSlot> getSortedSlotsForEntries(List<TimetableEntry> entries) {
        // Detect slot type from the entries
        String detectedType = "TYPE_1";
        for (TimetableEntry e : entries) {
            if (e.getTimeSlot() != null && e.getTimeSlot().getType() != null) {
                detectedType = e.getTimeSlot().getType();
                break;
            }
        }
        final String slotType = detectedType;
        return timeSlotRepo.findAll().stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
            .filter(s -> slotType.equals(s.getType()))
            .sorted(Comparator.comparing(TimeSlot::getStartTime))
            .collect(Collectors.toList());
    }

    private Map<String, TimetableEntry> buildLookup(List<TimetableEntry> entries) {
        Map<String, TimetableEntry> lookup = new HashMap<>();
        for (TimetableEntry e : entries) {
            if (e.getDayOfWeek() != null && e.getTimeSlot() != null) {
                lookup.put(e.getDayOfWeek().name() + ":" + e.getTimeSlot().getId(), e);
            }
        }
        return lookup;
    }

    private String formatTime(TimeSlot slot) {
        String start = slot.getStartTime() != null ? slot.getStartTime().toString().substring(0, 5) : "??:??";
        String end = slot.getEndTime() != null ? slot.getEndTime().toString().substring(0, 5) : "??:??";
        return start + "-" + end;
    }

    private void addHeaderCell(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    /**
     * Generate PDF export containing timetables for all divisions of a department, each on a new page.
     */
    public byte[] generateDepartmentPDF(Long departmentId, Long academicYearId, Semester semester) throws Exception {
        List<Division> divisions = divisionRepo.findByDepartmentId(departmentId);
        if (divisions.isEmpty()) {
            throw new RuntimeException("No divisions found for department ID: " + departmentId);
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 20, 20, 30, 20);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // Fonts
        Font headerFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(27, 42, 78));
        Font subFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
        Font thFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
        Font cellFont = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.BLACK);
        Font cellBold = new Font(Font.HELVETICA, 7, Font.BOLD, new Color(27, 42, 78));
        Font breakFont = new Font(Font.HELVETICA, 7, Font.ITALIC, Color.GRAY);

        for (int i = 0; i < divisions.size(); i++) {
            Division division = divisions.get(i);
            
            // Get all entries for division and filter by semester
            List<TimetableEntry> allEntries = timetableService.getDivisionTimetable(division.getId(), academicYearId);
            List<TimetableEntry> entries = allEntries.stream()
                .filter(e -> e.getSemester() == semester)
                .collect(Collectors.toList());
                
            AcademicYear year = academicYearRepo.findById(academicYearId).orElse(null);
            List<TimeSlot> slots = getSortedSlots(division);

            String title = String.format("Timetable — %s %s — Year %d — %s — %s",
                division.getDepartment() != null ? division.getDepartment().getName() : "",
                division.getName(),
                division.getYear(),
                year != null ? year.getYearName() : "",
                semester != null ? semester.name().replace("_", " ") : "");

            if (i > 0) {
                doc.newPage();
            }

            // Header
            doc.add(new Paragraph("MIT Academy of Engineering", new Font(Font.HELVETICA, 10, Font.BOLD, Color.DARK_GRAY)));
            doc.add(new Paragraph(title, headerFont));
            doc.add(new Paragraph("Generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), subFont));
            doc.add(new Paragraph(" "));

            // Now draw the table
            Map<String, TimetableEntry> lookup = buildLookup(entries);
            java.util.Set<String> mergedCells = new java.util.HashSet<>();

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{12, 15, 15, 15, 15, 15, 13});

            // Header row
            Color headerBg = new Color(27, 42, 78);
            addHeaderCell(table, "Time", thFont, headerBg);
            for (String day : DAY_LABELS) {
                addHeaderCell(table, day, thFont, headerBg);
            }

            // Data rows
            for (int slotIdx = 0; slotIdx < slots.size(); slotIdx++) {
                TimeSlot slot = slots.get(slotIdx);
                
                if (Boolean.TRUE.equals(slot.getIsBreak())) {
                    PdfPCell timeCell = new PdfPCell(new Phrase(formatTime(slot) + "\n" + slot.getSlotName(), breakFont));
                    timeCell.setBackgroundColor(new Color(240, 240, 240));
                    timeCell.setPadding(4);
                    table.addCell(timeCell);

                    PdfPCell breakCell = new PdfPCell(new Phrase(slot.getSlotName(), breakFont));
                    breakCell.setColspan(6);
                    breakCell.setBackgroundColor(new Color(240, 240, 240));
                    breakCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    breakCell.setPadding(4);
                    table.addCell(breakCell);
                    continue;
                }

                // Time cell
                PdfPCell timeCell = new PdfPCell();
                timeCell.addElement(new Phrase(formatTime(slot), cellBold));
                timeCell.addElement(new Phrase(slot.getSlotName(), subFont));
                timeCell.setBackgroundColor(new Color(248, 249, 250));
                timeCell.setPadding(4);
                table.addCell(timeCell);

                // Day cells
                for (int d = 0; d < DAY_NAMES.length; d++) {
                    String day = DAY_NAMES[d];
                    String key = day + ":" + slot.getId();
                    String cellKey = day + ":" + slotIdx;
                    
                    if (mergedCells.contains(cellKey)) {
                        PdfPCell cell = new PdfPCell();
                        cell.setPadding(3);
                        cell.setMinimumHeight(35);
                        table.addCell(cell);
                        continue;
                    }
                    
                    TimetableEntry entry = lookup.get(key);
                    PdfPCell cell = new PdfPCell();
                    cell.setPadding(3);
                    cell.setMinimumHeight(35);

                    if (entry != null) {
                        String courseName = getCourseShortName(entry.getCourse());
                        String profInitials = entry.getTeacher() != null ? getProfessorShortName(entry.getTeacher()) : "-";
                        String roomLocation = entry.getRoom() != null ? entry.getRoom().getRoomNumber() : "-";
                        boolean isLab = entry.getCourse() != null && entry.getCourse().getCourseType() == CourseType.LAB;

                        String yearLabel = "";
                        if (entry.getDivision() != null && entry.getDivision().getYear() != null) {
                            yearLabel = getYearLabel(entry.getDivision().getYear());
                        }

                        boolean shouldMerge = false;
                        if (isLab && slotIdx + 1 < slots.size()) {
                            TimeSlot nextSlot = slots.get(slotIdx + 1);
                            if (!Boolean.TRUE.equals(nextSlot.getIsBreak())) {
                                String nextKey = day + ":" + nextSlot.getId();
                                TimetableEntry nextEntry = lookup.get(nextKey);
                                
                                if (nextEntry != null && 
                                    nextEntry.getCourse() != null && 
                                    nextEntry.getCourse().getCourseType() == CourseType.LAB &&
                                    entry.getCourse().getId().equals(nextEntry.getCourse().getId()) &&
                                    entry.getTeacher().getId().equals(nextEntry.getTeacher().getId()) &&
                                    entry.getRoom().getId().equals(nextEntry.getRoom().getId()) &&
                                    entry.getDayOfWeek() == nextEntry.getDayOfWeek()) {
                                    
                                    if (entry.getLabSessionGroup() != null && nextEntry.getLabSessionGroup() != null) {
                                        shouldMerge = entry.getLabSessionGroup().getId().equals(nextEntry.getLabSessionGroup().getId());
                                    } else if (entry.getBatch() != null && nextEntry.getBatch() != null) {
                                        shouldMerge = entry.getBatch().getId().equals(nextEntry.getBatch().getId());
                                    } else {
                                        shouldMerge = true;
                                    }
                                }
                            }
                        }

                        String cellText;
                        if (isLab && entry.getBatch() != null) {
                            cellText = String.format("%s %s\n%s\n%s\n%s", 
                                yearLabel, entry.getBatch().getName(), courseName, profInitials, roomLocation);
                        } else {
                            cellText = String.format("%s\n%s\n%s", courseName, profInitials, roomLocation);
                        }

                        if (shouldMerge) {
                            cell.setColspan(1);
                            cell.setRowspan(2);
                            mergedCells.add(day + ":" + (slotIdx + 1));
                        }

                        cell.addElement(new Phrase(cellText, cellFont));
                        cell.setBackgroundColor(isLab ? new Color(243, 232, 255) : new Color(232, 240, 254));
                    } else {
                        cell.addElement(new Phrase("-", breakFont));
                    }
                    table.addCell(cell);
                }
            }
            doc.add(table);
        }
        doc.close();
        return out.toByteArray();
    }

    /**
     * Generate Excel export containing worksheets for each division of a department.
     */
    public byte[] generateDepartmentExcel(Long departmentId, Long academicYearId, Semester semester) throws Exception {
        List<Division> divisions = divisionRepo.findByDepartmentId(departmentId);
        if (divisions.isEmpty()) {
            throw new RuntimeException("No divisions found for department ID: " + departmentId);
        }

        XSSFWorkbook wb = new XSSFWorkbook();

        // Styles shared across sheets
        XSSFCellStyle headerStyle = wb.createCellStyle();
        XSSFFont headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 10);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{27, 42, 78}, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);

        XSSFCellStyle cellStyle = wb.createCellStyle();
        cellStyle.setWrapText(true);
        cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);

        XSSFCellStyle breakStyle = wb.createCellStyle();
        breakStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 240, (byte) 240, (byte) 240}, null));
        breakStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        breakStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFFont breakFont = wb.createFont();
        breakFont.setItalic(true);
        breakFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        breakStyle.setFont(breakFont);

        XSSFCellStyle theoryStyle = wb.createCellStyle();
        theoryStyle.cloneStyleFrom(cellStyle);
        theoryStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 232, (byte) 240, (byte) 254}, null));
        theoryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFCellStyle labStyle = wb.createCellStyle();
        labStyle.cloneStyleFrom(cellStyle);
        labStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 243, (byte) 232, (byte) 255}, null));
        labStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFCellStyle titleStyle = wb.createCellStyle();
        XSSFFont titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        for (Division division : divisions) {
            // Get all entries for division and filter by semester
            List<TimetableEntry> allEntries = timetableService.getDivisionTimetable(division.getId(), academicYearId);
            List<TimetableEntry> entries = allEntries.stream()
                .filter(e -> e.getSemester() == semester)
                .collect(Collectors.toList());
                
            AcademicYear year = academicYearRepo.findById(academicYearId).orElse(null);
            List<TimeSlot> slots = getSortedSlots(division);

            String sheetName = division.getName() + " Year " + division.getYear();
            if (sheetName.length() > 31) {
                sheetName = sheetName.substring(0, 31);
            }
            XSSFSheet sheet = wb.createSheet(sheetName);

            String title = String.format("Timetable — %s %s — Year %d — %s — %s",
                division.getDepartment() != null ? division.getDepartment().getName() : "",
                division.getName(),
                division.getYear(),
                year != null ? year.getYearName() : "",
                semester != null ? semester.name().replace("_", " ") : "");

            // Title rows
            Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("MIT Academy of Engineering");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            Row subtitleRow = sheet.createRow(1);
            subtitleRow.createCell(0).setCellValue(title);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

            Row dateRow = sheet.createRow(2);
            dateRow.createCell(0).setCellValue("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));

            // Header row
            int rowIdx = 4;
            Row headerRow = sheet.createRow(rowIdx++);
            String[] headers = {"Time", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            Map<String, TimetableEntry> lookup = buildLookup(entries);
            java.util.Set<String> mergedCells = new java.util.HashSet<>();

            // Data rows
            for (int slotIdx = 0; slotIdx < slots.size(); slotIdx++) {
                TimeSlot slot = slots.get(slotIdx);
                Row row = sheet.createRow(rowIdx++);

                if (Boolean.TRUE.equals(slot.getIsBreak())) {
                    org.apache.poi.ss.usermodel.Cell timeC = row.createCell(0);
                    timeC.setCellValue(formatTime(slot));
                    timeC.setCellStyle(breakStyle);
                    
                    org.apache.poi.ss.usermodel.Cell breakC = row.createCell(1);
                    breakC.setCellValue(slot.getSlotName());
                    breakC.setCellStyle(breakStyle);
                    for (int i = 2; i <= 6; i++) {
                        row.createCell(i).setCellStyle(breakStyle);
                    }
                    sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 6));
                    continue;
                }

                // Time cell
                org.apache.poi.ss.usermodel.Cell timeCell = row.createCell(0);
                timeCell.setCellValue(formatTime(slot) + "\n" + slot.getSlotName());
                timeCell.setCellStyle(cellStyle);
                row.setHeightInPoints(45);

                // Day cells
                for (int d = 0; d < DAY_NAMES.length; d++) {
                    String key = DAY_NAMES[d] + ":" + slot.getId();
                    String cellKey = DAY_NAMES[d] + ":" + slotIdx;
                    
                    if (mergedCells.contains(cellKey)) {
                        org.apache.poi.ss.usermodel.Cell cell = row.createCell(d + 1);
                        cell.setCellStyle(cellStyle);
                        continue;
                    }
                    
                    TimetableEntry entry = lookup.get(key);
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(d + 1);

                    if (entry != null) {
                        String courseName = getCourseShortName(entry.getCourse());
                        String profInitials = entry.getTeacher() != null ? getProfessorShortName(entry.getTeacher()) : "-";
                        String roomLocation = entry.getRoom() != null ? entry.getRoom().getRoomNumber() : "-";
                        boolean isLab = entry.getCourse() != null && entry.getCourse().getCourseType() == CourseType.LAB;

                        String yearLabel = "";
                        if (entry.getDivision() != null && entry.getDivision().getYear() != null) {
                            yearLabel = getYearLabel(entry.getDivision().getYear());
                        }

                        boolean shouldMerge = false;
                        if (isLab && slotIdx + 1 < slots.size()) {
                            TimeSlot nextSlot = slots.get(slotIdx + 1);
                            if (!Boolean.TRUE.equals(nextSlot.getIsBreak())) {
                                String nextKey = DAY_NAMES[d] + ":" + nextSlot.getId();
                                TimetableEntry nextEntry = lookup.get(nextKey);
                                
                                if (nextEntry != null && 
                                    nextEntry.getCourse() != null && 
                                    nextEntry.getCourse().getCourseType() == CourseType.LAB &&
                                    entry.getCourse().getId().equals(nextEntry.getCourse().getId()) &&
                                    entry.getTeacher().getId().equals(nextEntry.getTeacher().getId()) &&
                                    entry.getRoom().getId().equals(nextEntry.getRoom().getId()) &&
                                    entry.getDayOfWeek() == nextEntry.getDayOfWeek()) {
                                    
                                    if (entry.getLabSessionGroup() != null && nextEntry.getLabSessionGroup() != null) {
                                        shouldMerge = entry.getLabSessionGroup().getId().equals(nextEntry.getLabSessionGroup().getId());
                                    } else if (entry.getBatch() != null && nextEntry.getBatch() != null) {
                                        shouldMerge = entry.getBatch().getId().equals(nextEntry.getBatch().getId());
                                    } else {
                                        shouldMerge = true;
                                    }
                                }
                            }
                        }

                        String displayText;
                        if (isLab && entry.getBatch() != null) {
                            displayText = yearLabel + " " + entry.getBatch().getName() + " - " + courseName + " - " + roomLocation;
                        } else {
                            displayText = yearLabel + " " + division.getName() + " - " + courseName + " - " + roomLocation;
                        }

                        if (shouldMerge) {
                            cell.setCellValue(displayText);
                            cell.setCellStyle(labStyle);
                            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx, d + 1, d + 1));
                            mergedCells.add(DAY_NAMES[d] + ":" + (slotIdx + 1));
                        } else {
                            cell.setCellValue(displayText);
                            cell.setCellStyle(isLab ? labStyle : theoryStyle);
                        }
                    } else {
                        cell.setCellValue("-");
                        cell.setCellStyle(cellStyle);
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < 7; i++) {
                sheet.setColumnWidth(i, i == 0 ? 4500 : 5000);
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }
}

