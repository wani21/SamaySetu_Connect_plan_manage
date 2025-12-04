/**
 * Extract user-friendly error message from API error response
 */
export const getErrorMessage = (error: any): string => {
  // Log the full error for debugging
  console.log('Error details:', {
    status: error.response?.status,
    data: error.response?.data,
    message: error.message,
  });

  if (!error.response) {
    return error.message || 'Network error. Please check your connection.';
  }

  const status = error.response.status;
  const data = error.response.data;

  // If data is HTML (error page), extract meaningful message
  if (typeof data === 'string' && (data.includes('<!DOCTYPE') || data.includes('<html'))) {
    // Backend returned HTML error page
    if (status === 403) {
      return 'Access forbidden. This might be due to a duplicate entry or permission issue.';
    }
    return `Server error (${status}). Please try again or contact support.`;
  }

  // Handle different status codes
  switch (status) {
    case 400:
      // Bad Request - validation errors
      if (typeof data === 'string') {
        return data;
      }
      if (data.message) {
        return data.message;
      }
      if (data.errors) {
        // Handle validation errors array
        return Object.values(data.errors).join(', ');
      }
      return 'Invalid data provided. Please check your input.';

    case 401:
      return 'Session expired. Please login again.';

    case 403:
      // Check if it's a database constraint error disguised as 403
      if (typeof data === 'string') {
        if (data.includes('Duplicate entry')) {
          const match = data.match(/Duplicate entry '(.+?)' for key '(.+?)'/);
          if (match) {
            const value = match[1];
            const key = match[2].replace(/.*\./, '').replace(/_/g, ' ');
            return `A record with ${key} "${value}" already exists.`;
          }
          return 'This entry already exists in the database.';
        }
        return data;
      }
      if (data.message) {
        if (typeof data.message === 'string' && data.message.includes('Duplicate entry')) {
          const match = data.message.match(/Duplicate entry '(.+?)' for key '(.+?)'/);
          if (match) {
            const value = match[1];
            const key = match[2].replace(/.*\./, '').replace(/_/g, ' ');
            return `A record with ${key} "${value}" already exists.`;
          }
          return 'This entry already exists in the database.';
        }
        return data.message;
      }
      return 'You do not have permission to perform this action.';

    case 404:
      return 'Resource not found.';

    case 409:
      // Conflict - duplicate entry (from GlobalExceptionHandler)
      if (typeof data === 'string') {
        return data;
      }
      if (data && typeof data === 'object' && data.message) {
        return data.message;
      }
      return 'This entry already exists.';

    case 500:
      // Check for specific database errors
      if (typeof data === 'string') {
        if (data.includes('Duplicate entry')) {
          const match = data.match(/Duplicate entry '(.+?)' for key '(.+?)'/);
          if (match) {
            const value = match[1];
            const key = match[2].replace('rooms.', '').replace('courses.', '').replace('departments.', '');
            return `A record with ${key} "${value}" already exists.`;
          }
          return 'This entry already exists in the database.';
        }
        if (data.includes('cannot be null')) {
          const match = data.match(/Column '(.+?)' cannot be null/);
          if (match) {
            return `${match[1].replace('_', ' ')} is required.`;
          }
        }
      }
      if (data && typeof data === 'object' && data.message) {
        return data.message;
      }
      return 'Server error. Please try again later.';

    default:
      if (typeof data === 'string') {
        return data;
      }
      return data.message || `Error: ${status}. Please try again.`;
  }
};

/**
 * Check if error is a duplicate entry error
 */
export const isDuplicateError = (error: any): boolean => {
  const message = getErrorMessage(error).toLowerCase();
  return message.includes('duplicate') || 
         message.includes('already exists') ||
         error.response?.status === 409;
};

/**
 * Check if error is a validation error
 */
export const isValidationError = (error: any): boolean => {
  return error.response?.status === 400;
};
