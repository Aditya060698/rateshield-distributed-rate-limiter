import { createBrowserRouter } from 'react-router-dom';
import { AppShell } from '../components/layout/AppShell';
import { ComparePage } from '../pages/ComparePage';
import { DashboardPage } from '../pages/DashboardPage';
import { HomePage } from '../pages/HomePage';
import { SimulationPage } from '../pages/SimulationPage';
import { NotFoundPage } from '../pages/NotFoundPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    errorElement: <NotFoundPage />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'dashboard',
        element: <DashboardPage />,
      },
      {
        path: 'simulate',
        element: <SimulationPage />,
      },
      {
        path: 'compare',
        element: <ComparePage />,
      },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
]);
