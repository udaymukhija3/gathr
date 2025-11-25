import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, ScrollView, RefreshControl } from 'react-native';
import {
  Text,
  Button,
  TextInput,
  Avatar,
  Card,
  Chip,
  ActivityIndicator,
  Divider,
  IconButton,
} from 'react-native-paper';
import Toast from 'react-native-toast-message';
import { usersApi } from '../services/api';
import { UserProfile, TrustScore } from '../types';
import { featureFlags } from '../config/featureFlags';

const INTEREST_OPTIONS = [
  { key: 'SPORTS', label: 'Sports', icon: 'basketball' },
  { key: 'FOOD', label: 'Food', icon: 'food' },
  { key: 'ART', label: 'Art', icon: 'palette' },
  { key: 'MUSIC', label: 'Music', icon: 'music' },
];

interface ProfileScreenProps {
  navigation?: any;
  onSettingsPress?: () => void;
}

export const ProfileScreen: React.FC<ProfileScreenProps> = ({
  navigation,
  onSettingsPress,
}) => {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [trustScore, setTrustScore] = useState<TrustScore | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);

  // Edit form state
  const [editName, setEditName] = useState('');
  const [editBio, setEditBio] = useState('');
  const [editInterests, setEditInterests] = useState<string[]>([]);

  const loadProfile = useCallback(async () => {
    try {
      const [profileData, trustData] = await Promise.all([
        usersApi.getMe(),
        featureFlags.trustScore
          ? usersApi.getTrustScore()
          : Promise.resolve<TrustScore | null>(null),
      ]);
      setProfile(profileData);
      setTrustScore(trustData);
      setEditName(profileData.name || '');
      setEditBio(profileData.bio || '');
      setEditInterests(profileData.interests || []);
    } catch (error: any) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: error.message || 'Could not load profile',
      });
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const handleRefresh = () => {
    setRefreshing(true);
    loadProfile();
  };

  const handleSave = async () => {
    if (!editName.trim()) {
      Toast.show({
        type: 'error',
        text1: 'Name required',
        text2: 'Please enter your name',
      });
      return;
    }

    setSaving(true);
    try {
      const updatedProfile = await usersApi.updateMe({
        name: editName.trim(),
        bio: editBio.trim(),
      });

      // Update interests separately if changed
      if (JSON.stringify(editInterests.sort()) !== JSON.stringify((profile?.interests || []).sort())) {
        await usersApi.updateInterests(editInterests);
      }

      setProfile({
        ...updatedProfile,
        interests: editInterests,
      });
      setEditing(false);
      Toast.show({
        type: 'success',
        text1: 'Profile Updated',
      });
    } catch (error: any) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: error.message || 'Could not save profile',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleCancelEdit = () => {
    setEditName(profile?.name || '');
    setEditBio(profile?.bio || '');
    setEditInterests(profile?.interests || []);
    setEditing(false);
  };

  const toggleInterest = (interest: string) => {
    setEditInterests((prev) =>
      prev.includes(interest)
        ? prev.filter((i) => i !== interest)
        : [...prev, interest]
    );
  };

  const getTrustScoreColor = (score: number) => {
    if (score >= 80) return '#4CAF50';
    if (score >= 60) return '#FFC107';
    return '#F44336';
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" />
        <Text style={styles.loadingText}>Loading profile...</Text>
      </View>
    );
  }

  return (
    <ScrollView
      style={styles.container}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
      }
    >
      {/* Header with Avatar */}
      <View style={styles.header}>
        <Avatar.Text
          size={80}
          label={profile?.name?.charAt(0)?.toUpperCase() || '?'}
          style={styles.avatar}
        />
        {!editing && (
          <View style={styles.headerActions}>
            <IconButton
              icon="pencil"
              size={20}
              onPress={() => setEditing(true)}
            />
            <IconButton
              icon="cog"
              size={20}
              onPress={onSettingsPress || (() => navigation?.navigate('Settings'))}
            />
          </View>
        )}
      </View>

      {/* Name and Phone */}
      <View style={styles.section}>
        {editing ? (
          <TextInput
            label="Name"
            value={editName}
            onChangeText={setEditName}
            mode="outlined"
            style={styles.input}
          />
        ) : (
          <>
            <Text variant="headlineSmall" style={styles.name}>
              {profile?.name || 'No name set'}
            </Text>
            <Text variant="bodyMedium" style={styles.phone}>
              {profile?.phone}
            </Text>
          </>
        )}
      </View>

      {/* Bio */}
      <Card style={styles.card}>
        <Card.Content>
          <Text variant="titleMedium" style={styles.cardTitle}>
            About
          </Text>
          {editing ? (
            <TextInput
              label="Bio"
              value={editBio}
              onChangeText={setEditBio}
              mode="outlined"
              multiline
              numberOfLines={3}
              placeholder="Tell others a bit about yourself..."
              style={styles.input}
            />
          ) : (
            <Text variant="bodyMedium" style={styles.bio}>
              {profile?.bio || 'No bio yet. Tap edit to add one!'}
            </Text>
          )}
        </Card.Content>
      </Card>

      {/* Interests */}
      <Card style={styles.card}>
        <Card.Content>
          <Text variant="titleMedium" style={styles.cardTitle}>
            Interests
          </Text>
          <View style={styles.interestsContainer}>
            {INTEREST_OPTIONS.map((interest) => {
              const isSelected = editing
                ? editInterests.includes(interest.key)
                : profile?.interests?.includes(interest.key);
              return (
                <Chip
                  key={interest.key}
                  icon={interest.icon}
                  selected={isSelected}
                  onPress={editing ? () => toggleInterest(interest.key) : undefined}
                  style={[
                    styles.interestChip,
                    isSelected && styles.interestChipSelected,
                  ]}
                  textStyle={isSelected ? styles.interestChipTextSelected : undefined}
                >
                  {interest.label}
                </Chip>
              );
            })}
          </View>
        </Card.Content>
      </Card>

      {/* Trust Score */}
      {featureFlags.trustScore && trustScore && (
        <Card style={styles.card}>
          <Card.Content>
            <Text variant="titleMedium" style={styles.cardTitle}>
              Trust Score
            </Text>
            <View style={styles.trustScoreContainer}>
              <View
                style={[
                  styles.trustScoreBadge,
                  { backgroundColor: getTrustScoreColor(trustScore.score) },
                ]}
              >
                <Text style={styles.trustScoreValue}>{trustScore.score}</Text>
              </View>
              <View style={styles.trustScoreDetails}>
                <Text variant="bodySmall">
                  Activities attended: {trustScore.activitiesAttended}
                </Text>
                <Text variant="bodySmall">
                  Average rating: {trustScore.averageRating.toFixed(1)} / 5
                </Text>
                {trustScore.noShows > 0 && (
                  <Text variant="bodySmall" style={styles.noShowText}>
                    No-shows: {trustScore.noShows}
                  </Text>
                )}
              </View>
            </View>
          </Card.Content>
        </Card>
      )}

      {/* Home Hub */}
      {profile?.homeHubName && (
        <Card style={styles.card}>
          <Card.Content>
            <Text variant="titleMedium" style={styles.cardTitle}>
              Home Hub
            </Text>
            <Chip icon="map-marker" style={styles.hubChip}>
              {profile.homeHubName}
            </Chip>
          </Card.Content>
        </Card>
      )}

      {/* Stats */}
      <Card style={styles.card}>
        <Card.Content>
          <Text variant="titleMedium" style={styles.cardTitle}>
            Activity Stats
          </Text>
          <View style={styles.statsRow}>
            <View style={styles.statItem}>
              <Text variant="headlineMedium" style={styles.statValue}>
                {profile?.activitiesCount || 0}
              </Text>
              <Text variant="bodySmall" style={styles.statLabel}>
                Activities
              </Text>
            </View>
            <Divider style={styles.statDivider} />
            <View style={styles.statItem}>
              <Text variant="headlineMedium" style={styles.statValue}>
                {trustScore?.activitiesAttended || 0}
              </Text>
              <Text variant="bodySmall" style={styles.statLabel}>
                Attended
              </Text>
            </View>
          </View>
        </Card.Content>
      </Card>

      {/* Edit Actions */}
      {editing && (
        <View style={styles.editActions}>
          <Button
            mode="outlined"
            onPress={handleCancelEdit}
            style={styles.editButton}
            disabled={saving}
          >
            Cancel
          </Button>
          <Button
            mode="contained"
            onPress={handleSave}
            style={styles.editButton}
            loading={saving}
            disabled={saving}
          >
            Save
          </Button>
        </View>
      )}

      <View style={styles.bottomPadding} />
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5F5F5',
  },
  loadingText: {
    marginTop: 16,
    color: '#666',
  },
  header: {
    alignItems: 'center',
    paddingVertical: 24,
    backgroundColor: '#6200EE',
    position: 'relative',
  },
  avatar: {
    backgroundColor: '#fff',
  },
  headerActions: {
    position: 'absolute',
    top: 8,
    right: 8,
    flexDirection: 'row',
  },
  section: {
    alignItems: 'center',
    paddingVertical: 16,
    backgroundColor: '#fff',
    marginBottom: 8,
  },
  name: {
    fontWeight: 'bold',
    color: '#333',
  },
  phone: {
    color: '#666',
    marginTop: 4,
  },
  card: {
    marginHorizontal: 16,
    marginBottom: 12,
    backgroundColor: '#fff',
  },
  cardTitle: {
    fontWeight: '600',
    marginBottom: 12,
    color: '#333',
  },
  input: {
    backgroundColor: '#fff',
  },
  bio: {
    color: '#666',
    lineHeight: 22,
  },
  interestsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  interestChip: {
    backgroundColor: '#E8E8E8',
  },
  interestChipSelected: {
    backgroundColor: '#6200EE',
  },
  interestChipTextSelected: {
    color: '#fff',
  },
  trustScoreContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  trustScoreBadge: {
    width: 60,
    height: 60,
    borderRadius: 30,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  trustScoreValue: {
    color: '#fff',
    fontSize: 22,
    fontWeight: 'bold',
  },
  trustScoreDetails: {
    flex: 1,
  },
  noShowText: {
    color: '#F44336',
  },
  hubChip: {
    alignSelf: 'flex-start',
  },
  statsRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
  },
  statItem: {
    alignItems: 'center',
    flex: 1,
  },
  statValue: {
    fontWeight: 'bold',
    color: '#6200EE',
  },
  statLabel: {
    color: '#666',
  },
  statDivider: {
    width: 1,
    height: 40,
    backgroundColor: '#E0E0E0',
  },
  editActions: {
    flexDirection: 'row',
    justifyContent: 'center',
    paddingHorizontal: 16,
    gap: 12,
    marginTop: 8,
  },
  editButton: {
    flex: 1,
  },
  bottomPadding: {
    height: 32,
  },
});
