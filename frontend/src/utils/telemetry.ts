import { eventsApi } from '../services/api';

/**
 * Telemetry utility for tracking user events and analytics.
 * All events are non-blocking and will not affect the main user flow.
 */

/**
 * Log a telemetry event
 * @param eventType - The type of event (e.g., 'feed_viewed', 'activity_joined')
 * @param properties - Optional key-value pairs with event metadata
 * @param activityId - Optional activity ID if the event is related to a specific activity
 */
export const trackEvent = async (
  eventType: string,
  properties?: Record<string, any>,
  activityId?: number
): Promise<void> => {
  try {
    await eventsApi.log(eventType, properties, activityId);
  } catch (error) {
    // Silently fail - telemetry errors should never disrupt the user experience
    console.warn(`[Telemetry] Failed to track event: ${eventType}`, error);
  }
};

/**
 * Track activity-related events
 */
export const trackActivity = {
  viewed: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('activity_detail_viewed', properties, activityId),

  joined: (activityId: number, status: 'INTERESTED' | 'CONFIRMED', properties?: Record<string, any>) =>
    trackEvent('activity_joined', { status, ...properties }, activityId),

  confirmed: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('activity_confirmed', properties, activityId),

  inviteModalOpened: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('invite_modal_opened', properties, activityId),

  inviteTokenGenerated: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('invite_token_generated', properties, activityId),

  inviteLinkCopied: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('invite_link_copied', properties, activityId),

  inviteTokenSubmitted: (activityId: number, success: boolean, properties?: Record<string, any>) =>
    trackEvent('invite_token_submitted', { success, ...properties }, activityId),

  inviteSMSSent: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('invite_sms_sent', properties, activityId),

  chatJoined: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('chat_joined', properties, activityId),

  full: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('activity_full_error', properties, activityId),
};

/**
 * Track feed-related events
 */
export const trackFeed = {
  viewed: (hubId?: number, properties?: Record<string, any>) =>
    trackEvent('feed_viewed', { hubId, ...properties }),

  hubSelected: (hubId: number, hubName: string, properties?: Record<string, any>) =>
    trackEvent('hub_selected', { hubId, hubName, ...properties }),

  refreshed: (hubId?: number, properties?: Record<string, any>) =>
    trackEvent('feed_refreshed', { hubId, ...properties }),

  createActivityClicked: (properties?: Record<string, any>) =>
    trackEvent('create_activity_clicked', properties),
};

/**
 * Track chat-related events
 */
export const trackChat = {
  opened: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('chat_opened', properties, activityId),

  messageSent: (activityId: number, textLength: number, properties?: Record<string, any>) =>
    trackEvent('message_sent', { textLength, ...properties }, activityId),

  messageFailed: (activityId: number, error: string, properties?: Record<string, any>) =>
    trackEvent('message_send_failed', { error, ...properties }, activityId),

  headingThereClicked: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('heading_there_clicked', properties, activityId),

  reportClicked: (activityId: number, targetUserId: number, properties?: Record<string, any>) =>
    trackEvent('report_clicked', { targetUserId, ...properties }, activityId),

  websocketConnected: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('websocket_connected', properties, activityId),

  websocketDisconnected: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('websocket_disconnected', properties, activityId),
};

/**
 * Track contacts-related events
 */
export const trackContacts = {
  screenViewed: (properties?: Record<string, any>) =>
    trackEvent('contacts_screen_viewed', properties),

  consentToggled: (consented: boolean, properties?: Record<string, any>) =>
    trackEvent('contacts_consent_toggled', { consented, ...properties }),

  uploadStarted: (contactCount: number, properties?: Record<string, any>) =>
    trackEvent('contacts_upload_started', { contactCount, ...properties }),

  uploadSuccess: (mutualsCount: number, properties?: Record<string, any>) =>
    trackEvent('contacts_upload_success', { mutualsCount, ...properties }),

  uploadFailed: (error: string, properties?: Record<string, any>) =>
    trackEvent('contacts_upload_failed', { error, ...properties }),

  permissionDenied: (properties?: Record<string, any>) =>
    trackEvent('contacts_permission_denied', properties),

  skipped: (properties?: Record<string, any>) =>
    trackEvent('contacts_skipped', properties),
};

/**
 * Track report-related events
 */
export const trackReport = {
  created: (targetUserId: number, activityId: number | undefined, reason: string) =>
    trackEvent('report_created', { targetUserId, activityId, reason }, activityId),
};

/**
 * Track authentication-related events
 */
export const trackAuth = {
  phoneEntryViewed: (properties?: Record<string, any>) =>
    trackEvent('phone_entry_viewed', properties),

  otpRequested: (phone: string, properties?: Record<string, any>) =>
    trackEvent('otp_requested', { phone, ...properties }),

  otpRequestFailed: (phone: string, error: string, properties?: Record<string, any>) =>
    trackEvent('otp_request_failed', { phone, error, ...properties }),

  otpVerifyViewed: (phone: string, properties?: Record<string, any>) =>
    trackEvent('otp_verify_viewed', { phone, ...properties }),

  otpVerified: (phone: string, properties?: Record<string, any>) =>
    trackEvent('otp_verified', { phone, ...properties }),

  otpVerifyFailed: (phone: string, error: string, properties?: Record<string, any>) =>
    trackEvent('otp_verify_failed', { phone, error, ...properties }),

  loginSuccess: (userId: number, properties?: Record<string, any>) =>
    trackEvent('login_success', { userId, ...properties }),

  logout: (properties?: Record<string, any>) =>
    trackEvent('logout', properties),
};

/**
 * Track settings-related events
 */
export const trackSettings = {
  screenViewed: (properties?: Record<string, any>) =>
    trackEvent('settings_screen_viewed', properties),

  contactsToggled: (enabled: boolean, properties?: Record<string, any>) =>
    trackEvent('settings_contacts_toggled', { enabled, ...properties }),

  privacyPolicyClicked: (properties?: Record<string, any>) =>
    trackEvent('settings_privacy_policy_clicked', properties),

  logoutClicked: (properties?: Record<string, any>) =>
    trackEvent('settings_logout_clicked', properties),

  deleteAccountClicked: (properties?: Record<string, any>) =>
    trackEvent('settings_delete_account_clicked', properties),

  deleteAccountConfirmed: (properties?: Record<string, any>) =>
    trackEvent('settings_delete_account_confirmed', properties),
};

/**
 * Track template-related events
 */
export const trackTemplate = {
  selectionViewed: (properties?: Record<string, any>) =>
    trackEvent('template_selection_viewed', properties),

  filterChanged: (filter: string, properties?: Record<string, any>) =>
    trackEvent('template_filter_changed', { filter, ...properties }),

  templateSelected: (templateId: number, templateName: string, properties?: Record<string, any>) =>
    trackEvent('template_selected', { templateId, templateName, ...properties }),

  startFromScratch: (properties?: Record<string, any>) =>
    trackEvent('template_start_from_scratch', properties),

  templateDeleted: (templateId: number, properties?: Record<string, any>) =>
    trackEvent('template_deleted', { templateId, ...properties }),
};

/**
 * Track activity creation events
 */
export const trackActivityCreation = {
  screenViewed: (fromTemplate: boolean, properties?: Record<string, any>) =>
    trackEvent('activity_creation_viewed', { fromTemplate, ...properties }),

  hubSelected: (hubId: number, hubName: string, properties?: Record<string, any>) =>
    trackEvent('activity_creation_hub_selected', { hubId, hubName, ...properties }),

  categorySelected: (category: string, properties?: Record<string, any>) =>
    trackEvent('activity_creation_category_selected', { category, ...properties }),

  submitted: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('activity_creation_submitted', properties, activityId),

  failed: (error: string, properties?: Record<string, any>) =>
    trackEvent('activity_creation_failed', { error, ...properties }),

  cancelled: (properties?: Record<string, any>) =>
    trackEvent('activity_creation_cancelled', properties),
};

/**
 * Track invite-related events
 */
export const trackInvite = {
  screenViewed: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('invite_screen_viewed', properties, activityId),

  linkCopied: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('invite_link_copied', properties, activityId),

  linkShared: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('invite_link_shared', properties, activityId),

  phoneInviteSent: (activityId: number, phone: string, properties?: Record<string, any>) =>
    trackEvent('invite_phone_sent', { phone, ...properties }, activityId),

  phoneInviteFailed: (activityId: number, error: string, properties?: Record<string, any>) =>
    trackEvent('invite_phone_failed', { error, ...properties }, activityId),
};

/**
 * Track feedback-related events
 */
export const trackFeedback = {
  modalOpened: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('feedback_modal_opened', properties, activityId),

  submitted: (activityId: number, didMeet: boolean, rating: number, properties?: Record<string, any>) =>
    trackEvent('feedback_submitted', { didMeet, rating, ...properties }, activityId),

  dismissed: (activityId: number, properties?: Record<string, any>) =>
    trackEvent('feedback_dismissed', properties, activityId),
};
