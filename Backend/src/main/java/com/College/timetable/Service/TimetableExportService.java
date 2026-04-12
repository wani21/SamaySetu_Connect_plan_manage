package com.College.timetable.Service;

import com.College.timetable.Entity.*;
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

    private static final String[] DAY_NAMES = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
    private static final String[] DAY_LABELS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    // ═══════════════════════════════════════════════════════════════
    // PDF GENERATION
    // ═══════════════════════════════════════════════════════════════

    public byte[] generateDivisionPDF(Long divisionId, Long academicYearId) throws Exception {
        List<TimetableEntry> entries = timetableService.getDivisionTimetable(divisionId, academicYearId);
        Division division = divisionRepo.findById(divisionId).orElse(null);
        AcademicYear year = academicYearRepo.findById(academicYearId).orElse(null);
        List<TimeSlot> slots = getSortedSlots(division);

        String title = String.format("Timetable — %s %s — Year %d — %s",
            division != null && division.getDepartment() != null ? division.getDepartment().getName() : "",
            division != null ? division.getName() : "",
            division != null ? division.getYear() : 0,
            year != null ? year.getYearName() : "");

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
        for (TimeSlot slot : slots) {
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
            for (String day : DAY_NAMES) {
                String key = day + ":" + slot.getId();
                TimetableEntry entry = lookup.get(key);

                PdfPCell cell = new PdfPCell();
                cell.setPadding(3);
                cell.setMinimumHeight(35);

                if (entry != null) {
                    String courseName = entry.getCourse() != null ? entry.getCourse().getName() : "-";
                    String teacherName = entry.getTeacher() != null ? entry.getTeacher().getName() : "-";
                    String roomNum = entry.getRoom() != null ? entry.getRoom().getRoomNumber() : "-";
                    boolean isLab = entry.getCourse() != null && entry.getCourse().getCourseType() == CourseType.LAB;

                    cell.addElement(new Phrase(courseName, cellBold));
                    cell.addElement(new Phrase(teacherName, cellFont));
                    cell.addElement(new Phrase(roomNum + (isLab && entry.getBatch() != null ? " (" + entry.getBatch().getName() + ")" : ""), cellFont));

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

    // ═══════════════════════════════════════════════════════════════
    // EXCEL GENERATION
    // ═══════════════════════════════════════════════════════════════

    public byte[] generateDivisionExcel(Long divisionId, Long academicYearId) throws Exception {
        List<TimetableEntry> entries = timetableService.getDivisionTimetable(divisionId, academicYearId);
        Division division = divisionRepo.findById(divisionId).orElse(null);
        AcademicYear year = academicYearRepo.findById(academicYearId).orElse(null);
        List<TimeSlot> slots = getSortedSlots(division);

        String sheetName = (division != null ? division.getName() + " Year " + division.getYear() : "Timetable");
        String title = String.format("Timetable — %s %s — Year %d — %s",
            division != null && division.getDepartment() != null ? division.getDepartment().getName() : "",
            division != null ? division.getName() : "",
            division != null ? division.getYear() : 0,
            year != null ? year.getYearName() : "");

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

        XSSFCellStyle labStyle = wb.createCellStyle();
        labStyle.cloneStyleFrom(cellStyle);
        labStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 243, (byte) 232, (byte) 255}, null));
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
        for (TimeSlot slot : slots) {
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
                TimetableEntry entry = lookup.get(key);
                org.apache.poi.ss.usermodel.Cell cell = row.createCell(d + 1);

                if (entry != null) {
                    String courseName = entry.getCourse() != null ? entry.getCourse().getName() : "-";
                    String teacherName = entry.getTeacher() != null ? entry.getTeacher().getName() : "-";
                    String roomNum = entry.getRoom() != null ? entry.getRoom().getRoomNumber() : "-";
                    boolean isLab = entry.getCourse() != null && entry.getCourse().getCourseType() == CourseType.LAB;
                    String batchInfo = isLab && entry.getBatch() != null ? " (" + entry.getBatch().getName() + ")" : "";

                    cell.setCellValue(courseName + "\n" + teacherName + "\n" + roomNum + batchInfo);
                    cell.setCellStyle(isLab ? labStyle : cellStyle);
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
}
