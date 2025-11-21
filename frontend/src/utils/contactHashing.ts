import { sha256 } from 'js-sha256';
import * as Contacts from 'expo-contacts';

/**
 * Normalize phone number to E.164 format (simple heuristic)
 * This is a basic implementation - production should use a proper library
 */
export const normalizePhoneNumber = (phone: string): string => {
  // Remove all non-digit characters
  let cleaned = phone.replace(/\D/g, '');

  // If starts with 0, replace with country code (India: +91)
  if (cleaned.startsWith('0')) {
    cleaned = '91' + cleaned.substring(1);
  }

  // If doesn't start with country code and is 10 digits, assume India (+91)
  if (cleaned.length === 10) {
    cleaned = '91' + cleaned;
  }

  // Ensure it starts with +
  if (!cleaned.startsWith('+')) {
    cleaned = '+' + cleaned;
  }

  return cleaned;
};

/**
 * Hash a phone number using SHA-256
 */
export const hashPhoneNumber = (phone: string): string => {
  const normalized = normalizePhoneNumber(phone);
  return sha256(normalized.toLowerCase().trim());
};

/**
 * Read contacts and return hashed phone numbers
 */
export const getHashedContacts = async (): Promise<string[]> => {
  try {
    const { status } = await Contacts.requestPermissionsAsync();
    if (status !== 'granted') {
      throw new Error('Contacts permission not granted');
    }

    const { data } = await Contacts.getContactsAsync({
      fields: [Contacts.Fields.PhoneNumbers],
    });

    const hashes = new Set<string>();

    data.forEach((contact) => {
      contact.phoneNumbers?.forEach((phoneNumber) => {
        const normalized = normalizePhoneNumber(phoneNumber.number);
        const hash = hashPhoneNumber(normalized);
        hashes.add(hash);
      });
    });

    return Array.from(hashes);
  } catch (error) {
    console.error('Error reading contacts:', error);
    throw error;
  }
};

