import { act, renderHook } from '@testing-library/react-native';
import { Activity } from '../../types';
import { useFeedSession } from '../useFeedSession';

describe('useFeedSession', () => {
  const activityFactory = (overrides: Partial<Activity> = {}): Activity => ({
    id: overrides.id ?? Math.floor(Math.random() * 1000),
    title: 'Test Activity',
    hubId: 1,
    hubName: 'Test Hub',
    category: 'SPORTS',
    startTime: new Date().toISOString(),
    endTime: new Date().toISOString(),
    createdBy: 1,
    createdByName: 'Tester',
    ...overrides,
  });

  it('sends viewed feedback only once per activity', () => {
    const feedClient = {
      trackFeedback: jest.fn().mockResolvedValue(undefined),
    };

    const { result } = renderHook(() =>
      useFeedSession({ feedClient: feedClient as any })
    );

    const activities = [
      activityFactory({ id: 1 }),
      activityFactory({ id: 2 }),
    ];

    act(() => result.current.markActivitiesViewed(activities));
    act(() => result.current.markActivitiesViewed(activities));

    expect(feedClient.trackFeedback).toHaveBeenCalledTimes(2);
    expect(feedClient.trackFeedback).toHaveBeenNthCalledWith(
      1,
      expect.objectContaining({ activityId: 1, action: 'viewed', position: 0 })
    );
    expect(feedClient.trackFeedback).toHaveBeenNthCalledWith(
      2,
      expect.objectContaining({ activityId: 2, action: 'viewed', position: 1 })
    );
  });

  it('resets session id and viewed set when resetSession is called', () => {
    const feedClient = {
      trackFeedback: jest.fn().mockResolvedValue(undefined),
    };

    const { result } = renderHook(() =>
      useFeedSession({ feedClient: feedClient as any })
    );

    const initialSessionId = result.current.sessionId;

    act(() => {
      result.current.resetSession();
    });

    expect(result.current.sessionId).not.toEqual(initialSessionId);

    const activity = activityFactory({ id: 42 });
    act(() => result.current.markActivitiesViewed([activity]));
    expect(feedClient.trackFeedback).toHaveBeenCalledWith(
      expect.objectContaining({ activityId: 42, sessionId: result.current.sessionId })
    );
  });
});

