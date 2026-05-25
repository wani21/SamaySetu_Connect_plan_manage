package com.College.timetable.Entity;

public enum Semester {
	SEM_1,
	SEM_2,
	SEM_3,
	SEM_4,
	SEM_5,
	SEM_6,
	SEM_7,
	SEM_8;
	
	/**
	 * Check if this semester is in the same series (odd or even) as another semester.
	 * Odd semesters (1,3,5,7) run August-December
	 * Even semesters (2,4,6,8) run January-May
	 * 
	 * @param other The other semester to compare with
	 * @return true if both semesters are in the same series (both odd or both even)
	 */
	public boolean isSameSeries(Semester other) {
		if (other == null) return false;
		return this.isOdd() == other.isOdd();
	}
	
	/**
	 * Check if this is an odd semester (1,3,5,7)
	 * @return true if odd semester
	 */
	public boolean isOdd() {
		return this == SEM_1 || this == SEM_3 || this == SEM_5 || this == SEM_7;
	}
	
	/**
	 * Check if this is an even semester (2,4,6,8)
	 * @return true if even semester
	 */
	public boolean isEven() {
		return !isOdd();
	}
}
