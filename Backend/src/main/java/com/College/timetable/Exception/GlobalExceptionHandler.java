package com.College.timetable.Exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		Map<String, String> error = new HashMap<>();
		String message = ex.getMessage();
		
		// Parse duplicate entry errors
		if (message != null && message.contains("Duplicate entry")) {
			// Extract the duplicate value and key
			if (message.contains("for key")) {
				int startIdx = message.indexOf("Duplicate entry '") + 17;
				int endIdx = message.indexOf("'", startIdx);
				String value = message.substring(startIdx, endIdx);
				
				int keyStartIdx = message.indexOf("for key '") + 9;
				int keyEndIdx = message.indexOf("'", keyStartIdx);
				String key = message.substring(keyStartIdx, keyEndIdx);
				
				// Clean up the key name - extract meaningful field name
				// Handle constraint names like 'rooms.UK7ljglxlj90ln3lbas4kl983m2'
				if (key.contains(".")) {
					key = key.substring(key.lastIndexOf(".") + 1);
				}
				
				// If it's a generated constraint name (starts with UK), try to infer the field from value pattern
				if (key.startsWith("UK") || key.length() > 20) {
					// Try to infer from the table name and value pattern
					if (message.contains("rooms.")) {
						// Check if value looks like a room number (e.g., H304, A101) or a name
						if (value.matches("^[A-Z]\\d{3}$")) {
							key = "room number";
						} else {
							key = "room name";
						}
					} else if (message.contains("courses.")) {
						// Check if value looks like a course code (short, uppercase) or name
						if (value.length() <= 10 && value.matches("^[A-Z0-9]+$")) {
							key = "course code";
						} else {
							key = "course name";
						}
					} else if (message.contains("departments.")) {
						// Check if value looks like a department code (short) or name
						if (value.length() <= 10) {
							key = "department code";
						} else {
							key = "department name";
						}
					} else {
						key = "this value";
					}
				} else {
					// Clean up readable constraint names
					key = key.replace("_", " ").toLowerCase();
				}
				
				error.put("message", "A record with " + key + " '" + value + "' already exists.");
			} else {
				error.put("message", "This entry already exists in the database.");
			}
		} else if (message != null && message.contains("cannot be null")) {
			// Extract column name
			int startIdx = message.indexOf("Column '") + 8;
			int endIdx = message.indexOf("'", startIdx);
			String column = message.substring(startIdx, endIdx).replace("_", " ");
			error.put("message", column + " is required.");
		} else {
			error.put("message", "Database constraint violation. Please check your input.");
		}
		
		error.put("status", "409");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("message", ex.getMessage());
		error.put("status", "404");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
		Map<String, Object> errors = new HashMap<>();
		Map<String, String> fieldErrors = new HashMap<>();
		
		ex.getBindingResult().getFieldErrors().forEach(error -> {
			fieldErrors.put(error.getField(), error.getDefaultMessage());
		});
		
		errors.put("message", "Validation failed");
		errors.put("errors", fieldErrors);
		errors.put("status", "400");
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("message", ex.getMessage());
		error.put("status", "400");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
		Map<String, String> error = new HashMap<>();
		error.put("message", "An unexpected error occurred: " + ex.getMessage());
		error.put("status", "500");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}
}
