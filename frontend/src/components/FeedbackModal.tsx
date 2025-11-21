import React, { useState } from 'react';
import { View, StyleSheet, ScrollView } from 'react-native';
import { Modal, Portal, Text, Button, RadioButton, Checkbox, TextInput } from 'react-native-paper';
import Toast from 'react-native-toast-message';
import { Activity } from '../types';

interface FeedbackModalProps {
  visible: boolean;
  activity: Activity;
  onClose: () => void;
  onSubmit: (feedback: {
    didMeet: boolean;
    experienceRating: number;
    wouldHangOutAgain: boolean;
    addedToContacts: boolean;
    comments?: string;
  }) => void;
}

export const FeedbackModal: React.FC<FeedbackModalProps> = ({
  visible,
  activity,
  onClose,
  onSubmit,
}) => {
  const [didMeet, setDidMeet] = useState<boolean | null>(null);
  const [experienceRating, setExperienceRating] = useState<number>(3);
  const [wouldHangOutAgain, setWouldHangOutAgain] = useState<boolean | null>(null);
  const [addedToContacts, setAddedToContacts] = useState(false);
  const [comments, setComments] = useState('');

  const handleSubmit = () => {
    if (didMeet === null) {
      Toast.show({
        type: 'error',
        text1: 'Required Field',
        text2: 'Please answer if you met or not',
      });
      return;
    }

    if (wouldHangOutAgain === null) {
      Toast.show({
        type: 'error',
        text1: 'Required Field',
        text2: 'Please answer if you would hang out again',
      });
      return;
    }

    onSubmit({
      didMeet,
      experienceRating,
      wouldHangOutAgain,
      addedToContacts,
      comments: comments.trim() || undefined,
    });

    // Reset form
    setDidMeet(null);
    setExperienceRating(3);
    setWouldHangOutAgain(null);
    setAddedToContacts(false);
    setComments('');
  };

  const renderStars = () => {
    return (
      <View style={styles.starsContainer}>
        {[1, 2, 3, 4, 5].map((rating) => (
          <Button
            key={rating}
            mode={experienceRating === rating ? 'contained' : 'outlined'}
            onPress={() => setExperienceRating(rating)}
            style={styles.starButton}
            compact
          >
            {rating === 1 ? '⭐' : rating === 2 ? '⭐⭐' : rating === 3 ? '⭐⭐⭐' : rating === 4 ? '⭐⭐⭐⭐' : '⭐⭐⭐⭐⭐'}
          </Button>
        ))}
      </View>
    );
  };

  return (
    <Portal>
      <Modal
        visible={visible}
        onDismiss={onClose}
        contentContainerStyle={styles.modalContent}
      >
        <ScrollView>
          <Text variant="headlineSmall" style={styles.title}>
            How was your experience?
          </Text>
          <Text variant="bodyMedium" style={styles.subtitle}>
            {activity.title}
          </Text>

          {/* Question 1: Did you meet? */}
          <View style={styles.section}>
            <Text variant="titleMedium" style={styles.question}>
              Did you meet?
            </Text>
            <RadioButton.Group
              onValueChange={(value) => setDidMeet(value === 'true')}
              value={didMeet === null ? '' : didMeet.toString()}
            >
              <View style={styles.radioOption}>
                <RadioButton.Item label="Yes, we met!" value="true" />
              </View>
              <View style={styles.radioOption}>
                <RadioButton.Item label="No, it didn't work out" value="false" />
              </View>
            </RadioButton.Group>
          </View>

          {/* Question 2: How was the experience? (Star Rating) */}
          {didMeet && (
            <View style={styles.section}>
              <Text variant="titleMedium" style={styles.question}>
                How was the experience?
              </Text>
              <Text variant="bodySmall" style={styles.hint}>
                Rate your overall experience
              </Text>
              {renderStars()}
              <View style={styles.ratingLabels}>
                <Text variant="bodySmall" style={styles.ratingLabel}>Poor</Text>
                <Text variant="bodySmall" style={styles.ratingLabel}>Excellent</Text>
              </View>
            </View>
          )}

          {/* Question 3: Would you hang out again? */}
          {didMeet && (
            <View style={styles.section}>
              <Text variant="titleMedium" style={styles.question}>
                Would you hang out with them again?
              </Text>
              <RadioButton.Group
                onValueChange={(value) => setWouldHangOutAgain(value === 'true')}
                value={wouldHangOutAgain === null ? '' : wouldHangOutAgain.toString()}
              >
                <View style={styles.radioOption}>
                  <RadioButton.Item label="Yes, definitely!" value="true" />
                </View>
                <View style={styles.radioOption}>
                  <RadioButton.Item label="No, not really" value="false" />
                </View>
              </RadioButton.Group>
            </View>
          )}

          {/* Question 4: Add to contacts */}
          {didMeet && wouldHangOutAgain && (
            <View style={styles.section}>
              <Text variant="titleMedium" style={styles.question}>
                Keep in touch
              </Text>
              <View style={styles.checkboxOption}>
                <Checkbox
                  status={addedToContacts ? 'checked' : 'unchecked'}
                  onPress={() => setAddedToContacts(!addedToContacts)}
                />
                <Text variant="bodyMedium" style={styles.checkboxLabel}>
                  Add participants to contacts
                </Text>
              </View>
              <Text variant="bodySmall" style={styles.hint}>
                We'll help you exchange contact info with participants
              </Text>
            </View>
          )}

          {/* Optional comments */}
          <View style={styles.section}>
            <Text variant="titleMedium" style={styles.question}>
              Additional comments (optional)
            </Text>
            <TextInput
              value={comments}
              onChangeText={setComments}
              mode="outlined"
              multiline
              numberOfLines={3}
              placeholder="Any other thoughts about your experience?"
              style={styles.commentsInput}
            />
          </View>

          {/* Actions */}
          <View style={styles.actions}>
            <Button mode="outlined" onPress={onClose} style={styles.button}>
              Later
            </Button>
            <Button mode="contained" onPress={handleSubmit} style={styles.button}>
              Submit
            </Button>
          </View>
        </ScrollView>
      </Modal>
    </Portal>
  );
};

const styles = StyleSheet.create({
  modalContent: {
    backgroundColor: 'white',
    padding: 24,
    margin: 20,
    borderRadius: 8,
    maxHeight: '90%',
  },
  title: {
    fontWeight: 'bold',
    marginBottom: 4,
  },
  subtitle: {
    color: '#666',
    marginBottom: 24,
  },
  section: {
    marginBottom: 24,
  },
  question: {
    fontWeight: '600',
    marginBottom: 12,
  },
  hint: {
    color: '#666',
    marginTop: 4,
    fontSize: 12,
  },
  radioOption: {
    marginBottom: 4,
  },
  checkboxOption: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 8,
  },
  checkboxLabel: {
    marginLeft: 8,
    flex: 1,
  },
  starsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 8,
  },
  starButton: {
    flex: 1,
    marginHorizontal: 2,
  },
  ratingLabels: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 8,
    paddingHorizontal: 4,
  },
  ratingLabel: {
    color: '#666',
    fontSize: 11,
  },
  commentsInput: {
    marginTop: 8,
  },
  actions: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 8,
  },
  button: {
    flex: 1,
  },
});
