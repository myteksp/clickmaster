import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { campaignsApi, sitesApi, scenariosApi } from '../api/endpoints';
import type { Site, Scenario } from '../api/types';

const LEVELS = [
  { value: 'HTTP_ONLY', label: 'HTTP Only — Simple page loads with proxy & user-agent rotation' },
  { value: 'BROWSER_NAVIGATION', label: 'Browser Navigation — Multi-path visits with realistic delays' },
  { value: 'FULL_BROWSER', label: 'Full Browser — Playwright-based with scrolls, clicks, full behavior' },
];

const PATTERNS = [
  { value: 'CONSTANT', label: 'Constant — Steady stream of visits' },
  { value: 'RAMP_UP', label: 'Ramp Up — Gradually increasing traffic' },
  { value: 'PULSE', label: 'Pulse — Bursts of traffic' },
  { value: 'REALISTIC_WAVE', label: 'Realistic Wave — Natural traffic patterns' },
];

export default function CampaignFormPage() {
  const navigate = useNavigate();
  const [sites, setSites] = useState<Site[]>([]);
  const [scenarios, setScenarios] = useState<Scenario[]>([]);
  const [loading, setLoading] = useState(false);

  const [name, setName] = useState('');
  const [siteId, setSiteId] = useState('');
  const [simulationLevel, setSimulationLevel] = useState('HTTP_ONLY');
  const [trafficPattern, setTrafficPattern] = useState('CONSTANT');
  const [visitsPerHour, setVisitsPerHour] = useState(100);
  const [durationMinutes, setDurationMinutes] = useState(60);
  const [geoDistribution, setGeoDistribution] = useState([{ countryCode: 'US', weight: 100 }]);
  const [selectedScenarios, setSelectedScenarios] = useState<{ scenarioId: string; entryUrl: string; weight: number }[]>([]);

  useEffect(() => {
    sitesApi.list().then(setSites).catch(console.error);
    scenariosApi.list().then(setScenarios).catch(console.error);
  }, []);

  const addGeo = () => setGeoDistribution([...geoDistribution, { countryCode: 'US', weight: 10 }]);
  const removeGeo = (i: number) => setGeoDistribution(geoDistribution.filter((_, idx) => idx !== i));
  const updateGeo = (i: number, field: string, value: string | number) => {
    const copy = [...geoDistribution];
    copy[i] = { ...copy[i], [field]: value };
    setGeoDistribution(copy);
  };

  const addScenario = () => {
    if (scenarios.length > 0) {
      setSelectedScenarios([...selectedScenarios, { scenarioId: scenarios[0].id, weight: 100, entryUrl: '' }]);
    }
  };
  const removeScenario = (i: number) => setSelectedScenarios(selectedScenarios.filter((_, idx) => idx !== i));
  const updateScenario = (i: number, field: string, value: string | number) => {
    const copy = [...selectedScenarios];
    copy[i] = { ...copy[i], [field]: value };
    setSelectedScenarios(copy);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const body = {
        siteId,
        name,
        simulationLevel,
        trafficPattern,
        visitsPerHour,
        durationMinutes,
        geoDistribution: geoDistribution.map((g) => ({
          countryCode: g.countryCode,
          city: null,
          weight: g.weight,
        })),
        deviceProfile: [],
        userAgentConfig: { rotation: 'RANDOM', customPool: [] },
        proxyConfig: { provider: 'ASOCKS' },
        scenarios: selectedScenarios.map((s) => ({
          scenarioId: s.scenarioId,
          entryUrl: s.entryUrl || null,
          weight: s.weight,
        })),
      };
      await campaignsApi.create(body);
      navigate('/campaigns');
    } catch (err) {
      console.error(err);
      alert('Failed to create campaign');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-3xl">
      <h1 className="text-2xl font-bold text-white mb-6">New Campaign</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-5 space-y-4">
          <h2 className="text-lg font-semibold text-white">Basic Settings</h2>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">Campaign Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:border-emerald-500"
              required
              placeholder="My Campaign"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">Target Site</label>
            <select
              value={siteId}
              onChange={(e) => setSiteId(e.target.value)}
              className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:border-emerald-500"
              required
            >
              <option value="">Select a site...</option>
              {sites.map((s) => (
                <option key={s.id} value={s.id}>{s.name} ({s.baseUrl})</option>
              ))}
            </select>
            {sites.length === 0 && (
              <p className="text-xs text-yellow-400 mt-1">No sites configured. Create one first.</p>
            )}
          </div>
        </div>

        <div className="bg-gray-900 border border-gray-800 rounded-lg p-5 space-y-4">
          <h2 className="text-lg font-semibold text-white">Simulation Level</h2>
          <div className="space-y-2">
            {LEVELS.map((l) => (
              <label
                key={l.value}
                className={`flex items-start gap-3 p-3 rounded-md border cursor-pointer transition-colors ${
                  simulationLevel === l.value
                    ? 'border-emerald-500 bg-emerald-500/10'
                    : 'border-gray-700 bg-gray-800 hover:border-gray-600'
                }`}
              >
                <input
                  type="radio"
                  name="simulationLevel"
                  value={l.value}
                  checked={simulationLevel === l.value}
                  onChange={(e) => setSimulationLevel(e.target.value)}
                  className="mt-0.5 accent-emerald-500"
                />
                <span className="text-sm text-gray-300">{l.label}</span>
              </label>
            ))}
          </div>
        </div>

        <div className="bg-gray-900 border border-gray-800 rounded-lg p-5 space-y-4">
          <h2 className="text-lg font-semibold text-white">Traffic Settings</h2>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Visits per Hour</label>
              <input
                type="number"
                value={visitsPerHour}
                onChange={(e) => setVisitsPerHour(Number(e.target.value))}
                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:border-emerald-500"
                min={1}
                max={10000}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Duration (minutes)</label>
              <input
                type="number"
                value={durationMinutes}
                onChange={(e) => setDurationMinutes(Number(e.target.value))}
                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:border-emerald-500"
                min={1}
                max={1440}
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">Traffic Pattern</label>
            <div className="grid grid-cols-2 gap-2">
              {PATTERNS.map((p) => (
                <label
                  key={p.value}
                  className={`flex items-center gap-2 p-3 rounded-md border cursor-pointer transition-colors ${
                    trafficPattern === p.value
                      ? 'border-emerald-500 bg-emerald-500/10'
                      : 'border-gray-700 bg-gray-800 hover:border-gray-600'
                  }`}
                >
                  <input
                    type="radio"
                    name="trafficPattern"
                    value={p.value}
                    checked={trafficPattern === p.value}
                    onChange={(e) => setTrafficPattern(e.target.value)}
                    className="accent-emerald-500"
                  />
                  <span className="text-sm text-gray-300">{p.label}</span>
                </label>
              ))}
            </div>
          </div>
        </div>

        <div className="bg-gray-900 border border-gray-800 rounded-lg p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-white">Geographic Distribution</h2>
            <button type="button" onClick={addGeo} className="text-xs px-3 py-1 bg-gray-800 hover:bg-gray-700 text-gray-300 rounded border border-gray-700">
              + Add Country
            </button>
          </div>

          {geoDistribution.map((geo, i) => (
            <div key={i} className="flex items-center gap-3">
              <input
                type="text"
                value={geo.countryCode}
                onChange={(e) => updateGeo(i, 'countryCode', e.target.value.toUpperCase())}
                className="w-24 px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white text-sm focus:outline-none focus:border-emerald-500"
                placeholder="US"
                maxLength={2}
              />
              <input
                type="range"
                value={geo.weight}
                onChange={(e) => updateGeo(i, 'weight', Number(e.target.value))}
                className="flex-1 accent-emerald-500"
                min={0}
                max={100}
              />
              <span className="text-sm text-gray-400 w-10 text-right">{geo.weight}%</span>
              {geoDistribution.length > 1 && (
                <button type="button" onClick={() => removeGeo(i)} className="text-xs text-red-400 hover:text-red-300">
                  Remove
                </button>
              )}
            </div>
          ))}
        </div>

        {simulationLevel !== 'HTTP_ONLY' && (
          <div className="bg-gray-900 border border-gray-800 rounded-lg p-5 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-white">Scenarios</h2>
              <button type="button" onClick={addScenario} className="text-xs px-3 py-1 bg-gray-800 hover:bg-gray-700 text-gray-300 rounded border border-gray-700">
                + Add Scenario
              </button>
            </div>
            {scenarios.length === 0 && (
              <p className="text-sm text-gray-500">No scenarios defined yet. Create scenarios first.</p>
            )}
            {selectedScenarios.map((s, i) => (
              <div key={i} className="flex items-center gap-3">
                <select
                  value={s.scenarioId}
                  onChange={(e) => updateScenario(i, 'scenarioId', e.target.value)}
                  className="flex-1 px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white text-sm focus:outline-none focus:border-emerald-500"
                >
                  {scenarios.map((sc) => (
                    <option key={sc.id} value={sc.id}>{sc.name}</option>
                  ))}
                </select>
                <input
                  type="text"
                  value={s.entryUrl}
                  onChange={(e) => updateScenario(i, 'entryUrl', e.target.value)}
                  className="w-48 px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white text-sm focus:outline-none focus:border-emerald-500"
                  placeholder="/landing"
                />
                <input
                  type="number"
                  value={s.weight}
                  onChange={(e) => updateScenario(i, 'weight', Number(e.target.value))}
                  className="w-20 px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white text-sm focus:outline-none focus:border-emerald-500"
                  min={1}
                  max={1000}
                />
                <button type="button" onClick={() => removeScenario(i)} className="text-xs text-red-400 hover:text-red-300">
                  Remove
                </button>
              </div>
            ))}
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 disabled:bg-emerald-800 text-white rounded-lg font-medium transition-colors"
        >
          {loading ? 'Creating...' : 'Create Campaign'}
        </button>
      </form>
    </div>
  );
}
