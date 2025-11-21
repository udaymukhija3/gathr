import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { Provider as PaperProvider, Portal } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import Toast from 'react-native-toast-message';
import { UserProvider, useUser } from './src/context/UserContext';
import { ErrorBoundary } from './src/components/ErrorBoundary';
import { theme } from './src/theme';
import { PhoneEntryScreen } from './src/screens/PhoneEntryScreen';
import { OtpVerifyScreen } from './src/screens/OtpVerifyScreen';
import { FeedScreen } from './src/screens/FeedScreen';
import { ActivityDetailScreen } from './src/screens/ActivityDetailScreen';
import { ChatScreen } from './src/screens/ChatScreen';
import { InviteScreen } from './src/screens/InviteScreen';
import { CreateActivityScreen } from './src/screens/CreateActivityScreen';
import { ContactsUploadScreen } from './src/screens/ContactsUploadScreen';
import { SettingsScreen } from './src/screens/SettingsScreen';
import { TemplateSelectionScreen } from './src/screens/TemplateSelectionScreen';
import { AuthResponse, ActivityTemplate } from './src/types';

export type RootStackParamList = {
  PhoneEntry: undefined;
  OtpVerify: { phone: string };
  Feed: undefined;
  ActivityDetail: { activityId: number };
  Chat: { activityId: number };
  Invite: { activityId: number };
  TemplateSelection: undefined;
  CreateActivity: { template?: ActivityTemplate };
  ContactsUpload: undefined;
  Settings: undefined;
};

const Stack = createStackNavigator<RootStackParamList>();

function AppNavigator() {
  const { user, isLoading } = useUser();
  const [authState, setAuthState] = React.useState<{
    phone: string;
  }>({ phone: '' });

  if (isLoading) {
    return null; // Loading screen
  }

  return (
    <SafeAreaProvider>
      <PaperProvider theme={theme}>
        <StatusBar style="auto" />
        <NavigationContainer>
          <Portal.Host>
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
            {!user ? (
              <>
                <Stack.Screen
                  name="PhoneEntry"
                  options={{ title: 'Welcome to Gatherly', headerShown: false }}
                >
                  {(props) => (
                    <PhoneEntryScreen
                      {...props}
                      onOtpSent={(phone) => {
                        setAuthState({ phone });
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
                        // Navigation handled by UserContext login
                        props.navigation.navigate('Feed');
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
                        props.navigation.navigate('TemplateSelection')
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
                  name="TemplateSelection"
                  options={{ title: 'Choose Template' }}
                >
                  {(props) => (
                    <TemplateSelectionScreen
                      onSelectTemplate={(template) => {
                        props.navigation.navigate('CreateActivity', {
                          template: template || undefined,
                        });
                      }}
                      onCancel={() => props.navigation.goBack()}
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
                      template={props.route.params?.template}
                      onSubmit={(activityId) => {
                        props.navigation.navigate('ActivityDetail', {
                          activityId,
                        });
                      }}
                      onCancel={() => props.navigation.goBack()}
                    />
                  )}
                </Stack.Screen>
                <Stack.Screen
                  name="ContactsUpload"
                  options={{ title: 'Upload Contacts' }}
                >
                  {(props) => (
                    <ContactsUploadScreen
                      {...props}
                      onComplete={() => props.navigation.goBack()}
                      onSkip={() => props.navigation.goBack()}
                    />
                  )}
                </Stack.Screen>
                <Stack.Screen
                  name="Settings"
                  options={{ title: 'Settings' }}
                >
                  {(props) => <SettingsScreen {...props} />}
                </Stack.Screen>
              </>
            )}
          </Stack.Navigator>
        </NavigationContainer>
          </Portal.Host>
      </PaperProvider>
      <Toast />
    </SafeAreaProvider>
  );
}

export default function App() {
  return (
    <ErrorBoundary>
      <UserProvider>
        <AppNavigator />
      </UserProvider>
    </ErrorBoundary>
  );
}

