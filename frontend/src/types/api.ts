export type AlgorithmType = 'FIXED_WINDOW' | 'SLIDING_WINDOW' | 'TOKEN_BUCKET';

export interface ApiError {
  code: string;
  message: string;
  details: string[];
  timestamp: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  error: ApiError | null;
}

export interface SimulationRequest {
  algorithm: AlgorithmType;
  clientKey: string;
  resource: string;
  limit: number;
  windowSizeInSeconds: number;
  requestCount: number;
}

export interface SimulationResult {
  status: 'ALLOWED' | 'BLOCKED';
  allowed: boolean;
  remainingTokens: number;
  retryAfterSeconds: number;
  message: string;
}

export interface DashboardEvent {
  id: string;
  createdAt: string;
  algorithm: AlgorithmType;
  clientKey: string;
  resource: string;
  allowed: boolean;
  remainingTokens: number;
  retryAfterSeconds: number;
}

export interface DashboardSummary {
  allowed: number;
  blocked: number;
  total: number;
}

export interface DashboardSnapshot {
  summary: DashboardSummary;
  events: DashboardEvent[];
}
