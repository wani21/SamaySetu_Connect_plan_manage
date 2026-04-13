package com.College.timetable.Entity;


public enum TimetableStatus {
    DRAFT,       // Being built by admin — not visible to students/teachers
    PUBLISHED,   // Live — visible to everyone
    ARCHIVED     // Previous semester — read-only history
}
