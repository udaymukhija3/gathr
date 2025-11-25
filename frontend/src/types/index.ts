export interface User {
  id: number;
  name: string;
  phone: string;
  verified: boolean;
  createdAt: string;
  bio?: string;
  avatarUrl?: string;
  interests?: string[];
  homeHubId?: number;
  latitude?: number;
  longitude?: number;
  onboardingCompleted?: boolean;
}

export interface UserProfile {
  id: number;
  name: string;
  phone: string;
  bio?: string;
  avatarUrl?: string;
  interests?: string[];
  homeHubId?: number;
  homeHubName?: string;
  trustScore?: number;
  activitiesCount?: number;
  onboardingCompleted?: boolean;
}

export interface UpdateProfileRequest {
  name?: string;
  bio?: string;
  avatarUrl?: string;
}

export interface TrustScore {
  score: number;
  activitiesAttended: number;
  noShows: number;
  averageRating: number;
  reportsReceived: number;
}

export interface UserActivity {
  id: number;
  title: string;
  hubId: number;
  hubName: string;
  category: ActivityCategory;
  startTime: string;
  endTime: string;
  status: 'SCHEDULED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  participationStatus: 'INTERESTED' | 'CONFIRMED' | 'LEFT';
  isCreator: boolean;
}

export interface Hub {
  id: number;
  name: string;
  area: string;
  description?: string;
  latitude?: number;
  longitude?: number;
  distance?: number; // Distance from user in km (when using nearest endpoint)
}

export type ActivityCategory = 'SPORTS' | 'FOOD' | 'ART' | 'MUSIC' | 'OUTDOOR' | 'GAMES' | 'LEARNING' | 'WELLNESS';

export interface Activity {
  id: number;
  title: string;
  hubId?: number;
  hubName?: string;
  locationName?: string;
  locationAddress?: string;
  placeId?: string;
  latitude?: number;
  longitude?: number;
  isUserLocation?: boolean;
  distanceKm?: number;
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
  spotsRemaining?: number;
  // Optional feed-specific metadata
  feedScore?: number;
  feedPrimaryReason?: string;
  feedSecondaryReason?: string;
  coldStartType?: string;
  isNewActivity?: boolean;
  recommendationMeta?: Record<string, any>;
  isInviteOnly?: boolean;
  revealIdentities?: boolean;
  maxMembers?: number;
  description?: string;
}

export interface FeedMetaPayload {
  ctaText?: string;
  scarcityMessage?: string;
  timeWindowLabel?: string;
  topActivityIds?: number[];
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
  hubId?: number;
  category: ActivityCategory;
  startTime: string;
  endTime: string;
  description?: string;
  isInviteOnly?: boolean;
  maxMembers?: number;
  placeId?: string;
  placeName?: string;
  placeAddress?: string;
  latitude?: number;
  longitude?: number;
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

