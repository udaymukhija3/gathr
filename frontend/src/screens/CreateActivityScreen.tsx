import React, { useEffect, useState } from 'react';
import { View, StyleSheet, ScrollView, TouchableOpacity, Modal, Platform } from 'react-native';
import { TextInput, Button, Text, Card, SegmentedButtons, Checkbox, Portal, IconButton, Chip } from 'react-native-paper';
import { format, addHours, addDays, setHours, setMinutes, startOfDay } from 'date-fns';
import Toast from 'react-native-toast-message';
import { Hub, ActivityCategory, CreateActivityRequest, ActivityTemplate } from '../types';
import { activitiesApi, hubsApi } from '../services/api';

// Simple Date/Time Picker Modal Component (no external dependencies)
interface DateTimePickerModalProps {
  value: Date;
  mode: 'date' | 'time';
  onConfirm: (date: Date) => void;
  onCancel: () => void;
  title: string;
}

const DateTimePickerModal: React.FC<DateTimePickerModalProps> = ({
  value,
  mode,
  onConfirm,
  onCancel,
  title,
}) => {
  const [selectedDate, setSelectedDate] = useState(value);

  // Generate date options (today + next 6 days)
  const dateOptions = Array.from({ length: 7 }, (_, i) => {
    const date = addDays(startOfDay(new Date()), i);
    return {
      date,
      label: i === 0 ? 'Today' : i === 1 ? 'Tomorrow' : format(date, 'EEE, MMM d'),
    };
  });

  // Generate time options (every 30 minutes from 6am to 11pm)
  const timeOptions = Array.from({ length: 34 }, (_, i) => {
    const hour = Math.floor(i / 2) + 6;
    const minute = (i % 2) * 30;
    return {
      hour,
      minute,
      label: format(setMinutes(setHours(new Date(), hour), minute), 'h:mm a'),
    };
  });

  const handleDateSelect = (date: Date) => {
    const newDate = new Date(selectedDate);
    newDate.setFullYear(date.getFullYear(), date.getMonth(), date.getDate());
    setSelectedDate(newDate);
  };

  const handleTimeSelect = (hour: number, minute: number) => {
    const newDate = new Date(selectedDate);
    newDate.setHours(hour, minute, 0, 0);
    setSelectedDate(newDate);
  };

  return (
    <Modal
      visible={true}
      transparent
      animationType="slide"
      onRequestClose={onCancel}
    >
      <View style={pickerStyles.overlay}>
        <TouchableOpacity style={pickerStyles.backdrop} onPress={onCancel} />
        <View style={pickerStyles.container}>
          <View style={pickerStyles.header}>
            <Button onPress={onCancel}>Cancel</Button>
            <Text variant="titleMedium" style={pickerStyles.title}>{title}</Text>
            <Button onPress={() => onConfirm(selectedDate)}>Done</Button>
          </View>

          {mode === 'date' ? (
            <View style={pickerStyles.optionsContainer}>
              <Text variant="bodyMedium" style={pickerStyles.sectionLabel}>Select Date</Text>
              <View style={pickerStyles.optionsGrid}>
                {dateOptions.map((opt, idx) => {
                  const isSelected = opt.date.toDateString() === selectedDate.toDateString();
                  return (
                    <Chip
                      key={idx}
                      selected={isSelected}
                      onPress={() => handleDateSelect(opt.date)}
                      style={[pickerStyles.chip, isSelected && pickerStyles.chipSelected]}
                      textStyle={isSelected ? pickerStyles.chipTextSelected : undefined}
                    >
                      {opt.label}
                    </Chip>
                  );
                })}
              </View>
            </View>
          ) : (
            <ScrollView style={pickerStyles.scrollContainer}>
              <Text variant="bodyMedium" style={pickerStyles.sectionLabel}>Select Time</Text>
              <View style={pickerStyles.optionsGrid}>
                {timeOptions.map((opt, idx) => {
                  const isSelected = selectedDate.getHours() === opt.hour && selectedDate.getMinutes() === opt.minute;
                  return (
                    <Chip
                      key={idx}
                      selected={isSelected}
                      onPress={() => handleTimeSelect(opt.hour, opt.minute)}
                      style={[pickerStyles.chip, isSelected && pickerStyles.chipSelected]}
                      textStyle={isSelected ? pickerStyles.chipTextSelected : undefined}
                    >
                      {opt.label}
                    </Chip>
                  );
                })}
              </View>
            </ScrollView>
          )}
        </View>
      </View>
    </Modal>
  );
};

const pickerStyles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  backdrop: {
    flex: 1,
  },
  container: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    paddingBottom: 20,
    maxHeight: '60%',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  title: {
    fontWeight: '600',
  },
  optionsContainer: {
    padding: 16,
  },
  scrollContainer: {
    padding: 16,
    maxHeight: 300,
  },
  sectionLabel: {
    fontWeight: '600',
    marginBottom: 12,
    color: '#333',
  },
  optionsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  chip: {
    marginBottom: 4,
  },
  chipSelected: {
    backgroundColor: '#6200EE',
  },
  chipTextSelected: {
    color: '#fff',
  },
});

interface CreateActivityScreenProps {
  template?: ActivityTemplate;
  onSubmit: (activityId: number) => void;
  onCancel: () => void;
}

type LocationMode = 'curated' | 'custom';

interface LocationSuggestion {
  id: string;
  name: string;
  address?: string;
  latitude: number;
  longitude: number;
}

export const CreateActivityScreen: React.FC<CreateActivityScreenProps> = ({
  template,
  onSubmit,
  onCancel,
}) => {
  const [hubs, setHubs] = useState<Hub[]>([]);
  const [title, setTitle] = useState(template?.title || '');
  const [category, setCategory] = useState<ActivityCategory>(template?.category || 'FOOD');
  const [hubId, setHubId] = useState<number | null>(null);
  const [startTime, setStartTime] = useState(new Date());
  const [endTime, setEndTime] = useState(
    template?.durationHours
      ? new Date(Date.now() + template.durationHours * 60 * 60 * 1000)
      : new Date(Date.now() + 2 * 60 * 60 * 1000)
  );
  const [description, setDescription] = useState(template?.description || '');
  const [inviteOnly, setInviteOnly] = useState(template?.isInviteOnly || false);
  const [maxMembers, setMaxMembers] = useState(template?.maxMembers || 4);
  const [loading, setLoading] = useState(false);

  const [locationMode, setLocationMode] = useState<LocationMode>('curated');
  const [locationQuery, setLocationQuery] = useState('');
  const [locationSuggestions, setLocationSuggestions] = useState<LocationSuggestion[]>([]);
  const [selectedSuggestion, setSelectedSuggestion] = useState<LocationSuggestion | null>(null);
  const [locationAddress, setLocationAddress] = useState('');
  const [latitudeInput, setLatitudeInput] = useState('');
  const [longitudeInput, setLongitudeInput] = useState('');
  const [isSearchingLocation, setIsSearchingLocation] = useState(false);
  const mapboxToken = process.env.EXPO_PUBLIC_MAPBOX_TOKEN;

  // Date/Time picker states
  const [showStartPicker, setShowStartPicker] = useState(false);
  const [showEndPicker, setShowEndPicker] = useState(false);
  const [pickerMode, setPickerMode] = useState<'date' | 'time'>('date');
  const [tempDate, setTempDate] = useState<Date>(new Date());

  useEffect(() => {
    loadHubs();
  }, []);

  useEffect(() => {
    if (locationMode === 'curated') {
      setLocationQuery('');
      setLocationAddress('');
      setLatitudeInput('');
      setLongitudeInput('');
      setSelectedSuggestion(null);
      return;
    }
  }, [locationMode]);

  useEffect(() => {
    if (!mapboxToken || locationMode !== 'custom' || locationQuery.trim().length < 3) {
      setLocationSuggestions([]);
      return;
    }
    const controller = new AbortController();
    const timeout = setTimeout(async () => {
      try {
        setIsSearchingLocation(true);
        const response = await fetch(
          `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(locationQuery)}.json?access_token=${mapboxToken}&limit=5`,
          { signal: controller.signal }
        );
        if (!response.ok) {
          throw new Error('Failed to search locations');
        }
        const data = await response.json();
        const suggestions: LocationSuggestion[] = (data.features || []).map((feature: any) => ({
          id: feature.id,
          name: feature.text,
          address: feature.place_name,
          latitude: feature.center[1],
          longitude: feature.center[0],
        }));
        setLocationSuggestions(suggestions);
      } catch (error) {
        if ((error as any).name !== 'AbortError') {
          console.error('Location search error:', error);
        }
      } finally {
        setIsSearchingLocation(false);
      }
    }, 400);

    return () => {
      controller.abort();
      clearTimeout(timeout);
    };
  }, [locationQuery, locationMode, mapboxToken]);

  const loadHubs = async () => {
    try {
      const hubsData = await hubsApi.getAll();
      setHubs(hubsData);
      if (hubsData.length > 0) {
        setHubId(hubsData[0].id);
      }
    } catch (error) {
      console.error('Error loading hubs:', error);
    }
  };

  const handleSuggestionSelect = (suggestion: LocationSuggestion) => {
    setSelectedSuggestion(suggestion);
    setLocationQuery(suggestion.name);
    setLocationAddress(suggestion.address || '');
    setLatitudeInput(suggestion.latitude.toString());
    setLongitudeInput(suggestion.longitude.toString());
    setLocationSuggestions([]);
  };

  const handleSubmit = async () => {
    if (!title.trim()) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Please enter a title',
      });
      return;
    }
    if (endTime <= startTime) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'End time must be after start time',
      });
      return;
    }

    setLoading(true);
    try {
      const request: CreateActivityRequest = {
        title: title.trim(),
        category,
        startTime: startTime.toISOString(),
        endTime: endTime.toISOString(),
        description: description.trim() || undefined,
        isInviteOnly: inviteOnly,
        maxMembers,
      };
      if (locationMode === 'curated') {
        if (!hubId) {
          Toast.show({
            type: 'error',
            text1: 'Error',
            text2: 'Please select a hub',
          });
          setLoading(false);
          return;
        }
        request.hubId = hubId;
      } else {
        const lat = parseFloat(latitudeInput);
        const lng = parseFloat(longitudeInput);
        if (!locationQuery.trim() || Number.isNaN(lat) || Number.isNaN(lng)) {
          Toast.show({
            type: 'error',
            text1: 'Error',
            text2: 'Select a location and coordinates',
          });
          setLoading(false);
          return;
        }
        request.placeName = locationQuery.trim();
        request.placeAddress = locationAddress.trim() || undefined;
        request.latitude = lat;
        request.longitude = lng;
        request.placeId = selectedSuggestion?.id;
      }
      const activity = await activitiesApi.create(request);
      Toast.show({
        type: 'success',
        text1: 'Success',
        text2: 'Activity created!',
      });
      onSubmit(activity.id);
    } catch (error) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Failed to create activity',
      });
    } finally {
      setLoading(false);
    }
  };

  const categoryButtons = [
    { value: 'FOOD', label: 'Food' },
    { value: 'SPORTS', label: 'Sports' },
    { value: 'ART', label: 'Art' },
    { value: 'MUSIC', label: 'Music' },
  ];

  return (
    <ScrollView style={styles.container}>
      <Card style={styles.card}>
        <Card.Content>
          <Text variant="headlineSmall" style={styles.title}>
            Create Your Plan
          </Text>
        </Card.Content>
      </Card>

      <Card style={styles.card}>
        <Card.Content>
          <TextInput
            label="Title"
            value={title}
            onChangeText={setTitle}
            mode="outlined"
            style={styles.input}
            placeholder="e.g., Coffee at Galleria"
          />

          <Text variant="bodyMedium" style={styles.label}>
            Category
          </Text>
          <SegmentedButtons
            value={category}
            onValueChange={(value) => setCategory(value as ActivityCategory)}
            buttons={categoryButtons}
            style={styles.segmentedButtons}
          />

          <Text variant="bodyMedium" style={styles.label}>
            Location
          </Text>
          <SegmentedButtons
            value={locationMode}
            onValueChange={(value) => setLocationMode(value as LocationMode)}
            buttons={[
              { value: 'curated', label: 'Curated Hubs' },
              { value: 'custom', label: 'Pick Any Place' },
            ]}
            style={styles.segmentedButtons}
          />

          {locationMode === 'curated' ? (
            <>
              <Text variant="bodySmall" style={styles.helperText}>
                Choose from popular hubs
              </Text>
              <View style={styles.hubButtons}>
                {hubs.map((hub) => (
                  <Button
                    key={hub.id}
                    mode={hubId === hub.id ? 'contained' : 'outlined'}
                    onPress={() => setHubId(hub.id)}
                    style={styles.hubButton}
                  >
                    {hub.name}
                  </Button>
                ))}
              </View>
            </>
          ) : (
            <View style={styles.customLocation}>
              <TextInput
                label={mapboxToken ? 'Search places' : 'Location name'}
                value={locationQuery}
                onChangeText={setLocationQuery}
                mode="outlined"
                style={styles.input}
                placeholder={mapboxToken ? 'Search cafes, parks, venues...' : 'Enter location name'}
              />
              {mapboxToken && (
                <>
                  {isSearchingLocation && (
                    <Text variant="bodySmall" style={styles.helperText}>
                      Searching...
                    </Text>
                  )}
                  {locationSuggestions.length > 0 && (
                    <View style={styles.suggestionList}>
                      {locationSuggestions.map((suggestion) => (
                        <TouchableOpacity
                          key={suggestion.id}
                          style={styles.suggestionItem}
                          onPress={() => handleSuggestionSelect(suggestion)}
                        >
                          <Text variant="bodyMedium">{suggestion.name}</Text>
                          <Text variant="bodySmall" style={styles.suggestionAddress}>
                            {suggestion.address}
                          </Text>
                        </TouchableOpacity>
                      ))}
                    </View>
                  )}
                </>
              )}

              <TextInput
                label="Address (optional)"
                value={locationAddress}
                onChangeText={setLocationAddress}
                mode="outlined"
                style={styles.input}
              />

              <View style={styles.coordinateRow}>
                <TextInput
                  label="Latitude"
                  value={latitudeInput}
                  onChangeText={setLatitudeInput}
                  keyboardType="decimal-pad"
                  mode="outlined"
                  style={[styles.input, styles.coordinateInput]}
                />
                <TextInput
                  label="Longitude"
                  value={longitudeInput}
                  onChangeText={setLongitudeInput}
                  keyboardType="decimal-pad"
                  mode="outlined"
                  style={[styles.input, styles.coordinateInput]}
                />
              </View>
            </View>
          )}

          <Text variant="bodyMedium" style={styles.label}>
            Start Time
          </Text>
          <TouchableOpacity
            onPress={() => {
              setTempDate(startTime);
              setPickerMode('date');
              setShowStartPicker(true);
            }}
          >
            <TextInput
              label="Start Time"
              value={format(startTime, 'EEE, MMM d, yyyy  h:mm a')}
              mode="outlined"
              style={styles.input}
              editable={false}
              pointerEvents="none"
              right={<TextInput.Icon icon="calendar-clock" />}
            />
          </TouchableOpacity>

          <Text variant="bodyMedium" style={styles.label}>
            End Time
          </Text>
          <TouchableOpacity
            onPress={() => {
              setTempDate(endTime);
              setPickerMode('date');
              setShowEndPicker(true);
            }}
          >
            <TextInput
              label="End Time"
              value={format(endTime, 'EEE, MMM d, yyyy  h:mm a')}
              mode="outlined"
              style={styles.input}
              editable={false}
              pointerEvents="none"
              right={<TextInput.Icon icon="calendar-clock" />}
            />
          </TouchableOpacity>

          {/* Quick time slots for "Tonight" */}
          <Text variant="bodySmall" style={styles.quickLabel}>
            Quick time slots (today):
          </Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.timeSlots}>
            {[18, 19, 20, 21].map((hour) => {
              const slotTime = setMinutes(setHours(startOfDay(new Date()), hour), 0);
              const isSelected = startTime.getHours() === hour && startTime.getMinutes() === 0 &&
                                 startTime.toDateString() === new Date().toDateString();
              return (
                <Button
                  key={hour}
                  mode={isSelected ? 'contained' : 'outlined'}
                  compact
                  style={styles.timeSlotButton}
                  onPress={() => {
                    setStartTime(slotTime);
                    setEndTime(addHours(slotTime, 2));
                  }}
                >
                  {format(slotTime, 'h:mm a')}
                </Button>
              );
            })}
          </ScrollView>

          <TextInput
            label="Description (optional)"
            value={description}
            onChangeText={setDescription}
            mode="outlined"
            style={styles.input}
            multiline
            numberOfLines={4}
            placeholder="Tell people more about this activity..."
          />

          <Text variant="bodyMedium" style={styles.label}>
            Max Members
          </Text>
          <TextInput
            label="Maximum Members"
            value={maxMembers.toString()}
            onChangeText={(text) => {
              const num = parseInt(text) || 4;
              setMaxMembers(Math.max(2, Math.min(20, num)));
            }}
            keyboardType="number-pad"
            mode="outlined"
            style={styles.input}
          />

          <View style={styles.checkboxContainer}>
            <Checkbox
              status={inviteOnly ? 'checked' : 'unchecked'}
              onPress={() => setInviteOnly(!inviteOnly)}
            />
            <Text variant="bodyMedium" style={styles.checkboxLabel}>
              Invite only (locked activity)
            </Text>
          </View>

          {template && (
            <Card style={styles.templateBadge}>
              <Card.Content>
                <Text variant="bodySmall" style={styles.templateText}>
                  📝 Using template: {template.name}
                </Text>
              </Card.Content>
            </Card>
          )}
        </Card.Content>
      </Card>

      <View style={styles.actions}>
        <Button mode="outlined" onPress={onCancel} style={styles.button}>
          Cancel
        </Button>
        <Button
          mode="contained"
          onPress={handleSubmit}
          loading={loading}
          disabled={loading}
          style={styles.button}
        >
          Create Activity
        </Button>
      </View>

      {/* Start Time Picker */}
      {showStartPicker && (
        <DateTimePickerModal
          value={tempDate}
          mode={pickerMode}
          onConfirm={(date) => {
            if (pickerMode === 'date') {
              setTempDate(date);
              setPickerMode('time');
            } else {
              setStartTime(date);
              // Auto-set end time to 2 hours after start
              if (date >= endTime) {
                setEndTime(addHours(date, 2));
              }
              setShowStartPicker(false);
              setPickerMode('date');
            }
          }}
          onCancel={() => {
            setShowStartPicker(false);
            setPickerMode('date');
          }}
          title={pickerMode === 'date' ? 'Select Date' : 'Select Start Time'}
        />
      )}

      {/* End Time Picker */}
      {showEndPicker && (
        <DateTimePickerModal
          value={tempDate}
          mode={pickerMode}
          onConfirm={(date) => {
            if (pickerMode === 'date') {
              setTempDate(date);
              setPickerMode('time');
            } else {
              setEndTime(date);
              setShowEndPicker(false);
              setPickerMode('date');
            }
          }}
          onCancel={() => {
            setShowEndPicker(false);
            setPickerMode('date');
          }}
          title={pickerMode === 'date' ? 'Select Date' : 'Select End Time'}
        />
      )}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  card: {
    margin: 16,
    marginBottom: 8,
  },
  title: {
    fontWeight: 'bold',
  },
  input: {
    marginBottom: 16,
  },
  label: {
    marginTop: 8,
    marginBottom: 8,
    fontWeight: '600',
  },
  segmentedButtons: {
    marginBottom: 16,
  },
  hubButtons: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 16,
  },
  hubButton: {
    marginRight: 8,
    marginBottom: 8,
  },
  helperText: {
    color: '#777',
    marginBottom: 8,
  },
  customLocation: {
    marginBottom: 16,
  },
  suggestionList: {
    borderWidth: 1,
    borderColor: '#E0E0E0',
    borderRadius: 12,
    marginBottom: 12,
    overflow: 'hidden',
  },
  suggestionItem: {
    padding: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#F1F1F1',
  },
  suggestionAddress: {
    color: '#666',
    marginTop: 4,
  },
  coordinateRow: {
    flexDirection: 'row',
    gap: 8,
  },
  coordinateInput: {
    flex: 1,
  },
  timeButton: {
    marginBottom: 16,
  },
  quickLabel: {
    marginTop: 4,
    marginBottom: 8,
    color: '#666',
  },
  timeSlots: {
    marginBottom: 16,
  },
  timeSlotButton: {
    marginRight: 8,
  },
  checkboxContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 8,
  },
  checkboxLabel: {
    marginLeft: 8,
  },
  note: {
    marginTop: -8,
    marginBottom: 16,
    color: '#666',
    fontStyle: 'italic',
    fontSize: 12,
  },
  actions: {
    flexDirection: 'row',
    padding: 16,
    gap: 8,
  },
  button: {
    flex: 1,
  },
  templateBadge: {
    marginTop: 16,
    backgroundColor: '#E3F2FD',
  },
  templateText: {
    color: '#1976D2',
    fontStyle: 'italic',
  },
});

