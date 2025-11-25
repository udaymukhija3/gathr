import { useCallback, useRef } from 'react';
import { Activity } from '../types';
import { feedApi } from '../services/api';

type FeedFeedbackAction = 'viewed' | 'clicked' | 'joined' | 'dismissed';

const generateSessionId = (): string => {
  return (
    Date.now().toString(36) +
    Math.random().toString(36).substring(2, 9)
  ).toUpperCase();
};

interface UseFeedSessionOptions {
  feedClient?: typeof feedApi;
}

export const useFeedSession = (options: UseFeedSessionOptions = {}) => {
  const { feedClient = feedApi } = options;
  const sessionIdRef = useRef<string>(generateSessionId());
  const viewedActivityIdsRef = useRef<Set<number>>(new Set());

  const resetSession = useCallback(() => {
    sessionIdRef.current = generateSessionId();
    viewedActivityIdsRef.current.clear();
  }, []);

  const sendFeedback = useCallback(
    (action: FeedFeedbackAction, activity: Activity, position?: number) => {
      if (!activity?.id) {
        return;
      }
      feedClient
        .trackFeedback({
          activityId: activity.id,
          action,
          position,
          score: activity.feedScore,
          hubId: activity.hubId,
          sessionId: sessionIdRef.current,
        })
        .catch(err => console.warn('Failed to send feed feedback', err));
    },
    [feedClient]
  );

  const markActivitiesViewed = useCallback(
    (items: Activity[]) => {
      items.forEach((activity, index) => {
        if (activity?.id == null) {
          return;
        }
        if (viewedActivityIdsRef.current.has(activity.id)) {
          return;
        }
        viewedActivityIdsRef.current.add(activity.id);
        sendFeedback('viewed', activity, index);
      });
    },
    [sendFeedback]
  );

  return {
    sessionId: sessionIdRef.current,
    resetSession,
    sendFeedback,
    markActivitiesViewed,
  };
};

