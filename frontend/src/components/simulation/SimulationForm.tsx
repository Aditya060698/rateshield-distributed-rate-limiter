import { useState } from 'react';
import type { ChangeEvent, FormEvent, ReactNode } from 'react';
import type { AlgorithmType, SimulationRequest } from '../../types/api';

interface SimulationFormProps {
  onSubmit: (payload: SimulationRequest) => Promise<void>;
  isSubmitting: boolean;
}

const defaultPayload: SimulationRequest = {
  algorithm: 'TOKEN_BUCKET',
  clientKey: 'demo-client',
  resource: '/api/search',
  limit: 5,
  windowSizeInSeconds: 60,
  requestCount: 10,
};

export function SimulationForm({ onSubmit, isSubmitting }: SimulationFormProps) {
  const [form, setForm] = useState<SimulationRequest>(defaultPayload);

  function handleChange(event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    const { name, value } = event.target;
    setForm((current) => ({
      ...current,
      [name]: name === 'limit' || name === 'windowSizeInSeconds' || name === 'requestCount' ? Number(value) : value,
    }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await onSubmit(form);
  }

  return (
    <form className="grid gap-5" onSubmit={handleSubmit}>
      <div className="grid gap-5 md:grid-cols-2">
        <Field label="Client Key">
          <input name="clientKey" value={form.clientKey} onChange={handleChange} className="input-base" />
        </Field>

        <Field label="Algorithm">
          <select
            name="algorithm"
            value={form.algorithm}
            onChange={handleChange}
            className="input-base"
          >
            <option value={'FIXED_WINDOW' satisfies AlgorithmType}>Fixed Window</option>
            <option value={'SLIDING_WINDOW' satisfies AlgorithmType}>Sliding Window</option>
            <option value={'TOKEN_BUCKET' satisfies AlgorithmType}>Token Bucket</option>
          </select>
        </Field>

        <Field label="Resource">
          <input name="resource" value={form.resource} onChange={handleChange} className="input-base" />
        </Field>

        <Field label="Requests To Send">
          <input name="requestCount" type="number" min={1} value={form.requestCount} onChange={handleChange} className="input-base" />
        </Field>

        <Field label="Limit">
          <input name="limit" type="number" min={1} value={form.limit} onChange={handleChange} className="input-base" />
        </Field>

        <Field label="Window Size In Seconds">
          <input
            name="windowSizeInSeconds"
            type="number"
            min={1}
            value={form.windowSizeInSeconds}
            onChange={handleChange}
            className="input-base"
          />
        </Field>
      </div>

      <button
        type="submit"
        disabled={isSubmitting}
        className="inline-flex w-full items-center justify-center rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400 sm:w-auto"
      >
        {isSubmitting ? 'Sending Requests...' : 'Send Requests'}
      </button>
    </form>
  );
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
