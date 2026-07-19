import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { campaignsApi } from '../api/endpoints';
import type { Campaign } from '../api/types';
import StatusBadge from '../components/StatusBadge';
import StatCard from '../components/StatCard';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';

export default function DashboardPage() {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        setCampaigns(await campaignsApi.list());
      } catch {
      } finally {
        setLoading(false);
      }
    };
    load();
    const interval = setInterval(load, 10000);
    return () => clearInterval(interval);
  }, []);

  if (loading) return <LoadingSpinner />;

  const running = campaigns.filter((c) => c.status === 'RUNNING');
  const completed = campaigns.filter((c) => c.status === 'COMPLETED');
  const totalVisits = completed.reduce((sum, c) => sum + (c.lastRunAt ? 1 : 0), 0);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-white">Dashboard</h1>
        <Link
          to="/campaigns/new"
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-md text-sm font-medium transition-colors"
        >
          + New Campaign
        </Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        <StatCard
          label="Running Now"
          value={running.length}
          color="text-emerald-400"
          subtext={running.length > 0 ? `${running.length} active campaign${running.length > 1 ? 's' : ''}` : 'No active campaigns'}
        />
        <StatCard
          label="Total Campaigns"
          value={campaigns.length}
          color="text-white"
          subtext={`${completed.length} completed`}
        />
        <StatCard
          label="Total Runs"
          value={totalVisits}
          color="text-blue-400"
          subtext="All time"
        />
      </div>

      <div className="bg-gray-900 border border-gray-800 rounded-lg overflow-hidden">
        <div className="px-4 py-3 border-b border-gray-800 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-white">Recent Campaigns</h2>
          {campaigns.length > 10 && (
            <Link to="/campaigns" className="text-xs text-emerald-400 hover:text-emerald-300">
              View all ({campaigns.length})
            </Link>
          )}
        </div>
        {campaigns.length === 0 ? (
          <EmptyState
            title="No campaigns yet"
            description="Create your first campaign to start generating traffic."
            action={
              <Link to="/campaigns/new" className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-md text-sm font-medium">
                Create Campaign
              </Link>
            }
          />
        ) : (
          <div className="divide-y divide-gray-800">
            {campaigns.slice(0, 10).map((c) => (
              <Link
                key={c.id}
                to={`/campaigns/${c.id}`}
                className="flex items-center justify-between px-4 py-3 hover:bg-gray-800/50 transition-colors"
              >
                <div className="min-w-0">
                  <p className="text-sm font-medium text-white truncate">{c.name}</p>
                  <p className="text-xs text-gray-500">
                    {c.siteName} &middot; {c.simulationLevel.replace(/_/g, ' ')} &middot; {c.visitsPerHour}/h
                  </p>
                </div>
                <StatusBadge status={c.status} />
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
