import { MD3LightTheme } from 'react-native-paper';

// Category colors for activities - expanded categories
export const categoryColors = {
  SPORTS: '#4CAF50',
  FOOD: '#FF9800',
  ART: '#9C27B0',
  MUSIC: '#2196F3',
  OUTDOOR: '#8BC34A',
  GAMES: '#E91E63',
  LEARNING: '#607D8B',
  WELLNESS: '#00BCD4',
} as const;

// Category icons - expanded
export const categoryIcons = {
  SPORTS: 'basketball',
  FOOD: 'food',
  ART: 'palette',
  MUSIC: 'music',
  OUTDOOR: 'tree',
  GAMES: 'gamepad-variant',
  LEARNING: 'book-open-variant',
  WELLNESS: 'meditation',
} as const;

// Category labels for display
export const categoryLabels = {
  SPORTS: 'Sports & Fitness',
  FOOD: 'Food & Drinks',
  ART: 'Art & Culture',
  MUSIC: 'Music & Nightlife',
  OUTDOOR: 'Outdoors & Nature',
  GAMES: 'Games & Social',
  LEARNING: 'Learning & Skills',
  WELLNESS: 'Wellness & Mindfulness',
} as const;

// Consistent spacing values
export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
} as const;

// Text colors with proper contrast
export const textColors = {
  primary: '#333333',
  secondary: '#555555',
  muted: '#777777',
  disabled: '#999999',
  inverse: '#FFFFFF',
  error: '#B00020',
  success: '#2E7D32',
  warning: '#E65100',
} as const;

// Status colors
export const statusColors = {
  scheduled: '#6200EE',
  active: '#4CAF50',
  completed: '#9E9E9E',
  cancelled: '#F44336',
} as const;

// Trust score colors
export const trustScoreColors = {
  high: '#4CAF50',    // 80+
  medium: '#FFC107',  // 60-79
  low: '#F44336',     // below 60
} as const;

export const getTrustScoreColor = (score: number): string => {
  if (score >= 80) return trustScoreColors.high;
  if (score >= 60) return trustScoreColors.medium;
  return trustScoreColors.low;
};

export const theme = {
  ...MD3LightTheme,
  colors: {
    ...MD3LightTheme.colors,
    primary: '#6200EE',
    secondary: '#03DAC6',
    error: '#B00020',
    surface: '#FFFFFF',
    background: '#F5F5F5',
  },
  // Custom extensions
  custom: {
    categoryColors,
    categoryIcons,
    categoryLabels,
    spacing,
    textColors,
    statusColors,
    trustScoreColors,
  },
};

