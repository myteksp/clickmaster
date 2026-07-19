import { useState, useEffect, type FormEvent } from 'react';
import { sitesApi } from '../api/endpoints';
import type { Site } from '../api/types';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';

export default function SitesPage() {
  const [sites, setSites] = useState<Site[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editSite, setEditSite] = useState<Site | null>(null);
  const [name, setName] = useState('');
  const [baseUrl, setBaseUrl] = useState('');
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Site | null>(null);
  const toast = useToast();

  const load = async () => {
    try {
      setSites(await sitesApi.list());
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to load sites');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openCreate = () => {
    setEditSite(null);
    setName('');
    setBaseUrl('');
    setShowForm(true);
  };

  const openEdit = (site: Site) => {
    setEditSite(site);
    setName(site.name);
    setBaseUrl(site.baseUrl);
    setShowForm(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) { toast.error('Please enter a site name'); return; }
    if (!baseUrl.trim()) { toast.error('Please enter a base URL'); return; }

    let url = baseUrl.trim();
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      url = 'https://' + url;
      setBaseUrl(url);
    }

    setSaving(true);
    try {
      if (editSite) {
        await sitesApi.update(editSite.id, { name: name.trim(), baseUrl: url });
        toast.success('Site updated');
      } else {
        await sitesApi.create({ name: name.trim(), baseUrl: url });
        toast.success('Site created');
      }
      setShowForm(false);
      load();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to save site');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await sitesApi.delete(deleteTarget.id);
      toast.success('Site deleted');
      setDeleteTarget(null);
      load();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to delete site');
      setDeleteTarget(null);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-white">Sites</h1>
        <button
          onClick={openCreate}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-md text-sm font-medium transition-colors"
        >
          + Add Site
        </button>
      </div>

      {showForm && (
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-5 mb-6">
          <h2 className="text-lg font-semibold text-white mb-4">{editSite ? 'Edit Site' : 'New Site'}</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Name</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:border-emerald-500"
                required
                placeholder="My Website"
                autoFocus
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Base URL</label>
              <input
                type="text"
                value={baseUrl}
                onChange={(e) => setBaseUrl(e.target.value)}
                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:border-emerald-500"
                required
                placeholder="https://example.com"
              />
              <p className="text-xs text-gray-500 mt-1">Protocol (https://) is added automatically if missing</p>
            </div>
            <div className="flex gap-2">
              <button
                type="submit"
                disabled={saving}
                className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:bg-emerald-800 text-white rounded-md text-sm font-medium"
              >
                {saving ? 'Saving...' : editSite ? 'Update' : 'Create'}
              </button>
              <button
                type="button"
                onClick={() => setShowForm(false)}
                className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-gray-300 rounded-md text-sm"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {sites.length === 0 && !showForm ? (
        <EmptyState
          title="No sites configured"
          description="Add your first site to start creating campaigns."
          action={
            <button onClick={openCreate} className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-md text-sm font-medium">
              Add Site
            </button>
          }
        />
      ) : (
        <div className="space-y-2">
          {sites.map((s) => (
            <div
              key={s.id}
              className="bg-gray-900 border border-gray-800 rounded-lg px-4 py-3 flex items-center justify-between hover:bg-gray-800/50 transition-colors"
            >
              <div className="min-w-0">
                <p className="text-sm font-medium text-white">{s.name}</p>
                <p className="text-xs text-gray-500 mt-0.5 truncate">{s.baseUrl}</p>
              </div>
              <div className="flex items-center gap-2 flex-shrink-0">
                <button onClick={() => openEdit(s)} className="text-xs text-gray-400 hover:text-white transition-colors">
                  Edit
                </button>
                <button onClick={() => setDeleteTarget(s)} className="text-xs text-gray-600 hover:text-red-400 transition-colors">
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete Site"
        message={`Delete "${deleteTarget?.name}"? This will also delete all associated campaigns.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
