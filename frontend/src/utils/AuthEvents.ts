type AuthEventType = 'unauthorized';
type AuthEventHandler = () => void;

class AuthEventEmitter {
  private listeners: Record<AuthEventType, AuthEventHandler[]> = {
    unauthorized: [],
  };

  on(event: AuthEventType, handler: AuthEventHandler) {
    if (!this.listeners[event]) {
      this.listeners[event] = [];
    }
    this.listeners[event].push(handler);
  }

  off(event: AuthEventType, handler: AuthEventHandler) {
    if (!this.listeners[event]) return;
    this.listeners[event] = this.listeners[event].filter((h) => h !== handler);
  }

  emit(event: AuthEventType) {
    if (!this.listeners[event]) return;
    this.listeners[event].forEach((handler) => handler());
  }
}

export const authEvents = new AuthEventEmitter();
