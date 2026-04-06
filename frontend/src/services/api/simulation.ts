import { apiClient } from './client';
import type { SimulationRequest, SimulationResult } from '../../types/api';

interface BackendSimulationRequest {
  algorithm: SimulationRequest['algorithm'];
  subjectType: string;
  subjectId: string;
  resource: string;
  limit: number;
  windowSizeInSeconds: number;
}

export async function simulateRateLimit(payload: SimulationRequest): Promise<SimulationResult> {
  const requestBody: BackendSimulationRequest = {
    algorithm: payload.algorithm,
    subjectType: 'CLIENT',
    subjectId: payload.clientKey,
    resource: payload.resource,
    limit: payload.limit,
    windowSizeInSeconds: payload.windowSizeInSeconds,
  };

  const response = await apiClient.post<SimulationResult, BackendSimulationRequest>('/simulate', requestBody);

  if (!response.data) {
    throw new Error('Simulation response did not include data');
  }

  return response.data;
}
