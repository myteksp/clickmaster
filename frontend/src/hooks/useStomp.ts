import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { SimulationStats, VisitEvent } from '../api/types';

interface UseStompOptions {
  runId: string | null;
  enabled: boolean;
}

export function useStomp({ runId, enabled }: UseStompOptions) {
  const clientRef = useRef<Client | null>(null);
  const [stats, setStats] = useState<SimulationStats | null>(null);
  const [visits, setVisits] = useState<VisitEvent[]>([]);
  const [connected, setConnected] = useState(false);

  const pushVisit = useCallback((visit: VisitEvent) => {
    setVisits((prev) => [visit, ...prev].slice(0, 100));
  }, []);

  useEffect(() => {
    if (!enabled || !runId) return;

    const token = localStorage.getItem('token');
    const wsUrl = `${window.location.protocol === 'https:' ? 'https' : 'http'}://${window.location.host}/ws`;

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        setConnected(true);

        client.subscribe(`/topic/stats/${runId}`, (msg) => {
          try {
            setStats(JSON.parse(msg.body));
          } catch {}
        });

        client.subscribe(`/topic/visits/${runId}`, (msg) => {
          try {
            const data = JSON.parse(msg.body);
            pushVisit(data as VisitEvent);
          } catch {}
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (_frame) => {
        setConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [runId, enabled, pushVisit]);

  const clearVisits = useCallback(() => setVisits([]), []);

  return { stats, visits, connected, clearVisits };
}
