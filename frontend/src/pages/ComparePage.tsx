import { useState } from 'react';
import type { ChangeEvent, FormEvent, ReactNode } from 'react';
import { SectionCard } from '../components/ui/SectionCard';
import { StatusCard } from '../components/ui/StatusCard';
import { recordSimulationEvent } from '../lib/dashboardMetrics';
import { simulateRateLimit } from '../services/api/simulation';
import type { AlgorithmType, SimulationRequest, SimulationResult } from '../types/api';

interface CompareFormState {
  clientKey: string;
  resource: string;
  limit: number;
  windowSizeInSeconds: number;
  requestCount: number;
}

interface CompareRun {
  algorithm: AlgorithmType;
  allowed: number;
  blocked: number;
  lastResult: SimulationResult | null;
}

const algorithms: AlgorithmType[] = ['FIXED_WINDOW', 'SLIDING_WINDOW', 'TOKEN_BUCKET'];

const defaultForm: CompareFormState = {
  clientKey: 'compare-client',
  resource: '/api/search',
  limit: 5,
  windowSizeInSeconds: 60,
  requestCount: 10,
};

export function ComparePage() {
  const [form, setForm] = useState<CompareFormState>(defaultForm);
  const [runs, setRuns] = useState<CompareRun[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target;
    setForm((current) => ({
      ...current,
      [name]: name === 'limit' || name === 'windowSizeInSeconds' || name === 'requestCount' ? Number(value) : value,
    }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setError('');
    setRuns([]);

    try {
      const nextRuns: CompareRun[] = [];

      for (const algorithm of algorithms) {
        let allowed = 0;
        let blocked = 0;
        let lastResult: SimulationResult | null = null;

        const payload: SimulationRequest = {
          algorithm,
          clientKey: `${form.clientKey}-${algorithm.toLowerCase()}`,
          resource: form.resource,
          limit: form.limit,
          windowSizeInSeconds: form.windowSizeInSeconds,
          requestCount: form.requestCount,
        };

        for (let index = 0; index < payload.requestCount; index += 1) {
          const result = await simulateRateLimit(payload);
          recordSimulationEvent(payload, result);
          lastResult = result;

          if (result.allowed) {
            allowed += 1;
          } else {
            blocked += 1;
          }
        }

        nextRuns.push({
          algorithm,
          allowed,
          blocked,
          lastResult,
        });

        setRuns([...nextRuns]);
      }
    } catch (submissionError) {
      setError(submissionError instanceof Error ? submissionError.message : 'Comparison failed');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="grid gap-6">
      <SectionCard
        title="Compare Algorithms"
        description="This page sends the same workload through fixed window, sliding window, and token bucket so you can compare how each algorithm behaves under identical settings."
      >
        <form className="grid gap-5" onSubmit={handleSubmit}>
          <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-5">
            <Field label="Client Key">
              <input name="clientKey" value={form.clientKey} onChange={handleChange} className="input-base" />
            </Field>

            <Field label="Resource">
              <input name="resource" value={form.resource} onChange={handleChange} className="input-base" />
            </Field>

            <Field label="Limit">
              <input name="limit" type="number" min={1} value={form.limit} onChange={handleChange} className="input-base" />
            </Field>

            <Field label="Window Size In Seconds">
              <input name="windowSizeInSeconds" type="number" min={1} value={form.windowSizeInSeconds} onChange={handleChange} className="input-base" />
            </Field>

            <Field label="Requests To Send">
              <input name="requestCount" type="number" min={1} value={form.requestCount} onChange={handleChange} className="input-base" />
            </Field>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="inline-flex w-full items-center justify-center rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400 sm:w-auto"
          >
            {isSubmitting ? 'Comparing Algorithms...' : 'Compare Algorithms'}
          </button>
        </form>
      </SectionCard>

      {error ? (
        <p className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">{error}</p>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-3">
        {runs.map((run) => (
          <SectionCard
            key={run.algorithm}
            title={formatAlgorithm(run.algorithm)}
            description={algorithmObservation(run.algorithm)}
          >
            <div className="grid gap-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <StatusCard label="Allowed" value={String(run.allowed)} tone="success" />
                <StatusCard label="Blocked" value={String(run.blocked)} tone="danger" />
              </div>
              <div className="rounded-[1.25rem] border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-700">
                <p className="font-semibold text-slate-950">What to observe</p>
                <p className="mt-2 leading-6">{algorithmDifference(run.algorithm)}</p>
              </div>
              <div className="rounded-[1.25rem] border border-slate-200 bg-white px-4 py-4 text-sm text-slate-700 shadow-sm">
                <p><span className="font-semibold text-slate-950">Last status:</span> {run.lastResult?.status ?? 'WAITING'}</p>
                <p className="mt-2"><span className="font-semibold text-slate-950">Remaining:</span> {run.lastResult?.remainingTokens ?? 0}</p>
                <p className="mt-2"><span className="font-semibold text-slate-950">Retry:</span> {run.lastResult?.retryAfterSeconds ?? 0}s</p>
              </div>
            </div>
          </SectionCard>
        ))}
      </div>

      <SectionCard
        title="Why Results Differ"
        description="The same request batch does not produce the same outcome because each algorithm models time and burst traffic differently."
      >
        <div className="grid gap-4 text-sm leading-6 text-slate-600 lg:grid-cols-3">
          <div className="rounded-[1.25rem] border border-slate-200 bg-white px-4 py-4 shadow-sm">
            <p className="font-semibold text-slate-950">Fixed Window</p>
            <p className="mt-2">Observe how it treats the whole time bucket as one counter. It is fast, but edge bursts near the boundary can feel unfair.</p>
          </div>
          <div className="rounded-[1.25rem] border border-slate-200 bg-white px-4 py-4 shadow-sm">
            <p className="font-semibold text-slate-950">Sliding Window</p>
            <p className="mt-2">Observe how it is stricter and fairer because every request timestamp stays in the active rolling window until it truly expires.</p>
          </div>
          <div className="rounded-[1.25rem] border border-slate-200 bg-white px-4 py-4 shadow-sm">
            <p className="font-semibold text-slate-950">Token Bucket</p>
            <p className="mt-2">Observe how it tolerates short bursts better. Saved tokens let requests through early, but sustained pressure still drains the bucket.</p>
          </div>
        </div>
      </SectionCard>
    </div>
  );
}

function formatAlgorithm(algorithm: AlgorithmType) {
  switch (algorithm) {
    case 'FIXED_WINDOW':
      return 'Fixed Window';
    case 'SLIDING_WINDOW':
      return 'Sliding Window';
    case 'TOKEN_BUCKET':
      return 'Token Bucket';
  }
}

function algorithmObservation(algorithm: AlgorithmType) {
  switch (algorithm) {
    case 'FIXED_WINDOW':
      return 'Best for seeing coarse quota behavior with the lowest operational complexity.';
    case 'SLIDING_WINDOW':
      return 'Best for seeing fairness because recent timestamps stay in the rolling window.';
    case 'TOKEN_BUCKET':
      return 'Best for seeing burst tolerance because tokens refill over time.';
  }
}

function algorithmDifference(algorithm: AlgorithmType) {
  switch (algorithm) {
    case 'FIXED_WINDOW':
      return 'If a batch fits in the current bucket, fixed window may allow more early requests before blocking. This is why its totals can look generous compared with sliding window.';
    case 'SLIDING_WINDOW':
      return 'Sliding window tends to block sooner during dense request bursts because it counts every request still inside the rolling interval instead of resetting at a boundary.';
    case 'TOKEN_BUCKET':
      return 'Token bucket often allows an initial burst, then starts blocking once the bucket empties. The exact mix depends on how much refill happens during the run.';
  }
}

interface FieldProps {
  label: string;
  children: ReactNode;
}

function Field({ label, children }: FieldProps) {
  return (
    <label className="grid gap-2 text-sm font-semibold text-slate-700">
      <span>{label}</span>
      {children}
    </label>
  );
}
