import { useState, useCallback } from 'react';
import { UserLocation, getCurrentLocation } from '../services/location';
import Toast from 'react-native-toast-message';

export const useFeedLocation = () => {
    const [userLocation, setUserLocation] = useState<UserLocation | null>(null);
    const [requestingLocation, setRequestingLocation] = useState(false);

    const requestLocation = useCallback(async (): Promise<UserLocation | null> => {
        setRequestingLocation(true);
        const location = await getCurrentLocation();
        setRequestingLocation(false);

        if (!location) {
            Toast.show({
                type: 'error',
                text1: 'Location needed',
                text2: 'Enable location services to see nearby plans',
            });
            return null;
        }

        setUserLocation(location);
        return location;
    }, []);

    return {
        userLocation,
        requestingLocation,
        requestLocation,
        setUserLocation
    };
};
