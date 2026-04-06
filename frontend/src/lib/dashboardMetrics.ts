import type { DashboardEvent, DashboardSnapshot, SimulationRequest, SimulationResult } from '../types/api';

const STORAGE_KEY = 'rateshield-dashboard-events';
const UPDATE_EVENT = 'rateshield-dashboard-updated';
const MAX_EVENTS = 120;

function canUseStorage() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined';
}

function readEvents(): DashboardEvent[] {
  if (!canUseStorage()) {
    return [];
  }

  const raw = window.localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return [];
  }

  try {
    return JSON.parse(raw) as DashboardEvent[];
  } catch {
    return [];
  }
}

function writeEvents(events: DashboardEvent[]) {
  if (!canUseStorage()) {
    return;
  }

  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(events));
  window.dispatchEvent(new Event(UPDATE_EVENT));
}

export function recordSimulationEvent(request: SimulationRequest, result: SimulationResult) {
  const current = readEvents();
  const nextEntry: DashboardEvent = {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
    createdAt: new Date().toISOString(),
    algorithm: request.algorithm,
    clientKey: request.clientKey,
    resource: request.resource,
    allowed: result.allowed,
    remainingTokens: result.remainingTokens,
    retryAfterSeconds: result.retryAfterSeconds,
  };

  const next = [...current, nextEntry].slice(-MAX_EVENTS);
  writeEvents(next);
}

export function readDashboardSnapshot(): DashboardSnapshot {
  const events = readEvents();
  const allowed = events.filter((event) => event.allowed).length;
  const blocked = events.length - allowed;

  return {
    summary: {
      allowed,
      blocked,
      total: events.length,
    },
    events,
  };
}

export function subscribeDashboard(listener: () => void) {
  if (typeof window === 'undefined') {
    return () => undefined;
  }

  const handleStorage = (event: StorageEvent) => {
    if (event.key === STORAGE_KEY) {
      listener();
    }
  };

  window.addEventListener(UPDATE_EVENT, listener);
  window.addEventListener('storage', handleStorage);

  return () => {
    window.removeEventListener(UPDATE_EVENT, listener);
    window.removeEventListener('storage', handleStorage);
  };
}
