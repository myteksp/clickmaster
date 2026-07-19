import { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { campaignsApi } from '../api/endpoints';
import type { Campaign } from '../api/types';
import StatusBadge from '../components/StatusBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import ConfirmDialog from '../components/ConfirmDialog';
import { useToast } from '../components/Toast';

type StatusFilter = 'ALL' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'DRAFT';

export default function CampaignsPage() {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [deleteTarget, setDeleteTarget] = useState<Campaign | null>(null);
  const toast = useToast();

  const load = async () => {
    try {
      setCampaigns(await campaignsApi.list());
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to load campaigns');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const filtered = useMemo(() => {
    return campaigns.filter((c) => {
      const matchesSearch = c.name.toLowerCase().includes(search.toLowerCase()) ||
        c.siteName?.toLowerCase().includes(search.toLowerCase());
      const matchesStatus = statusFilter === 'ALL' || c.status === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [campaigns, search, statusFilter]);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await campaignsApi.delete(deleteTarget.id);
      toast.success('Campaign deleted');
      setDeleteTarget(null);
      load();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to delete');
      setDeleteTarget(null);
    }
  };

  if (loading) return <LoadingSpinner label="Loading campaigns..." />;

  const statusCounts = campaigns.reduce((acc, c) => {
    acc[c.status] = (acc[c.status] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-white">Campaigns</h1>
        <Link
          to="/campaigns/new"
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-md text-sm font-medium transition-colors"
        >
          + New Campaign
        </Link>
      </div>

      <div className="flex flex-col sm:flex-row gap-3 mb-4">
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by name or site..."
          className="flex-1 px-3 py-2 bg-gray-900 border border-gray-800 rounded-md text-white text-sm focus:outline-none focus:border-emerald-500"
        />
        <div className="flex gap-1">
          {(['ALL', 'RUNNING', 'PAUSED', 'COMPLETED', 'DRAFT'] as StatusFilter[]).map((s) => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`px-3 py-2 rounded-md text-xs font-medium transition-colors ${
                statusFilter === s
                  ? 'bg-gray-800 text-white border border-gray-700'
                  : 'text-gray-500 hover:text-gray-300'
              }`}
            >
              {s === 'ALL' ? 'All' : `${s.charAt(0)}${s.slice(1).toLowerCase()}`}
              {s !== 'ALL' && statusCounts[s] ? ` (${statusCounts[s]})` : ''}
            </button>
          ))}
        </div>
      </div>

      {filtered.length === 0 ? (
        campaigns.length === 0 ? (
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
          <EmptyState title="No matching campaigns" description="Try adjusting your search or filter." />
        )
      ) : (
        <div className="bg-gray-900 border border-gray-800 rounded-lg overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-800 text-left">
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase">Name</th>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase hidden sm:table-cell">Level</th>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase hidden md:table-cell">Rate</th>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase">Status</th>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-800">
              {filtered.map((c) => (
                <tr key={c.id} className="hover:bg-gray-800/50 transition-colors">
                  <td className="px-4 py-3">
                    <Link to={`/campaigns/${c.id}`} className="text-sm font-medium text-white hover:text-emerald-400">
                      {c.name}
                    </Link>
                    <p className="text-xs text-gray-500">{c.siteName}</p>
                  </td>
                  <td className="px-4 py-3 hidden sm:table-cell">
                    <span className="text-xs text-gray-400">{c.simulationLevel.replace(/_/g, ' ')}</span>
                  </td>
                  <td className="px-4 py-3 hidden md:table-cell">
                    <span className="text-xs text-gray-400">{c.visitsPerHour}/h</span>
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={c.status} />
                  </td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex justify-end gap-2">
                      <Link
                        to={`/campaigns/${c.id}/edit`}
                        className="text-xs text-gray-500 hover:text-blue-400 transition-colors"
                      >
                        Edit
                      </Link>
                      <button
                        onClick={() => setDeleteTarget(c)}
                        className="text-xs text-gray-500 hover:text-red-400 transition-colors"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete Campaign"
        message={`Are you sure you want to delete "${deleteTarget?.name}"? This action cannot be undone.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
