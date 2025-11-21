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
  interestedCount?: number;
  confirmedCount?: number;
  totalParticipants?: number;
  peopleCount?: number; // Alias for totalParticipants
  mutualsCount?: number;
  isInviteOnly?: boolean;
  revealIdentities?: boolean;
  maxMembers?: number;
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
  isInviteOnly?: boolean;
  maxMembers?: number;
}

export interface Participant {
  userId: number;
  firstName?: string;
  mutualsCount?: number;
  revealed?: boolean;
}

export interface ReportRequest {
  targetUserId: number;
  activityId?: number;
  reason: string;
}

export interface ContactUploadRequest {
  hashes: string[];
}

export interface ContactUploadResponse {
  mutualsCount: number;
}

export type ParticipationStatus = 'INTERESTED' | 'CONFIRMED';

export interface ActivityTemplate {
  id: number;
  name: string;
  title: string;
  category: ActivityCategory;
  durationHours?: number;
  description?: string;
  isSystemTemplate: boolean;
  isInviteOnly: boolean;
  maxMembers: number;
}

export interface CreateTemplateRequest {
  name: string;
  title: string;
  category: ActivityCategory;
  durationHours?: number;
  description?: string;
  isInviteOnly?: boolean;
  maxMembers?: number;
}

