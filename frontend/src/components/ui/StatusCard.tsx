interface StatusCardProps {
  label: string;
  value: string;
  tone?: 'default' | 'success' | 'danger';
}

const toneClasses: Record<NonNullable<StatusCardProps['tone']>, string> = {
  default: 'border-slate-200 bg-white/80 text-slate-950',
  success: 'border-emerald-200 bg-emerald-50 text-emerald-950',
  danger: 'border-rose-200 bg-rose-50 text-rose-950',
};

export function StatusCard({ label, value, tone = 'default' }: StatusCardProps) {
  return (
    <div className={`rounded-[1.5rem] border p-5 shadow-sm ${toneClasses[tone]}`}>
      <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">{label}</p>
      <p className="mt-3 text-2xl font-black tracking-tight">{value}</p>
    </div>
  );
}
