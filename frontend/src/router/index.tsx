import { lazy, Suspense } from 'react';
import type { ComponentType, ReactElement } from 'react';
import { createBrowserRouter } from 'react-router-dom';
import { AppShell } from '../components/layout/AppShell';

const HomePage = lazy(() => loadPage(() => import('../pages/HomePage'), 'HomePage'));
const DashboardPage = lazy(() => loadPage(() => import('../pages/DashboardPage'), 'DashboardPage'));
const SimulationPage = lazy(() => loadPage(() => import('../pages/SimulationPage'), 'SimulationPage'));
const ComparePage = lazy(() => loadPage(() => import('../pages/ComparePage'), 'ComparePage'));
const NotFoundPage = lazy(() => loadPage(() => import('../pages/NotFoundPage'), 'NotFoundPage'));

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    errorElement: withSuspense(<NotFoundPage />),
    children: [
      {
        index: true,
        element: withSuspense(<HomePage />),
      },
      {
        path: 'dashboard',
        element: withSuspense(<DashboardPage />),
      },
      {
        path: 'simulate',
        element: withSuspense(<SimulationPage />),
      },
      {
        path: 'compare',
        element: withSuspense(<ComparePage />),
      },
    ],
  },
  {
    path: '*',
    element: withSuspense(<NotFoundPage />),
  },
]);

function loadPage<TModule extends Record<string, ComponentType>>(
  factory: () => Promise<TModule>,
  exportName: keyof TModule,
) {
  return factory().then((module) => ({
    default: module[exportName] as ComponentType,
  }));
}

function withSuspense(element: ReactElement) {
  return (
    <Suspense
      fallback={
        <div className="grid min-h-[40vh] place-items-center rounded-[2rem] border border-dashed border-slate-300 bg-white/70 p-10 text-center shadow-sm">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.28em] text-slate-500">Loading</p>
            <p className="mt-3 text-lg font-semibold text-slate-900">Preparing page bundle...</p>
          </div>
        </div>
      }
    >
      {element}
    </Suspense>
  );
}
