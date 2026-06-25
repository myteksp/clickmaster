import { useState, useEffect, type FormEvent } from 'react';
import { sitesApi } from '../api/endpoints';
import type { Site } from '../api/types';

export default function SitesPage() {
  const [sites, setSites] = useState<Site[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editSite, setEditSite] = useState<Site | null>(null);
  const [name, setName] = useState('');
  const [baseUrl, setBaseUrl] = useState('');
  const [saving, setSaving] = useState(false);

  const load = async () => {
    try {
      setSites(await sitesApi.list());
    } catch (err) {
      console.error(err);
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
    setSaving(true);
    try {
      if (editSite) {
        await sitesApi.update(editSite.id, { name, baseUrl });
      } else {
        await sitesApi.create({ name, baseUrl });
      }
      setShowForm(false);
      load();
    } catch (err) {
      console.error(err);
      alert('Failed to save site');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Delete this site? This will also delete all associated campaigns.')) return;
    await sitesApi.delete(id);
    load();
  };

  if (loading) return <div className="text-gray-400">Loading...</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-white">Sites</h1>
        <button
          onClick={openCreate}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-md text-sm font-medium transition-colors"
        >
          Add Site
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
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Base URL</label>
              <input
                type="url"
                value={baseUrl}
                onChange={(e) => setBaseUrl(e.target.value)}
                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:border-emerald-500"
                required
                placeholder="https://example.com"
              />
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
        <div className="text-center text-gray-500 py-12">
          No sites configured. Add your first site to start creating campaigns.
        </div>
      ) : (
        <div className="space-y-2">
          {sites.map((s) => (
            <div
              key={s.id}
              className="bg-gray-900 border border-gray-800 rounded-lg px-4 py-3 flex items-center justify-between hover:bg-gray-800/50 transition-colors"
            >
              <div>
                <p className="text-sm font-medium text-white">{s.name}</p>
                <p className="text-xs text-gray-500 mt-0.5">{s.baseUrl}</p>
              </div>
              <div className="flex items-center gap-2">
                <button onClick={() => openEdit(s)} className="text-xs text-gray-400 hover:text-white transition-colors">
                  Edit
                </button>
                <button onClick={() => handleDelete(s.id)} className="text-xs text-gray-600 hover:text-red-400 transition-colors">
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
