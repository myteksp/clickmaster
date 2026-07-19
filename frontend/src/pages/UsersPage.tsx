import { useState, useEffect, type FormEvent } from 'react';
import { usersApi } from '../api/endpoints';
import type { UserAccount } from '../api/types';
import { useToast } from '../components/Toast';
import { useAuth } from '../context/AuthContext';
import ConfirmDialog from '../components/ConfirmDialog';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';

export default function UsersPage() {
  const { user } = useAuth();
  const [users, setUsers] = useState<UserAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<UserAccount | null>(null);
  const toast = useToast();

  const load = async () => {
    try {
      setUsers(await usersApi.list());
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await usersApi.create({ name, email, password });
      toast.success('User created');
      setShowForm(false);
      setName(''); setEmail(''); setPassword('');
      load();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to create user');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await usersApi.delete(deleteTarget.id);
      toast.success('User deleted');
      setDeleteTarget(null);
      load();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to delete user');
      setDeleteTarget(null);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-white">Users</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-md text-sm font-medium"
        >
          {showForm ? 'Cancel' : '+ Add User'}
        </button>
      </div>

      {showForm && (
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-5 mb-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Name</label>
              <input
                type="text" value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:border-emerald-500"
                required autoFocus
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Email</label>
              <input
                type="email" value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:border-emerald-500"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Password</label>
              <input
                type="password" value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:border-emerald-500"
                required minLength={6}
              />
            </div>
            <button
              type="submit" disabled={saving}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:bg-emerald-800 text-white rounded-md text-sm font-medium"
            >
              {saving ? 'Creating...' : 'Create User'}
            </button>
          </form>
        </div>
      )}

      {users.length === 0 ? (
        <EmptyState title="No users" description="This shouldn't happen — you're logged in." />
      ) : (
        <div className="bg-gray-900 border border-gray-800 rounded-lg overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-800 text-left">
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase">Name</th>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase">Email</th>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase hidden sm:table-cell">Created</th>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-800">
              {users.map((u) => (
                <tr key={u.id} className="hover:bg-gray-800/50">
                  <td className="px-4 py-3">
                    <span className="text-sm font-medium text-white">{u.name}</span>
                    {u.id === user?.userId && (
                      <span className="ml-2 text-[10px] px-1.5 py-0.5 rounded bg-emerald-900/60 text-emerald-400">YOU</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-400">{u.email}</td>
                  <td className="px-4 py-3 text-xs text-gray-500 hidden sm:table-cell">
                    {new Date(u.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-4 py-3 text-right">
                    {u.id !== user?.userId && (
                      <button
                        onClick={() => setDeleteTarget(u)}
                        className="text-xs text-gray-600 hover:text-red-400"
                      >
                        Delete
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete User"
        message={`Delete "${deleteTarget?.name}"? This removes their account and all associated data.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
