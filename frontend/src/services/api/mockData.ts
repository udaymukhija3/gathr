import { Activity, Hub, Message } from '../../types';

export const MOCK_HUBS: Hub[] = [
  { id: 1, name: 'Cyberhub', area: 'Cyber City', description: 'A bustling hub with restaurants, cafes, and entertainment venues', latitude: 28.4946, longitude: 77.0895 },
  { id: 2, name: 'Galleria', area: 'DLF Galleria', description: 'Shopping and dining destination in the heart of Gurgaon', latitude: 28.4595, longitude: 77.0723 },
  { id: 3, name: '32nd Avenue', area: 'Sector 32', description: 'Popular food and nightlife street', latitude: 28.4593, longitude: 77.0672 },
];

export const MOCK_ACTIVITIES: Activity[] = [
  {
    id: 1,
    title: 'Coffee at Galleria',
    hubId: 2,
    hubName: 'Galleria',
    locationName: 'Third Wave Coffee, Galleria',
    locationAddress: 'Galleria Market, Gurugram',
    latitude: 28.4595,
    longitude: 77.0266,
    isUserLocation: false,
    category: 'FOOD',
    startTime: '2025-11-12T18:30:00+05:30',
    endTime: '2025-11-12T20:00:00+05:30',
    createdBy: 1,
    createdByName: 'Test User',
    peopleCount: 7,
    mutualsCount: 2,
    isInviteOnly: true,
  },
  {
    id: 2,
    title: 'Pickleball Session',
    hubId: 1,
    hubName: 'Cyberhub',
    locationName: 'Cyberhub Pickleball Court',
    locationAddress: 'Cyber City, Gurugram',
    latitude: 28.4946,
    longitude: 77.0886,
    isUserLocation: false,
    category: 'SPORTS',
    startTime: '2025-11-12T19:00:00+05:30',
    endTime: '2025-11-12T21:00:00+05:30',
    createdBy: 1,
    createdByName: 'Test User',
    peopleCount: 5,
    mutualsCount: 1,
    isInviteOnly: false,
  },
  {
    id: 3,
    title: 'Pottery Workshop',
    hubId: 3,
    hubName: '32nd Avenue',
    locationName: 'Clay Station, 32nd Avenue',
    locationAddress: '32nd Avenue, Sector 15',
    latitude: 28.4591,
    longitude: 77.0679,
    isUserLocation: false,
    category: 'ART',
    startTime: '2025-11-12T18:00:00+05:30',
    endTime: '2025-11-12T19:30:00+05:30',
    createdBy: 1,
    createdByName: 'Test User',
    peopleCount: 6,
    mutualsCount: 3,
    isInviteOnly: true,
  },
  {
    id: 4,
    title: 'Badminton Night',
    hubId: 1,
    hubName: 'Cyberhub',
    locationName: 'DLF Badminton Arena',
    locationAddress: 'Cyberhub Sports Complex',
    latitude: 28.4951,
    longitude: 77.0901,
    isUserLocation: false,
    category: 'SPORTS',
    startTime: '2025-11-12T20:00:00+05:30',
    endTime: '2025-11-12T21:30:00+05:30',
    createdBy: 1,
    createdByName: 'Test User',
    peopleCount: 4,
    mutualsCount: 0,
    isInviteOnly: false,
  },
  {
    id: 5,
    title: 'Street Cricket',
    hubId: 1,
    hubName: 'Cyberhub',
    locationName: 'Cyberhub Parking Lot',
    locationAddress: 'Cyber City',
    latitude: 28.494,
    longitude: 77.087,
    isUserLocation: false,
    category: 'SPORTS',
    startTime: '2025-11-12T21:00:00+05:30',
    endTime: '2025-11-12T22:30:00+05:30',
    createdBy: 1,
    createdByName: 'Test User',
    peopleCount: 9,
    mutualsCount: 4,
    isInviteOnly: false,
  },
];

export const MOCK_MESSAGES: Record<number, Message[]> = {
  1: [
    {
      id: 1,
      activityId: 1,
      userId: 1,
      userName: 'User #23',
      text: 'Hey! Looking forward to this!',
      createdAt: new Date().toISOString(),
    },
    {
      id: 2,
      activityId: 1,
      userId: 2,
      userName: 'User #45',
      text: 'Same here! See you there.',
      createdAt: new Date().toISOString(),
    },
  ],
};

const EARTH_RADIUS_KM = 6371;

export const distanceBetween = (lat1: number, lon1: number, lat2: number, lon2: number) => {
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const rLat1 = toRad(lat1);
  const rLat2 = toRad(lat2);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(rLat1) * Math.cos(rLat2) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return EARTH_RADIUS_KM * c;
};

