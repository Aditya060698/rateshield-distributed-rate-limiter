import { Link } from 'react-router-dom';
import { SectionCard } from '../components/ui/SectionCard';
import { StatusCard } from '../components/ui/StatusCard';

export function HomePage() {
  return (
    <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
      <SectionCard
        title="What This Frontend Covers"
        description="The frontend is split into an overview, a live dashboard, a simulator, and an algorithm comparison screen. That keeps each screen focused while the shared service layer handles backend communication."
      >
        <div className="grid gap-4 text-sm leading-6 text-slate-600">
          <p>
            Use the simulator to generate request traffic, open the dashboard to inspect metrics, and use the compare page to see how the same workload behaves across all three algorithms.
          </p>
          <p>
            Routing separates overview, dashboard, simulation, and comparison concerns so the project can grow without collapsing into one large page component.
          </p>
          <div className="flex flex-wrap gap-3">
            <Link
              to="/simulate"
              className="inline-flex rounded-full bg-cyan-500 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-cyan-400"
            >
              Open Simulator
            </Link>
            <Link
              to="/dashboard"
              className="inline-flex rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
            >
              Open Dashboard
            </Link>
            <Link
              to="/compare"
              className="inline-flex rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-emerald-400"
            >
              Compare Algorithms
            </Link>
          </div>
        </div>
      </SectionCard>

      <div className="grid gap-4">
        <StatusCard label="Algorithms" value="3 Ready" />
        <StatusCard label="Backend Endpoint" value="POST /simulate" />
        <StatusCard label="Analysis View" value="Compare Page" />
      </div>
    </div>
  );
}
