import { useState, useCallback, useRef, useEffect } from 'react';
import { AxiosError } from 'axios';
import { getErrorMessage } from '../utils/errorHandler';
import toast from 'react-hot-toast';

interface UseApiCallOptions {
  showErrorToast?: boolean;
}

interface UseApiCallReturn<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  execute: (...args: any[]) => Promise<T | null>;
  reset: () => void;
}

/**
 * Custom hook for consistent API call handling with error management
 * Provides automatic error handling, loading states, and optional toast notifications
 *
 * @param apiFunction - The API function to execute
 * @param options - Configuration options (showErrorToast)
 * @returns Object with data, loading, error states and execute/reset functions
 *
 * @example
 * const { data, loading, error, execute } = useApiCall(authAPI.login, { showErrorToast: true });
 * const result = await execute({ email: 'user@example.com', password: 'pass' });
 */
export function useApiCall<T>(
  apiFunction: (...args: any[]) => Promise<any>,
  options: UseApiCallOptions = {}
): UseApiCallReturn<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const execute = useCallback(async (...args: any[]): Promise<T | null> => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiFunction(...args);
      const result = response.data ?? response;
      if (mountedRef.current) {
        setData(result);
      }
      return result;
    } catch (err) {
      const errorMessage = err instanceof AxiosError
        ? getErrorMessage(err)
        : 'An unexpected error occurred';
      if (mountedRef.current) {
        setError(errorMessage);
      }
      if (options.showErrorToast) {
        toast.error(errorMessage);
      }
      return null;
    } finally {
      if (mountedRef.current) {
        setLoading(false);
      }
    }
  }, [apiFunction, options.showErrorToast]);

  const reset = useCallback(() => {
    setData(null);
    setError(null);
    setLoading(false);
  }, []);

  return { data, loading, error, execute, reset };
}
