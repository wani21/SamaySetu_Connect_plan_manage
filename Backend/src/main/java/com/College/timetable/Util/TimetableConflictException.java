package com.College.timetable.Util;

import java.util.List;

public class TimetableConflictException extends RuntimeException{
	private final List<String> conflicts;

    /**
     * Constructs a new exception with a list of conflict messages.
     *
     * @param conflicts List of conflict descriptions
     */
    public TimetableConflictException(List<String> conflicts) {
        super(buildMessage(conflicts));
        this.conflicts = conflicts;
    }

    /**
     * Constructs a new exception with a list of conflict messages and a cause.
     *
     * @param conflicts List of conflict descriptions
     * @param cause The underlying cause of the exception
     */
    public TimetableConflictException(List<String> conflicts, Throwable cause) {
        super(buildMessage(conflicts), cause);
        this.conflicts = conflicts;
    }

    /**
     * Gets the list of conflict messages.
     *
     * @return List of conflict descriptions (never null)
     */
    public List<String> getConflicts() {
        return conflicts;
    }

    /**
     * Builds a combined message from the conflict list.
     *
     * @param conflicts List of conflict messages
     * @return Combined message string
     */
    private static String buildMessage(List<String> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return "No conflicts provided";
        }
        if (conflicts.size() == 1) {
            return conflicts.get(0);
        }
        return String.format("%d conflicts detected: %s", 
            conflicts.size(), String.join("; ", conflicts));
    }

}
