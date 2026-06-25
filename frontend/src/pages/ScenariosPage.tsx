import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { scenariosApi } from '../api/endpoints';
import type { Scenario } from '../api/types';

export default function ScenariosPage() {
  const [scenarios, setScenarios] = useState<Scenario[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const load = async () => {
    try {
      setScenarios(await scenariosApi.list());
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleDelete = async (id: string) => {
    if (!confirm('Delete this scenario?')) return;
    await scenariosApi.delete(id);
    load();
  };

  if (loading) return <div className="text-gray-400">Loading...</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-white">Scenarios</h1>
        <Link
          to="/scenarios/new"
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-md text-sm font-medium transition-colors"
        >
          New Scenario
        </Link>
      </div>

      {scenarios.length === 0 ? (
        <div className="text-center text-gray-500 py-12">
          No scenarios yet. Scenarios define user behavior patterns for Level 2 & 3 simulations.
        </div>
      ) : (
        <div className="space-y-2">
          {scenarios.map((s) => (
            <div
              key={s.id}
              className="bg-gray-900 border border-gray-800 rounded-lg px-4 py-3 flex items-center justify-between hover:bg-gray-800/50 transition-colors"
            >
              <div>
                <p className="text-sm font-medium text-white">{s.name}</p>
                <p className="text-xs text-gray-500 mt-0.5">
                  {s.steps.length} steps &middot; {s.description || 'No description'}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => navigate(`/scenarios/${s.id}`)}
                  className="text-xs text-gray-400 hover:text-white transition-colors"
                >
                  Edit
                </button>
                <button
                  onClick={() => handleDelete(s.id)}
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
