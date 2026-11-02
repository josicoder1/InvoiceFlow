import React, { useState } from 'react';
import { NavLink, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard, Users, FileText, Briefcase, Settings, LogOut,
  User as UserIcon, Menu, X, Sparkles, ChevronRight
} from 'lucide-react';

const DashboardLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [logoError, setLogoError] = useState(false);

  React.useEffect(() => {
    setLogoError(false);
  }, [user?.businessLogoUrl]);

  const navigation = [
    { name: 'Dashboard', to: '/dashboard', icon: LayoutDashboard },
    { name: 'Clients', to: '/clients', icon: Users },
    { name: 'Invoices', to: '/invoices', icon: FileText },
    { name: 'Proposals', to: '/proposals', icon: Briefcase },
    { name: 'Settings', to: '/settings', icon: Settings },
  ];

  const currentRouteName = navigation.find((n) => location.pathname.startsWith(n.to))?.name || 'Workspace';

  return (
    <div className="h-screen bg-[#070913] flex text-zinc-100 relative selection:bg-indigo-500/30 overflow-hidden">
      {/* Background ambient lighting */}
      <div className="fixed top-0 left-64 w-[600px] h-[350px] bg-indigo-600/[0.04] rounded-full blur-[140px] pointer-events-none -z-10" />
      <div className="fixed bottom-0 right-0 w-[500px] h-[350px] bg-purple-600/[0.03] rounded-full blur-[140px] pointer-events-none -z-10" />

      {/* Mobile Backdrop */}
      {mobileMenuOpen && (
        <div
          className="fixed inset-0 bg-black/70 backdrop-blur-sm z-40 lg:hidden"
          onClick={() => setMobileMenuOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-60 h-screen bg-[#090d1a] border-r border-white/[0.08] flex flex-col shrink-0 transition-transform duration-300 lg:static lg:translate-x-0 ${
          mobileMenuOpen ? 'translate-x-0 shadow-2xl' : '-translate-x-full'
        }`}
      >
        {/* Logo Header */}
        <div className="h-14 flex items-center justify-between px-5 border-b border-white/[0.08]">
          <Link to="/dashboard" className="flex items-center gap-2.5 group">
            <div className="w-7 h-7 rounded-lg bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center shadow-md shadow-indigo-500/20 group-hover:scale-105 transition-all">
              <Sparkles className="w-3.5 h-3.5 text-white" />
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-sm font-bold tracking-tight text-white">
                InvoiceFlow
              </span>
              <span className="text-[9px] font-mono-num font-semibold text-zinc-400 bg-white/[0.05] px-1.5 py-0.5 rounded border border-white/[0.08]">
                v1.0
              </span>
            </div>
          </Link>
          <button
            onClick={() => setMobileMenuOpen(false)}
            className="lg:hidden p-1.5 rounded-lg text-zinc-400 hover:text-white hover:bg-white/[0.05]"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* User Profile Card */}
        <div className="p-2.5 mx-3 my-2 rounded-lg bg-white/[0.02] border border-white/[0.08] flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-md bg-gradient-to-br from-indigo-500/20 to-purple-500/20 border border-indigo-500/30 flex items-center justify-center text-indigo-300 font-bold text-xs shrink-0 overflow-hidden">
            {user?.businessLogoUrl && !logoError ? (
              <img
                src={user.businessLogoUrl}
                alt="Logo"
                onError={() => setLogoError(true)}
                className="w-full h-full object-cover"
              />
            ) : (
              user?.fullName ? user.fullName.substring(0, 2).toUpperCase() : <UserIcon className="w-3.5 h-3.5" />
            )}
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-xs font-semibold truncate text-white leading-tight">{user?.fullName || 'User'}</p>
            <p className="text-[11px] text-zinc-400 truncate mt-0.5">{user?.businessName || 'Freelance / Business'}</p>
          </div>
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 px-3 space-y-1 overflow-y-auto">
          {navigation.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.name}
                to={item.to}
                onClick={() => setMobileMenuOpen(false)}
                className={({ isActive }) =>
                  `flex items-center justify-between px-3 py-2 rounded-lg text-xs font-semibold tracking-normal transition-all duration-150 group ${
                    isActive
                      ? 'bg-indigo-500/15 border border-indigo-500/30 text-white shadow-sm shadow-indigo-500/10'
                      : 'text-zinc-400 hover:text-white hover:bg-white/[0.03] border border-transparent'
                  }`
                }
              >
                {({ isActive }) => (
                  <>
                    <div className="flex items-center gap-2.5">
                      <Icon
                        className={`w-4 h-4 transition-colors ${
                          isActive ? 'text-indigo-400' : 'text-zinc-500 group-hover:text-zinc-200'
                        }`}
                      />
                      <span>{item.name}</span>
                    </div>
                    {isActive && (
                      <span className="w-1.5 h-1.5 rounded-full bg-indigo-400 shadow-sm shadow-indigo-400" />
                    )}
                  </>
                )}
              </NavLink>
            );
          })}
        </nav>

        {/* Logout Button */}
        <div className="p-3 border-t border-white/[0.08]">
          <button
            onClick={logout}
            className="w-full flex items-center gap-2.5 px-3 py-2 text-zinc-400 hover:text-rose-400 hover:bg-rose-500/[0.08] border border-transparent hover:border-rose-500/20 rounded-lg text-xs font-medium transition-all"
          >
            <LogOut className="w-4 h-4" />
            Sign Out
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
        {/* Sticky Header */}
        <header className="h-12 lg:h-13 shrink-0 border-b border-white/[0.08] bg-[#060810]/90 backdrop-blur-xl flex items-center justify-between px-5 lg:px-6 z-30">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setMobileMenuOpen(true)}
              className="lg:hidden p-1.5 rounded-md text-zinc-400 hover:text-white hover:bg-white/[0.05]"
            >
              <Menu className="w-4 h-4" />
            </button>
            <div className="flex items-center gap-2 text-xs font-medium text-zinc-400">
              <span className="hidden sm:inline text-zinc-500">App</span>
              <ChevronRight className="w-3.5 h-3.5 text-zinc-600 hidden sm:inline" />
              <h2 className="font-semibold text-xs text-white">
                {currentRouteName}
              </h2>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <span className="inline-flex items-center gap-1.5 text-[11px] px-2.5 py-1 bg-white/[0.03] border border-white/[0.08] text-zinc-300 rounded-md font-medium">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 shadow-sm shadow-emerald-400"></span>
              {user?.plan || 'PRO'} Tier
            </span>
          </div>
        </header>

        {/* Content */}
        <main className="p-3 sm:p-4 lg:p-4.5 flex-1 overflow-y-auto max-w-7xl w-full mx-auto flex flex-col min-h-0">
          {children}
        </main>
      </div>
    </div>
  );
};

export default DashboardLayout;
