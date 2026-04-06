import type { ReactNode } from 'react';

interface SectionCardProps {
  title: string;
  description: string;
  children: ReactNode;
}

export function SectionCard({ title, description, children }: SectionCardProps) {
  return (
    <section className="rounded-[2rem] border border-white/70 bg-white/80 p-6 shadow-[0_20px_80px_rgba(15,23,42,0.06)] backdrop-blur">
      <div className="mb-6">
        <h2 className="text-2xl font-black tracking-tight text-slate-950">{title}</h2>
        <p className="mt-2 text-sm leading-6 text-slate-600">{description}</p>
      </div>
      {children}
    </section>
  );
}
