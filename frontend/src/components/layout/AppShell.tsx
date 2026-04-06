import { NavLink, Outlet } from 'react-router-dom';

const navigation = [
  { label: 'Overview', to: '/' },
  { label: 'Dashboard', to: '/dashboard' },
  { label: 'Simulator', to: '/simulate' },
  { label: 'Compare', to: '/compare' },
];

export function AppShell() {
  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(15,23,42,0.18),_transparent_35%),linear-gradient(180deg,_#f8fafc_0%,_#eef2ff_100%)] text-slate-950">
      <div className="mx-auto flex min-h-screen max-w-7xl flex-col px-4 py-6 sm:px-6 lg:px-8">
        <header className="mb-10 flex flex-col gap-6 rounded-[2rem] border border-white/70 bg-white/75 p-6 shadow-[0_20px_80px_rgba(15,23,42,0.08)] backdrop-blur md:flex-row md:items-end md:justify-between">
          <div className="max-w-2xl">
            <p className="mb-3 inline-flex items-center rounded-full bg-slate-950 px-3 py-1 text-xs font-semibold uppercase tracking-[0.28em] text-cyan-200">
              Distributed Rate Limiting Lab
            </p>
            <h1 className="text-4xl font-black tracking-tight text-slate-950 sm:text-5xl">
              RateShield Frontend
            </h1>
            <p className="mt-3 max-w-xl text-sm leading-6 text-slate-600 sm:text-base">
              Explore fixed window, sliding window, and token bucket behavior through a focused simulation UI connected to your Spring Boot backend.
            </p>
          </div>

          <nav className="flex flex-wrap gap-3">
            {navigation.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  [
                    'rounded-full px-4 py-2 text-sm font-semibold transition',
                    isActive
                      ? 'bg-slate-950 text-white shadow-lg'
                      : 'bg-slate-100 text-slate-700 hover:bg-slate-200',
                  ].join(' ')
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </header>

        <main className="flex-1">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
