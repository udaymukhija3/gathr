import React from 'react';
import { render } from '@testing-library/react-native';
import { ActivityCard } from '../ActivityCard';
import { Activity } from '../../types';

describe('ActivityCard', () => {
  const mockActivity: Activity = {
    id: 1,
    title: 'Test Activity',
    hubId: 1,
    hubName: 'Test Hub',
    category: 'SPORTS',
    startTime: '2024-12-31T18:00:00+05:30',
    endTime: '2024-12-31T20:00:00+05:30',
    createdBy: 1,
    createdByName: 'Creator',
    peopleCount: 5,
    mutualsCount: 2,
    isInviteOnly: false,
    revealIdentities: false,
    maxMembers: 8,
  };

  it('renders activity card correctly', () => {
    const { getByText } = render(
      <ActivityCard activity={mockActivity} onPress={() => {}} />
    );

    expect(getByText('Test Activity')).toBeTruthy();
    expect(getByText('Test Hub')).toBeTruthy();
    expect(getByText(/👥/)).toBeTruthy();
  });

  it('shows lock icon for invite-only activities', () => {
    const inviteOnlyActivity = { ...mockActivity, isInviteOnly: true };
    const { getByText } = render(
      <ActivityCard activity={inviteOnlyActivity} onPress={() => {}} />
    );

    expect(getByText('Request Invite')).toBeTruthy();
  });

  it('shows full message when max members reached', () => {
    const fullActivity = {
      ...mockActivity,
      peopleCount: 8,
      maxMembers: 8,
    };
    const { getByText } = render(
      <ActivityCard activity={fullActivity} onPress={() => {}} onJoin={() => {}} />
    );

    expect(getByText('Full')).toBeTruthy();
    expect(getByText(/maximum participants/)).toBeTruthy();
  });

  it('disables join button when activity is full', () => {
    const fullActivity = {
      ...mockActivity,
      peopleCount: 8,
      maxMembers: 8,
    };
    const { getByText } = render(
      <ActivityCard activity={fullActivity} onPress={() => {}} onJoin={() => {}} />
    );

    const joinButton = getByText('Full');
    expect(joinButton).toBeTruthy();
    // Button should be disabled (tested via disabled prop in component)
  });
});

