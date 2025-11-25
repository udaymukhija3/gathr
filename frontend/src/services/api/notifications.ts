import { apiRequest, MOCK_MODE } from './client';

export const notificationsApi = {
  getUnread: async (): Promise<any[]> => {
    if (MOCK_MODE) {
      return [
        {
          id: 1,
          type: 'ACTIVITY_REMINDER',
          title: 'Activity starting soon',
          message: 'Coffee at Galleria starts in 30 minutes',
          activityId: 1,
          createdAt: new Date().toISOString(),
          readAt: null,
        },
        {
          id: 2,
          type: 'NEW_MESSAGE',
          title: 'New message',
          message: 'User #23: See you there!',
          activityId: 1,
          createdAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
          readAt: null,
        },
      ];
    }
    return apiRequest('/notifications');
  },

  getUnreadCount: async (): Promise<number> => {
    if (MOCK_MODE) {
      return 2;
    }
    const response = await apiRequest<{ count: number }>('/notifications/unread-count');
    return response.count;
  },

  markAsRead: async (notificationId: number): Promise<void> => {
    if (MOCK_MODE) {
      console.log('Mock: Marking notification as read', notificationId);
      return;
    }
    await apiRequest(`/notifications/${notificationId}/read`, {
      method: 'POST',
    });
  },

  getPreferences: async (): Promise<any> => {
    if (MOCK_MODE) {
      return {
        pushEnabled: true,
        activityReminders: true,
        chatNotifications: true,
        promotionalNotifications: false,
        quietHoursStart: null,
        quietHoursEnd: null,
      };
    }
    return apiRequest('/notifications/preferences');
  },

  updatePreferences: async (preferences: any): Promise<void> => {
    if (MOCK_MODE) {
      console.log('Mock: Updating notification preferences', preferences);
      return;
    }
    await apiRequest('/notifications/preferences', {
      method: 'PUT',
      body: JSON.stringify(preferences),
    });
  },
};

