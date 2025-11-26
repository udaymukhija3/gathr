import React from 'react';
import { View, StyleSheet } from 'react-native';
import { Text, IconButton } from 'react-native-paper';

interface FeedHeaderProps {
    onNotificationPress: () => void;
    onMyActivitiesPress: () => void;
    onProfilePress: () => void;
}

export const FeedHeader: React.FC<FeedHeaderProps> = ({
    onNotificationPress,
    onMyActivitiesPress,
    onProfilePress,
}) => {
    return (
        <View style={styles.header}>
            <Text variant="headlineSmall" style={styles.title}>
                Tonight
            </Text>
            <View style={styles.headerActions}>
                <IconButton
                    icon="bell-outline"
                    size={24}
                    onPress={onNotificationPress}
                />
                <IconButton
                    icon="calendar-check"
                    size={24}
                    onPress={onMyActivitiesPress}
                />
                <IconButton
                    icon="account-circle"
                    size={24}
                    onPress={onProfilePress}
                />
            </View>
        </View>
    );
};

const styles = StyleSheet.create({
    header: {
        padding: 16,
        paddingRight: 8,
        backgroundColor: '#FFF',
        borderBottomWidth: 1,
        borderBottomColor: '#E0E0E0',
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    headerActions: {
        flexDirection: 'row',
    },
    title: {
        fontWeight: 'bold',
    },
});
