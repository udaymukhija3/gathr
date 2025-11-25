import React, { useState, useEffect, useCallback, useRef } from 'react';
import { View, StyleSheet, Dimensions, Platform } from 'react-native';
import {
  Text,
  Card,
  Chip,
  Button,
  ActivityIndicator,
  IconButton,
  FAB,
} from 'react-native-paper';
import MapView, { Marker, Callout, Region, PROVIDER_GOOGLE } from 'react-native-maps';
import Toast from 'react-native-toast-message';
import { format, parseISO } from 'date-fns';
import { Activity, ActivityCategory, Hub } from '../types';
import { activitiesApi, hubsApi } from '../services/api';
import { getCurrentLocation } from '../services/location';
import { MutualBadge } from '../components/MutualBadge';

const { width, height } = Dimensions.get('window');
const ASPECT_RATIO = width / height;
const LATITUDE_DELTA = 0.0922;
const LONGITUDE_DELTA = LATITUDE_DELTA * ASPECT_RATIO;

const CATEGORY_COLORS: Record<ActivityCategory, string> = {
  SPORTS: '#4CAF50',
  FOOD: '#FF9800',
  ART: '#9C27B0',
  MUSIC: '#2196F3',
  OUTDOOR: '#00BCD4',
  GAMES: '#FF5722',
  LEARNING: '#3F51B5',
  WELLNESS: '#E91E63',
};

const CATEGORY_MARKERS: Record<ActivityCategory, string> = {
  SPORTS: '🏀',
  FOOD: '🍽️',
  ART: '🎨',
  MUSIC: '🎵',
  OUTDOOR: '🌳',
  GAMES: '🎮',
  LEARNING: '📚',
  WELLNESS: '🧘',
};

interface MapViewScreenProps {
  navigation?: any;
  onActivitySelect?: (activityId: number) => void;
}

export const MapViewScreen: React.FC<MapViewScreenProps> = ({
  navigation,
  onActivitySelect,
}) => {
  const mapRef = useRef<MapView>(null);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [hubs, setHubs] = useState<Hub[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedActivity, setSelectedActivity] = useState<Activity | null>(null);
  const [userLocation, setUserLocation] = useState<{ latitude: number; longitude: number } | null>(null);
  const [region, setRegion] = useState<Region>({
    latitude: 28.4595, // Default to Gurgaon
    longitude: 77.0266,
    latitudeDelta: LATITUDE_DELTA,
    longitudeDelta: LONGITUDE_DELTA,
  });
  const [selectedCategory, setSelectedCategory] = useState<ActivityCategory | null>(null);
  const [showHubs, setShowHubs] = useState(true);
  const [radiusKm, setRadiusKm] = useState(10);

  useEffect(() => {
    initializeMap();
  }, []);

  const initializeMap = async () => {
    setLoading(true);
    try {
      // Get user location
      const location = await getCurrentLocation();
      if (location) {
        setUserLocation(location);
        setRegion({
          latitude: location.latitude,
          longitude: location.longitude,
          latitudeDelta: LATITUDE_DELTA,
          longitudeDelta: LONGITUDE_DELTA,
        });
      }

      // Load data in parallel
      await Promise.all([loadActivities(location), loadHubs(location)]);
    } catch (error) {
      console.error('Error initializing map:', error);
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Could not load map data',
      });
    } finally {
      setLoading(false);
    }
  };

  const loadActivities = async (location?: { latitude: number; longitude: number } | null) => {
    try {
      const loc = location || userLocation;
      if (!loc) {
        // Fallback: load from all hubs
        const hubList = await hubsApi.getAll();
        if (hubList.length > 0) {
          const allActivities = await activitiesApi.getByHub(hubList[0].id);
          setActivities(allActivities);
        }
        return;
      }

      const nearbyActivities = await activitiesApi.getNearby(
        loc.latitude,
        loc.longitude,
        radiusKm
      );
      setActivities(nearbyActivities);
    } catch (error) {
      console.error('Error loading activities:', error);
    }
  };

  const loadHubs = async (location?: { latitude: number; longitude: number } | null) => {
    try {
      const loc = location || userLocation;
      if (loc) {
        const nearestHubs = await hubsApi.getNearest(loc.latitude, loc.longitude, 10);
        setHubs(nearestHubs);
      } else {
        const allHubs = await hubsApi.getAll();
        setHubs(allHubs);
      }
    } catch (error) {
      console.error('Error loading hubs:', error);
    }
  };

  const handleRefresh = useCallback(() => {
    loadActivities(userLocation);
  }, [userLocation, radiusKm]);

  const handleCenterOnUser = () => {
    if (userLocation && mapRef.current) {
      mapRef.current.animateToRegion({
        ...userLocation,
        latitudeDelta: LATITUDE_DELTA,
        longitudeDelta: LONGITUDE_DELTA,
      });
    }
  };

  const handleActivityPress = (activity: Activity) => {
    setSelectedActivity(activity);
    if (activity.latitude && activity.longitude && mapRef.current) {
      mapRef.current.animateToRegion({
        latitude: activity.latitude,
        longitude: activity.longitude,
        latitudeDelta: 0.01,
        longitudeDelta: 0.01 * ASPECT_RATIO,
      });
    }
  };

  const handleViewDetails = (activity: Activity) => {
    if (onActivitySelect) {
      onActivitySelect(activity.id);
    } else {
      navigation?.navigate('ActivityDetail', { activityId: activity.id });
    }
  };

  const filteredActivities = selectedCategory
    ? activities.filter((a) => a.category === selectedCategory)
    : activities;

  const formatTime = (dateString: string) => {
    try {
      return format(parseISO(dateString), 'h:mm a');
    } catch {
      return '';
    }
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" />
        <Text style={styles.loadingText}>Loading map...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <MapView
        ref={mapRef}
        style={styles.map}
        provider={Platform.OS === 'android' ? PROVIDER_GOOGLE : undefined}
        initialRegion={region}
        showsUserLocation
        showsMyLocationButton={false}
        showsCompass
        onRegionChangeComplete={setRegion}
      >
        {/* Activity Markers */}
        {filteredActivities.map((activity) => {
          if (!activity.latitude || !activity.longitude) return null;

          const isSelected = selectedActivity?.id === activity.id;
          const markerColor = CATEGORY_COLORS[activity.category] || '#6200EE';

          return (
            <Marker
              key={`activity-${activity.id}`}
              coordinate={{
                latitude: activity.latitude,
                longitude: activity.longitude,
              }}
              pinColor={markerColor}
              onPress={() => handleActivityPress(activity)}
            >
              <View style={[
                styles.customMarker,
                { backgroundColor: markerColor },
                isSelected && styles.selectedMarker,
              ]}>
                <Text style={styles.markerEmoji}>
                  {CATEGORY_MARKERS[activity.category] || '📍'}
                </Text>
              </View>
              <Callout onPress={() => handleViewDetails(activity)}>
                <View style={styles.callout}>
                  <Text style={styles.calloutTitle}>{activity.title}</Text>
                  <Text style={styles.calloutTime}>
                    {formatTime(activity.startTime)}
                  </Text>
                  {activity.mutualsCount && activity.mutualsCount > 0 && (
                    <Text style={styles.calloutMutuals}>
                      {activity.mutualsCount} mutual{activity.mutualsCount > 1 ? 's' : ''} going
                    </Text>
                  )}
                  <Text style={styles.calloutAction}>Tap to view details</Text>
                </View>
              </Callout>
            </Marker>
          );
        })}

        {/* Hub Markers */}
        {showHubs && hubs.map((hub) => {
          if (!hub.latitude || !hub.longitude) return null;

          return (
            <Marker
              key={`hub-${hub.id}`}
              coordinate={{
                latitude: hub.latitude,
                longitude: hub.longitude,
              }}
              opacity={0.7}
            >
              <View style={styles.hubMarker}>
                <Text style={styles.hubMarkerText}>🏢</Text>
              </View>
              <Callout>
                <View style={styles.callout}>
                  <Text style={styles.calloutTitle}>{hub.name}</Text>
                  <Text style={styles.calloutSubtitle}>{hub.area}</Text>
                </View>
              </Callout>
            </Marker>
          );
        })}
      </MapView>

      {/* Category Filter */}
      <View style={styles.filterContainer}>
        <Chip
          selected={selectedCategory === null}
          onPress={() => setSelectedCategory(null)}
          style={styles.filterChip}
        >
          All
        </Chip>
        {Object.keys(CATEGORY_COLORS).slice(0, 4).map((category) => (
          <Chip
            key={category}
            selected={selectedCategory === category}
            onPress={() =>
              setSelectedCategory(
                selectedCategory === category ? null : (category as ActivityCategory)
              )
            }
            style={[
              styles.filterChip,
              selectedCategory === category && {
                backgroundColor: CATEGORY_COLORS[category as ActivityCategory],
              },
            ]}
            textStyle={selectedCategory === category ? { color: 'white' } : undefined}
          >
            {CATEGORY_MARKERS[category as ActivityCategory]} {category}
          </Chip>
        ))}
      </View>

      {/* Center on user button */}
      <FAB
        icon="crosshairs-gps"
        style={styles.locationFab}
        onPress={handleCenterOnUser}
        small
      />

      {/* Refresh button */}
      <FAB
        icon="refresh"
        style={styles.refreshFab}
        onPress={handleRefresh}
        small
      />

      {/* Selected Activity Card */}
      {selectedActivity && (
        <Card style={styles.selectedCard}>
          <Card.Content>
            <View style={styles.selectedCardHeader}>
              <View style={styles.selectedCardInfo}>
                <Chip
                  style={{
                    backgroundColor: CATEGORY_COLORS[selectedActivity.category] + '30',
                  }}
                  textStyle={{
                    color: CATEGORY_COLORS[selectedActivity.category],
                    fontSize: 11,
                  }}
                >
                  {selectedActivity.category}
                </Chip>
                <Text variant="titleMedium" style={styles.selectedCardTitle}>
                  {selectedActivity.title}
                </Text>
                <Text variant="bodySmall" style={styles.selectedCardTime}>
                  {formatTime(selectedActivity.startTime)} • {selectedActivity.locationName || selectedActivity.hubName}
                </Text>
              </View>
              <IconButton
                icon="close"
                size={20}
                onPress={() => setSelectedActivity(null)}
              />
            </View>

            <View style={styles.selectedCardStats}>
              <Text variant="bodySmall">
                👥 {selectedActivity.peopleCount || selectedActivity.totalParticipants || 0} going
              </Text>
              {selectedActivity.mutualsCount && selectedActivity.mutualsCount > 0 && (
                <MutualBadge count={selectedActivity.mutualsCount} />
              )}
              {selectedActivity.distanceKm && (
                <Text variant="bodySmall" style={styles.distance}>
                  📍 {selectedActivity.distanceKm.toFixed(1)} km away
                </Text>
              )}
            </View>

            <Button
              mode="contained"
              onPress={() => handleViewDetails(selectedActivity)}
              style={styles.viewDetailsButton}
            >
              View Details
            </Button>
          </Card.Content>
        </Card>
      )}

      {/* Activity count indicator */}
      <View style={styles.countBadge}>
        <Text style={styles.countText}>
          {filteredActivities.length} activities nearby
        </Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
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
  map: {
    flex: 1,
  },
  customMarker: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: 'white',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  selectedMarker: {
    width: 44,
    height: 44,
    borderRadius: 22,
    borderWidth: 3,
  },
  markerEmoji: {
    fontSize: 18,
  },
  hubMarker: {
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: '#E0E0E0',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#9E9E9E',
  },
  hubMarkerText: {
    fontSize: 14,
  },
  callout: {
    padding: 8,
    minWidth: 150,
    maxWidth: 200,
  },
  calloutTitle: {
    fontWeight: 'bold',
    fontSize: 14,
    marginBottom: 4,
  },
  calloutSubtitle: {
    fontSize: 12,
    color: '#666',
  },
  calloutTime: {
    fontSize: 12,
    color: '#666',
  },
  calloutMutuals: {
    fontSize: 11,
    color: '#6200EE',
    marginTop: 2,
  },
  calloutAction: {
    fontSize: 10,
    color: '#999',
    marginTop: 4,
    fontStyle: 'italic',
  },
  filterContainer: {
    position: 'absolute',
    top: 16,
    left: 8,
    right: 8,
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
  },
  filterChip: {
    height: 32,
  },
  locationFab: {
    position: 'absolute',
    right: 16,
    bottom: 200,
    backgroundColor: 'white',
  },
  refreshFab: {
    position: 'absolute',
    right: 16,
    bottom: 260,
    backgroundColor: 'white',
  },
  selectedCard: {
    position: 'absolute',
    bottom: 16,
    left: 16,
    right: 16,
    backgroundColor: 'white',
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
  },
  selectedCardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  selectedCardInfo: {
    flex: 1,
  },
  selectedCardTitle: {
    fontWeight: '600',
    marginTop: 8,
  },
  selectedCardTime: {
    color: '#666',
    marginTop: 4,
  },
  selectedCardStats: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 12,
    alignItems: 'center',
  },
  distance: {
    color: '#666',
  },
  viewDetailsButton: {
    marginTop: 12,
  },
  countBadge: {
    position: 'absolute',
    bottom: 16,
    alignSelf: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
  },
  countText: {
    color: 'white',
    fontSize: 12,
    fontWeight: '500',
  },
});
