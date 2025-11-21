import React from 'react';
import { fireEvent, waitFor } from '@testing-library/react-native';
import { FeedbackModal } from '../FeedbackModal';
import { render, mockActivity } from '../../test-utils/test-helpers';
import Toast from 'react-native-toast-message';

jest.mock('react-native-toast-message');

describe('FeedbackModal', () => {
  const mockOnClose = jest.fn();
  const mockOnSubmit = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders when visible', () => {
    const { getByText } = render(
      <FeedbackModal
        visible={true}
        activity={mockActivity}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />
    );

    expect(getByText('How was your experience?')).toBeTruthy();
    expect(getByText('Coffee at Galleria')).toBeTruthy();
  });

  it('does not render when not visible', () => {
    const { queryByText } = render(
      <FeedbackModal
        visible={false}
        activity={mockActivity}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />
    );

    expect(queryByText('How was your experience?')).toBeNull();
  });

  it('shows required field error when submitting without answering "Did you meet?"', async () => {
    const { getByText } = render(
      <FeedbackModal
        visible={true}
        activity={mockActivity}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />
    );

    fireEvent.press(getByText('Submit'));

    await waitFor(() => {
      expect(Toast.show).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'error',
          text1: 'Required Field',
        })
      );
    });

    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('shows conditional questions when user met', () => {
    const { getByText } = render(
      <FeedbackModal
        visible={true}
        activity={mockActivity}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />
    );

    // Select "Yes, we met"
    fireEvent.press(getByText('Yes, we met!'));

    // Check if conditional questions appear
    expect(getByText('How was the experience?')).toBeTruthy();
    expect(getByText('Would you hang out with them again?')).toBeTruthy();
  });

  it('hides conditional questions when user did not meet', () => {
    const { getByText, queryByText } = render(
      <FeedbackModal
        visible={true}
        activity={mockActivity}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />
    );

    // Select "No, it didn't work out"
    fireEvent.press(getByText("No, it didn't work out"));

    // Conditional questions should not appear
    expect(queryByText('Would you hang out with them again?')).toBeNull();
  });

  it('shows "Add to contacts" option when user would hang out again', () => {
    const { getByText } = render(
      <FeedbackModal
        visible={true}
        activity={mockActivity}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />
    );

    // Select "Yes, we met"
    fireEvent.press(getByText('Yes, we met!'));

    // Select "Yes, definitely!" for hang out again
    fireEvent.press(getByText('Yes, definitely!'));

    // Check if "Add to contacts" appears
    expect(getByText('Add participants to contacts')).toBeTruthy();
  });

  it('submits feedback with correct data when all required fields are filled', async () => {
    const { getByText } = render(
      <FeedbackModal
        visible={true}
        activity={mockActivity}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />
    );

    // Answer all required questions
    fireEvent.press(getByText('Yes, we met!'));
    fireEvent.press(getByText('Yes, definitely!'));
    fireEvent.press(getByText('Submit'));

    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          didMeet: true,
          wouldHangOutAgain: true,
          experienceRating: 3, // Default rating
          addedToContacts: false,
        })
      );
    });
  });

  it('calls onClose when "Later" button is pressed', () => {
    const { getByText } = render(
      <FeedbackModal
        visible={true}
        activity={mockActivity}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />
    );

    fireEvent.press(getByText('Later'));
    expect(mockOnClose).toHaveBeenCalled();
  });
});
