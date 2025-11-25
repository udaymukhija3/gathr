import React from 'react';
import { View, StyleSheet } from 'react-native';
import { ActivityIndicator } from 'react-native-paper';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useUser } from '../context/UserContext';
import { PhoneEntryScreen } from '../screens/PhoneEntryScreen';
import { OtpVerifyScreen } from '../screens/OtpVerifyScreen';
import { OnboardingScreen } from '../screens/OnboardingScreen';
import { FeedScreen } from '../screens/FeedScreen';
import { MapViewScreen } from '../screens/MapViewScreen';
import { TemplateSelectionScreen } from '../screens/TemplateSelectionScreen';
import { ChatsScreen } from '../screens/ChatsScreen';
import { ProfileScreen } from '../screens/ProfileScreen';
import { ActivityDetailScreen } from '../screens/ActivityDetailScreen';
import { ChatScreen } from '../screens/ChatScreen';
import { InviteScreen } from '../screens/InviteScreen';
import { CreateActivityScreen } from '../screens/CreateActivityScreen';
import { ContactsUploadScreen } from '../screens/ContactsUploadScreen';
import { SettingsScreen } from '../screens/SettingsScreen';
import { NotificationsScreen } from '../screens/NotificationsScreen';
import { MyActivitiesScreen } from '../screens/MyActivitiesScreen';
import { ActivityTemplate } from '../types';

type AuthStackParamList = {
  PhoneEntry: undefined;
  OtpVerify: { phone: string };
};

type OnboardingStackParamList = {
  Onboarding: undefined;
};

type MainTabParamList = {
  Home: undefined;
  Map: undefined;
  Create: undefined;
  Chat: undefined;
  Profile: undefined;
};

export type AppStackParamList = {
  Tabs: undefined;
  ActivityDetail: { activityId: number };
  Chat: { activityId: number };
  Invite: { activityId: number };
  TemplateSelection: undefined;
  CreateActivity: { template?: ActivityTemplate };
  ContactsUpload: undefined;
  Settings: undefined;
  MyActivities: undefined;
  Notifications: undefined;
};

const AuthStack = createStackNavigator<AuthStackParamList>();
const OnboardingStack = createStackNavigator<OnboardingStackParamList>();
const AppStack = createStackNavigator<AppStackParamList>();
const Tab = createBottomTabNavigator<MainTabParamList>();

export const AppNavigator: React.FC = () => {
  const { user, isLoading } = useUser();
  const hasCompletedOnboarding = Boolean(user?.onboardingCompleted);
  const shouldShowOnboarding = Boolean(user && !hasCompletedOnboarding);

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <NavigationContainer>
      {!user ? (
        <AuthNavigator />
      ) : shouldShowOnboarding ? (
        <OnboardingNavigator />
      ) : (
        <AuthenticatedNavigator />
      )}
    </NavigationContainer>
  );
};

const AuthNavigator = () => (
  <AuthStack.Navigator
    screenOptions={{
      headerShown: false,
    }}
  >
    <AuthStack.Screen name="PhoneEntry">
      {(props) => (
        <PhoneEntryScreen
          {...props}
          onOtpSent={(phone) =>
            props.navigation.navigate('OtpVerify', { phone })
          }
        />
      )}
    </AuthStack.Screen>
    <AuthStack.Screen name="OtpVerify">
      {(props) => (
        <OtpVerifyScreen
          {...props}
          phone={props.route.params.phone}
          onVerified={() => {
            // UserContext will handle navigation once logged in
          }}
          onBack={() => props.navigation.goBack()}
        />
      )}
    </AuthStack.Screen>
  </AuthStack.Navigator>
);

const OnboardingNavigator = () => (
  <OnboardingStack.Navigator screenOptions={{ headerShown: false }}>
    <OnboardingStack.Screen name="Onboarding">
      {(props) => <OnboardingScreen {...props} onComplete={() => {}} />}
    </OnboardingStack.Screen>
  </OnboardingStack.Navigator>
);

const AuthenticatedNavigator = () => (
  <AppStack.Navigator
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
    <AppStack.Screen
      name="Tabs"
      component={MainTabs}
      options={{ headerShown: false }}
    />
    <AppStack.Screen
      name="ActivityDetail"
      component={ActivityDetailScreen}
      options={{ title: 'Activity Details' }}
    />
    <AppStack.Screen
      name="Chat"
      component={ChatScreen}
      options={{ title: 'Group Chat' }}
    />
    <AppStack.Screen
      name="Invite"
      component={InviteScreen}
      options={{ title: 'Invite Friends' }}
    />
    <AppStack.Screen
      name="TemplateSelection"
      component={TemplateSelectionScreen}
      options={{ title: 'Choose Template' }}
    />
    <AppStack.Screen
      name="CreateActivity"
      component={CreateActivityScreen}
      options={{ title: 'Create Activity' }}
    />
    <AppStack.Screen
      name="ContactsUpload"
      component={ContactsUploadScreen}
      options={{ title: 'Upload Contacts' }}
    />
    <AppStack.Screen
      name="Settings"
      component={SettingsScreen}
      options={{ title: 'Settings' }}
    />
    <AppStack.Screen
      name="MyActivities"
      component={MyActivitiesScreen}
      options={{ title: 'My Activities' }}
    />
    <AppStack.Screen
      name="Notifications"
      component={NotificationsScreen}
      options={{ title: 'Notifications' }}
    />
  </AppStack.Navigator>
);

const MainTabs = () => (
  <Tab.Navigator
    screenOptions={({ route }) => ({
      headerShown: false,
      tabBarActiveTintColor: '#6200EE',
      tabBarInactiveTintColor: '#888',
      tabBarLabelStyle: styles.tabLabel,
      tabBarIcon: ({ color, size }) => (
        <MaterialCommunityIcons
          name={getTabIcon(route.name as keyof MainTabParamList)}
          color={color}
          size={size}
        />
      ),
    })}
  >
    <Tab.Screen
      name="Home"
      options={{ tabBarLabel: 'Home', tabBarTestID: 'tab-home' }}
    >
      {(props) => (
        <FeedScreen
          {...props}
          onCreateActivity={() =>
            props.navigation.getParent()?.navigate('TemplateSelection')
          }
          onActivityPress={(activityId) =>
            props.navigation
              .getParent()
              ?.navigate('ActivityDetail', { activityId })
          }
        />
      )}
    </Tab.Screen>
    <Tab.Screen
      name="Map"
      options={{ tabBarLabel: 'Map', tabBarTestID: 'tab-map' }}
    >
      {(props) => (
        <MapViewScreen
          {...props}
          onActivitySelect={(activityId) =>
            props.navigation
              .getParent()
              ?.navigate('ActivityDetail', { activityId })
          }
        />
      )}
    </Tab.Screen>
    <Tab.Screen
      name="Create"
      options={{ tabBarLabel: 'Create', tabBarTestID: 'tab-create' }}
    >
      {(props) => (
        <TemplateSelectionScreen
          onSelectTemplate={(template) =>
            props.navigation
              .getParent()
              ?.navigate('CreateActivity', {
                template: template || undefined,
              })
          }
          onCancel={() => props.navigation.navigate('Home')}
        />
      )}
    </Tab.Screen>
    <Tab.Screen
      name="Chat"
      options={{ tabBarLabel: 'Chat', tabBarTestID: 'tab-chat' }}
    >
      {(props) => (
        <ChatsScreen
          {...props}
          onOpenChat={(activityId) =>
            props.navigation.getParent()?.navigate('Chat', { activityId })
          }
        />
      )}
    </Tab.Screen>
    <Tab.Screen
      name="Profile"
      options={{ tabBarLabel: 'Profile', tabBarTestID: 'tab-profile' }}
    >
      {(props) => (
        <ProfileScreen
          {...props}
          onSettingsPress={() =>
            props.navigation.getParent()?.navigate('Settings')
          }
        />
      )}
    </Tab.Screen>
  </Tab.Navigator>
);

const getTabIcon = (route: keyof MainTabParamList) => {
  switch (route) {
    case 'Home':
      return 'home-variant';
    case 'Map':
      return 'map-search';
    case 'Create':
      return 'plus-circle';
    case 'Chat':
      return 'message-text';
    case 'Profile':
    default:
      return 'account-circle';
  }
};

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  tabLabel: {
    fontSize: 12,
  },
});


