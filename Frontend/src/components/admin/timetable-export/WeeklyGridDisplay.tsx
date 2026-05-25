import React, { useMemo } from 'react';
import { WeeklyGridDisplayProps, TimetableEntry, TimeSlot } from '../../../types/timetableExport';

/**
 * WeeklyGridDisplay Component
 * 
 * Displays timetable in a weekly grid format with:
 * - Days as columns (Monday-Saturday)
 * - Time slots as rows
 * - Color-coded entries (blue for theory, purple for labs)
 * - Break rows spanning all day columns
 * - Support for multiple entries in same slot
 * - Responsive design with horizontal scroll
 * - Accessibility features (ARIA labels, semantic HTML)
 * 
 * Requirements: 9.1-9.5, 10.1-10.5, 14.1-14.3
 */
export const WeeklyGridDisplay: React.FC<WeeklyGridDisplayProps> = ({ data, viewType }) => {
  const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'] as const;
  
  const DAY_LABELS: Record<string, string> = {
    MONDAY: 'Monday',
    TUESDAY: 'Tuesday',
    WEDNESDAY: 'Wednesday',
    THURSDAY: 'Thursday',
    FRIDAY: 'Friday',
    SATURDAY: 'Saturday',
  };

  // Group entries by day and time slot for efficient lookup
  const entriesByDayAndSlot = useMemo(() => {
    const grouped: Record<string, TimetableEntry[]> = {};
    
    data.entries.forEach((entry) => {
      const key = `${entry.dayOfWeek}-${entry.timeSlotId}`;
      if (!grouped[key]) {
        grouped[key] = [];
      }
      grouped[key].push(entry);
    });
    
    return grouped;
  }, [data.entries]);

  // Get entries for a specific day and time slot
  const getEntriesForCell = (day: string, slotId: number): TimetableEntry[] => {
    const key = `${day}-${slotId}`;
    return entriesByDayAndSlot[key] || [];
  };

  // Format time from "HH:mm:ss" to "HH:mm"
  const formatTime = (time: string): string => {
    return time ? time.substring(0, 5) : '';
  };

  // Get year label from year number
  const getYearLabel = (year?: number): string => {
    if (!year) return '';
    switch (year) {
      case 1: return 'FY';
      case 2: return 'SY';
      case 3: return 'TY';
      case 4: return 'B.Tech';
      default: return `Year ${year}`;
    }
  };

  // Memoized entry card renderer for performance
  const EntryCard = React.memo<{ entry: TimetableEntry; index: number }>(({ entry, index }) => {
    const isLab = entry.isLabSession;
    const yearLabel = getYearLabel(entry.divisionYear);
    
    // Format display text based on view type and entry type
    let displayText = '';
    if (viewType === 'PROFESSOR') {
      if (isLab && entry.batchName) {
        // Faculty Lab: "FY B1 - Course Name - H306B"
        displayText = `${yearLabel} ${entry.batchName} - ${entry.courseName} - ${entry.roomNumber || ''}`;
      } else {
        // Faculty Theory: "SY A - Course Name - H301"
        const divName = entry.divisionName.split(' ').pop() || ''; // Extract division letter (A, B, C)
        displayText = `${yearLabel} ${divName} - ${entry.courseName} - ${entry.roomNumber || ''}`;
      }
    } else {
      // Room view - keep existing format
      displayText = entry.courseName;
    }
    
    return (
      <div
        className={`p-2 rounded text-xs transition-shadow hover:shadow-md ${
          isLab
            ? 'bg-purple-50 border border-purple-300'
            : 'bg-blue-50 border border-blue-300'
        } ${index > 0 ? 'mt-1' : ''}`}
        role="article"
        aria-label={`${entry.courseName} - ${entry.divisionName}`}
      >
        {/* Main display text */}
        <p className={`font-semibold text-[11px] ${isLab ? 'text-purple-900' : 'text-blue-900'}`}>
          {displayText}
        </p>
        
        {/* Professor (for room view only) */}
        {viewType === 'ROOM' && entry.professorName && (
          <p className="text-gray-600 text-[10px] mt-1 truncate" title={`Professor: ${entry.professorName}`}>
            👤 {entry.professorName}
          </p>
        )}
      </div>
    );
  });

  EntryCard.displayName = 'EntryCard';

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      {/* Header with entity information */}
      <div className="mb-4 pb-4 border-b border-gray-200">
        <h2 className="text-xl font-semibold text-gray-900">
          {data.entityName}
          {data.entityIdentifier && (
            <span className="text-gray-600 text-base ml-2">
              ({data.entityIdentifier})
            </span>
          )}
        </h2>
        <p className="text-sm text-gray-600 mt-1">
          {data.academicYearName} • {data.semesterLabel}
        </p>
      </div>

      {/* Timetable Grid */}
      <div className="overflow-x-auto">
        <table 
          className="w-full border-collapse border border-gray-300"
          role="table"
          aria-label="Weekly timetable grid"
        >
          <thead>
            <tr className="bg-gray-100">
              <th 
                className="border border-gray-300 px-3 py-2 text-sm font-semibold text-gray-700 min-w-[100px]"
                scope="col"
              >
                Day / Time
              </th>
              {DAYS.map((day) => (
                <th
                  key={day}
                  className="border border-gray-300 px-3 py-2 text-sm font-semibold text-gray-700 min-w-[150px]"
                  scope="col"
                >
                  {DAY_LABELS[day]}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.timeSlots.map((slot: TimeSlot) => {
              // Break rows span all columns
              if (slot.isBreak) {
                return (
                  <tr key={slot.id} className="bg-gray-50">
                    <td 
                      className="border border-gray-300 px-3 py-2 text-xs text-gray-600 font-medium"
                      scope="row"
                    >
                      {formatTime(slot.startTime)} - {formatTime(slot.endTime)}
                    </td>
                    <td
                      colSpan={6}
                      className="border border-gray-300 px-3 py-2 text-center text-sm font-medium text-gray-500 bg-gray-100"
                      role="cell"
                      aria-label={`Break: ${slot.slotName}`}
                    >
                      {slot.slotName}
                    </td>
                  </tr>
                );
              }

              // Regular time slot row
              return (
                <tr key={slot.id}>
                  {/* Time slot column */}
                  <td 
                    className="border border-gray-300 px-3 py-2 text-xs text-gray-600 font-medium bg-gray-50"
                    scope="row"
                  >
                    <div>{formatTime(slot.startTime)}</div>
                    <div>to</div>
                    <div>{formatTime(slot.endTime)}</div>
                    <div className="text-[10px] text-gray-500 mt-1">{slot.slotName}</div>
                  </td>
                  
                  {/* Day columns */}
                  {DAYS.map((day) => {
                    const entries = getEntriesForCell(day, slot.id);
                    
                    return (
                      <td
                        key={`${day}-${slot.id}`}
                        className="border border-gray-300 p-2 align-top min-h-[80px]"
                        role="cell"
                        aria-label={entries.length > 0 ? `${entries.length} class(es) scheduled` : 'No classes scheduled'}
                      >
                        {entries.length === 0 ? (
                          // Empty cell
                          <div className="h-16" aria-hidden="true" />
                        ) : (
                          // Render all entries in this cell
                          <div className="space-y-1">
                            {entries.map((entry, index) => (
                              <EntryCard key={`${entry.id}-${index}`} entry={entry} index={index} />
                            ))}
                          </div>
                        )}
                      </td>
                    );
                  })}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Legend */}
      <div className="mt-4 flex items-center gap-6 text-sm text-gray-600" role="note" aria-label="Color legend">
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 bg-blue-50 border border-blue-300 rounded" aria-hidden="true"></div>
          <span>Theory</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 bg-purple-50 border border-purple-300 rounded" aria-hidden="true"></div>
          <span>Lab</span>
        </div>
      </div>
    </div>
  );
};
