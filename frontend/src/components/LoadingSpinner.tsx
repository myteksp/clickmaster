export default function LoadingSpinner({ label = 'Loading...' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center py-12">
      <div className="flex flex-col items-center gap-3">
        <div className="w-8 h-8 border-2 border-gray-700 border-t-emerald-500 rounded-full animate-spin" />
        <p className="text-sm text-gray-500">{label}</p>
      </div>
    </div>
  );
}
