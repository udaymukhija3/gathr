import { useState, useCallback } from 'react';
import { useUser } from '../context/UserContext';
import axios, { AxiosError, AxiosRequestConfig } from 'axios';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080';
const MOCK_MODE = process.env.EXPO_PUBLIC_MOCK_MODE === 'true';

interface UseApiOptions {
  showErrorToast?: boolean;
  retries?: number;
  retryDelay?: number;
}

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

const isRetryableError = (error: AxiosError): boolean => {
  if (!error.response) {
    // Network errors are retryable
    return true;
  }
  const status = error.response.status;
  // Retry on 5xx errors and 408 (timeout)
  return status >= 500 || status === 408;
};

export const useApi = <T = any>(options: UseApiOptions = {}) => {
  const { token } = useUser();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const maxRetries = options.retries ?? 2;
  const retryDelay = options.retryDelay ?? 1000;

  const request = useCallback(
    async (config: AxiosRequestConfig): Promise<T> => {
      setLoading(true);
      setError(null);

      let lastError: AxiosError | null = null;

      for (let attempt = 0; attempt <= maxRetries; attempt++) {
        try {
          const headers: Record<string, string> = {
            'Content-Type': 'application/json',
            ...config.headers,
          };

          if (token) {
            headers['Authorization'] = `Bearer ${token}`;
          }

          const response = await axios({
            ...config,
            url: `${API_BASE_URL}${config.url}`,
            headers,
            timeout: 15000, // 15 second timeout
          });

          setLoading(false);
          return response.data;
        } catch (err) {
          lastError = err as AxiosError;

          // If this is not the last attempt and error is retryable, retry
          if (attempt < maxRetries && isRetryableError(lastError)) {
            console.log(`Request failed, retrying (attempt ${attempt + 1}/${maxRetries})...`);
            await delay(retryDelay * (attempt + 1)); // Exponential backoff
            continue;
          }

          // If we've exhausted retries or error is not retryable, throw
          break;
        }
      }

      // Handle the error
      const axiosError = lastError!;
      let errorMessage = 'An error occurred';

      const response = axiosError.response;
      const status = response?.status;
      const data = (response as any)?.data || {};
      const messageLower = axiosError.message?.toLowerCase() || '';
      const isTimeoutError =
        axiosError.code === 'ECONNABORTED' ||
        messageLower.includes('timeout') ||
        axiosError.message?.includes("reading 'data'");

      if (typeof status === 'number') {
        if (status === 429) {
          errorMessage = 'Too many requests. Please try again later.';
        } else if (status === 403) {
          errorMessage = data.error || 'Access forbidden';
        } else if (status === 409) {
          errorMessage = data.error || 'Conflict - activity may be full';
        } else if (status === 401) {
          errorMessage = 'Authentication required';
        } else if (status >= 500) {
          errorMessage = 'Server error. Please try again later.';
        } else {
          errorMessage = data.error || data.message || `HTTP error: ${status}`;
        }
      } else if (isTimeoutError) {
        errorMessage = 'Request timeout. Please try again.';
      } else if (axiosError.request) {
        errorMessage = 'Network error. Please check your connection.';
      } else {
        errorMessage = axiosError.message || 'An unexpected error occurred';
      }

      setError(errorMessage);
      setLoading(false);

      if (options.showErrorToast !== false) {
        console.error('API Error:', errorMessage);
      }

      throw new Error(errorMessage);
    },
    [token, options.showErrorToast, maxRetries, retryDelay]
  );

  return {
    request,
    loading,
    error,
  };
};

