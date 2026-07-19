import { useState, useRef, useEffect, useCallback } from 'react';
import { sitesApi } from '../api/endpoints';
import type { DiscoveredElement, ClickTarget, SessionState, NavigationStep } from '../api/types';
import { useToast } from './Toast';

interface ClickTargetPickerProps {
  siteId: string;
  targets: ClickTarget[];
  onChange: (targets: ClickTarget[]) => void;
}

type Mode = 'navigate' | 'select';

export default function ClickTargetPicker({ siteId, targets, onChange }: ClickTargetPickerProps) {
  const [session, setSession] = useState<SessionState | null>(null);
  const [loading, setLoading] = useState(false);
  const [navigating, setNavigating] = useState(false);
  const [showPicker, setShowPicker] = useState(false);
  const [mode, setMode] = useState<Mode>('navigate');
  const [hoveredEl, setHoveredEl] = useState<string | null>(null);
  const [selectedKeys, setSelectedKeys] = useState<Set<string>>(new Set());
  const imgRef = useRef<HTMLImageElement>(null);
  const targetsRef = useRef(targets);
  const [scale, setScale] = useState(1);
  const toast = useToast();

  useEffect(() => { targetsRef.current = targets; }, [targets]);

  const targetKey = (t: ClickTarget) => `${t.pagePath || 'home'}::${t.selector}`;

  useEffect(() => {
    setSelectedKeys(new Set(targets.map(targetKey)));
  }, [targets]);

  useEffect(() => {
    if (!imgRef.current || !session) return;
    const updateScale = () => {
      if (imgRef.current && session) {
        setScale(imgRef.current.clientWidth / session.pageWidth);
      }
    };
    updateScale();
    const observer = new ResizeObserver(updateScale);
    observer.observe(imgRef.current);
    return () => observer.disconnect();
  }, [session]);

  const cleanupSession = useCallback(() => {
    if (session?.sessionId) {
      sitesApi.closeSession(siteId, session.sessionId).catch(() => {});
    }
  }, [session, siteId]);

  useEffect(() => () => cleanupSession(), [cleanupSession]);

  const openPicker = async () => {
    setLoading(true);
    try {
      const state = await sitesApi.startSession(siteId);
      setSession(state);
      setMode('navigate');
      setShowPicker(true);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to start session');
    } finally {
      setLoading(false);
    }
  };

  const closePicker = () => {
    cleanupSession();
    setShowPicker(false);
    setSession(null);
  };

  const handleElementClick = async (el: DiscoveredElement) => {
    if (mode === 'select') {
      const key = `${session?.currentUrl || 'home'}::${el.selector}`;
      if (selectedKeys.has(key)) return;

      const navSteps: NavigationStep[] = (session?.currentPath || []).map(p => ({
        selector: p.selector,
        text: p.text,
        waitAfterMs: p.waitAfterMs,
      }));

      const newTarget: ClickTarget = {
        selector: el.selector,
        text: el.text,
        tag: el.tag,
        pagePath: session?.currentUrl || 'home',
        navigationSteps: navSteps,
        probability: 80,
        delayBeforeMs: 1000,
        delayAfterMs: 2000,
      };

      const updated = [...targetsRef.current, newTarget];
      targetsRef.current = updated;
      onChange(updated);
      setSelectedKeys(prev => new Set([...prev, key]));
      toast.success(`Target added: ${el.text}`);
      return;
    }

    // Navigate mode — click element in live session
    setNavigating(true);
    try {
      const state = await sitesApi.sessionClick(siteId, session!.sessionId, el.selector, el.text);
      setSession(state);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Navigation failed');
    } finally {
      setNavigating(false);
    }
  };

  const handleBack = async () => {
    if (!session) return;
    setNavigating(true);
    try {
      const state = await sitesApi.sessionBack(siteId, session.sessionId);
      setSession(state);
    } catch (err) {
      toast.error('Back failed');
    } finally {
      setNavigating(false);
    }
  };

  const updateTarget = (key: string, field: string, value: number) => {
    onChange(targets.map(t => targetKey(t) === key ? { ...t, [field]: value } : t));
  };

  const removeTarget = (key: string) => {
    const updated = targetsRef.current.filter(t => targetKey(t) !== key);
    targetsRef.current = updated;
    onChange(updated);
    setSelectedKeys(prev => {
      const next = new Set(prev);
      next.delete(key);
      return next;
    });
  };

  if (targets.length === 0 && !showPicker) {
    return (
      <div>
        <button
          type="button"
          onClick={openPicker}
          disabled={loading || !siteId}
          className="text-xs px-3 py-1.5 bg-gray-800 hover:bg-gray-700 text-gray-300 rounded border border-gray-700 disabled:opacity-50"
        >
          {loading ? 'Starting session...' : '🔍 Open Interactive Picker'}
        </button>
        <p className="text-xs text-gray-500 mt-1.5">
          Navigate the live site, click through tabs, and select elements as click targets. Targets behind multiple clicks are fully supported.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {targets.length > 0 && (
        <div className="space-y-2">
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs font-medium text-gray-400">Configured targets (executed in order):</span>
          </div>
          {targets.map((t) => {
            const key = targetKey(t);
            return (
              <div key={key} className="flex items-center gap-3 bg-gray-800/50 rounded-md p-2.5">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-xs font-mono px-1.5 py-0.5 rounded bg-gray-700 text-gray-400">{t.tag}</span>
                    <span className="text-sm text-white truncate">{t.text}</span>
                  </div>
                  {t.navigationSteps && t.navigationSteps.length > 0 && (
                    <div className="flex items-center gap-1 mt-1 flex-wrap">
                      <span className="text-[10px] text-gray-600">path:</span>
                      {t.navigationSteps.map((step, i) => (
                        <span key={i} className="text-[10px] px-1 py-0.5 rounded bg-blue-900/40 text-blue-400">
                          {step.text}
                        </span>
                      ))}
                      <span className="text-[10px] text-gray-600">→</span>
                      <span className="text-[10px] px-1 py-0.5 rounded bg-emerald-900/40 text-emerald-400">{t.text}</span>
                    </div>
                  )}
                  <p className="text-xs text-gray-500 font-mono mt-0.5 truncate">{t.selector}</p>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  <div className="flex items-center gap-1">
                    <input
                      type="range" min={0} max={100} value={t.probability}
                      onChange={(e) => updateTarget(key, 'probability', Number(e.target.value))}
                      className="w-14 accent-emerald-500"
                    />
                    <span className="text-xs text-gray-400 w-7">{t.probability}%</span>
                  </div>
                  <button type="button" onClick={() => removeTarget(key)} className="text-xs text-gray-600 hover:text-red-400">✕</button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={openPicker}
          disabled={loading}
          className="text-xs px-3 py-1.5 bg-gray-800 hover:bg-gray-700 text-gray-300 rounded border border-gray-700 disabled:opacity-50"
        >
          {loading ? 'Starting...' : targets.length > 0 ? '🔍 Open Picker Again' : '🔍 Open Interactive Picker'}
        </button>
        {targets.length > 0 && (
          <span className="text-xs text-gray-500">{targets.length} target{targets.length !== 1 ? 's' : ''}</span>
        )}
      </div>

      {showPicker && session && (
        <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4" onClick={closePicker}>
          <div
            className="bg-gray-900 border border-gray-700 rounded-lg max-w-6xl w-full max-h-[90vh] flex flex-col overflow-hidden"
            onClick={e => e.stopPropagation()}
          >
            {/* Header */}
            <div className="flex items-center justify-between px-4 py-3 border-b border-gray-800">
              <div className="flex items-center gap-4">
                <div className="flex rounded-md overflow-hidden border border-gray-700">
                  <button
                    type="button"
                    onClick={() => setMode('navigate')}
                    className={`px-3 py-1 text-xs font-medium transition-colors ${
                      mode === 'navigate' ? 'bg-blue-600 text-white' : 'bg-gray-800 text-gray-400 hover:text-white'
                    }`}
                  >
                    🔗 Navigate
                  </button>
                  <button
                    type="button"
                    onClick={() => setMode('select')}
                    className={`px-3 py-1 text-xs font-medium transition-colors ${
                      mode === 'select' ? 'bg-emerald-600 text-white' : 'bg-gray-800 text-gray-400 hover:text-white'
                    }`}
                  >
                    ✅ Select Target
                  </button>
                </div>
                <div className="text-xs text-gray-500">
                  {mode === 'navigate'
                    ? 'Click elements to navigate the live site'
                    : 'Click elements to add as click targets'}
                </div>
              </div>
              <button
                type="button"
                onClick={closePicker}
                className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded text-xs font-medium"
              >
                Done ({targets.length})
              </button>
            </div>

            {/* Breadcrumb + back */}
            <div className="flex items-center gap-2 px-4 py-2 border-b border-gray-800 bg-gray-900/50">
              <button
                type="button"
                onClick={handleBack}
                disabled={!session.currentPath?.length || navigating}
                className="text-xs px-2 py-1 bg-gray-800 hover:bg-gray-700 text-gray-300 rounded disabled:opacity-30"
              >
                ← Back
              </button>
              <div className="flex items-center gap-1 text-xs overflow-x-auto">
                <span className="text-gray-500">Home</span>
                {session.currentPath?.map((step, i) => (
                  <span key={i} className="flex items-center gap-1">
                    <span className="text-gray-600">→</span>
                    <span className="text-blue-400">{step.text}</span>
                  </span>
                ))}
              </div>
              {navigating && <span className="text-xs text-yellow-400 animate-pulse">Loading...</span>}
            </div>

            {/* Screenshot + overlays */}
            <div className="overflow-auto flex-1 p-4">
              <div className="relative inline-block">
                <img
                  ref={imgRef}
                  src={session.screenshot}
                  alt="Site preview"
                  className="block max-w-full"
                  style={{ width: '100%' }}
                  draggable={false}
                />
                {session.elements.map((el, i) => {
                  const elKey = `${session.currentUrl}::${el.selector}`;
                  const isSelected = selectedKeys.has(elKey);
                  const isHovered = hoveredEl === elKey;
                  return (
                    <button
                      key={`${el.selector}-${i}`}
                      type="button"
                      onClick={() => handleElementClick(el)}
                      onMouseEnter={() => setHoveredEl(elKey)}
                      onMouseLeave={() => setHoveredEl(null)}
                      className={`absolute border-2 rounded transition-all cursor-pointer ${
                        mode === 'select' && isSelected
                          ? 'bg-emerald-500/30 border-emerald-400'
                          : mode === 'select'
                          ? isHovered
                            ? 'bg-emerald-500/20 border-emerald-400'
                            : 'bg-transparent border-dashed border-emerald-700 hover:border-emerald-400 hover:bg-emerald-500/10'
                          : isHovered
                          ? 'bg-blue-500/20 border-blue-400'
                          : 'bg-transparent border-dashed border-gray-500 hover:border-blue-400 hover:bg-blue-500/10'
                      }`}
                      style={{
                        left: `${el.x * scale}px`,
                        top: `${el.y * scale}px`,
                        width: `${el.width * scale}px`,
                        height: `${el.height * scale}px`,
                      }}
                      title={mode === 'navigate' ? `Click to navigate: ${el.text}` : `Select: ${el.text}`}
                    >
                      {mode === 'select' && isSelected && (
                        <span className="absolute -top-5 left-0 text-[10px] px-1 py-0.5 rounded bg-emerald-500 text-white whitespace-nowrap">
                          ✓ {el.text.substring(0, 20)}
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
