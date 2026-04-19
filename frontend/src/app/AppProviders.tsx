import type { ReactNode } from 'react';
import { RenderWarmupGate } from '../components/layout/RenderWarmupGate';

interface AppProvidersProps {
  children: ReactNode;
}

export function AppProviders({ children }: AppProvidersProps) {
  return (
    <>
      <RenderWarmupGate />
      {children}
    </>
  );
}
