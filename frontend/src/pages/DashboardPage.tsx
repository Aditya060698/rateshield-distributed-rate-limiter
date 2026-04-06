import { useEffect, useState } from 'react';
import { DashboardCharts } from '../components/dashboard/DashboardCharts';
import { SectionCard } from '../components/ui/SectionCard';
import { StatusCard } from '../components/ui/StatusCard';
import { readDashboardSnapshot, subscribeDashboard } from '../lib/dashboardMetrics';
import type { DashboardSnapshot } from '../types/api';

export function DashboardPage() {
  const [snapshot, setSnapshot] = useState<DashboardSnapshot>(() => readDashboardSnapshot());

  useEffect(() => {
    return subscribeDashboard(() => {
      setSnapshot(readDashboardSnapshot());
    });
  }, []);

  return (
    <div className="grid gap-6">
      <div className="grid gap-4 md:grid-cols-3">
        <StatusCard label="Allowed Requests" value={String(snapshot.summary.allowed)} tone="success" />
        <StatusCard label="Blocked Requests" value={String(snapshot.summary.blocked)} tone="danger" />
        <StatusCard label="Total Events" value={String(snapshot.summary.total)} />
      </div>

      <DashboardCharts summary={snapshot.summary} events={snapshot.events} />

      <SectionCard
        title="Latest Metrics Events"
        description="The dashboard listens to simulator updates and redraws itself whenever new request results are stored."
      >
        {snapshot.events.length === 0 ? (
          <p className="text-sm leading-6 text-slate-600">
            No simulation events have been recorded yet. Run the simulator to feed data into the dashboard.
          </p>
        ) : (
          <div className="grid gap-3">
            {snapshot.events.slice(-8).reverse().map((event) => (
              <div key={event.id} className="rounded-[1.25rem] border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 shadow-sm">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <p className="font-semibold text-slate-950">{event.clientKey}</p>
                  <p className={event.allowed ? 'font-semibold text-emerald-700' : 'font-semibold text-rose-700'}>
                    {event.allowed ? 'ALLOWED' : 'BLOCKED'}
                  </p>
                </div>
                <p className="mt-2 text-slate-600">{event.algorithm} • {event.resource}</p>
                <p className="mt-1 text-slate-500">Remaining: {event.remainingTokens} • Retry: {event.retryAfterSeconds}s</p>
              </div>
            ))}
          </div>
        )}
      </SectionCard>
    </div>
  );
}
