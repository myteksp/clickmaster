import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { campaignsApi } from '../api/endpoints';
import type { Campaign, CampaignRun } from '../api/types';
import { useStomp } from '../hooks/useStomp';
import StatusBadge from '../components/StatusBadge';
import StatCard from '../components/StatCard';
import LoadingSpinner from '../components/LoadingSpinner';
import ConfirmDialog from '../components/ConfirmDialog';
import { useToast } from '../components/Toast';

export default function CampaignDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useToast();

  const [campaign, setCampaign] = useState<Campaign | null>(null);
  const [runs, setRuns] = useState<CampaignRun[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [showStop, setShowStop] = useState(false);
  const [showDelete, setShowDelete] = useState(false);
  const [visitFilter, setVisitFilter] = useState<'ALL' | 'SUCCESS' | 'FAILED'>('ALL');

  const isRunning = campaign?.status === 'RUNNING';
  const activeRunId = runs.find((r) => r.status === 'RUNNING')?.id ?? null;

  const { stats, visits, connected } = useStomp({
    runId: activeRunId,
    enabled: isRunning,
  });

  const load = useCallback(async () => {
    if (!id) return;
    try {
      const [c, r] = await Promise.all([campaignsApi.get(id), campaignsApi.runs(id)]);
      setCampaign(c);
      setRuns(r);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to load campaign');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    load();
    if (isRunning) {
      const interval = setInterval(load, 10000);
      return () => clearInterval(interval);
    }
  }, [id, isRunning, load]);

  const handleAction = async (action: 'start' | 'stop' | 'pause' | 'resume') => {
    if (!id) return;
    setActionLoading(true);
    try {
      if (action === 'start') await campaignsApi.start(id);
      else if (action === 'stop') await campaignsApi.stop(id);
      else if (action === 'pause') await campaignsApi.pause(id);
      else if (action === 'resume') await campaignsApi.resume(id);
      toast.success(`Campaign ${action}${action.endsWith('e') ? 'd' : 'ed'}`);
      await load();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : `Failed to ${action}`);
    } finally {
      setActionLoading(false);
      setShowStop(false);
    }
  };

  const handleDelete = async () => {
    if (!id) return;
    try {
      await campaignsApi.delete(id);
      toast.success('Campaign deleted');
      navigate('/campaigns');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to delete');
      setShowDelete(false);
    }
  };

  if (loading) return <LoadingSpinner />;
  if (!campaign) return <div className="text-gray-400">Campaign not found</div>;

  const isPaused = campaign.status === 'PAUSED';
  const liveStats = stats || (runs[0] ? {
    totalVisits: runs[0].totalVisits,
    successfulVisits: runs[0].successfulVisits,
    failedVisits: runs[0].failedVisits,
    activeProxies: 0,
    visitsPerSecond: 0,
    elapsedTime: '',
    campaignId: campaign.id,
    campaignRunId: runs[0].id,
    estimatedRemaining: 'N/A',
  } : null);

  const successRate = liveStats && liveStats.totalVisits > 0
    ? Math.round((liveStats.successfulVisits / liveStats.totalVisits) * 100)
    : null;

  const filteredVisits = visitFilter === 'ALL'
    ? visits
    : visits.filter((v) => visitFilter === 'SUCCESS' ? v.success : !v.success);

  return (
    <div>
      <div className="flex items-start justify-between mb-6 gap-4">
        <div className="min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <Link to="/campaigns" className="text-xs text-gray-500 hover:text-gray-300">Campaigns</Link>
            <span className="text-gray-700">/</span>
          </div>
          <h1 className="text-2xl font-bold text-white truncate">{campaign.name}</h1>
          <p className="text-sm text-gray-400 mt-0.5">
            {campaign.siteName} &mdash; {campaign.siteBaseUrl}
          </p>
          <div className="flex items-center gap-3 mt-2">
            <StatusBadge status={campaign.status} />
            {isRunning && (
              <span className={`flex items-center gap-1 text-xs ${connected ? 'text-emerald-400' : 'text-gray-500'}`}>
                <span className={`w-1.5 h-1.5 rounded-full ${connected ? 'bg-emerald-400 animate-pulse' : 'bg-gray-600'}`} />
                {connected ? 'Live' : 'Connecting...'}
              </span>
            )}
          </div>
        </div>
        <div className="flex gap-2 flex-shrink-0">
          {!isRunning && !isPaused && (
            <button
              onClick={() => handleAction('start')}
              disabled={actionLoading}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-md text-sm font-medium disabled:opacity-50"
            >
              Start
            </button>
          )}
          {isRunning && (
            <>
              <button
                onClick={() => handleAction('pause')}
                disabled={actionLoading}
                className="px-4 py-2 bg-yellow-600 hover:bg-yellow-500 text-white rounded-md text-sm font-medium disabled:opacity-50"
              >
                Pause
              </button>
              <button
                onClick={() => setShowStop(true)}
                disabled={actionLoading}
                className="px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-md text-sm font-medium disabled:opacity-50"
              >
                Stop
              </button>
            </>
          )}
          {isPaused && (
            <>
              <button
                onClick={() => handleAction('resume')}
                disabled={actionLoading}
                className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-md text-sm font-medium disabled:opacity-50"
              >
                Resume
              </button>
              <button
                onClick={() => setShowStop(true)}
                disabled={actionLoading}
                className="px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-md text-sm font-medium disabled:opacity-50"
              >
                Stop
              </button>
            </>
          )}
          <Link
            to={`/campaigns/${campaign.id}/edit`}
            className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-gray-300 rounded-md text-sm font-medium"
          >
            Edit
          </Link>
          {!isRunning && (
            <button
              onClick={() => setShowDelete(true)}
              className="px-4 py-2 bg-gray-800 hover:bg-red-900 text-gray-400 hover:text-red-300 rounded-md text-sm font-medium"
            >
              Delete
            </button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
        <StatCard label="Level" value={campaign.simulationLevel.replace(/_/g, ' ')} color="text-blue-400" />
        <StatCard label="Rate" value={`${campaign.visitsPerHour}/h`} color="text-purple-400" />
        <StatCard label="Duration" value={`${campaign.durationMinutes}m`} color="text-yellow-400" />
        <StatCard
          label="Success Rate"
          value={successRate !== null ? `${successRate}%` : '--'}
          color={successRate !== null && successRate > 80 ? 'text-emerald-400' : 'text-yellow-400'}
        />
      </div>

      {liveStats && (
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-4 mb-6">
          <h3 className="text-sm font-semibold text-white mb-3">
            {isRunning ? 'Live Stats' : 'Final Stats'}
          </h3>
          <div className="grid grid-cols-3 md:grid-cols-6 gap-4 text-center">
            <div>
              <p className="text-2xl font-bold text-white">{liveStats.totalVisits}</p>
              <p className="text-xs text-gray-500">Total</p>
            </div>
            <div>
              <p className="text-2xl font-bold text-emerald-400">{liveStats.successfulVisits}</p>
              <p className="text-xs text-gray-500">Success</p>
            </div>
            <div>
              <p className="text-2xl font-bold text-red-400">{liveStats.failedVisits}</p>
              <p className="text-xs text-gray-500">Failed</p>
            </div>
            <div>
              <p className="text-2xl font-bold text-blue-400">{liveStats.activeProxies}</p>
              <p className="text-xs text-gray-500">Proxies</p>
            </div>
            <div>
              <p className="text-2xl font-bold text-purple-400">{liveStats.visitsPerSecond}</p>
              <p className="text-xs text-gray-500">Visits/s</p>
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-300">{liveStats.elapsedTime || '--'}</p>
              <p className="text-xs text-gray-500">Elapsed</p>
            </div>
          </div>
        </div>
      )}

      {isRunning && visits.length > 0 && (
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-4 mb-6">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-semibold text-white">Live Visit Feed</h3>
            <div className="flex gap-1">
              {(['ALL', 'SUCCESS', 'FAILED'] as const).map((f) => (
                <button
                  key={f}
                  onClick={() => setVisitFilter(f)}
                  className={`px-2 py-0.5 text-xs rounded ${
                    visitFilter === f
                      ? f === 'SUCCESS' ? 'bg-emerald-900/60 text-emerald-400' : f === 'FAILED' ? 'bg-red-900/60 text-red-400' : 'bg-gray-800 text-white'
                      : 'text-gray-500 hover:text-gray-300'
                  }`}
                >
                  {f === 'ALL' ? 'All' : f === 'SUCCESS' ? 'OK' : 'Fail'}
                </button>
              ))}
            </div>
          </div>
          <div className="space-y-1 max-h-64 overflow-y-auto">
            {filteredVisits.map((v, i) => (
              <div key={`${i}-${v.responseTimeMs}`} className="flex items-center gap-3 text-xs font-mono py-0.5">
                <span className={v.success ? 'text-emerald-400' : 'text-red-400'}>
                  {v.success ? 'OK ' : 'ERR'}
                </span>
                <span className="text-gray-400 w-8">{v.statusCode || '--'}</span>
                <span className="text-gray-500 w-16">{v.responseTimeMs}ms</span>
                <span className="text-gray-500 flex-1 truncate">{v.path}</span>
                <span className="text-gray-600 hidden sm:inline">{v.proxyAddress}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="bg-gray-900 border border-gray-800 rounded-lg overflow-hidden">
        <div className="px-4 py-3 border-b border-gray-800">
          <h3 className="text-sm font-semibold text-white">Run History</h3>
        </div>
        {runs.length === 0 ? (
          <div className="p-6 text-center text-gray-500 text-sm">No runs yet</div>
        ) : (
          <div className="divide-y divide-gray-800">
            {runs.map((r) => (
              <div key={r.id} className="px-4 py-3 flex items-center justify-between text-sm">
                <div className="flex items-center gap-2">
                  <span className={`inline-block w-2 h-2 rounded-full ${
                    r.status === 'RUNNING' ? 'bg-emerald-400 animate-pulse' :
                    r.status === 'COMPLETED' ? 'bg-blue-400' :
                    r.status === 'FAILED' ? 'bg-red-400' : 'bg-gray-500'
                  }`} />
                  <span className="text-gray-300">{r.status}</span>
                </div>
                <div className="text-gray-500 text-xs">
                  {r.totalVisits} visits &middot; {r.successfulVisits} ok &middot; {r.failedVisits} fail
                </div>
                <div className="text-gray-600 text-xs hidden sm:block">
                  {new Date(r.startedAt).toLocaleString()}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <ConfirmDialog
        open={showStop}
        title="Stop Campaign"
        message="This will stop all running visits and release proxy ports. The campaign cannot be resumed."
        confirmLabel="Stop"
        destructive
        onConfirm={() => handleAction('stop')}
        onCancel={() => setShowStop(false)}
      />
      <ConfirmDialog
        open={showDelete}
        title="Delete Campaign"
        message={`Delete "${campaign.name}"? This permanently removes the campaign and all run history.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setShowDelete(false)}
      />
    </div>
  );
}
