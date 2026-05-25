-- Database Optimization: Indexes for SamaySetu Timetable Creation

-- Indexes for frequent timetable conflict validation and reading
CREATE INDEX IF NOT EXISTS idx_timetable_entries_teacher_conflict 
ON timetable_entries (user_id, day_of_week, time_slot_id, academic_year_id, status);

CREATE INDEX IF NOT EXISTS idx_timetable_entries_room_conflict 
ON timetable_entries (classroom_id, day_of_week, time_slot_id, academic_year_id, status);

CREATE INDEX IF NOT EXISTS idx_timetable_entries_division_conflict 
ON timetable_entries (division_id, day_of_week, time_slot_id, academic_year_id, status);

CREATE INDEX IF NOT EXISTS idx_timetable_entries_batch_conflict 
ON timetable_entries (batch_id, day_of_week, time_slot_id, academic_year_id, status);

CREATE INDEX IF NOT EXISTS idx_timetable_entries_semester_series 
ON timetable_entries (semester, academic_year_id, status);
