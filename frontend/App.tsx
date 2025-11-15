import React, { useState, useEffect } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { Provider as PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import { getToken } from './src/services/api';
import { theme } from './src/theme';
import { PhoneEntryScreen } from './src/screens/PhoneEntryScreen';
import { OtpVerifyScreen } from './src/screens/OtpVerifyScreen';
import { FeedScreen } from './src/screens/FeedScreen';
import { ActivityDetailScreen } from './src/screens/ActivityDetailScreen';
import { ChatScreen } from './src/screens/ChatScreen';
import { InviteScreen } from './src/screens/InviteScreen';
import { CreateActivityScreen } from './src/screens/CreateActivityScreen';
import { AuthResponse } from './src/types';

export type RootStackParamList = {
  PhoneEntry: undefined;
  OtpVerify: { phone: string };
  Feed: undefined;
  ActivityDetail: { activityId: number };
  Chat: { activityId: number };
  Invite: { activityId: number };
  CreateActivity: undefined;
};

const Stack = createStackNavigator<RootStackParamList>();

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [authState, setAuthState] = useState<{
    phone: string;
    authResponse: AuthResponse | null;
  }>({ phone: '', authResponse: null });

  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    try {
      const token = await getToken();
      setIsAuthenticated(!!token);
    } catch (error) {
      console.error('Error checking auth:', error);
      setIsAuthenticated(false);
    } finally {
      setIsLoading(false);
    }
  };

  const handleOtpSent = (phone: string) => {
    setAuthState({ ...authState, phone });
  };

  const handleOtpVerified = (authResponse: AuthResponse) => {
    setAuthState({ ...authState, authResponse });
    setIsAuthenticated(true);
  };

  if (isLoading) {
    return null; // Or a loading screen
  }

  return (
    <SafeAreaProvider>
      <PaperProvider theme={theme}>
        <StatusBar style="auto" />
        <NavigationContainer>
          <Stack.Navigator
            screenOptions={{
              headerStyle: {
                backgroundColor: '#6200EE',
              },
              headerTintColor: '#fff',
              headerTitleStyle: {
                fontWeight: 'bold',
              },
            }}
          >
            {!isAuthenticated ? (
              <>
                <Stack.Screen
                  name="PhoneEntry"
                  options={{ title: 'Welcome to Gatherly', headerShown: false }}
                >
                  {(props) => (
                    <PhoneEntryScreen
                      {...props}
                      onOtpSent={(phone) => {
                        handleOtpSent(phone);
                        props.navigation.navigate('OtpVerify', { phone });
                      }}
                    />
                  )}
                </Stack.Screen>
                <Stack.Screen
                  name="OtpVerify"
                  options={{ title: 'Verify OTP', headerShown: false }}
                >
                  {(props) => (
                    <OtpVerifyScreen
                      {...props}
                      phone={props.route.params.phone}
                      onVerified={(authResponse) => {
                        handleOtpVerified(authResponse);
                      }}
                      onBack={() => props.navigation.goBack()}
                    />
                  )}
                </Stack.Screen>
              </>
            ) : (
              <>
                <Stack.Screen
                  name="Feed"
                  options={{ title: 'Tonight in Gurgaon' }}
                >
                  {(props) => (
                    <FeedScreen
                      {...props}
                      onCreateActivity={() =>
                        props.navigation.navigate('CreateActivity')
                      }
                      onActivityPress={(activityId) =>
                        props.navigation.navigate('ActivityDetail', {
                          activityId,
                        })
                      }
                    />
                  )}
                </Stack.Screen>
                <Stack.Screen
                  name="ActivityDetail"
                  options={{ title: 'Activity Details' }}
                >
                  {(props) => (
                    <ActivityDetailScreen
                      {...props}
                      activityId={props.route.params.activityId}
                      onJoinChat={() =>
                        props.navigation.navigate('Chat', {
                          activityId: props.route.params.activityId,
                        })
                      }
                      onInvite={() =>
                        props.navigation.navigate('Invite', {
                          activityId: props.route.params.activityId,
                        })
                      }
                    />
                  )}
                </Stack.Screen>
                <Stack.Screen name="Chat" options={{ title: 'Group Chat' }}>
                  {(props) => (
                    <ChatScreen
                      {...props}
                      activityId={props.route.params.activityId}
                    />
                  )}
                </Stack.Screen>
                <Stack.Screen name="Invite" options={{ title: 'Invite Friends' }}>
                  {(props) => (
                    <InviteScreen
                      {...props}
                      activityId={props.route.params.activityId}
                    />
                  )}
                </Stack.Screen>
                <Stack.Screen
                  name="CreateActivity"
                  options={{ title: 'Create Activity' }}
                >
                  {(props) => (
                    <CreateActivityScreen
                      {...props}
                      onSubmit={(activityId) => {
                        props.navigation.navigate('ActivityDetail', {
                          activityId,
                        });
                      }}
                      onCancel={() => props.navigation.goBack()}
                    />
                  )}
                </Stack.Screen>
              </>
            )}
          </Stack.Navigator>
        </NavigationContainer>
      </PaperProvider>
    </SafeAreaProvider>
  );
}

