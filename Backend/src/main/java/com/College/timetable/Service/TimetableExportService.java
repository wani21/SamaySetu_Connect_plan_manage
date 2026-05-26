package com.College.timetable.Service;

import com.College.timetable.Entity.*;
import com.College.timetable.IO.TimetableEntryDTO;
import com.College.timetable.IO.TimetableExportDTO;
import com.College.timetable.IO.TimeSlotDTO;
import com.College.timetable.Repository.*;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
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
     * Generate professor initials from full name
     * Example: "Abhijeet Purushottam Rane" -> "APR"
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
                        String courseName = entry.getCourse() != null ? entry.getCourse().getName() : "";
                        String profInitials = getProfessorInitials(professor.getName());
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
                        String courseName = entry.getCourse() != null ? entry.getCourse().getName() : "";
                        String profInitials = getProfessorInitials(professor.getName());
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
        
        // Get time slots
        List<TimeSlot> slots = timeSlotRepo.findAll().stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
            .filter(s -> "TYPE_1".equals(s.getType()))
            .sorted(Comparator.comparing(TimeSlot::getStartTime))
            .collect(Collectors.toList());
        
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
                    String courseName = entry.getCourse() != null ? entry.getCourse().getName() : "-";
                    String profInitials = entry.getTeacher() != null ? getProfessorInitials(entry.getTeacher().getName()) : "-";
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
        
        // Get time slots
        List<TimeSlot> slots = timeSlotRepo.findAll().stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
            .filter(s -> "TYPE_1".equals(s.getType()))
            .sorted(Comparator.comparing(TimeSlot::getStartTime))
            .collect(Collectors.toList());
        
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
                    String courseName = entry.getCourse() != null ? entry.getCourse().getName() : "-";
                    String profInitials = entry.getTeacher() != null ? getProfessorInitials(entry.getTeacher().getName()) : "-";
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

        String title = String.format("Timetable — %s %s — Year %d — %s — %s",
            division != null && division.getDepartment() != null ? division.getDepartment().getName() : "",
            division != null ? division.getName() : "",
            division != null ? division.getYear() : 0,
            year != null ? year.getYearName() : "",
            semester != null ? semester.name().replace("_", " ") : "");

        return buildPDF(entries, slots, title);
    }

    public byte[] generateTeacherPDF(Long teacherId, Long academicYearId) throws Exception {
        List<TimetableEntry> entries = timetableService.getTeacherTimetable(teacherId, academicYearId);
        TeacherEntity teacher = teacherRepo.findById(teacherId).orElse(null);
        AcademicYear year = academicYearRepo.findById(academicYearId).orElse(null);
        List<TimeSlot> slots = getSortedSlots(null);

        String title = String.format("Timetable — %s (%s) — %s",
            teacher != null ? teacher.getName() : "Unknown",
            teacher != null ? teacher.getEmployeeId() : "",
            year != null ? year.getYearName() : "");

        return buildPDF(entries, slots, title);
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
                    String courseName = entry.getCourse() != null ? entry.getCourse().getName() : "-";
                    String profInitials = entry.getTeacher() != null ? getProfessorInitials(entry.getTeacher().getName()) : "-";
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

        String sheetName = (division != null ? division.getName() + " Year " + division.getYear() + " " + (semester != null ? semester.name() : "") : "Timetable");
        String title = String.format("Timetable — %s %s — Year %d — %s — %s",
            division != null && division.getDepartment() != null ? division.getDepartment().getName() : "",
            division != null ? division.getName() : "",
            division != null ? division.getYear() : 0,
            year != null ? year.getYearName() : "",
            semester != null ? semester.name().replace("_", " ") : "");

        return buildExcel(entries, slots, sheetName, title);
    }

    public byte[] generateTeacherExcel(Long teacherId, Long academicYearId) throws Exception {
        List<TimetableEntry> entries = timetableService.getTeacherTimetable(teacherId, academicYearId);
        TeacherEntity teacher = teacherRepo.findById(teacherId).orElse(null);
        AcademicYear year = academicYearRepo.findById(academicYearId).orElse(null);
        List<TimeSlot> slots = getSortedSlots(null);

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
                    String courseName = entry.getCourse() != null ? entry.getCourse().getName() : "-";
                    String profInitials = entry.getTeacher() != null ? getProfessorInitials(entry.getTeacher().getName()) : "-";
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
                        String courseName = entry.getCourse() != null ? entry.getCourse().getName() : "-";
                        String profInitials = entry.getTeacher() != null ? getProfessorInitials(entry.getTeacher().getName()) : "-";
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
                        String courseName = entry.getCourse() != null ? entry.getCourse().getName() : "-";
                        String profInitials = entry.getTeacher() != null ? getProfessorInitials(entry.getTeacher().getName()) : "-";
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
