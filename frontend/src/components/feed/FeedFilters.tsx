import React from 'react';
import { ScrollView, StyleSheet, Text } from 'react-native';
import { Chip } from 'react-native-paper';
import { FeedFilter, FILTER_OPTIONS } from '../../hooks/useFeedActivities';

interface FeedFiltersProps {
    activeFilter: FeedFilter;
    onFilterChange: (filter: FeedFilter) => void;
    userInterests: string[];
}

export const FeedFilters: React.FC<FeedFiltersProps> = ({
    activeFilter,
    onFilterChange,
    userInterests,
}) => {
    return (
        <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            style={styles.filterContainer}
            contentContainerStyle={styles.filterContent}
        >
            {FILTER_OPTIONS.map((option) => {
                const isActive = activeFilter === option.key;
                const isUserInterest = userInterests.includes(option.key);

                return (
                    <Chip
                        key={option.key}
                        icon={option.icon}
                        selected={isActive}
                        onPress={() => onFilterChange(option.key)}
                        style={[
                            styles.filterChip,
                            isActive && styles.filterChipActive,
                            isUserInterest && !isActive && styles.filterChipInterest,
                        ]}
                        textStyle={[
                            styles.filterChipText,
                            isActive && styles.filterChipTextActive,
                        ]}
                    >
                        {option.label}
                        {isUserInterest && !isActive && (
                            <Text style={styles.interestDot}> •</Text>
                        )}
                    </Chip>
                );
            })}
        </ScrollView>
    );
};

const styles = StyleSheet.create({
    filterContainer: {
        backgroundColor: '#FFF',
        borderBottomWidth: 1,
        borderBottomColor: '#E0E0E0',
        maxHeight: 56,
    },
    filterContent: {
        paddingHorizontal: 12,
        paddingVertical: 10,
        gap: 8,
    },
    filterChip: {
        backgroundColor: '#F5F5F5',
        marginRight: 4,
    },
    filterChipActive: {
        backgroundColor: '#6200EE',
    },
    filterChipInterest: {
        borderColor: '#6200EE',
        borderWidth: 1,
    },
    filterChipText: {
        color: '#333',
    },
    filterChipTextActive: {
        color: '#FFF',
    },
    interestDot: {
        color: '#6200EE',
        fontWeight: 'bold',
    },
});
