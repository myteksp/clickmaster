import type { ReactNode } from 'react';

interface StatCardProps {
  label: string;
  value: ReactNode;
  subtext?: string;
  color?: string;
  icon?: ReactNode;
}

export default function StatCard({ label, value, subtext, color = 'text-white', icon }: StatCardProps) {
  return (
    <div className="bg-gray-900 border border-gray-800 rounded-lg p-4">
      <div className="flex items-center justify-between mb-1">
        <p className="text-xs text-gray-500 uppercase tracking-wide">{label}</p>
        {icon && <span className="text-gray-600">{icon}</span>}
      </div>
      <p className={`text-2xl font-bold ${color}`}>{value}</p>
      {subtext && <p className="text-xs text-gray-500 mt-0.5">{subtext}</p>}
    </div>
  );
}
