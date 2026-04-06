import { useState } from 'react';
import { SimulationForm } from '../components/simulation/SimulationForm';
import { SectionCard } from '../components/ui/SectionCard';
import { StatusCard } from '../components/ui/StatusCard';
import { recordSimulationEvent } from '../lib/dashboardMetrics';
import { simulateRateLimit } from '../services/api/simulation';
import type { SimulationRequest, SimulationResult } from '../types/api';

interface RequestRunSummary {
  total: number;
  allowed: number;
  blocked: number;
}

interface RequestHistoryEntry {
  index: number;
  status: SimulationResult['status'];
  remainingTokens: number;
  retryAfterSeconds: number;
  message: string;
}

const emptySummary: RequestRunSummary = {
  total: 0,
  allowed: 0,
  blocked: 0,
};

export function SimulationPage() {
  const [lastResult, setLastResult] = useState<SimulationResult | null>(null);
  const [summary, setSummary] = useState<RequestRunSummary>(emptySummary);
  const [history, setHistory] = useState<RequestHistoryEntry[]>([]);
  const [error, setError] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(payload: SimulationRequest) {
    setIsSubmitting(true);
    setError('');
    setLastResult(null);
    setSummary(emptySummary);
    setHistory([]);

    let allowedCount = 0;
    let blockedCount = 0;
    const nextHistory: RequestHistoryEntry[] = [];

    try {
      for (let index = 0; index < payload.requestCount; index += 1) {
        const result = await simulateRateLimit(payload);
        recordSimulationEvent(payload, result);

        if (result.allowed) {
          allowedCount += 1;
        } else {
          blockedCount += 1;
        }

        nextHistory.push({
          index: index + 1,
          status: result.status,
          remainingTokens: result.remainingTokens,
          retryAfterSeconds: result.retryAfterSeconds,
          message: result.message,
        });

        setLastResult(result);
        setSummary({
          total: index + 1,
          allowed: allowedCount,
          blocked: blockedCount,
        });
        setHistory([...nextHistory]);
      }
    } catch (submissionError) {
      setError(submissionError instanceof Error ? submissionError.message : 'Simulation failed');
    } finally {
      setIsSubmitting(false);
    }
  }

  const decisionTone = !lastResult ? 'default' : lastResult.allowed ? 'success' : 'danger';

  return (
    <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
      <div className="grid gap-6">
        <SectionCard
          title="Simulation Console"
          description="Enter a client key, choose an algorithm, and send a sequence of requests through the backend. The page keeps aggregate counts locally while each request still goes through the API service layer."
        >
          <SimulationForm onSubmit={handleSubmit} isSubmitting={isSubmitting} />
        </SectionCard>

        <SectionCard
          title="Request Timeline"
          description="Each row represents one request sent to the backend in order. This makes it easy to see exactly when the limiter starts blocking traffic."
        >
          {history.length === 0 ? (
            <p className="text-sm leading-6 text-slate-600">
              Send a batch of requests to populate the timeline.
            </p>
          ) : (
            <div className="overflow-hidden rounded-[1.5rem] border border-slate-200">
              <div className="grid grid-cols-[0.7fr_1fr_1fr_1fr] gap-3 bg-slate-100 px-4 py-3 text-xs font-bold uppercase tracking-[0.2em] text-slate-500">
                <span>#</span>
                <span>Status</span>
                <span>Remaining</span>
                <span>Retry</span>
              </div>
              <div className="divide-y divide-slate-200 bg-white">
                {history.map((entry) => (
                  <div key={entry.index} className="grid grid-cols-[0.7fr_1fr_1fr_1fr] gap-3 px-4 py-3 text-sm text-slate-700">
                    <span className="font-semibold text-slate-950">{entry.index}</span>
                    <span className={entry.status === 'ALLOWED' ? 'font-semibold text-emerald-700' : 'font-semibold text-rose-700'}>
                      {entry.status}
                    </span>
                    <span>{entry.remainingTokens}</span>
                    <span>{entry.retryAfterSeconds}s</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </SectionCard>
      </div>

      <div className="grid gap-4 self-start">
        <StatusCard label="Allowed" value={String(summary.allowed)} tone="success" />
        <StatusCard label="Blocked" value={String(summary.blocked)} tone="danger" />
        <StatusCard label="Total Sent" value={String(summary.total)} />
        <StatusCard label="Last Decision" value={lastResult?.status ?? 'WAITING'} tone={decisionTone} />
        <StatusCard label="Remaining Tokens" value={String(lastResult?.remainingTokens ?? 0)} />
        <StatusCard label="Retry After" value={`${lastResult?.retryAfterSeconds ?? 0}s`} />

        <SectionCard
          title="Backend Response"
          description="The final request response is shown here exactly as the simulation service returned it."
        >
          {error ? (
            <p className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">{error}</p>
          ) : lastResult ? (
            <div className="grid gap-3 text-sm text-slate-700">
              <p><span className="font-semibold text-slate-950">Allowed:</span> {String(lastResult.allowed)}</p>
              <p><span className="font-semibold text-slate-950">Message:</span> {lastResult.message}</p>
              <p><span className="font-semibold text-slate-950">Remaining:</span> {lastResult.remainingTokens}</p>
              <p><span className="font-semibold text-slate-950">Retry:</span> {lastResult.retryAfterSeconds}s</p>
            </div>
          ) : (
            <p className="text-sm leading-6 text-slate-600">
              Run the simulator to compare allowed and blocked requests for the selected algorithm.
            </p>
          )}
        </SectionCard>
      </div>
    </div>
  );
}
