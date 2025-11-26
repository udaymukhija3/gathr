import React from 'react';
import { View, StyleSheet } from 'react-native';
import { Chip } from 'react-native-paper';

interface FeedModeToggleProps {
    feedMode: 'hub' | 'nearby';
    requestingLocation: boolean;
    onEnableNearby: () => void;
    onDisableNearby: () => void;
}

export const FeedModeToggle: React.FC<FeedModeToggleProps> = ({
    feedMode,
    requestingLocation,
    onEnableNearby,
    onDisableNearby,
}) => {
    return (
        <View style={styles.modeToggle}>
            <Chip
                icon="domain"
                selected={feedMode === 'hub'}
                onPress={onDisableNearby}
                style={styles.modeChip}
            >
                Curated Hubs
            </Chip>
            <Chip
                icon="map-marker-radius"
                selected={feedMode === 'nearby'}
                onPress={onEnableNearby}
                style={styles.modeChip}
                disabled={requestingLocation}
            >
                {requestingLocation ? 'Locating...' : 'Near Me'}
            </Chip>
        </View>
    );
};

const styles = StyleSheet.create({
    modeToggle: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        marginTop: 8,
        gap: 8,
    },
    modeChip: {
        flex: 1,
    },
});
