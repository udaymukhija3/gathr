import { renderHook, waitFor } from '@testing-library/react-native';
import { useApi } from '../useApi';
import axios from 'axios';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

// Mock UserContext
jest.mock('../../context/UserContext', () => ({
  useUser: () => ({
    token: 'mock-token',
    user: { id: 1, name: 'Test User' },
  }),
}));

describe('useApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns loading state correctly', async () => {
    mockedAxios.mockImplementationOnce(() =>
      new Promise((resolve) => setTimeout(() => resolve({ data: {} }), 100))
    );

    const { result } = renderHook(() => useApi());

    expect(result.current.loading).toBe(false);

    const promise = result.current.request({ method: 'GET', url: '/test' });

    await waitFor(() => {
      expect(result.current.loading).toBe(true);
    });

    await promise;

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
  });

  it('makes successful API request', async () => {
    const mockData = { id: 1, name: 'Test' };
    mockedAxios.mockResolvedValueOnce({ data: mockData });

    const { result } = renderHook(() => useApi());

    const data = await result.current.request({
      method: 'GET',
      url: '/test',
    });

    expect(data).toEqual(mockData);
    expect(mockedAxios).toHaveBeenCalledWith(
      expect.objectContaining({
        url: expect.stringContaining('/test'),
        headers: expect.objectContaining({
          Authorization: 'Bearer mock-token',
        }),
      })
    );
  });

  it('handles network errors with retry', async () => {
    const networkError = {
      request: {},
      response: undefined,
    };

    // Fail twice, succeed on third attempt
    mockedAxios
      .mockRejectedValueOnce(networkError)
      .mockRejectedValueOnce(networkError)
      .mockResolvedValueOnce({ data: { success: true } });

    const { result } = renderHook(() => useApi({ retries: 2 }));

    const data = await result.current.request({
      method: 'GET',
      url: '/test',
    });

    expect(data).toEqual({ success: true });
    expect(mockedAxios).toHaveBeenCalledTimes(3); // Initial + 2 retries
  });

  it('handles 500 errors with retry', async () => {
    const serverError = {
      response: {
        status: 500,
        data: { error: 'Server error' },
      },
    };

    // Fail once, succeed on second attempt
    mockedAxios
      .mockRejectedValueOnce(serverError)
      .mockResolvedValueOnce({ data: { success: true } });

    const { result } = renderHook(() => useApi({ retries: 1 }));

    const data = await result.current.request({
      method: 'GET',
      url: '/test',
    });

    expect(data).toEqual({ success: true });
    expect(mockedAxios).toHaveBeenCalledTimes(2);
  });

  it('does not retry on 4xx errors', async () => {
    const clientError = {
      response: {
        status: 404,
        data: { error: 'Not found' },
      },
    };

    mockedAxios.mockRejectedValueOnce(clientError);

    const { result } = renderHook(() => useApi({ retries: 2 }));

    await expect(
      result.current.request({ method: 'GET', url: '/test' })
    ).rejects.toThrow();

    expect(mockedAxios).toHaveBeenCalledTimes(1); // No retries for 4xx
  });

  it('sets error state on failure', async () => {
    const error = {
      response: {
        status: 404,
        data: { error: 'Not found' },
      },
    };

    mockedAxios.mockRejectedValueOnce(error);

    const { result } = renderHook(() => useApi());

    await expect(
      result.current.request({ method: 'GET', url: '/test' })
    ).rejects.toThrow();

    await waitFor(() => {
      expect(result.current.error).toBe('Not found');
    });
  });

  it('handles timeout errors', async () => {
    const timeoutError = {
      code: 'ECONNABORTED',
      message: 'timeout of 15000ms exceeded',
    };

    mockedAxios.mockRejectedValueOnce(timeoutError);

    const { result } = renderHook(() => useApi());

    await expect(
      result.current.request({ method: 'GET', url: '/test' })
    ).rejects.toThrow('Request timeout');
  });

  it('applies exponential backoff for retries', async () => {
    const networkError = { request: {}, response: undefined };

    mockedAxios
      .mockRejectedValueOnce(networkError)
      .mockRejectedValueOnce(networkError)
      .mockResolvedValueOnce({ data: { success: true } });

    const { result } = renderHook(() => useApi({ retries: 2, retryDelay: 100 }));

    const startTime = Date.now();
    await result.current.request({ method: 'GET', url: '/test' });
    const duration = Date.now() - startTime;

    // Should have delayed: 100ms + 200ms = 300ms minimum
    expect(duration).toBeGreaterThanOrEqual(250);
  });
});
