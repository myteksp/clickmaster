import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { campaignsApi } from '../api/endpoints';
import type { Campaign, CampaignRun, SimulationStats, VisitEvent } from '../api/types';
import { Client } from '@stomp/stompjs';

let stompClient: Client | null = null;

export default function CampaignDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [campaign, setCampaign] = useState<Campaign | null>(null);
  const [runs, setRuns] = useState<CampaignRun[]>([]);
  const [stats, setStats] = useState<SimulationStats | null>(null);
  const [visits, setVisits] = useState<VisitEvent[]>([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    if (!id) return;
    try {
      const [c, r] = await Promise.all([campaignsApi.get(id), campaignsApi.runs(id)]);
      setCampaign(c);
      setRuns(r);
      if (c.status === 'RUNNING') {
        try {
          const s = await campaignsApi.stats(id);
          setStats(s);
        } catch {}
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    const interval = setInterval(load, 3000);
    return () => {
      clearInterval(interval);
      if (stompClient) stompClient.deactivate();
    };
  }, [id]);

  useEffect(() => {
    if (!id || !campaign || campaign.status !== 'RUNNING') return;
    const token = localStorage.getItem('token');
    const runId = runs[0]?.id;
    if (!runId) return;

    const client = new Client({
      brokerURL: `ws://${window.location.host}/ws`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        client.subscribe(`/topic/visits/${runId}`, (msg) => {
          const data = JSON.parse(msg.body);
          if (data.type === 'visit') {
            setVisits((prev) => [data as VisitEvent, ...prev].slice(0, 50));
          }
        });
        client.subscribe(`/topic/stats/${runId}`, (msg) => {
          setStats(JSON.parse(msg.body));
        });
      },
    });
    client.activate();
    stompClient = client;

    return () => { client.deactivate(); stompClient = null; };
  }, [id, campaign?.status, runs]);

  const handleStart = async () => { if (id) { await campaignsApi.start(id); load(); } };
  const handleStop = async () => { if (id) { await campaignsApi.stop(id); load(); } };
  const handlePause = async () => { if (id) { await campaignsApi.pause(id); load(); } };
  const handleResume = async () => { if (id) { await campaignsApi.resume(id); load(); } };
  const handleDelete = async () => {
    if (!id || !confirm('Delete this campaign?')) return;
    await campaignsApi.delete(id);
    navigate('/campaigns');
  };

  if (loading) return <div className="text-gray-400">Loading...</div>;
  if (!campaign) return <div className="text-gray-400">Campaign not found</div>;

  const isRunning = campaign.status === 'RUNNING';
  const isPaused = campaign.status === 'PAUSED';

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-white">{campaign.name}</h1>
          <p className="text-sm text-gray-400">{campaign.siteName} &mdash; {campaign.siteBaseUrl}</p>
        </div>
        <div className="flex gap-2">
          {!isRunning && !isPaused && (
            <button onClick={handleStart} className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-md text-sm font-medium">
              Start
            </button>
          )}
          {isRunning && (
            <>
              <button onClick={handlePause} className="px-4 py-2 bg-yellow-600 hover:bg-yellow-500 text-white rounded-md text-sm font-medium">
                Pause
              </button>
              <button onClick={handleStop} className="px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-md text-sm font-medium">
                Stop
              </button>
            </>
          )}
          {isPaused && (
            <>
              <button onClick={handleResume} className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-md text-sm font-medium">
                Resume
              </button>
              <button onClick={handleStop} className="px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-md text-sm font-medium">
                Stop
              </button>
            </>
          )}
          {!isRunning && (
            <button onClick={handleDelete} className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-gray-300 rounded-md text-sm font-medium">
              Delete
            </button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-4 gap-4 mb-6">
        <StatCard label="Status" value={campaign.status} color="text-emerald-400" />
        <StatCard label="Level" value={campaign.simulationLevel.replace(/_/g, ' ')} color="text-blue-400" />
        <StatCard label="Visits/Hour" value={String(campaign.visitsPerHour)} color="text-purple-400" />
        <StatCard label="Duration" value={`${campaign.durationMinutes}m`} color="text-yellow-400" />
      </div>

      {stats && (
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-4 mb-6">
          <h3 className="text-sm font-semibold text-white mb-3">Live Stats</h3>
          <div className="grid grid-cols-6 gap-4 text-center">
            <div><p className="text-2xl font-bold text-white">{stats.totalVisits}</p><p className="text-xs text-gray-400">Total</p></div>
            <div><p className="text-2xl font-bold text-emerald-400">{stats.successfulVisits}</p><p className="text-xs text-gray-400">Success</p></div>
            <div><p className="text-2xl font-bold text-red-400">{stats.failedVisits}</p><p className="text-xs text-gray-400">Failed</p></div>
            <div><p className="text-2xl font-bold text-blue-400">{stats.activeProxies}</p><p className="text-xs text-gray-400">Proxies</p></div>
            <div><p className="text-2xl font-bold text-purple-400">{stats.visitsPerSecond}</p><p className="text-xs text-gray-400">Visits/s</p></div>
            <div><p className="text-2xl font-bold text-gray-300">{stats.elapsedTime}</p><p className="text-xs text-gray-400">Elapsed</p></div>
          </div>
        </div>
      )}

      {isRunning && visits.length > 0 && (
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-4 mb-6">
          <h3 className="text-sm font-semibold text-white mb-3">Live Visits</h3>
          <div className="space-y-1 max-h-64 overflow-y-auto">
            {visits.map((v, i) => (
              <div key={i} className="flex items-center gap-3 text-xs font-mono">
                <span className={v.success ? 'text-emerald-400' : 'text-red-400'}>
                  {v.success ? 'OK' : 'ERR'}
                </span>
                <span className="text-gray-400">{v.statusCode || '--'}</span>
                <span className="text-gray-500">{v.responseTimeMs}ms</span>
                <span className="text-gray-500">{v.path}</span>
                <span className="text-gray-600">{v.proxyAddress}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="bg-gray-900 border border-gray-800 rounded-lg">
        <div className="px-4 py-3 border-b border-gray-800">
          <h3 className="text-sm font-semibold text-white">Run History</h3>
        </div>
        {runs.length === 0 ? (
          <div className="p-4 text-center text-gray-500 text-sm">No runs yet</div>
        ) : (
          <div className="divide-y divide-gray-800">
            {runs.map((r) => (
              <div key={r.id} className="px-4 py-2 flex items-center justify-between text-sm">
                <div>
                  <span className={`inline-block w-2 h-2 rounded-full mr-2 ${
                    r.status === 'RUNNING' ? 'bg-emerald-400' :
                    r.status === 'COMPLETED' ? 'bg-blue-400' :
                    r.status === 'FAILED' ? 'bg-red-400' : 'bg-gray-500'
                  }`} />
                  <span className="text-gray-300">{r.status}</span>
                </div>
                <div className="text-gray-500 text-xs">
                  {r.totalVisits} visits ({r.successfulVisits} ok, {r.failedVisits} fail)
                </div>
                <div className="text-gray-600 text-xs">
                  {new Date(r.startedAt).toLocaleString()}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function StatCard({ label, value, color }: { label: string; value: string; color: string }) {
  return (
    <div className="bg-gray-900 border border-gray-800 rounded-lg p-3">
      <p className="text-xs text-gray-500">{label}</p>
      <p className={`text-lg font-semibold ${color}`}>{value}</p>
    </div>
  );
}
