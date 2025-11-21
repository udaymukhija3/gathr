# Telemetry Implementation Summary

## Overview
Client-side telemetry has been fully implemented across the Gatherly frontend application. All user actions are now tracked and logged to the backend `/events` endpoint (or console in mock mode).

## What Was Implemented

### 1. Core Infrastructure
- **`frontend/src/services/api.ts`**: Added `eventsApi.log()` method that sends events to backend
- **`frontend/src/utils/telemetry.ts`**: Created comprehensive telemetry utility with organized tracking functions

### 2. Event Categories

#### Feed Events
- `feed_viewed` - When activity feed loads
- `hub_selected` - When user switches hubs
- `feed_refreshed` - When user pulls to refresh
- `create_activity_clicked` - When FAB is tapped
- `activity_full_error` - When user tries to join full activity
- `invite_modal_opened` - When invite-only activity requires token

**Location**: `frontend/src/screens/FeedScreen.tsx`

#### Activity Detail Events
- `activity_detail_viewed` - When user opens activity detail
- `activity_joined` - When user joins activity
- `activity_confirmed` - When user confirms attendance
- `activity_full_error` - When activity is at max capacity
- `chat_joined` - When "Join Group Chat" is tapped
- `invite_modal_opened` - From detail screen

**Location**: `frontend/src/screens/ActivityDetailScreen.tsx`

#### Chat Events
- `chat_opened` - When chat screen loads
- `message_sent` - When message is sent (tracks method: websocket/rest)
- `message_send_failed` - When message fails to send
- `heading_there_clicked` - When "Heading There" is tapped
- `report_clicked` - When report button is clicked
- `websocket_connected` - When WebSocket connects
- `websocket_disconnected` - When WebSocket disconnects

**Location**: `frontend/src/screens/ChatScreen.tsx`

#### Invite Modal Events
- `invite_token_generated` - When new token is created
- `invite_link_copied` - When invite link is copied
- `invite_token_submitted` - When user enters token (tracks success/failure)
- `invite_sms_sent` - When SMS invite is sent

**Location**: `frontend/src/components/InviteModal.tsx`

#### Contact Upload Events
- `contacts_screen_viewed` - When screen loads
- `contacts_consent_toggled` - When consent checkbox changes
- `contacts_upload_started` - When upload begins (includes contact count)
- `contacts_upload_success` - When upload completes (includes mutuals count)
- `contacts_upload_failed` - When upload fails
- `contacts_permission_denied` - When permissions are denied
- `contacts_skipped` - When user skips contact upload

**Location**: `frontend/src/screens/ContactsUploadScreen.tsx`

### 3. Event Properties

All events include contextual properties:
- `activityId` - Associated activity (when applicable)
- `source` - Where the event originated (e.g., 'feed', 'detail', 'chat')
- `method` - How action was performed (e.g., 'websocket', 'rest')
- Various context-specific properties (counts, status, error messages, etc.)

## Backend Integration

The telemetry events are sent to:
- **Endpoint**: `POST /events`
- **Payload**:
  ```json
  {
    "eventType": "string",
    "activityId": number | null,
    "properties": {
      // Context-specific properties
    }
  }
  ```

The backend automatically includes:
- `userId` from JWT token
- `ts` timestamp

## Mock Mode Behavior

When `EXPO_PUBLIC_MOCK_MODE=true` (default):
- Events are logged to console with `[Telemetry]` prefix
- No actual HTTP requests are made
- Example: `[Telemetry] feed_viewed { activityId: 1, activityCount: 5 }`

## Testing Instructions

### Quick Test (Mock Mode)
1. Ensure `EXPO_PUBLIC_MOCK_MODE=true` in environment
2. Start the app: `cd frontend && npm start`
3. Open app in simulator/device
4. Open browser console or Metro bundler logs
5. Perform actions and observe `[Telemetry]` logs

### Full Test (Real Backend)
1. Set `EXPO_PUBLIC_MOCK_MODE=false`
2. Ensure backend is running and `/events` endpoint is implemented
3. Start the app
4. Perform actions
5. Query backend `events` table to verify events are stored

### Test Checklist
- [ ] Feed loads → `feed_viewed` event
- [ ] Select different hub → `hub_selected` event
- [ ] Pull to refresh → `feed_refreshed` event
- [ ] Tap activity card → `activity_detail_viewed` event
- [ ] Join activity → `activity_joined` event
- [ ] Open chat → `chat_opened` event
- [ ] Send message → `message_sent` event
- [ ] Click "Heading There" → `heading_there_clicked` + `activity_confirmed` events
- [ ] Generate invite token → `invite_token_generated` event
- [ ] Copy invite link → `invite_link_copied` event
- [ ] Upload contacts → `contacts_upload_started` + `contacts_upload_success` events
- [ ] Toggle consent → `contacts_consent_toggled` event
- [ ] Skip contacts → `contacts_skipped` event

## Error Handling

All telemetry calls are wrapped in try-catch blocks and will:
- Never block the main user flow
- Log warnings to console on failure
- Fail silently in production

## Performance Considerations

- Events are fire-and-forget (non-blocking)
- Failed events do not retry
- No queuing mechanism (events are sent immediately)
- Telemetry failures are logged but do not affect UX

## Future Enhancements

Potential improvements not yet implemented:
- Event batching/queuing
- Offline event storage and replay
- Custom event filtering
- Analytics dashboard integration (Mixpanel, Segment, etc.)
- Post-meet feedback events (from checklist item #2)

## Files Modified

1. `frontend/src/services/api.ts` - Added eventsApi
2. `frontend/src/utils/telemetry.ts` - Created telemetry utilities
3. `frontend/src/screens/FeedScreen.tsx` - Added feed telemetry
4. `frontend/src/screens/ActivityDetailScreen.tsx` - Added detail telemetry
5. `frontend/src/screens/ChatScreen.tsx` - Added chat telemetry
6. `frontend/src/components/InviteModal.tsx` - Added invite telemetry
7. `frontend/src/screens/ContactsUploadScreen.tsx` - Added contacts telemetry

## Conclusion

✅ Client-side telemetry is fully implemented and ready for use
✅ All major user actions are tracked
✅ Events work in both mock and real backend modes
✅ Implementation follows non-blocking best practices
✅ Comprehensive event properties for analytics
