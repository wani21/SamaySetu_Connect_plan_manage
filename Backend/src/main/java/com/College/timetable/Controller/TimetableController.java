package com.College.timetable.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.College.timetable.Entity.TimetableEntry;
import com.College.timetable.IO.ManualTimetableRequest;
import com.College.timetable.Service.TableService;
import com.College.timetable.Service.TimetableService;

@RestController
@RequestMapping("/api/timetable")
public class TimetableController {

    @Autowired
    private TimetableService timetableService;
    
    @Autowired
    TableService table;

    @GetMapping("/division/{divisionId}")
    public ResponseEntity<List<TimetableEntry>> getTimetableForDivision(
            @PathVariable Long divisionId,
            @RequestParam(required = true) Long academicYearId) {

        List<TimetableEntry> timetable = timetableService.getTimetableForDivision(divisionId, academicYearId);
        return ResponseEntity.ok(timetable);
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<TimetableEntry>> getTimetableForTeacher(
            @PathVariable Long teacherId,
            @RequestParam(required = true) Long academicYearId) {

        List<TimetableEntry> timetable = timetableService.getTimetableForTeacher(teacherId, academicYearId);
        return ResponseEntity.ok(timetable);
    }
    @PostMapping("/manual")
    public ResponseEntity<TimetableEntry> addManualEntry(@RequestBody ManualTimetableRequest request) {

        return ResponseEntity.ok(
            table.create(request)
        );
    }
}
