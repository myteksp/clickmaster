import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { campaignsApi, sitesApi, scenariosApi, asocksApi } from '../api/endpoints';
import type { Site, Scenario, Campaign, ClickTarget } from '../api/types';
import { RadioCard, CollapsibleSection } from '../components/FormField';
import ClickTargetPicker from '../components/ClickTargetPicker';
import { useToast } from '../components/Toast';

const LEVELS = [
  { value: 'HTTP_ONLY', title: 'HTTP Only', description: 'Fast page loads with proxy & user-agent rotation', recommended: true },
  { value: 'BROWSER_NAVIGATION', title: 'Browser Navigation', description: 'Multi-path visits with realistic delays & cookies' },
  { value: 'FULL_BROWSER', title: 'Full Browser', description: 'Playwright with scrolls, clicks, full human behavior' },
];

const PATTERNS = [
  { value: 'CONSTANT', title: 'Constant', description: 'Steady stream of visits' },
  { value: 'RAMP_UP', title: 'Ramp Up', description: 'Gradually increasing traffic' },
  { value: 'PULSE', title: 'Pulse', description: 'Bursts of traffic with pauses' },
  { value: 'REALISTIC_WAVE', title: 'Realistic Wave', description: 'Natural day/night traffic patterns' },
];

const PRESETS = {
  quick: { simulationLevel: 'HTTP_ONLY', visitsPerHour: 100, durationMinutes: 60, trafficPattern: 'CONSTANT' },
  balanced: { simulationLevel: 'BROWSER_NAVIGATION', visitsPerHour: 200, durationMinutes: 120, trafficPattern: 'REALISTIC_WAVE' },
  heavy: { simulationLevel: 'FULL_BROWSER', visitsPerHour: 50, durationMinutes: 240, trafficPattern: 'REALISTIC_WAVE' },
};

const DURATIONS = [
  { label: '30m', value: 30 },
  { label: '1h', value: 60 },
  { label: '2h', value: 120 },
  { label: '4h', value: 240 },
];

const POPULAR_COUNTRIES = [
  { code: 'DE', name: 'Germany' },
  { code: 'US', name: 'United States' },
  { code: 'GB', name: 'United Kingdom' },
  { code: 'FR', name: 'France' },
  { code: 'CA', name: 'Canada' },
  { code: 'AU', name: 'Australia' },
  { code: 'NL', name: 'Netherlands' },
  { code: 'JP', name: 'Japan' },
];

export default function CampaignFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useToast();
  const isEdit = !!id;

  const [sites, setSites] = useState<Site[]>([]);
  const [scenarios, setScenarios] = useState<Scenario[]>([]);
  const [countries, setCountries] = useState(POPULAR_COUNTRIES);
  const [loading, setLoading] = useState(isEdit);
  const [submitting, setSubmitting] = useState(false);

  const [name, setName] = useState('');
  const [siteId, setSiteId] = useState('');
  const [simulationLevel, setSimulationLevel] = useState('HTTP_ONLY');
  const [trafficPattern, setTrafficPattern] = useState('CONSTANT');
  const [visitsPerHour, setVisitsPerHour] = useState(100);
  const [durationMinutes, setDurationMinutes] = useState(60);
  const [geoDistribution, setGeoDistribution] = useState<{ countryCode: string; weight: number }[]>([{ countryCode: 'DE', weight: 100 }]);
  const [selectedScenarios, setSelectedScenarios] = useState<{ scenarioId: string; entryUrl: string; weight: number }[]>([]);
  const [clickTargets, setClickTargets] = useState<ClickTarget[]>([]);

  useEffect(() => {
    sitesApi.list().then(setSites).catch(() => {});
    scenariosApi.list().then(setScenarios).catch(() => {});
    asocksApi.countries().then((c) => { if (c.length > 0) setCountries(c); }).catch(() => {});

    if (isEdit && id) {
      campaignsApi.get(id).then((c: Campaign) => {
        setName(c.name);
        setSiteId(c.siteId);
        setSimulationLevel(c.simulationLevel);
        setTrafficPattern(c.trafficPattern);
        setVisitsPerHour(c.visitsPerHour);
        setDurationMinutes(c.durationMinutes);
        if (c.geoDistribution?.length) {
          setGeoDistribution(c.geoDistribution.map((g) => ({ countryCode: g.countryCode, weight: g.weight })));
        }
        if (c.scenarios?.length) {
          setSelectedScenarios(c.scenarios.map((s) => ({ scenarioId: s.scenarioId, entryUrl: s.entryUrl || '', weight: s.weight })));
        }
        if (c.clickTargets?.length) {
          setClickTargets(c.clickTargets);
        }
        setLoading(false);
      }).catch(() => {
        toast.error('Failed to load campaign');
        navigate('/campaigns');
      });
    }
  }, [id]);

  const applyPreset = (preset: keyof typeof PRESETS) => {
    const p = PRESETS[preset];
    setSimulationLevel(p.simulationLevel);
    setVisitsPerHour(p.visitsPerHour);
    setDurationMinutes(p.durationMinutes);
    setTrafficPattern(p.trafficPattern);
    toast.info(`Applied "${preset}" preset`);
  };

  const geoTotal = geoDistribution.reduce((sum, g) => sum + g.weight, 0);

  const addGeo = () => {
    const available = countries.find((c) => !geoDistribution.some((g) => g.countryCode === c.code));
    if (available) {
      setGeoDistribution([...geoDistribution, { countryCode: available.code, weight: 0 }]);
    }
  };
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
    if (!siteId) { toast.error('Please select a target site'); return; }
    if (!name.trim()) { toast.error('Please enter a campaign name'); return; }

    setSubmitting(true);
    try {
      const body = {
        siteId,
        name: name.trim(),
        simulationLevel,
        trafficPattern,
        visitsPerHour,
        durationMinutes,
        geoDistribution: geoDistribution.map((g) => ({
          countryCode: g.countryCode,
          countryName: countries.find((c) => c.code === g.countryCode)?.name || g.countryCode,
          city: null,
          weight: g.weight,
        })),
        deviceProfile: [],
        userAgentConfig: { rotation: 'RANDOM', customPool: [] },
        proxyConfig: { provider: 'ASOCKS' },
        clickTargets,
        scenarios: selectedScenarios.map((s) => ({
          scenarioId: s.scenarioId,
          entryUrl: s.entryUrl || null,
          weight: s.weight,
        })),
      };
      if (isEdit && id) {
        await campaignsApi.update(id, body);
        toast.success('Campaign updated');
        navigate(`/campaigns/${id}`);
      } else {
        const created = await campaignsApi.create(body);
        toast.success('Campaign created');
        navigate(`/campaigns/${created.id}`);
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to save campaign');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="text-gray-400">Loading...</div>;

  return (
    <div className="max-w-3xl">
      <div className="flex items-center gap-3 mb-6">
        <h1 className="text-2xl font-bold text-white">{isEdit ? 'Edit Campaign' : 'New Campaign'}</h1>
        {isEdit && (
          <span className="text-xs px-2 py-0.5 rounded bg-gray-800 text-gray-400">Editing</span>
        )}
      </div>

      {!isEdit && (
        <div className="flex gap-2 mb-6">
          <span className="text-xs text-gray-500 self-center mr-2">Presets:</span>
          {(Object.keys(PRESETS) as Array<keyof typeof PRESETS>).map((key) => (
            <button
              key={key}
              type="button"
              onClick={() => applyPreset(key)}
              className="px-3 py-1 text-xs rounded-md bg-gray-800 hover:bg-gray-700 text-gray-300 border border-gray-700 capitalize transition-colors"
            >
              {key === 'quick' ? 'Quick Start' : key}
            </button>
          ))}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-5 space-y-4">
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:border-emerald-500 text-lg"
            placeholder="Campaign name..."
            required
          />
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
              <p className="text-xs text-yellow-400 mt-1">
                No sites configured. <a href="/sites" className="underline">Create one first.</a>
              </p>
            )}
          </div>
        </div>

        <div className="bg-gray-900 border border-gray-800 rounded-lg p-5 space-y-3">
          <h2 className="text-sm font-semibold text-gray-300">Simulation Level</h2>
          <div className="space-y-2">
            {LEVELS.map((l) => (
              <RadioCard
                key={l.value}
                name="simulationLevel"
                value={l.value}
                checked={simulationLevel === l.value}
                onChange={setSimulationLevel}
                title={l.title}
                description={l.description}
                recommended={l.recommended}
              />
            ))}
          </div>
        </div>

        <div className="bg-gray-900 border border-gray-800 rounded-lg p-5 space-y-4">
          <h2 className="text-sm font-semibold text-gray-300">Traffic</h2>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-gray-400 mb-1">Visits per Hour</label>
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
              <label className="block text-sm text-gray-400 mb-1">Duration</label>
              <div className="flex gap-1">
                {DURATIONS.map((d) => (
                  <button
                    key={d.value}
                    type="button"
                    onClick={() => setDurationMinutes(d.value)}
                    className={`flex-1 py-2 text-xs rounded-md transition-colors ${
                      durationMinutes === d.value
                        ? 'bg-emerald-600 text-white'
                        : 'bg-gray-800 text-gray-400 hover:bg-gray-700'
                    }`}
                  >
                    {d.label}
                  </button>
                ))}
                <input
                  type="number"
                  value={durationMinutes}
                  onChange={(e) => setDurationMinutes(Number(e.target.value))}
                  className="w-16 px-2 py-2 bg-gray-800 border border-gray-700 rounded-md text-white text-xs text-center focus:outline-none focus:border-emerald-500"
                  min={1}
                  max={1440}
                />
              </div>
            </div>
          </div>
          <div>
            <label className="block text-sm text-gray-400 mb-2">Traffic Pattern</label>
            <div className="grid grid-cols-2 gap-2">
              {PATTERNS.map((p) => (
                <RadioCard
                  key={p.value}
                  name="trafficPattern"
                  value={p.value}
                  checked={trafficPattern === p.value}
                  onChange={setTrafficPattern}
                  title={p.title}
                  description={p.description}
                />
              ))}
            </div>
          </div>
          <div className="text-xs text-gray-500 pt-2 border-t border-gray-800">
            Est. total: <span className="text-gray-300 font-medium">~{Math.round(visitsPerHour * (durationMinutes / 60))} visits</span>
            {simulationLevel === 'FULL_BROWSER' && visitsPerHour > 200 && (
              <span className="text-yellow-500 ml-2">{'\u26A0'} Full Browser is limited to ~200/h on a single machine</span>
            )}
          </div>
        </div>

        {simulationLevel !== 'HTTP_ONLY' && (
          <div className="bg-gray-900 border border-gray-800 rounded-lg p-5 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-gray-300">Scenarios</h2>
              {scenarios.length > 0 && (
                <button type="button" onClick={addScenario} className="text-xs px-3 py-1 bg-gray-800 hover:bg-gray-700 text-gray-300 rounded border border-gray-700">
                  + Add
                </button>
              )}
            </div>
            {scenarios.length === 0 && (
              <div className="text-xs text-gray-500">
                No scenarios available.{' '}
                <a href="/scenarios/new" className="text-emerald-400 hover:text-emerald-300 underline">
                  Create a scenario first
                </a>{' '}
                to define multi-step user behavior.
              </div>
            )}
            {selectedScenarios.map((s, i) => (
              <div key={i} className="flex items-center gap-2">
                <select
                  value={s.scenarioId}
                  onChange={(e) => updateScenario(i, 'scenarioId', e.target.value)}
                  className="flex-1 px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white text-sm"
                >
                  {scenarios.map((sc) => (
                    <option key={sc.id} value={sc.id}>{sc.name}</option>
                  ))}
                </select>
                <input
                  type="text"
                  value={s.entryUrl}
                  onChange={(e) => updateScenario(i, 'entryUrl', e.target.value)}
                  className="w-40 px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white text-sm"
                  placeholder="/landing"
                />
                <button type="button" onClick={() => removeScenario(i)} className="text-xs text-red-400 hover:text-red-300">
                  Remove
                </button>
              </div>
            ))}
          </div>
        )}

        {simulationLevel === 'FULL_BROWSER' && siteId && (
          <div className="bg-gray-900 border border-gray-800 rounded-lg p-5 space-y-3">
            <h2 className="text-sm font-semibold text-gray-300">Click Targets</h2>
            <p className="text-xs text-gray-500">Select specific elements on your site that should be clicked during each visit.</p>
            <ClickTargetPicker
              siteId={siteId}
              targets={clickTargets}
              onChange={setClickTargets}
            />
          </div>
        )}

        <CollapsibleSection title="Advanced Settings" badge={geoDistribution.length > 1 ? `${geoDistribution.length} countries` : undefined}>
          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="text-sm font-medium text-gray-300">Geographic Distribution</label>
              <button type="button" onClick={addGeo} className="text-xs px-3 py-1 bg-gray-800 hover:bg-gray-700 text-gray-300 rounded border border-gray-700">
                + Add Country
              </button>
            </div>
            <div className="space-y-2">
              {geoDistribution.map((geo, i) => (
                <div key={i} className="flex items-center gap-3">
                  <select
                    value={geo.countryCode}
                    onChange={(e) => updateGeo(i, 'countryCode', e.target.value)}
                    className="w-40 px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white text-sm"
                  >
                    {countries.map((c) => (
                      <option key={c.code} value={c.code}>{c.name}</option>
                    ))}
                  </select>
                  <input
                    type="range"
                    value={geo.weight}
                    onChange={(e) => updateGeo(i, 'weight', Number(e.target.value))}
                    className="flex-1 accent-emerald-500"
                    min={0}
                    max={100}
                  />
                  <span className="text-sm text-gray-400 w-12 text-right">
                    {geoTotal > 0 ? Math.round((geo.weight / geoTotal) * 100) : 0}%
                  </span>
                  {geoDistribution.length > 1 && (
                    <button type="button" onClick={() => removeGeo(i)} className="text-xs text-red-400 hover:text-red-300">
                      Remove
                    </button>
                  )}
                </div>
              ))}
            </div>
            {geoTotal !== 100 && geoDistribution.length > 1 && (
              <p className="text-xs text-gray-500 mt-2">
                Weights are normalized automatically (current sum: {geoTotal})
              </p>
            )}
          </div>
        </CollapsibleSection>

        <div className="flex gap-3 pt-2">
          <button
            type="button"
            onClick={() => navigate('/campaigns')}
            className="px-4 py-2 text-sm text-gray-400 hover:text-white transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="flex-1 py-3 bg-emerald-600 hover:bg-emerald-500 disabled:bg-emerald-800 text-white rounded-lg font-medium transition-colors"
          >
            {submitting ? 'Saving...' : isEdit ? 'Update Campaign' : 'Create Campaign'}
          </button>
        </div>
      </form>
    </div>
  );
}
