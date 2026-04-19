import { useEffect, useMemo, useState } from 'react';
import { apiClient } from '../../services/api/client';
import type { HealthResponse, SimulationRequest, SimulationResult } from '../../types/api';

const WARMUP_STORAGE_KEY = 'rateshield-render-warmup-complete';
const WARMUP_DURATION_SECONDS = 50;

const warmupSimulationPayload: SimulationRequest = {
  algorithm: 'FIXED_WINDOW',
  clientKey: 'render-warmup-client',
  resource: '/warmup',
  limit: 5,
  windowSizeInSeconds: 60,
  requestCount: 1,
};

interface BackendSimulationRequest {
  algorithm: SimulationRequest['algorithm'];
  subjectType: string;
  subjectId: string;
  resource: string;
  limit: number;
  windowSizeInSeconds: number;
}

function buildWarmupRequest(payload: SimulationRequest): BackendSimulationRequest {
  return {
    algorithm: payload.algorithm,
    subjectType: 'CLIENT',
    subjectId: payload.clientKey,
    resource: payload.resource,
    limit: payload.limit,
    windowSizeInSeconds: payload.windowSizeInSeconds,
  };
}

async function warmupBackend() {
  await Promise.allSettled([
    apiClient.get<HealthResponse>('/api/v1/health'),
    apiClient.post<SimulationResult, BackendSimulationRequest>('/simulate', buildWarmupRequest(warmupSimulationPayload)),
  ]);
}

function hasCompletedWarmup() {
  if (typeof window === 'undefined') {
    return true;
  }

  return window.localStorage.getItem(WARMUP_STORAGE_KEY) === 'true';
}

export function RenderWarmupGate() {
  const [remainingSeconds, setRemainingSeconds] = useState(WARMUP_DURATION_SECONDS);
  const [isVisible, setIsVisible] = useState(() => !hasCompletedWarmup());

  useEffect(() => {
    if (!isVisible) {
      return;
    }

    void warmupBackend();

    const intervalId = window.setInterval(() => {
      setRemainingSeconds((current) => {
        if (current <= 1) {
          window.clearInterval(intervalId);
          window.localStorage.setItem(WARMUP_STORAGE_KEY, 'true');
          setIsVisible(false);
          return 0;
        }

        return current - 1;
      });
    }, 1000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [isVisible]);

  const progress = useMemo(() => {
    return ((WARMUP_DURATION_SECONDS - remainingSeconds) / WARMUP_DURATION_SECONDS) * 100;
  }, [remainingSeconds]);

  if (!isVisible) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 px-4 backdrop-blur-sm">
      <div className="w-full max-w-lg rounded-[2rem] border border-white/15 bg-slate-950 px-6 py-8 text-white shadow-[0_24px_90px_rgba(15,23,42,0.45)] sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-[0.32em] text-cyan-300">Starting Backend</p>
        <h2 className="mt-4 text-3xl font-black tracking-tight sm:text-4xl">Wait {remainingSeconds}s while Render wakes up</h2>
        <p className="mt-4 text-sm leading-6 text-slate-300 sm:text-base">
          The backend can take around 50 seconds on the first visit. RateShield is sending warm-up requests to the health
          and simulation APIs now.
        </p>

        <div className="mt-8">
          <div className="h-3 overflow-hidden rounded-full bg-white/10">
            <div
              className="h-full rounded-full bg-gradient-to-r from-cyan-400 via-sky-300 to-emerald-300 transition-[width] duration-1000 ease-linear"
              style={{ width: `${progress}%` }}
            />
          </div>
          <div className="mt-3 flex items-center justify-between text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">
            <span>Preparing services</span>
            <span>{remainingSeconds}s left</span>
          </div>
        </div>
      </div>
    </div>
  );
}
