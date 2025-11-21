import React, { useState } from 'react';
import { View, StyleSheet, Modal } from 'react-native';
import { TextInput, Button, Text, Surface, Portal, SegmentedButtons } from 'react-native-paper';

interface ReportModalProps {
  visible: boolean;
  onClose: () => void;
  onSubmit: (reason: string) => void;
  targetUserName?: string;
}

const REPORT_REASONS = [
  { value: 'Inappropriate behavior', label: 'Inappropriate Behavior' },
  { value: 'Harassment', label: 'Harassment' },
  { value: 'Spam', label: 'Spam' },
  { value: 'Safety concern', label: 'Safety Concern' },
  { value: 'Other', label: 'Other' },
];

export const ReportModal: React.FC<ReportModalProps> = ({
  visible,
  onClose,
  onSubmit,
  targetUserName,
}) => {
  const [selectedReason, setSelectedReason] = useState('');
  const [customReason, setCustomReason] = useState('');

  const handleSubmit = () => {
    const reason = selectedReason === 'Other' ? customReason : selectedReason;
    if (!reason.trim()) {
      return;
    }
    onSubmit(reason);
    setSelectedReason('');
    setCustomReason('');
  };

  return (
    <Portal>
      <Modal
        visible={visible}
        transparent
        animationType="slide"
        onRequestClose={onClose}
      >
        <View style={styles.overlay}>
          <Surface style={styles.modal}>
            <Text variant="headlineSmall" style={styles.title}>
              Report User
            </Text>
            
            {targetUserName && (
              <Text variant="bodyMedium" style={styles.subtitle}>
                Reporting: {targetUserName}
              </Text>
            )}

            <Text variant="bodyMedium" style={styles.description}>
              Please select a reason for reporting this user:
            </Text>

            <View style={styles.reasonsContainer}>
              {REPORT_REASONS.map((reason) => (
                <Button
                  key={reason.value}
                  mode={selectedReason === reason.value ? 'contained' : 'outlined'}
                  onPress={() => setSelectedReason(reason.value)}
                  style={styles.reasonButton}
                >
                  {reason.label}
                </Button>
              ))}
            </View>

            {selectedReason === 'Other' && (
              <TextInput
                label="Please describe the issue"
                value={customReason}
                onChangeText={setCustomReason}
                mode="outlined"
                multiline
                numberOfLines={3}
                style={styles.customInput}
                placeholder="Describe the issue..."
              />
            )}

            <View style={styles.actions}>
              <Button
                mode="contained"
                onPress={handleSubmit}
                disabled={!selectedReason || (selectedReason === 'Other' && !customReason.trim())}
                style={styles.submitButton}
              >
                Submit Report
              </Button>
              <Button mode="text" onPress={onClose} style={styles.cancelButton}>
                Cancel
              </Button>
            </View>
          </Surface>
        </View>
      </Modal>
    </Portal>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'flex-end',
  },
  modal: {
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 24,
    maxHeight: '80%',
  },
  title: {
    fontWeight: 'bold',
    marginBottom: 8,
    textAlign: 'center',
  },
  subtitle: {
    marginBottom: 16,
    textAlign: 'center',
    color: '#666',
  },
  description: {
    marginBottom: 16,
    color: '#666',
  },
  reasonsContainer: {
    marginBottom: 16,
    gap: 8,
  },
  reasonButton: {
    marginBottom: 8,
  },
  customInput: {
    marginTop: 8,
    marginBottom: 16,
  },
  actions: {
    marginTop: 16,
  },
  submitButton: {
    marginBottom: 8,
  },
  cancelButton: {
    marginTop: 8,
  },
});

