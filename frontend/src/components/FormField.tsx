import type { ReactNode, InputHTMLAttributes, SelectHTMLAttributes } from 'react';

const inputClass =
  'w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white text-sm focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-colors';

const labelClass = 'block text-sm font-medium text-gray-300 mb-1';

interface FieldProps {
  label: string;
  error?: string;
  hint?: string;
  children: ReactNode;
}

export function Field({ label, error, hint, children }: FieldProps) {
  return (
    <div>
      <label className={labelClass}>{label}</label>
      {children}
      {hint && !error && <p className="text-xs text-gray-500 mt-1">{hint}</p>}
      {error && <p className="text-xs text-red-400 mt-1">{error}</p>}
    </div>
  );
}

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  hint?: string;
}

export function TextInput({ label, error, hint, className = '', ...props }: InputProps) {
  return (
    <Field label={label} error={error} hint={hint}>
      <input className={`${inputClass} ${className}`} {...props} />
    </Field>
  );
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string;
  error?: string;
  hint?: string;
  children: ReactNode;
}

export function Select({ label, error, hint, children, className = '', ...props }: SelectProps) {
  return (
    <Field label={label} error={error} hint={hint}>
      <select className={`${inputClass} ${className}`} {...props}>
        {children}
      </select>
    </Field>
  );
}

interface RadioCardProps {
  name: string;
  value: string;
  checked: boolean;
  onChange: (value: string) => void;
  title: string;
  description: string;
  recommended?: boolean;
}

export function RadioCard({ name, value, checked, onChange, title, description, recommended }: RadioCardProps) {
  return (
    <label
      className={`flex items-start gap-3 p-3 rounded-md border cursor-pointer transition-colors ${
        checked
          ? 'border-emerald-500 bg-emerald-500/10'
          : 'border-gray-700 bg-gray-800 hover:border-gray-600'
      }`}
    >
      <input
        type="radio"
        name={name}
        value={value}
        checked={checked}
        onChange={(e) => onChange(e.target.value)}
        className="mt-0.5 accent-emerald-500"
      />
      <div>
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-white">{title}</span>
          {recommended && (
            <span className="text-[10px] px-1.5 py-0.5 rounded bg-emerald-900/60 text-emerald-400 font-medium uppercase tracking-wide">
              Recommended
            </span>
          )}
        </div>
        <span className="text-xs text-gray-400 mt-0.5 block">{description}</span>
      </div>
    </label>
  );
}

interface CollapsibleSectionProps {
  title: string;
  badge?: string;
  defaultOpen?: boolean;
  children: ReactNode;
}

export function CollapsibleSection({ title, badge, defaultOpen = false, children }: CollapsibleSectionProps) {
  return (
    <details className="bg-gray-900 border border-gray-800 rounded-lg overflow-hidden" open={defaultOpen}>
      <summary className="px-5 py-3 cursor-pointer flex items-center justify-between hover:bg-gray-800/50 transition-colors">
        <div className="flex items-center gap-2">
          <span className="text-sm font-semibold text-white">{title}</span>
          {badge && (
            <span className="text-xs px-1.5 py-0.5 rounded bg-gray-800 text-gray-400">{badge}</span>
          )}
        </div>
        <svg className="w-4 h-4 text-gray-500 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </summary>
      <div className="px-5 py-4 space-y-4 border-t border-gray-800">{children}</div>
    </details>
  );
}
