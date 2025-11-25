import { apiRequest, MOCK_MODE } from './client';

export const eventsApi = {
  log: async (
    eventType: string,
    properties?: Record<string, any>,
    activityId?: number
  ): Promise<void> => {
    if (MOCK_MODE) {
      console.log('[Telemetry]', eventType, { activityId, ...properties });
      return;
    }

    try {
      await apiRequest('/events', {
        method: 'POST',
        body: JSON.stringify({
          eventType,
          activityId,
          properties,
        }),
      });
    } catch (error) {
      console.warn('Failed to log event:', eventType, error);
    }
  },
};

