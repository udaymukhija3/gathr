import { useState, useEffect, useRef, useCallback } from 'react';
import { useUser } from '../context/UserContext';

const API_WS_BASE = process.env.EXPO_PUBLIC_WS_URL || 'ws://localhost:8080';

export interface WebSocketMessage {
  type: 'message' | 'join' | 'leave' | 'heading_now' | 'reveal';
  payload: any;
}

interface UseWebSocketOptions {
  activityId: number;
  onMessage?: (message: WebSocketMessage) => void;
  onError?: (error: Event) => void;
  enabled?: boolean;
}

export const useWebSocket = (options: UseWebSocketOptions) => {
  const { token } = useUser();
  const { activityId, onMessage, onError, enabled = true } = options;
  const [isConnected, setIsConnected] = useState(false);
  const [shouldReconnect, setShouldReconnect] = useState(true);
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const reconnectAttempts = useRef(0);
  const maxReconnectAttempts = 5;

  const connect = useCallback(() => {
    if (!enabled || !token) return;

    try {
      const wsUrl = `${API_WS_BASE}/ws/activities/${activityId}?token=${token}`;
      const ws = new WebSocket(wsUrl);

      ws.onopen = () => {
        console.log('WebSocket connected');
        setIsConnected(true);
        reconnectAttempts.current = 0;
      };

      ws.onmessage = (event) => {
        try {
          const data: WebSocketMessage = JSON.parse(event.data);
          onMessage?.(data);
        } catch (error) {
          console.error('Error parsing WebSocket message:', error);
        }
      };

      ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        setIsConnected(false);
        onError?.(error);
      };

      ws.onclose = () => {
        console.log('WebSocket closed');
        setIsConnected(false);

        if (shouldReconnect && reconnectAttempts.current < maxReconnectAttempts) {
          const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.current), 8000);
          reconnectAttempts.current++;
          reconnectTimeoutRef.current = setTimeout(() => {
            connect();
          }, delay);
        }
      };

      wsRef.current = ws;
    } catch (error) {
      console.error('Error creating WebSocket:', error);
      setIsConnected(false);
    }
  }, [activityId, token, enabled, onMessage, onError, shouldReconnect]);

  const disconnect = useCallback(() => {
    setShouldReconnect(false);
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
  }, []);

  const sendMessage = useCallback((message: WebSocketMessage) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message));
      return true;
    }
    return false;
  }, []);

  useEffect(() => {
    if (enabled) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [enabled, connect, disconnect]);

  return {
    isConnected,
    sendMessage,
    disconnect,
    reconnect: connect,
  };
};

