import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { scenariosApi } from '../api/endpoints';
import type { ScenarioStep } from '../api/types';

const ACTION_TYPES = [
  { value: 'LOAD', label: 'Load Page' },
  { value: 'CLICK', label: 'Click Element' },
  { value: 'SCROLL', label: 'Scroll' },
  { value: 'WAIT', label: 'Wait / Pause' },
  { value: 'HOVER', label: 'Hover Element' },
  { value: 'TYPE', label: 'Type Text' },
  { value: 'EXTRACT_TEXT', label: 'Extract Text' },
  { value: 'SCREENSHOT', label: 'Take Screenshot' },
  { value: 'CUSTOM_JS', label: 'Custom JavaScript' },
];

export default function ScenarioFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = !!id;

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [steps, setSteps] = useState<ScenarioStep[]>([
    { id: null, orderIndex: 0, actionType: 'LOAD', selector: '', value: '', delayBeforeMs: 1000, delayAfterMs: 2000, probability: 1.0, config: null },
  ]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (id) {
      scenariosApi.get(id).then((s) => {
        setName(s.name);
        setDescription(s.description || '');
        setSteps(s.steps.length > 0 ? s.steps : steps);
      }).catch(console.error);
    }
  }, [id]);

  const addStep = () => {
    setSteps([...steps, {
      id: null,
      orderIndex: steps.length,
      actionType: 'CLICK',
      selector: '',
      value: '',
      delayBeforeMs: 500,
      delayAfterMs: 1000,
      probability: 1.0,
      config: null,
    }]);
  };

  const removeStep = (i: number) => setSteps(steps.filter((_, idx) => idx !== i));

  const updateStep = (i: number, field: keyof ScenarioStep, value: unknown) => {
    const copy = [...steps];
    copy[i] = { ...copy[i], [field]: value, orderIndex: i };
    setSteps(copy);
  };

  const moveStep = (i: number, dir: number) => {
    const newIdx = i + dir;
    if (newIdx < 0 || newIdx >= steps.length) return;
    const copy = [...steps];
    [copy[i], copy[newIdx]] = [copy[newIdx], copy[i]];
    copy.forEach((s, idx) => { s.orderIndex = idx; });
    setSteps(copy);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const body = { name, description, steps };
      if (isEdit && id) {
        await scenariosApi.update(id, body);
      } else {
        await scenariosApi.create(body);
      }
      navigate('/scenarios');
    } catch (err) {
      console.error(err);
      alert('Failed to save scenario');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-3xl">
      <h1 className="text-2xl font-bold text-white mb-6">{isEdit ? 'Edit Scenario' : 'New Scenario'}</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-5 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:border-emerald-500"
              required
              placeholder="Explore pricing page"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">Description</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:border-emerald-500 resize-none"
              rows={2}
              placeholder="User browses pricing, scrolls down, clicks sign up..."
            />
          </div>
        </div>

        <div className="bg-gray-900 border border-gray-800 rounded-lg p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-white">Steps ({steps.length})</h2>
            <button type="button" onClick={addStep} className="text-xs px-3 py-1 bg-emerald-600 hover:bg-emerald-500 text-white rounded">
              + Add Step
            </button>
          </div>

          <div className="space-y-3">
            {steps.map((step, i) => (
              <div key={i} className="bg-gray-800 border border-gray-700 rounded-lg p-4">
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-medium text-gray-400">Step {i + 1}</span>
                  <div className="flex items-center gap-1">
                    <button type="button" onClick={() => moveStep(i, -1)} disabled={i === 0} className="text-xs px-2 py-0.5 bg-gray-700 hover:bg-gray-600 rounded disabled:opacity-30 text-gray-300">
                      Up
                    </button>
                    <button type="button" onClick={() => moveStep(i, 1)} disabled={i === steps.length - 1} className="text-xs px-2 py-0.5 bg-gray-700 hover:bg-gray-600 rounded disabled:opacity-30 text-gray-300">
                      Down
                    </button>
                    <button type="button" onClick={() => removeStep(i)} className="text-xs px-2 py-0.5 bg-red-900/50 hover:bg-red-800/50 text-red-300 rounded">
                      Delete
                    </button>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs text-gray-400 mb-1">Action</label>
                    <select
                      value={step.actionType}
                      onChange={(e) => updateStep(i, 'actionType', e.target.value)}
                      className="w-full px-2 py-1.5 bg-gray-700 border border-gray-600 rounded text-white text-sm focus:outline-none focus:border-emerald-500"
                    >
                      {ACTION_TYPES.map((a) => (
                        <option key={a.value} value={a.value}>{a.label}</option>
                      ))}
                    </select>
                  </div>
                  {(step.actionType === 'CLICK' || step.actionType === 'HOVER' || step.actionType === 'TYPE' || step.actionType === 'EXTRACT_TEXT') && (
                    <div>
                      <label className="block text-xs text-gray-400 mb-1">CSS Selector</label>
                      <input
                        type="text"
                        value={step.selector || ''}
                        onChange={(e) => updateStep(i, 'selector', e.target.value)}
                        className="w-full px-2 py-1.5 bg-gray-700 border border-gray-600 rounded text-white text-sm focus:outline-none focus:border-emerald-500"
                        placeholder=".button, #signup"
                      />
                    </div>
                  )}
                  {step.actionType === 'TYPE' && (
                    <div>
                      <label className="block text-xs text-gray-400 mb-1">Text Value</label>
                      <input
                        type="text"
                        value={step.value || ''}
                        onChange={(e) => updateStep(i, 'value', e.target.value)}
                        className="w-full px-2 py-1.5 bg-gray-700 border border-gray-600 rounded text-white text-sm focus:outline-none focus:border-emerald-500"
                        placeholder="Text to type"
                      />
                    </div>
                  )}
                </div>

                <div className="grid grid-cols-3 gap-3 mt-3">
                  <div>
                    <label className="block text-xs text-gray-400 mb-1">Delay Before (ms)</label>
                    <input
                      type="number"
                      value={step.delayBeforeMs}
                      onChange={(e) => updateStep(i, 'delayBeforeMs', Number(e.target.value))}
                      className="w-full px-2 py-1.5 bg-gray-700 border border-gray-600 rounded text-white text-sm focus:outline-none focus:border-emerald-500"
                      min={0}
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-gray-400 mb-1">Delay After (ms)</label>
                    <input
                      type="number"
                      value={step.delayAfterMs}
                      onChange={(e) => updateStep(i, 'delayAfterMs', Number(e.target.value))}
                      className="w-full px-2 py-1.5 bg-gray-700 border border-gray-600 rounded text-white text-sm focus:outline-none focus:border-emerald-500"
                      min={0}
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-gray-400 mb-1">Probability (0-1)</label>
                    <input
                      type="number"
                      value={step.probability}
                      onChange={(e) => updateStep(i, 'probability', Number(e.target.value))}
                      className="w-full px-2 py-1.5 bg-gray-700 border border-gray-600 rounded text-white text-sm focus:outline-none focus:border-emerald-500"
                      min={0}
                      max={1}
                      step={0.1}
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 disabled:bg-emerald-800 text-white rounded-lg font-medium transition-colors"
        >
          {loading ? 'Saving...' : isEdit ? 'Update Scenario' : 'Create Scenario'}
        </button>
      </form>
    </div>
  );
}
