# Codebase Analysis & Next Steps

## ✅ What's Complete

### Core Infrastructure
- ✅ UserContext with authentication state management
- ✅ useApi hook with error handling
- ✅ useWebSocket hook with reconnection logic
- ✅ Contact hashing utility (SHA-256)
- ✅ Mock data and API client
- ✅ TypeScript types updated
- ✅ Toast notifications configured

### Screens Implemented
- ✅ PhoneEntryScreen (with rate limiting handling)
- ✅ OtpVerifyScreen (with display name prompt)
- ✅ FeedScreen (Home)
- ✅ ActivityDetailScreen
- ✅ ChatScreen (polling only)
- ✅ CreateActivityScreen
- ✅ ContactsUploadScreen
- ✅ SettingsScreen

### Components
- ✅ ActivityCard (with invite-only, max members)
- ✅ InviteModal (created but not integrated)
- ✅ AvatarAnon
- ✅ HubSelector
- ✅ MutualBadge
- ✅ ChatBubble

---

## ⚠️ Critical Issues to Fix

### 1. **ChatScreen Missing WebSocket Integration** 🔴 HIGH PRIORITY
**Current State:** Only uses polling (every 3 seconds)
**Required:** Integrate useWebSocket hook with polling fallback

**File:** `src/screens/ChatScreen.tsx`
**Action:**
```typescript
// Add WebSocket integration
import { useWebSocket } from '../hooks/useWebSocket';

// In component:
const { isConnected, sendMessage } = useWebSocket({
  activityId,
  onMessage: (msg) => {
    if (msg.type === 'message') {
      setMessages(prev => [...prev, msg.payload]);
    }
  },
  enabled: true,
});

// Update handleSend to use WebSocket
const handleSend = async () => {
  // Try WebSocket first, fallback to REST
  if (isConnected) {
    sendMessage({ type: 'message', payload: { text: messageText } });
  } else {
    await messagesApi.create(activityId, messageText);
  }
};
```

### 2. **Invite Token Flow Not Integrated** 🔴 HIGH PRIORITY
**Current State:** InviteModal exists but not used in ActivityDetailScreen
**Required:** Integrate invite token flow for invite-only activities

**Files:** 
- `src/screens/ActivityDetailScreen.tsx`
- `src/screens/FeedScreen.tsx`

**Action:**
```typescript
// In ActivityDetailScreen.tsx
import { InviteModal } from '../components/InviteModal';
const [showInviteModal, setShowInviteModal] = useState(false);

// Update handleJoin:
const handleJoin = async (inviteToken?: string) => {
  if (activity.isInviteOnly && !inviteToken) {
    setShowInviteModal(true);
    return;
  }
  // ... rest of join logic
};

// Add InviteModal component
<InviteModal
  visible={showInviteModal}
  activityId={activityId}
  onClose={() => setShowInviteModal(false)}
  onTokenEntered={handleJoin}
/>
```

### 3. **Confirm Endpoint Not Used** 🟡 MEDIUM PRIORITY
**Current State:** ChatScreen uses `join` with CONFIRMED status
**Required:** Use dedicated `/activities/:id/confirm` endpoint

**File:** `src/screens/ChatScreen.tsx`
**Action:**
```typescript
// Replace:
await activitiesApi.join(activityId, 'CONFIRMED');

// With:
await activitiesApi.confirm(activityId);
```

### 4. **Report Functionality Missing in ChatScreen** 🟡 MEDIUM PRIORITY
**Current State:** No report button in chat
**Required:** Add report button with modal

**File:** `src/screens/ChatScreen.tsx`
**Action:** Add report button in header, create ReportModal component

### 5. **Message Expiry Countdown Missing** 🟡 MEDIUM PRIORITY
**Current State:** Shows static message about expiry
**Required:** Calculate and show countdown timer

**File:** `src/screens/ChatScreen.tsx`
**Action:**
```typescript
const getExpiryCountdown = () => {
  if (!activity) return '';
  const expiry = parseISO(activity.endTime).getTime() + 24 * 60 * 60 * 1000;
  const now = Date.now();
  const diff = expiry - now;
  if (diff < 0) return 'Messages expired';
  const hours = Math.floor(diff / (60 * 60 * 1000));
  const minutes = Math.floor((diff % (60 * 60 * 1000)) / (60 * 1000));
  return `Messages expire in ${hours}h ${minutes}m`;
};
```

### 6. **FeedScreen Join Doesn't Handle Invite Tokens** 🟡 MEDIUM PRIORITY
**Current State:** Direct join call, no invite token handling
**Required:** Show InviteModal for invite-only activities

**File:** `src/screens/FeedScreen.tsx`
**Action:** Similar to ActivityDetailScreen - check isInviteOnly and show modal

---

## 📋 Missing Features

### 1. **Post-Meet Feedback Popup** 🟢 LOW PRIORITY
**Required:** After activity end time, prompt "Did you meet?" and "Would you meet again?"
**Action:** Create PostMeetFeedbackScreen component, trigger after activity.endTime

### 2. **Settings Navigation** 🟢 LOW PRIORITY
**Required:** Add Settings button to navigation/header
**Action:** Add Settings link in FeedScreen header or bottom tab

### 3. **Error Toast Configuration** 🟢 LOW PRIORITY
**Required:** Ensure Toast is properly configured globally
**Action:** Verify Toast root component in App.tsx (already added ✅)

### 4. **Activity Templates in CreateActivityScreen** 🟢 LOW PRIORITY
**Required:** Quick-create buttons (Coffee Tonight, Pickleball 7pm, etc.)
**Action:** Add template buttons that pre-fill form

---

## 🔧 Code Quality Improvements

### 1. **Error Handling**
- ✅ Toast notifications added
- ⚠️ Need consistent error handling across all screens
- ⚠️ Network error retry logic

### 2. **Loading States**
- ✅ Most screens have loading indicators
- ⚠️ Add skeleton loaders for better UX

### 3. **Type Safety**
- ✅ Types defined
- ⚠️ Some `any` types in error handlers (acceptable for now)

### 4. **Testing**
- ✅ ActivityCard test added
- ⚠️ Need more component tests
- ⚠️ Integration tests for flows

---

## 🚀 Immediate Action Items (Priority Order)

### Phase 1: Critical Fixes (Do First)
1. **Integrate WebSocket in ChatScreen** (30 min)
   - Add useWebSocket hook
   - Update message sending logic
   - Test WebSocket + polling fallback

2. **Integrate InviteModal in ActivityDetailScreen** (20 min)
   - Add state for modal visibility
   - Update handleJoin to check invite-only
   - Wire up InviteModal component

3. **Fix FeedScreen invite token handling** (15 min)
   - Similar to ActivityDetailScreen
   - Show modal when joining invite-only from feed

### Phase 2: Important Features (Do Next)
4. **Add Report functionality to ChatScreen** (45 min)
   - Create ReportModal component
   - Add report button in chat header
   - Wire up reportsApi.create

5. **Fix confirm endpoint usage** (10 min)
   - Update ChatScreen to use confirm endpoint
   - Update API service if needed

6. **Add message expiry countdown** (20 min)
   - Calculate expiry time
   - Update UI to show countdown
   - Handle expired state

### Phase 3: Polish & Testing (Do Last)
7. **Add Settings navigation** (15 min)
   - Add Settings button/link
   - Test navigation flow

8. **Add activity templates** (30 min)
   - Create template data
   - Add quick-create buttons
   - Pre-fill form on selection

9. **Add post-meet feedback** (1 hour)
   - Create feedback screen
   - Add timing logic
   - Wire up event logging

---

## 📝 Testing Checklist

Before considering complete:

- [ ] WebSocket connects and receives messages
- [ ] Polling fallback works when WebSocket fails
- [ ] Invite token flow works end-to-end
- [ ] Report creation works from chat
- [ ] Message expiry countdown displays correctly
- [ ] Max members enforcement works
- [ ] Identity reveal logic displays correctly
- [ ] Contact upload works and shows mutuals
- [ ] Rate limiting shows proper error
- [ ] All error states handled gracefully

---

## 🔗 Backend Integration Status

**Ready to Connect:** ✅
- All API endpoints defined
- Error handling in place
- Mock mode available for testing

**Backend Checklist:** See `BACKEND_INTEGRATION.md`

**Next:** Connect to real backend and test all endpoints

---

## 📦 Dependencies Status

**All Required Dependencies:** ✅ Installed
- react-native-toast-message ✅
- js-sha256 ✅
- axios ✅
- expo-contacts ✅
- All others ✅

**No Missing Dependencies**

---

## 🎯 Recommended Next Steps (In Order)

1. **Fix WebSocket integration** (30 min) - Critical for real-time chat
2. **Fix invite token flow** (35 min) - Critical for invite-only feature
3. **Add report functionality** (45 min) - Important for moderation
4. **Add message expiry countdown** (20 min) - UX improvement
5. **Test end-to-end flows** (1 hour) - Ensure everything works
6. **Connect to backend** - Use BACKEND_INTEGRATION.md checklist
7. **Polish & deploy** - Final touches before production

---

## 📚 Documentation Status

- ✅ README.md - Complete
- ✅ BACKEND_INTEGRATION.md - Complete
- ✅ MANUAL_STEPS.md - Complete
- ✅ NEXT_STEPS.md - This file

**All documentation is in place!**

---

## 💡 Quick Wins (Easy Fixes)

1. **Add Settings to navigation** - 15 min
2. **Fix confirm endpoint** - 10 min  
3. **Add message countdown** - 20 min
4. **Improve error messages** - 30 min

These can be done quickly and improve UX significantly.

---

## 🐛 Known Issues

1. **ChatScreen polling inefficient** - Should use WebSocket primarily
2. **InviteModal not integrated** - Created but not used
3. **No report button in chat** - Feature missing
4. **Static expiry message** - Should show countdown

All documented above with fixes.

---

## ✨ Summary

**Status:** ~85% Complete
**Critical Issues:** 3 (WebSocket, Invite tokens, Reports)
**Estimated Time to Production-Ready:** 3-4 hours of focused work

**The codebase is solid and well-structured. Main gaps are integration of existing components and a few missing features. Once critical fixes are done, it's ready for backend integration and testing.**

