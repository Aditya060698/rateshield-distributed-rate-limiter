import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ComposedChart,
  Line,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { DashboardEvent, DashboardSummary } from '../../types/api';
import { SectionCard } from '../ui/SectionCard';

interface DashboardChartsProps {
  summary: DashboardSummary;
  events: DashboardEvent[];
}

export function DashboardCharts({ summary, events }: DashboardChartsProps) {
  const decisionData = [
    { name: 'Allowed', value: summary.allowed, color: '#059669' },
    { name: 'Blocked', value: summary.blocked, color: '#e11d48' },
  ];

  const timelineData = events.slice(-20).map((event, index) => ({
    request: index + 1,
    remainingTokens: event.remainingTokens,
    blocked: event.allowed ? 0 : 1,
  }));

  const algorithmData = ['FIXED_WINDOW', 'SLIDING_WINDOW', 'TOKEN_BUCKET'].map((algorithm) => ({
    algorithm,
    total: events.filter((event) => event.algorithm === algorithm).length,
  }));

  return (
    <div className="grid gap-6">
      <div className="grid gap-6 lg:grid-cols-2">
        <SectionCard
          title="Decision Split"
          description="This chart groups all stored simulator events into allowed and blocked outcomes."
        >
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={decisionData} dataKey="value" nameKey="name" innerRadius={68} outerRadius={100} paddingAngle={4}>
                  {decisionData.map((entry) => (
                    <Cell key={entry.name} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </SectionCard>

        <SectionCard
          title="Algorithm Activity"
          description="This bar chart shows how many requests were simulated through each algorithm."
        >
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={algorithmData}>
                <CartesianGrid strokeDasharray="4 4" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="algorithm" tickLine={false} axisLine={false} fontSize={12} />
                <YAxis allowDecimals={false} tickLine={false} axisLine={false} fontSize={12} />
                <Tooltip />
                <Bar dataKey="total" radius={[14, 14, 0, 0]} fill="#0f172a" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </SectionCard>
      </div>

      <SectionCard
        title="Recent Request Flow"
        description="The chart plots remaining tokens for recent requests, while the bars mark requests that were blocked."
      >
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={timelineData}>
              <CartesianGrid strokeDasharray="4 4" vertical={false} stroke="#e2e8f0" />
              <XAxis dataKey="request" tickLine={false} axisLine={false} fontSize={12} />
              <YAxis allowDecimals={false} tickLine={false} axisLine={false} fontSize={12} />
              <Tooltip />
              <Bar dataKey="blocked" fill="#fb7185" radius={[10, 10, 0, 0]} />
              <Line type="monotone" dataKey="remainingTokens" stroke="#06b6d4" strokeWidth={3} dot={{ r: 3 }} />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </SectionCard>
    </div>
  );
}
