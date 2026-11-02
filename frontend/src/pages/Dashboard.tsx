import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import DashboardLayout from '../components/DashboardLayout';
import { dashboardApi } from '../api/dashboard';
import {
  AlertTriangle, ArrowRight, DollarSign, Users, FileText,
  TrendingUp, Clock, ArrowUpRight
} from 'lucide-react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);

const formatRelativeTime = (iso: string) => {
  const diff = Date.now() - new Date(iso).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
};

const Skeleton: React.FC<{ className?: string }> = ({ className = '' }) => (
  <div className={`animate-pulse bg-white/[0.04] rounded-2xl ${className}`} />
);

const Dashboard: React.FC = () => {
  const { user } = useAuth();
  const needsSetup = !user?.businessName;

  const summaryQuery = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: dashboardApi.getDashboardSummary,
  });

  const chartQuery = useQuery({
    queryKey: ['dashboard-revenue-chart'],
    queryFn: dashboardApi.getRevenueChart,
  });

  const activityQuery = useQuery({
    queryKey: ['dashboard-activity'],
    queryFn: dashboardApi.getActivityFeed,
  });

  const summary = summaryQuery.data;
  const stats = [
    {
      title: 'Total Revenue',
      value: summary ? formatCurrency(summary.totalRevenue) : '—',
      icon: DollarSign,
      change: summary?.totalOverdueThisMonth
        ? `${formatCurrency(summary.totalOverdueThisMonth)} overdue`
        : 'All accounts in good standing',
      badgeColor: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20',
      iconGradient: 'from-emerald-500/20 to-teal-500/20 text-emerald-400 border-emerald-500/30',
    },
    {
      title: 'Active Clients',
      value: summary ? String(summary.activeClientsCount) : '—',
      icon: Users,
      change: 'Active billing relationships',
      badgeColor: 'text-indigo-400 bg-indigo-500/10 border-indigo-500/20',
      iconGradient: 'from-indigo-500/20 to-blue-500/20 text-indigo-400 border-indigo-500/30',
    },
    {
      title: 'Outstanding Balance',
      value: summary ? formatCurrency(summary.totalOutstanding) : '—',
      icon: FileText,
      change: 'Pending or awaiting payout',
      badgeColor: 'text-amber-400 bg-amber-500/10 border-amber-500/20',
      iconGradient: 'from-amber-500/20 to-orange-500/20 text-amber-400 border-amber-500/30',
    },
    {
      title: 'Proposal Conversion',
      value: summary ? `${summary.conversionRate.toFixed(1)}%` : '—',
      icon: TrendingUp,
      change: 'Proposals accepted to date',
      badgeColor: 'text-purple-400 bg-purple-500/10 border-purple-500/20',
      iconGradient: 'from-purple-500/20 to-pink-500/20 text-purple-400 border-purple-500/30',
    },
  ];

  return (
    <DashboardLayout>
      <div className="h-full flex flex-col gap-2.5 sm:gap-3">
        {/* Top greeting banner */}
        <div className="flex items-center justify-between gap-3 shrink-0 pb-1 border-b border-white/[0.06]">
          <div className="flex items-center gap-2.5">
            <h1 className="text-lg sm:text-xl font-bold text-white tracking-tight">
              Welcome back, {user?.fullName?.split(' ')[0] || 'User'}
            </h1>
            <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 shadow-sm shadow-emerald-400"></span>
              Operational
            </span>
          </div>
          <p className="text-[11px] text-zinc-400 hidden sm:block">
            Financial performance &amp; cashflow overview
          </p>
        </div>

        {/* Needs Setup Warning Card */}
        {needsSetup && (
          <div className="px-3 py-1.5 bg-amber-500/[0.08] border border-amber-500/30 rounded-lg flex items-center justify-between gap-3 shrink-0 backdrop-blur-sm">
            <div className="flex items-center gap-2 text-xs">
              <AlertTriangle className="w-3.5 h-3.5 text-amber-400 shrink-0" />
              <span className="text-amber-200 font-medium">Complete your business profile:</span>
              <span className="text-amber-400/80 hidden md:inline">Setup business address, VAT/tax credentials, and brand logo.</span>
            </div>
            <Link
              to="/settings"
              className="px-2.5 py-1 bg-amber-500 hover:bg-amber-400 text-black text-[11px] font-bold rounded flex items-center gap-1 shrink-0 transition-all shadow-sm"
            >
              Configure
              <ArrowRight className="w-3 h-3" />
            </Link>
          </div>
        )}

        {summaryQuery.isError && (
          <div className="p-2.5 bg-rose-500/10 border border-rose-500/20 rounded-lg text-rose-400 text-xs shrink-0">
            Failed to load dashboard summary metrics. Please refresh the page.
          </div>
        )}

        {/* 4 Stats Grid */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-2.5 sm:gap-3 shrink-0">
          {summaryQuery.isLoading
            ? Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="sharp-card p-3 rounded-lg">
                  <Skeleton className="h-3 w-16 mb-2" />
                  <Skeleton className="h-6 w-24 mb-1" />
                  <Skeleton className="h-2.5 w-14" />
                </div>
              ))
            : stats.map((stat, i) => {
                const Icon = stat.icon;
                return (
                  <div
                    key={i}
                    className="sharp-card-hover p-3 rounded-lg flex flex-col justify-between"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-bold tracking-wider text-zinc-400 uppercase">
                        {stat.title}
                      </span>
                      <div className={`p-1 rounded bg-gradient-to-br ${stat.iconGradient} border`}>
                        <Icon className="w-3 h-3" />
                      </div>
                    </div>
                    <div className="mt-2">
                      <h3 className="text-lg lg:text-xl font-bold text-white font-mono-num tracking-tight">
                        {stat.value}
                      </h3>
                      <p className="text-[10px] text-zinc-400 mt-0.5 flex items-center gap-1 truncate">
                        {stat.change}
                      </p>
                    </div>
                  </div>
                );
              })}
        </div>

        {/* Chart + Activity Side-by-side */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-2.5 sm:gap-3 flex-1 min-h-[220px] lg:min-h-0 items-stretch">
          {/* Revenue Chart */}
          <div className="lg:col-span-2 sharp-card p-3 sm:p-3.5 rounded-lg flex flex-col min-h-0">
            <div className="flex items-center justify-between mb-2 shrink-0">
              <div>
                <h3 className="text-xs sm:text-sm font-bold text-white tracking-tight">Revenue Trajectory</h3>
                <p className="text-[10px] text-zinc-400">Paid invoices over 12 rolling months</p>
              </div>
              <span className="text-[10px] px-2 py-0.5 rounded bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 font-bold font-mono-num uppercase tracking-wider">
                12M Cashflow
              </span>
            </div>

            <div className="flex-1 w-full min-h-[140px]">
              {chartQuery.isLoading ? (
                <Skeleton className="h-full w-full" />
              ) : chartQuery.isError ? (
                <div className="h-full flex items-center justify-center text-xs text-rose-400">
                  Failed to load revenue chart data.
                </div>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={chartQuery.data} margin={{ top: 8, right: 8, left: -24, bottom: 0 }}>
                    <defs>
                      <linearGradient id="revenueGradient" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
                        <stop offset="95%" stopColor="#6366f1" stopOpacity={0.0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                    <XAxis
                      dataKey="month"
                      tick={{ fill: '#94a3b8', fontSize: 10 }}
                      axisLine={false}
                      tickLine={false}
                    />
                    <YAxis
                      tick={{ fill: '#94a3b8', fontSize: 10 }}
                      axisLine={false}
                      tickLine={false}
                      tickFormatter={(v) => `$${v}`}
                    />
                    <Tooltip
                      contentStyle={{
                        background: '#090d1a',
                        border: '1px solid rgba(255,255,255,0.12)',
                        borderRadius: 6,
                        boxShadow: '0 8px 20px -4px rgba(0, 0, 0, 0.6)',
                        fontSize: '11px',
                        padding: '6px 10px',
                      }}
                      labelStyle={{ color: '#f1f5f9', fontWeight: 600 }}
                      itemStyle={{ color: '#818cf8', fontFamily: 'JetBrains Mono' }}
                      formatter={(value: number) => [formatCurrency(value), 'Revenue']}
                    />
                    <Area
                      type="monotone"
                      dataKey="revenue"
                      stroke="#6366f1"
                      strokeWidth={1.5}
                      fill="url(#revenueGradient)"
                      activeDot={{ r: 4, fill: '#6366f1', stroke: '#ffffff', strokeWidth: 1.5 }}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              )}
            </div>
          </div>

          {/* Activity Feed */}
          <div className="sharp-card p-3 sm:p-3.5 rounded-lg flex flex-col min-h-0">
            <div className="flex items-center justify-between mb-2 shrink-0">
              <div>
                <h3 className="text-xs sm:text-sm font-bold text-white tracking-tight">Recent Activity</h3>
                <p className="text-[10px] text-zinc-400">Real-time event stream</p>
              </div>
              <Clock className="w-3.5 h-3.5 text-zinc-500" />
            </div>

            <div className="flex-1 overflow-y-auto space-y-1 min-h-[140px] pr-1">
              {activityQuery.isLoading ? (
                <div className="space-y-2">
                  {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-8 w-full" />)}
                </div>
              ) : activityQuery.isError ? (
                <p className="text-xs text-rose-400 py-4 text-center">Failed to load activity stream.</p>
              ) : activityQuery.data && activityQuery.data.length === 0 ? (
                <div className="h-full flex flex-col items-center justify-center text-center py-4">
                  <div className="w-7 h-7 rounded bg-white/[0.03] border border-white/[0.06] flex items-center justify-center text-zinc-500 mb-1.5">
                    <FileText className="w-3.5 h-3.5" />
                  </div>
                  <p className="text-[11px] text-zinc-400">No events yet.</p>
                </div>
              ) : (
                <div className="divide-y divide-white/[0.04]">
                  {activityQuery.data?.map((event, i) => (
                    <Link
                      key={i}
                      to={event.linkTarget}
                      className="group flex items-center justify-between py-1.5 hover:bg-white/[0.03] -mx-1 px-1.5 rounded transition-all"
                    >
                      <div className="min-w-0 pr-2">
                        <span className="text-[11px] font-medium text-zinc-200 group-hover:text-indigo-300 block truncate transition-colors">
                          {event.description}
                        </span>
                        <span className="text-[9px] text-zinc-500 block font-mono-num">
                          {formatRelativeTime(event.timestamp)}
                        </span>
                      </div>
                      <ArrowUpRight className="w-3 h-3 text-zinc-600 group-hover:text-zinc-300 shrink-0 transition-colors" />
                    </Link>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Quick Launchpad Strip */}
        <div className="sharp-card px-3 py-2 rounded-lg shrink-0">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
            {[
              {
                step: '01',
                title: 'Business Identity',
                desc: 'Company address, tax details & logo',
                link: '/settings',
                label: 'Setup Profile',
              },
              {
                step: '02',
                title: 'Register Clients',
                desc: 'Client directory & payment terms',
                link: '/clients',
                label: 'Add Client',
              },
              {
                step: '03',
                title: 'Invoices & Billing',
                desc: 'Draft line items & issue PDF links',
                link: '/invoices',
                label: 'Manage Invoices',
              },
            ].map((step, idx) => (
              <Link
                key={idx}
                to={step.link}
                className="group bg-white/[0.015] hover:bg-white/[0.04] border border-white/[0.06] hover:border-indigo-500/40 px-2.5 py-1.5 rounded-md flex items-center justify-between transition-all duration-150"
              >
                <div className="flex items-center gap-2 min-w-0">
                  <span className="text-[9px] font-mono-num font-bold text-indigo-400 bg-indigo-500/10 px-1 py-0.5 rounded border border-indigo-500/20 shrink-0">
                    {step.step}
                  </span>
                  <div className="min-w-0">
                    <h4 className="font-bold text-[11px] text-white truncate group-hover:text-indigo-200 transition-colors">
                      {step.title}
                    </h4>
                    <p className="text-[10px] text-zinc-400 truncate">
                      {step.desc}
                    </p>
                  </div>
                </div>
                <ArrowRight className="w-3 h-3 text-zinc-500 group-hover:text-indigo-400 group-hover:translate-x-0.5 transition-all shrink-0 ml-1.5" />
              </Link>
            ))}
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default Dashboard;
