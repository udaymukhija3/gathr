import React from 'react';
import { Text } from 'react-native';
import { act, fireEvent, render } from '@testing-library/react-native';
import { ErrorBoundary } from '../ErrorBoundary';
import Toast from 'react-native-toast-message';

jest.mock('react-native-toast-message', () => ({
  show: jest.fn(),
}));

// Component that throws an error
const ThrowError = ({ shouldThrow }: { shouldThrow: boolean }) => {
  if (shouldThrow) {
    throw new Error('Test error');
  }
  return <Text>No error</Text>;
};

describe('ErrorBoundary', () => {
  // Suppress console.error for these tests
  const originalError = console.error;
  beforeAll(() => {
    console.error = jest.fn();
  });

  afterAll(() => {
    console.error = originalError;
  });

  it('renders children when there is no error', () => {
    const { getByText } = render(
      <ErrorBoundary>
        <Text>Child component</Text>
      </ErrorBoundary>
    );

    expect(getByText('Child component')).toBeTruthy();
  });

  it('renders error UI when an error occurs', () => {
    const { getByText } = render(
      <ErrorBoundary>
        <ThrowError shouldThrow={true} />
      </ErrorBoundary>
    );

    expect(getByText('Oops! Something went wrong')).toBeTruthy();
    expect(getByText(/We encountered an unexpected error/)).toBeTruthy();
  });

  it('shows "Try Again" button in error state', () => {
    const { getByText } = render(
      <ErrorBoundary>
        <ThrowError shouldThrow={true} />
      </ErrorBoundary>
    );

    expect(getByText('Try Again')).toBeTruthy();
  });

  it('resets error state when the internal reset handler is invoked', () => {
    const boundaryRef = React.createRef<ErrorBoundary>();

    const { getByText, rerender } = render(
      <ErrorBoundary ref={boundaryRef}>
        <ThrowError shouldThrow={true} />
      </ErrorBoundary>
    );

    expect(getByText('Oops! Something went wrong')).toBeTruthy();

    act(() => {
      boundaryRef.current?.setState({
        hasError: false,
        error: null,
        errorInfo: null,
      });
    });

    rerender(
      <ErrorBoundary ref={boundaryRef}>
        <ThrowError shouldThrow={false} />
      </ErrorBoundary>
    );

    expect(getByText('No error')).toBeTruthy();
  });

  it('renders custom fallback when provided', () => {
    const customFallback = <Text>Custom error message</Text>;

    const { getByText } = render(
      <ErrorBoundary fallback={customFallback}>
        <ThrowError shouldThrow={true} />
      </ErrorBoundary>
    );

    expect(getByText('Custom error message')).toBeTruthy();
  });

  it('logs error to console', () => {
    const consoleSpy = jest.spyOn(console, 'error');

    render(
      <ErrorBoundary>
        <ThrowError shouldThrow={true} />
      </ErrorBoundary>
    );

    expect(consoleSpy).toHaveBeenCalled();
  });
});
