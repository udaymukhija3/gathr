# Gathr: Zero Thumb-Twiddling Execution Roadmap

## The Anti-Paralysis Protocol: Ship Something Every Day

### Current Reality Check
- **Backend:** 70% complete but missing critical production features
- **Frontend:** 0% visible (major red flag)
- **ML:** 0% ready (3+ months away realistically)
- **Safety:** 40% complete (basic features only)
- **Users:** 0 (no validation)

**Verdict:** You're planning a Mars mission when you haven't left the launchpad.

---

## WEEK 1: Emergency MVP Sprint (Dec 2-6, 2024)

### Monday: SMS & Frontend Bootstrap
**Morning (4 hours):**
```bash
# Backend task
1. Sign up for Twilio ($20 credit)
2. Replace mock OTP with real SMS:
   - Add Twilio SDK to pom.xml
   - Create TwilioService.java
   - Update AuthController
   - Test with your phone
```

**Afternoon (4 hours):**
```bash
# Frontend emergency setup
npx create-expo-app gathr-mobile --template
cd gathr-mobile
npm install @react-navigation/native axios
# Create basic screens:
# - PhoneLogin.tsx
# - OTPVerify.tsx
# - HubList.tsx
# - ActivityList.tsx
```

**Success Metric:** Send yourself a real OTP and log in from the app

### Tuesday: Core User Flow
**Morning:**
```javascript
// Complete the basic flow
screens/
├── ActivityDetail.tsx    // Show activity info
├── JoinActivity.tsx      // One-click join
└── ParticipantList.tsx   // See who's joining

// Minimal viable components
components/
├── ActivityCard.tsx      // Title, time, participants
└── SafetyBadge.tsx      // "2 mutuals" indicator
```

**Afternoon:**
```java
// Add missing backend endpoint
@GetMapping("/activities/upcoming")
public List<Activity> getMyUpcomingActivities() {
    // Return user's joined activities for next 7 days
}
```

**Success Metric:** Join an activity and see yourself in participants

### Wednesday: Push Notifications
**Morning:**
```bash
# Backend
1. Add Firebase Admin SDK
2. Create NotificationService.java
3. Add device token endpoint

# Frontend  
expo install expo-notifications
# Request permissions
# Send token to backend
```

**Afternoon:**
```java
// Trigger notifications
@EventListener
public void onActivityStartingSoon(ActivityStartEvent event) {
    // Send "Your meetup starts in 30 mins" notification
    notificationService.sendToParticipants(
        event.getActivityId(),
        "Starting soon at " + event.getLocation()
    );
}
```

**Success Metric:** Receive a push notification 30 mins before activity

### Thursday: Feedback Loop
**Morning:**
```javascript
// Post-activity feedback screen
screens/FeedbackModal.tsx:
- Did you show up? [Yes/No]
- Rate experience [1-5 stars]
- Would meet again? [User selector]
- Quick feedback [Text input]
```

**Afternoon:**
```sql
-- Create feedback table
CREATE TABLE activity_feedback (
    id SERIAL PRIMARY KEY,
    activity_id INTEGER,
    user_id INTEGER,
    showed_up BOOLEAN,
    rating INTEGER,
    feedback TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Success Metric:** Complete feedback for a test activity

### Friday: Deploy & Test
**Morning:**
```bash
# Backend deployment
1. Deploy to Heroku/Railway (free tier)
2. Setup PostgreSQL on Supabase (free)
3. Configure environment variables
4. Run migrations

# Frontend deployment
expo build:android
expo build:ios  # If you have Mac
```

**Afternoon:**
```
REAL USER TEST:
1. Get 3 friends to download TestFlight/APK
2. Create activity: "Coffee at Starbucks Cyberhub, 5pm"
3. Have them join
4. Actually meet
5. Collect feedback
```

**Success Metric:** 3 real humans use the app and meet IRL

---

## WEEK 2: Safety & Scale (Dec 9-13, 2024)

### Monday: Identity Reveal Fix
```java
@Service
public class IdentityRevealService {
    
    public boolean shouldRevealIdentities(Long activityId) {
        // FIX: Only count CONFIRMED participants
        int confirmed = participantRepository
            .countByActivityIdAndStatus(activityId, CONFIRMED);
        
        return confirmed >= REVEAL_THRESHOLD; // 3
    }
    
    @EventListener
    public void onThresholdReached(ThresholdEvent event) {
        // Push notification: "Identities revealed!"
        // Update UI to show real names
    }
}
```

### Tuesday: Rate Limiting
```java
@Bean
public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
    FilterRegistrationBean<RateLimitFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new RateLimitFilter(redis));
    registrationBean.addUrlPatterns("/api/*");
    return registrationBean;
}

// Limits per endpoint:
// /activities/create: 5 per hour
// /messages/send: 100 per hour  
// /join: 10 per hour
```

### Wednesday: WebSocket Chat
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
}

// Real-time messaging
@MessageMapping("/chat/{activityId}")
@SendTo("/topic/activity/{activityId}")
public ChatMessage sendMessage(@DestinationVariable Long activityId, 
                              ChatMessage message) {
    return messageService.process(activityId, message);
}
```

### Thursday: Location Verification
```javascript
// hooks/useLocationVerification.ts
export const useLocationVerification = (activity: Activity) => {
  const [isNearby, setIsNearby] = useState(false);
  
  useEffect(() => {
    Location.getCurrentPositionAsync().then(position => {
      const distance = calculateDistance(
        position.coords,
        activity.hub.coordinates
      );
      
      setIsNearby(distance < 500); // 500 meters
      
      if (isNearby) {
        // Auto-check in
        api.checkIn(activity.id, position);
      }
    });
  }, []);
  
  return { isNearby };
};
```

### Friday: 10-Person Alpha Test
```
TEST PROTOCOL:
1. Recruit 10 people from Cyberhub coworking
2. Create 3 activities:
   - Lunch at 12:30pm
   - Coffee at 3:30pm
   - Drinks at 6:30pm
3. Track metrics:
   - Sign-up rate
   - Join rate
   - Show-up rate
   - Feedback scores
4. Fix critical bugs same day
```

---

## WEEK 3: Intelligence Layer (Dec 16-20, 2024)

### Monday: Basic Recommendations
```python
# Simple heuristic scorer
def score_activity(user_id, activity):
    score = 0
    
    # Mutual connections (highest weight)
    mutuals = get_mutual_count(user_id, activity.participants)
    score += mutuals * 0.4
    
    # Time preference
    if is_preferred_time(user_id, activity.start_time):
        score += 0.3
    
    # Category match
    if activity.category in user_preferences[user_id]:
        score += 0.2
    
    # Distance
    distance = calculate_distance(user_location, activity.hub)
    score += (1 - min(distance / 5000, 1)) * 0.1
    
    return score
```

### Tuesday: Event Tracking Enhancement
```java
@EventListener
public void trackEverything(ApplicationEvent event) {
    Map<String, Object> properties = new HashMap<>();
    
    if (event instanceof ActivityJoinedEvent) {
        properties.put("activity_id", e.getActivityId());
        properties.put("user_id", e.getUserId());
        properties.put("mutuals_count", e.getMutualsCount());
        properties.put("time_to_start", e.getTimeToStart());
    }
    
    // Send to analytics
    mixpanel.track(event.getClass().getSimpleName(), properties);
    
    // Store for ML
    eventRepository.save(Event.from(event, properties));
}
```

### Wednesday: A/B Testing Framework
```java
@Service
public class ExperimentService {
    
    public boolean isInExperiment(Long userId, String experiment) {
        // Consistent hashing for user bucketing
        int bucket = Math.abs((userId + experiment).hashCode()) % 100;
        
        return switch(experiment) {
            case "EARLY_REVEAL" -> bucket < 50;  // 50% get early identity reveal
            case "SMART_NOTIFY" -> bucket < 30;  // 30% get ML-timed notifications
            case "SOCIAL_PROOF" -> bucket < 40;  // 40% see "3 friends joined"
            default -> false;
        };
    }
}
```

### Thursday: Trust Score v1
```sql
-- Calculate trust score
UPDATE users SET trust_score = (
    SELECT 
        100  -- Base score
        + (COUNT(*) FILTER (WHERE showed_up) * 5)      -- +5 per show up
        - (COUNT(*) FILTER (WHERE NOT showed_up) * 10) -- -10 per no-show  
        + (AVG(rating) * 10)                           -- Rating bonus
        - (reports_received * 20)                       -- -20 per report
    FROM activity_feedback
    WHERE user_id = users.id
);
```

### Friday: Data Pipeline Setup
```python
# airflow/dags/daily_metrics.py
@dag(schedule_interval='@daily')
def calculate_daily_metrics():
    
    @task
    def extract_events():
        return db.query("SELECT * FROM events WHERE created_at > NOW() - INTERVAL '1 day'")
    
    @task
    def calculate_metrics(events):
        return {
            'dau': len(set(e['user_id'] for e in events)),
            'activities_created': sum(1 for e in events if e['type'] == 'activity_created'),
            'show_up_rate': calculate_show_up_rate(events),
            'avg_mutuals': calculate_avg_mutuals(events)
        }
    
    @task
    def send_slack_summary(metrics):
        slack.send(f"Daily Metrics: {metrics}")
    
    events = extract_events()
    metrics = calculate_metrics(events)
    send_slack_summary(metrics)
```

---

## WEEK 4: Polish & Launch Prep (Dec 23-27, 2024)

### Monday: Performance Optimization
```java
// Add caching
@Cacheable(value = "activities", key = "#hubId")
public List<Activity> getActivities(Long hubId) {
    return activityRepository.findUpcoming(hubId);
}

// Database indexes
CREATE INDEX CONCURRENTLY idx_activities_hub_time 
ON activities(hub_id, start_time) 
WHERE status = 'ACTIVE';

// Connection pooling
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
```

### Tuesday: Error Handling
```javascript
// Global error boundary
export class ErrorBoundary extends Component {
  componentDidCatch(error, errorInfo) {
    // Log to Sentry
    Sentry.captureException(error, {
      contexts: { react: { componentStack: errorInfo.componentStack }}
    });
    
    // Show user-friendly error
    this.setState({ 
      hasError: true,
      message: getReadableError(error)
    });
  }
}

// API error handling
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // Refresh token
      return refreshToken().then(() => axios(error.config));
    }
    
    Toast.show({
      text: getErrorMessage(error),
      type: 'danger'
    });
    
    return Promise.reject(error);
  }
);
```

### Wednesday: Onboarding Flow
```javascript
// screens/Onboarding.tsx
const OnboardingFlow = () => {
  const screens = [
    {
      title: "Meet Nearby People",
      description: "Find 2-4 person hangouts at places you know",
      image: require('./assets/onboarding1.png')
    },
    {
      title: "Safety First",
      description: "See mutual connections before meeting",
      image: require('./assets/onboarding2.png')
    },
    {
      title: "Show Up & Vibe",
      description: "Small groups, real connections",
      image: require('./assets/onboarding3.png')
    }
  ];
  
  return (
    <AppIntroSlider
      data={screens}
      onDone={completeOnboarding}
      showSkipButton
    />
  );
};
```

### Thursday: Launch Checklist
```markdown
## Pre-Launch Checklist

### Legal
- [ ] Privacy Policy
- [ ] Terms of Service  
- [ ] Age verification (18+)
- [ ] Data deletion endpoint

### Security
- [ ] SSL certificates
- [ ] API rate limiting
- [ ] Input validation
- [ ] SQL injection prevention
- [ ] XSS protection

### Operations  
- [ ] Error monitoring (Sentry)
- [ ] Analytics (Mixpanel)
- [ ] Uptime monitoring (UptimeRobot)
- [ ] Database backups
- [ ] Runbook documented

### Marketing
- [ ] App Store listing
- [ ] Play Store listing
- [ ] Landing page
- [ ] Instagram account
- [ ] WhatsApp broadcast list
```

### Friday: Soft Launch
```
LAUNCH PLAN:
1. 9:00 AM - Deploy to production
2. 10:00 AM - Invite 20 beta testers
3. 12:00 PM - Create lunch activity
4. 1:00 PM - Monitor first real meetup
5. 3:00 PM - Coffee meetup
6. 6:00 PM - Happy hour meetup
7. 8:00 PM - Collect feedback
8. 10:00 PM - Fix critical bugs
```

---

## Success Metrics & Milestones

### Week 1 Success Criteria:
- [ ] 5 real users
- [ ] 1 successful meetup
- [ ] NPS > 7

### Week 2 Success Criteria:
- [ ] 25 users
- [ ] 5 successful meetups
- [ ] 60% show-up rate

### Week 3 Success Criteria:
- [ ] 100 users
- [ ] 20 meetups
- [ ] 1 organic meetup (not seeded)

### Week 4 Success Criteria:
- [ ] 250 users
- [ ] 50 meetups
- [ ] 20% week-over-week retention

---

## Daily Standup Template

```markdown
## Gathr Daily Standup - [Date]

### Yesterday:
- Shipped: [specific feature/fix]
- Metrics: [users/meetups/show-ups]
- Blockers resolved: [list]

### Today:
- Morning: [specific task]
- Afternoon: [specific task]
- Success metric: [measurable outcome]

### Blockers:
- [Blocker] → [Proposed solution]

### User Feedback:
- [Actual quote from user]
- Action: [What we'll do about it]

Time: 15 minutes MAX
```

---

## The Anti-Paralysis Rules

1. **No feature takes more than 2 days**
   - If it does, cut scope
   - Ship incomplete but functional

2. **No meetings without shipping**
   - Cancel all meetings until something is live
   - Meeting agenda = user feedback only

3. **No planning beyond next week**
   - This week: Ship
   - Next week: Fix what broke
   - Week after: We'll see

4. **No perfect, only better**
   - v1: Embarrassing but works
   - v2: Less embarrassing  
   - v3: Actually good

5. **No excuses, only experiments**
   - "We need ML first" → Test with manual curation
   - "We need scale first" → Start with 10 people
   - "We need funding first" → Use free tiers

---

## Emergency Pivot Options (If Week 2 Fails)

### Option A: "Lunch Roulette"
- Only lunch, only weekdays
- Only Cyberhub
- Match 2 people randomly
- No chat, just show up

### Option B: "Coffee Walks"  
- Morning walks with coffee
- Same route daily (Cyberhub to Leisure Valley)
- Join by showing up
- No app needed

### Option C: "Founder Drinks"
- Weekly Friday 6pm
- Same bar (Soi 7)
- Founders/freelancers only
- Vetted by LinkedIn

---

## Final Message

**Stop reading this document. Start coding.**

The only metric that matters right now:
**"How many humans met IRL because of Gathr this week?"**

If the answer is zero, nothing else matters.

Ship garbage that connects humans > Perfect code that connects nobody.

Your competition isn't other apps.
It's human inertia.

Break it.

---

*P.S. - If you're still reading this instead of coding, you're thumb twiddling. Close this. Open your IDE. Ship something in the next hour.*