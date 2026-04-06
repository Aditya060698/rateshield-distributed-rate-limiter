import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div className="grid min-h-[50vh] place-items-center rounded-[2rem] border border-dashed border-slate-300 bg-white/70 p-10 text-center shadow-sm">
      <div>
        <p className="text-sm font-semibold uppercase tracking-[0.3em] text-slate-500">404</p>
        <h2 className="mt-3 text-3xl font-black tracking-tight text-slate-950">Page not found</h2>
        <p className="mt-3 text-sm leading-6 text-slate-600">
          The route does not exist yet. Head back to the overview and continue from there.
        </p>
        <Link
          to="/"
          className="mt-6 inline-flex rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
        >
          Back to Overview
        </Link>
      </div>
    </div>
  );
}
