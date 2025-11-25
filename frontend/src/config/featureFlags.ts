const parseBoolean = (value?: string): boolean => {
  if (value === undefined || value === null) {
    return false;
  }
  return ['1', 'true', 'yes'].includes(value.toLowerCase());
};

const trustScoreFlag = parseBoolean(process.env.EXPO_PUBLIC_ENABLE_TRUST_SCORE);

export const featureFlags = {
  trustScore: trustScoreFlag,
};


