import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { campaignsApi } from '../api/endpoints';
import type { Campaign } from '../api/types';

export default function CampaignsPage() {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    try {
      setCampaigns(await campaignsApi.list());
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleDelete = async (id: string) => {
    if (!confirm('Delete this campaign?')) return;
    await campaignsApi.delete(id);
    load();
  };

  const statusColors: Record<string, string> = {
    DRAFT: 'text-gray-400 bg-gray-800',
    READY: 'text-indigo-300 bg-indigo-900/40',
    RUNNING: 'text-emerald-300 bg-emerald-900/40',
    PAUSED: 'text-yellow-300 bg-yellow-900/40',
    COMPLETED: 'text-blue-300 bg-blue-900/40',
    FAILED: 'text-red-300 bg-red-900/40',
    CANCELLED: 'text-gray-500 bg-gray-800',
  };

  if (loading) return <div className="text-gray-400">Loading...</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-white">Campaigns</h1>
        <Link
          to="/campaigns/new"
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-md text-sm font-medium transition-colors"
        >
          New Campaign
        </Link>
      </div>

      {campaigns.length === 0 ? (
        <div className="text-center text-gray-500 py-12">
          No campaigns yet. Create your first one.
        </div>
      ) : (
        <div className="space-y-2">
          {campaigns.map((c) => (
            <div
              key={c.id}
              className="bg-gray-900 border border-gray-800 rounded-lg px-4 py-3 flex items-center justify-between hover:bg-gray-800/50 transition-colors"
            >
              <div className="flex items-center gap-4">
                <div>
                  <Link to={`/campaigns/${c.id}`} className="text-sm font-medium text-white hover:text-emerald-400">
                    {c.name}
                  </Link>
                  <p className="text-xs text-gray-500 mt-0.5">
                    {c.siteName} &middot; {c.simulationLevel.replace(/_/g, ' ')} &middot; {c.visitsPerHour}/h
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span className={`text-xs px-2 py-0.5 rounded ${statusColors[c.status]}`}>
                  {c.status}
                </span>
                <button
                  onClick={() => handleDelete(c.id)}
                  className="text-xs text-gray-600 hover:text-red-400 transition-colors"
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
