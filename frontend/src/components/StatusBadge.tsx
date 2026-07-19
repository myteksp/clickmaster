const statusConfig: Record<string, { color: string; dot: string; label?: string }> = {
  RUNNING: { color: 'text-emerald-300 bg-emerald-900/40 border-emerald-700', dot: 'bg-emerald-400 animate-pulse' },
  PAUSED: { color: 'text-yellow-300 bg-yellow-900/40 border-yellow-700', dot: 'bg-yellow-400' },
  COMPLETED: { color: 'text-blue-300 bg-blue-900/40 border-blue-700', dot: 'bg-blue-400' },
  FAILED: { color: 'text-red-300 bg-red-900/40 border-red-700', dot: 'bg-red-400' },
  DRAFT: { color: 'text-gray-400 bg-gray-800 border-gray-700', dot: 'bg-gray-500' },
  READY: { color: 'text-indigo-300 bg-indigo-900/40 border-indigo-700', dot: 'bg-indigo-400' },
  CANCELLED: { color: 'text-gray-500 bg-gray-800 border-gray-700', dot: 'bg-gray-600' },
};

export default function StatusBadge({ status }: { status: string }) {
  const config = statusConfig[status] || statusConfig.DRAFT;
  return (
    <span className={`inline-flex items-center gap-1.5 text-xs px-2 py-0.5 rounded border ${config.color}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${config.dot}`} />
      {config.label || status}
    </span>
  );
}
