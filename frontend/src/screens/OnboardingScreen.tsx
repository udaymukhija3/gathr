import React, { useState, useEffect } from 'react';
import {
  View,
  StyleSheet,
  ScrollView,
  Dimensions,
  Animated,
  ActivityIndicator,
} from 'react-native';
import {
  Text,
  Button,
  TextInput,
  Chip,
  Card,
  ProgressBar,
  IconButton,
  Badge,
} from 'react-native-paper';
import Toast from 'react-native-toast-message';
import { useUser } from '../context/UserContext';
import { hubsApi, usersApi } from '../services/api';
import { Hub } from '../types';
import { getCurrentLocation, formatDistance, UserLocation } from '../services/location';

const { width } = Dimensions.get('window');

// Enhanced interest options with sub-categories and more options
const INTEREST_OPTIONS = [
  { key: 'SPORTS', label: 'Sports & Fitness', icon: 'basketball', emoji: '🏀', subInterests: ['Running', 'Basketball', 'Tennis', 'Yoga', 'Gym', 'Cricket', 'Swimming', 'Cycling'] },
  { key: 'FOOD', label: 'Food & Drinks', icon: 'food', emoji: '🍕', subInterests: ['Coffee', 'Fine Dining', 'Street Food', 'Wine Tasting', 'Brunch', 'Cooking Classes', 'Food Tours'] },
  { key: 'ART', label: 'Art & Culture', icon: 'palette', emoji: '🎨', subInterests: ['Museums', 'Photography', 'Crafts', 'Theatre', 'Dance', 'Art Walks', 'Galleries'] },
  { key: 'MUSIC', label: 'Music & Nightlife', icon: 'music', emoji: '🎵', subInterests: ['Concerts', 'Karaoke', 'DJ Nights', 'Live Music', 'Jazz Bars', 'Open Mics'] },
  { key: 'OUTDOOR', label: 'Outdoors & Nature', icon: 'tree', emoji: '🌲', subInterests: ['Hiking', 'Picnics', 'Camping', 'Bird Watching', 'Gardening', 'Beach'] },
  { key: 'GAMES', label: 'Games & Social', icon: 'gamepad-variant', emoji: '🎮', subInterests: ['Board Games', 'Video Games', 'Trivia', 'Poker Night', 'Escape Rooms', 'Puzzles'] },
  { key: 'LEARNING', label: 'Learning & Skills', icon: 'book-open-variant', emoji: '📚', subInterests: ['Book Clubs', 'Languages', 'Workshops', 'Tech Talks', 'Lectures', 'Study Groups'] },
  { key: 'WELLNESS', label: 'Wellness & Mindfulness', icon: 'meditation', emoji: '🧘', subInterests: ['Meditation', 'Spa Days', 'Sound Baths', 'Breathwork', 'Journaling', 'Retreats'] },
];

// Activity preferences
const ACTIVITY_PREFERENCES = [
  { key: 'GROUP_SIZE_SMALL', label: 'Small groups (2-4)', icon: 'account-group' },
  { key: 'GROUP_SIZE_LARGE', label: 'Large groups (5+)', icon: 'account-multiple' },
  { key: 'WEEKDAY_EVENING', label: 'Weekday evenings', icon: 'weather-night' },
  { key: 'WEEKENDS', label: 'Weekends', icon: 'calendar-weekend' },
];

interface OnboardingScreenProps {
  navigation?: any;
  onComplete: () => void;
}

export const OnboardingScreen: React.FC<OnboardingScreenProps> = ({
  navigation,
  onComplete,
}) => {
  const { user, refreshUser } = useUser();
  const [step, setStep] = useState(0);
  const [name, setName] = useState(user?.name || '');
  const [interests, setInterests] = useState<string[]>(user?.interests || []);
  const [preferences, setPreferences] = useState<string[]>([]);
  const [hubs, setHubs] = useState<Hub[]>([]);
  const [selectedHubId, setSelectedHubId] = useState<number | null>(user?.homeHubId || null);
  const [loading, setLoading] = useState(false);
  const [fadeAnim] = useState(new Animated.Value(1));

  // Location state
  const [userLocation, setUserLocation] = useState<UserLocation | null>(null);
  const [locationLoading, setLocationLoading] = useState(false);
  const [locationError, setLocationError] = useState<string | null>(null);

  const totalSteps = 4; // Added location step
  const progress = (step + 1) / totalSteps;

  useEffect(() => {
    // Try to get location early for a smoother experience
    detectLocation();
    loadHubs();
  }, []);

  const detectLocation = async () => {
    setLocationLoading(true);
    setLocationError(null);
    try {
      const location = await getCurrentLocation();
      if (location) {
        setUserLocation(location);
        // Load hubs sorted by distance
        await loadNearestHubs(location.latitude, location.longitude);
      } else {
        setLocationError('Could not detect location. You can select a hub manually.');
      }
    } catch (error) {
      console.error('Location detection error:', error);
      setLocationError('Location detection failed. Please select a hub manually.');
    } finally {
      setLocationLoading(false);
    }
  };

  const loadNearestHubs = async (latitude: number, longitude: number) => {
    try {
      const nearestHubs = await hubsApi.getNearest(latitude, longitude);
      if (nearestHubs.length > 0) {
        setHubs(nearestHubs);
        // Auto-select the nearest hub
        if (!selectedHubId) {
          setSelectedHubId(nearestHubs[0].id);
        }
      }
    } catch (error) {
      console.error('Error loading nearest hubs:', error);
      // Fall back to loading all hubs
      await loadHubs();
    }
  };

  const loadHubs = async () => {
    try {
      const hubsData = await hubsApi.getAll();
      setHubs(hubsData);
      if (hubsData.length > 0 && !selectedHubId) {
        setSelectedHubId(hubsData[0].id);
      }
    } catch (error) {
      console.error('Error loading hubs:', error);
    }
  };

  const animateTransition = (callback: () => void) => {
    Animated.sequence([
      Animated.timing(fadeAnim, {
        toValue: 0,
        duration: 150,
        useNativeDriver: true,
      }),
      Animated.timing(fadeAnim, {
        toValue: 1,
        duration: 150,
        useNativeDriver: true,
      }),
    ]).start();
    setTimeout(callback, 150);
  };

  const handleNext = () => {
    if (step === 0 && !name.trim()) {
      Toast.show({
        type: 'error',
        text1: 'Name required',
        text2: 'Please enter your name to continue',
      });
      return;
    }
    if (step === 1 && interests.length === 0) {
      Toast.show({
        type: 'error',
        text1: 'Select interests',
        text2: 'Pick at least one interest to continue',
      });
      return;
    }

    if (step < totalSteps - 1) {
      animateTransition(() => setStep(step + 1));
    } else {
      handleComplete();
    }
  };

  const handleBack = () => {
    if (step > 0) {
      animateTransition(() => setStep(step - 1));
    }
  };

  const toggleInterest = (interest: string) => {
    setInterests((prev) =>
      prev.includes(interest)
        ? prev.filter((i) => i !== interest)
        : [...prev, interest]
    );
  };

  const handleComplete = async () => {
    setLoading(true);
    try {
      // Update profile with name
      await usersApi.updateMe({ name: name.trim() });

      // Update interests
      if (interests.length > 0) {
        await usersApi.updateInterests(interests);
      }

      // Refresh user data
      await refreshUser();

      Toast.show({
        type: 'success',
        text1: 'Welcome to Gathr!',
        text2: "You're all set. Let's find some activities!",
      });

      onComplete();
    } catch (error: any) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: error.message || 'Could not save your profile',
      });
    } finally {
      setLoading(false);
    }
  };

  const renderStep0 = () => (
    <View style={styles.stepContainer}>
      <Text style={styles.emoji}>👋</Text>
      <Text variant="headlineMedium" style={styles.title}>
        Welcome to Gathr!
      </Text>
      <Text variant="bodyLarge" style={styles.subtitle}>
        Let's set up your profile so others can recognize you.
      </Text>

      <TextInput
        label="What should we call you?"
        value={name}
        onChangeText={setName}
        mode="outlined"
        style={styles.input}
        placeholder="Your name"
        autoFocus
        maxLength={30}
      />
      <Text variant="bodySmall" style={styles.hint}>
        This is how you'll appear to others in activities.
      </Text>
    </View>
  );

  const renderStep1 = () => (
    <View style={styles.stepContainer}>
      <Text style={styles.emoji}>✨</Text>
      <Text variant="headlineMedium" style={styles.title}>
        What are you into?
      </Text>
      <Text variant="bodyLarge" style={styles.subtitle}>
        Pick your interests to find activities you'll love.
      </Text>

      <View style={styles.interestsGrid}>
        {INTEREST_OPTIONS.map((interest) => {
          const isSelected = interests.includes(interest.key);
          return (
            <Chip
              key={interest.key}
              icon={interest.icon}
              selected={isSelected}
              onPress={() => toggleInterest(interest.key)}
              style={[
                styles.interestChip,
                isSelected && styles.interestChipSelected,
              ]}
              textStyle={[
                styles.interestChipText,
                isSelected && styles.interestChipTextSelected,
              ]}
            >
              {interest.emoji} {interest.label}
            </Chip>
          );
        })}
      </View>

      <Text variant="bodySmall" style={styles.hint}>
        You can change these later in your profile.
      </Text>
    </View>
  );

  const renderStep2 = () => (
    <View style={styles.stepContainer}>
      <Text style={styles.emoji}>📍</Text>
      <Text variant="headlineMedium" style={styles.title}>
        Choose your hub
      </Text>
      <Text variant="bodyLarge" style={styles.subtitle}>
        Hubs are neighborhoods where activities happen. Pick your home base.
      </Text>

      {/* Location Detection */}
      <View style={styles.locationSection}>
        {locationLoading ? (
          <View style={styles.locationStatus}>
            <ActivityIndicator size="small" color="#6200EE" />
            <Text variant="bodyMedium" style={styles.locationText}>
              Detecting your location...
            </Text>
          </View>
        ) : userLocation ? (
          <View style={styles.locationStatus}>
            <IconButton icon="map-marker-check" iconColor="#4CAF50" size={20} />
            <Text variant="bodyMedium" style={styles.locationText}>
              Location detected! Showing nearest hubs.
            </Text>
          </View>
        ) : (
          <View style={styles.locationStatus}>
            <Button
              mode="outlined"
              icon="crosshairs-gps"
              onPress={detectLocation}
              compact
            >
              Detect My Location
            </Button>
            {locationError && (
              <Text variant="bodySmall" style={styles.locationError}>
                {locationError}
              </Text>
            )}
          </View>
        )}
      </View>

      <View style={styles.hubsContainer}>
        {hubs.map((hub) => {
          const isSelected = selectedHubId === hub.id;
          return (
            <Card
              key={hub.id}
              style={[styles.hubCard, isSelected && styles.hubCardSelected]}
              onPress={() => setSelectedHubId(hub.id)}
            >
              <Card.Content style={styles.hubCardContent}>
                <View style={styles.hubHeader}>
                  <View style={styles.hubTitleRow}>
                    <Text
                      variant="titleMedium"
                      style={[
                        styles.hubName,
                        isSelected && styles.hubNameSelected,
                      ]}
                    >
                      {hub.name}
                    </Text>
                    {hub.distance !== undefined && (
                      <Badge style={styles.distanceBadge}>
                        {formatDistance(hub.distance)}
                      </Badge>
                    )}
                  </View>
                  {isSelected && (
                    <IconButton
                      icon="check-circle"
                      iconColor="#6200EE"
                      size={24}
                    />
                  )}
                </View>
                <Text variant="bodySmall" style={styles.hubArea}>
                  {hub.area}
                </Text>
                {hub.description && (
                  <Text variant="bodySmall" style={styles.hubDescription}>
                    {hub.description}
                  </Text>
                )}
              </Card.Content>
            </Card>
          );
        })}
      </View>

      <Text variant="bodySmall" style={styles.hint}>
        You can explore all hubs once you're in the app.
      </Text>
    </View>
  );

  const togglePreference = (pref: string) => {
    setPreferences((prev) =>
      prev.includes(pref)
        ? prev.filter((p) => p !== pref)
        : [...prev, pref]
    );
  };

  const renderStep3 = () => (
    <View style={styles.stepContainer}>
      <Text style={styles.emoji}>🎯</Text>
      <Text variant="headlineMedium" style={styles.title}>
        Almost there!
      </Text>
      <Text variant="bodyLarge" style={styles.subtitle}>
        Help us personalize your experience. When do you prefer to hang out?
      </Text>

      <View style={styles.preferencesContainer}>
        {ACTIVITY_PREFERENCES.map((pref) => {
          const isSelected = preferences.includes(pref.key);
          return (
            <Chip
              key={pref.key}
              icon={pref.icon}
              selected={isSelected}
              onPress={() => togglePreference(pref.key)}
              style={[
                styles.preferenceChip,
                isSelected && styles.preferenceChipSelected,
              ]}
              textStyle={[
                styles.preferenceChipText,
                isSelected && styles.preferenceChipTextSelected,
              ]}
            >
              {pref.label}
            </Chip>
          );
        })}
      </View>

      <Text variant="bodySmall" style={styles.hint}>
        This helps us recommend activities that fit your schedule.
      </Text>

      <View style={styles.readySection}>
        <Text variant="titleMedium" style={styles.readyTitle}>
          You're all set!
        </Text>
        <Text variant="bodyMedium" style={styles.readyText}>
          Tap "Let's Go!" to start discovering activities near you.
        </Text>
      </View>
    </View>
  );

  const renderCurrentStep = () => {
    switch (step) {
      case 0:
        return renderStep0();
      case 1:
        return renderStep1();
      case 2:
        return renderStep2();
      case 3:
        return renderStep3();
      default:
        return null;
    }
  };

  return (
    <View style={styles.container}>
      {/* Progress Bar */}
      <View style={styles.progressContainer}>
        <ProgressBar
          progress={progress}
          color="#6200EE"
          style={styles.progressBar}
        />
        <Text variant="bodySmall" style={styles.progressText}>
          Step {step + 1} of {totalSteps}
        </Text>
      </View>

      {/* Content */}
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        <Animated.View style={{ opacity: fadeAnim }}>
          {renderCurrentStep()}
        </Animated.View>
      </ScrollView>

      {/* Navigation Buttons */}
      <View style={styles.footer}>
        {step > 0 ? (
          <Button mode="outlined" onPress={handleBack} style={styles.backButton}>
            Back
          </Button>
        ) : (
          <View style={styles.backButton} />
        )}

        <Button
          mode="contained"
          onPress={handleNext}
          loading={loading}
          disabled={loading}
          style={styles.nextButton}
        >
          {step === totalSteps - 1 ? "Let's Go!" : 'Continue'}
        </Button>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  progressContainer: {
    paddingHorizontal: 24,
    paddingTop: 16,
    paddingBottom: 8,
  },
  progressBar: {
    height: 6,
    borderRadius: 3,
  },
  progressText: {
    marginTop: 8,
    color: '#666',
    textAlign: 'center',
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    paddingHorizontal: 24,
    paddingTop: 24,
    paddingBottom: 100,
  },
  stepContainer: {
    alignItems: 'center',
  },
  emoji: {
    fontSize: 64,
    marginBottom: 16,
  },
  title: {
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 8,
    color: '#333',
  },
  subtitle: {
    textAlign: 'center',
    color: '#666',
    marginBottom: 32,
    paddingHorizontal: 16,
  },
  input: {
    width: '100%',
    marginBottom: 8,
    backgroundColor: '#fff',
  },
  hint: {
    color: '#999',
    textAlign: 'center',
    marginTop: 8,
  },
  interestsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 12,
    marginBottom: 16,
  },
  interestChip: {
    paddingVertical: 8,
    paddingHorizontal: 4,
    backgroundColor: '#F5F5F5',
  },
  interestChipSelected: {
    backgroundColor: '#6200EE',
  },
  interestChipText: {
    fontSize: 15,
    color: '#333',
  },
  interestChipTextSelected: {
    color: '#fff',
  },
  hubsContainer: {
    width: '100%',
    gap: 12,
  },
  hubCard: {
    backgroundColor: '#F9F9F9',
    borderWidth: 2,
    borderColor: 'transparent',
  },
  hubCardSelected: {
    backgroundColor: '#F3E5F5',
    borderColor: '#6200EE',
  },
  hubCardContent: {
    paddingVertical: 12,
  },
  hubHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  hubName: {
    fontWeight: '600',
    color: '#333',
  },
  hubNameSelected: {
    color: '#6200EE',
  },
  hubArea: {
    color: '#666',
    marginTop: 2,
  },
  hubDescription: {
    color: '#888',
    marginTop: 4,
  },
  hubTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    flex: 1,
  },
  distanceBadge: {
    backgroundColor: '#E8F5E9',
    color: '#2E7D32',
    fontSize: 12,
  },
  locationSection: {
    width: '100%',
    marginBottom: 16,
    alignItems: 'center',
  },
  locationStatus: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    flexWrap: 'wrap',
    gap: 8,
  },
  locationText: {
    color: '#666',
  },
  locationError: {
    color: '#E53935',
    marginTop: 4,
    textAlign: 'center',
    width: '100%',
  },
  preferencesContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 12,
    marginBottom: 24,
  },
  preferenceChip: {
    paddingVertical: 8,
    paddingHorizontal: 4,
    backgroundColor: '#F5F5F5',
  },
  preferenceChipSelected: {
    backgroundColor: '#6200EE',
  },
  preferenceChipText: {
    fontSize: 14,
    color: '#333',
  },
  preferenceChipTextSelected: {
    color: '#fff',
  },
  readySection: {
    marginTop: 24,
    padding: 20,
    backgroundColor: '#F3E5F5',
    borderRadius: 12,
    alignItems: 'center',
    width: '100%',
  },
  readyTitle: {
    fontWeight: '600',
    color: '#6200EE',
    marginBottom: 8,
  },
  readyText: {
    color: '#666',
    textAlign: 'center',
  },
  footer: {
    flexDirection: 'row',
    paddingHorizontal: 24,
    paddingVertical: 16,
    borderTopWidth: 1,
    borderTopColor: '#E0E0E0',
    backgroundColor: '#fff',
  },
  backButton: {
    flex: 1,
    marginRight: 8,
  },
  nextButton: {
    flex: 2,
  },
});
