import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { campaignsApi } from '../api/endpoints';
import type { Campaign, SimulationStats, VisitEvent } from '../api/types';
import { Client } from '@stomp/stompjs';

let stompClient: Client | null = null;

function connectWebSocket(runId: string, onVisit: (e: VisitEvent) => void, onStats: (s: SimulationStats) => void) {
  if (stompClient) stompClient.deactivate();

  const token = localStorage.getItem('token');
  const client = new Client({
    brokerURL: `ws://${window.location.host}/ws`,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(`/topic/visits/${runId}`, (msg) => {
        const data = JSON.parse(msg.body);
        if (data.type === 'visit') onVisit(data as VisitEvent);
      });
      client.subscribe(`/topic/stats/${runId}`, (msg) => {
        onStats(JSON.parse(msg.body));
      });
    },
  });
  client.activate();
  stompClient = client;
}

function disconnectWebSocket() {
  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
  }
}

export default function DashboardPage() {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState(true);

  const loadCampaigns = async () => {
    try {
      const data = await campaignsApi.list();
      setCampaigns(data);
    } catch (err) {
      console.error('Failed to load campaigns', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCampaigns();
    const interval = setInterval(loadCampaigns, 5000);
    return () => {
      clearInterval(interval);
      disconnectWebSocket();
    };
  }, []);

  const running = campaigns.filter((c) => c.status === 'RUNNING').length;
  const totalRuns = campaigns.length;
  const active = campaigns.filter((c) => c.status === 'RUNNING' || c.status === 'PAUSED').length;

  const statusBadge = (status: string) => {
    const colors: Record<string, string> = {
      RUNNING: 'bg-emerald-900/50 text-emerald-300 border-emerald-700',
      PAUSED: 'bg-yellow-900/50 text-yellow-300 border-yellow-700',
      COMPLETED: 'bg-blue-900/50 text-blue-300 border-blue-700',
      FAILED: 'bg-red-900/50 text-red-300 border-red-700',
      DRAFT: 'bg-gray-800 text-gray-400 border-gray-700',
      READY: 'bg-indigo-900/50 text-indigo-300 border-indigo-700',
      CANCELLED: 'bg-gray-800 text-gray-500 border-gray-700',
    };
    return (
      <span className={`text-xs px-2 py-0.5 rounded border ${colors[status] || colors.DRAFT}`}>
        {status}
      </span>
    );
  };

  if (loading) {
    return <div className="text-gray-400">Loading...</div>;
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-white">Dashboard</h1>
        <Link
          to="/campaigns/new"
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-md text-sm font-medium transition-colors"
        >
          New Campaign
        </Link>
      </div>

      <div className="grid grid-cols-3 gap-4 mb-8">
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-4">
          <p className="text-sm text-gray-400">Running</p>
          <p className="text-3xl font-bold text-emerald-400">{running}</p>
        </div>
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-4">
          <p className="text-sm text-gray-400">Active</p>
          <p className="text-3xl font-bold text-yellow-400">{active}</p>
        </div>
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-4">
          <p className="text-sm text-gray-400">Total Campaigns</p>
          <p className="text-3xl font-bold text-white">{totalRuns}</p>
        </div>
      </div>

      <div className="bg-gray-900 border border-gray-800 rounded-lg">
        <div className="px-4 py-3 border-b border-gray-800">
          <h2 className="text-lg font-semibold text-white">Recent Campaigns</h2>
        </div>
        {campaigns.length === 0 ? (
          <div className="p-6 text-center text-gray-500">
            No campaigns yet.{' '}
            <Link to="/campaigns/new" className="text-emerald-400 hover:text-emerald-300">
              Create your first campaign
            </Link>
          </div>
        ) : (
          <div className="divide-y divide-gray-800">
            {campaigns.slice(0, 10).map((campaign) => (
              <Link
                key={campaign.id}
                to={`/campaigns/${campaign.id}`}
                className="flex items-center justify-between px-4 py-3 hover:bg-gray-800/50 transition-colors"
              >
                <div>
                  <p className="text-sm font-medium text-white">{campaign.name}</p>
                  <p className="text-xs text-gray-500">
                    {campaign.siteName} &middot; {campaign.simulationLevel.replace('_', ' ')} &middot;{' '}
                    {campaign.visitsPerHour}/h
                  </p>
                </div>
                {statusBadge(campaign.status)}
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
