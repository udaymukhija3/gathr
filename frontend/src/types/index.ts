export interface User {
  id: number;
  name: string;
  phone: string;
  verified: boolean;
  createdAt: string;
}

export interface Hub {
  id: number;
  name: string;
  area: string;
  description?: string;
}

export type ActivityCategory = 'SPORTS' | 'FOOD' | 'ART' | 'MUSIC';

export interface Activity {
  id: number;
  title: string;
  hubId: number;
  hubName: string;
  category: ActivityCategory;
  startTime: string;
  endTime: string;
  createdBy: number;
  createdByName: string;
  peopleCount?: number;
  mutualsCount?: number;
  isInviteOnly?: boolean;
  description?: string;
}

export interface Participant {
  anonId: string;
  mutualsCount: number;
  revealed: boolean;
  name?: string;
  userId: number;
}

export interface ActivityDetail extends Activity {
  participants: Participant[];
  messagesCount?: number;
}

export interface Message {
  id: number;
  activityId: number;
  userId: number;
  userName: string;
  text: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface CreateActivityRequest {
  title: string;
  hubId: number;
  category: ActivityCategory;
  startTime: string;
  endTime: string;
  description?: string;
  inviteOnly?: boolean;
}

export type ParticipationStatus = 'INTERESTED' | 'CONFIRMED';

