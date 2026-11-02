import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import DashboardLayout from '../components/DashboardLayout';
import { clientsApi } from '../api/clients';
import type { ClientHistory } from '../types';
import { ArrowLeft, Mail, MapPin, DollarSign, Edit, AlertCircle, Loader2, FileText, ChevronRight } from 'lucide-react';

const ClientDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [history, setHistory] = useState<ClientHistory | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    const fetchHistory = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await clientsApi.getClientHistory(id);
        setHistory(data);
      } catch (err: any) {
        setError(err.response?.data?.detail || 'Failed to load client details.');
      } finally {
        setLoading(false);
      }
    };
    fetchHistory();
  }, [id]);

  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center py-32 text-zinc-500 text-xs">
          <Loader2 className="w-5 h-5 animate-spin mr-2" /> Loading client profile...
        </div>
      </DashboardLayout>
    );
  }

  if (error || !history) {
    return (
      <DashboardLayout>
        <div className="text-center py-24 text-rose-400 flex flex-col items-center gap-3">
          <AlertCircle className="w-8 h-8" />
          <span className="text-sm">{error || 'Client not found'}</span>
          <button
            onClick={() => navigate('/clients')}
            className="mt-2 px-4 py-2 sharp-btn-secondary text-xs"
          >
            Back to Clients
          </button>
        </div>
      </DashboardLayout>
    );
  }

  const { client, invoices, totalBilled } = history;

  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/clients')}
              className="p-2 sharp-btn-secondary text-zinc-400 hover:text-white"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <div>
              <div className="flex items-center gap-2.5">
                <h1 className="text-xl font-bold tracking-tight text-white">
                  {client.name}
                </h1>
                <span className="text-[10px] font-mono-num font-semibold px-2 py-0.5 rounded bg-white/[0.04] border border-white/[0.08] text-zinc-400">
                  {client.currency}
                </span>
              </div>
              {client.company && (
                <p className="text-zinc-400 text-xs mt-0.5">{client.company}</p>
              )}
            </div>
          </div>
          <button
            onClick={() => navigate('/clients', { state: { editClientId: client.id } })}
            className="flex items-center gap-1.5 px-3 py-1.5 sharp-btn-secondary text-xs font-semibold"
          >
            <Edit className="w-3.5 h-3.5" />
            Edit Profile
          </button>
        </div>

        {/* Stats Row */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="sharp-card p-5">
            <span className="sharp-label block">Total Billed</span>
            <span className="text-2xl font-bold font-mono-num text-white mt-1.5 block">
              {new Intl.NumberFormat('en-US', { style: 'currency', currency: client.currency }).format(totalBilled)}
            </span>
          </div>
          <div className="sharp-card p-5">
            <span className="sharp-label block">Invoices Issued</span>
            <span className="text-2xl font-bold font-mono-num text-white mt-1.5 block">
              {invoices.length}
            </span>
          </div>
        </div>

        {/* Profile Details Card */}
        <div className="sharp-card p-6 space-y-4">
          <h2 className="sharp-label">Contact & Billing Profile</h2>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
            <div className="p-3.5 rounded-lg bg-white/[0.015] border border-white/[0.06] space-y-1">
              <span className="text-zinc-500 flex items-center gap-1.5 font-medium">
                <Mail className="w-3.5 h-3.5 text-indigo-400" /> Email
              </span>
              <p className="text-zinc-200 font-semibold truncate pt-1">{client.email}</p>
            </div>
            <div className="p-3.5 rounded-lg bg-white/[0.015] border border-white/[0.06] space-y-1">
              <span className="text-zinc-500 flex items-center gap-1.5 font-medium">
                <DollarSign className="w-3.5 h-3.5 text-emerald-400" /> Currency
              </span>
              <p className="text-zinc-200 font-semibold font-mono-num pt-1">{client.currency}</p>
            </div>
            <div className="p-3.5 rounded-lg bg-white/[0.015] border border-white/[0.06] space-y-1">
              <span className="text-zinc-500 flex items-center gap-1.5 font-medium">
                <MapPin className="w-3.5 h-3.5 text-purple-400" /> Address
              </span>
              <p className="text-zinc-200 font-semibold truncate pt-1">{client.address || 'No physical address'}</p>
            </div>
          </div>
        </div>

        {/* Invoice history */}
        <div className="sharp-card overflow-hidden">
          <div className="px-5 py-3.5 border-b border-white/[0.08] flex items-center justify-between">
            <div className="flex items-center gap-2">
              <FileText className="w-4 h-4 text-indigo-400" />
              <h2 className="text-xs font-semibold text-white uppercase tracking-wider">Invoice History</h2>
            </div>
            <span className="font-mono-num text-xs text-zinc-400">{invoices.length} entries</span>
          </div>

          {invoices.length === 0 ? (
            <div className="p-12 text-center text-zinc-500 text-xs">
              No invoices issued for this client yet.
            </div>
          ) : (
            <div className="divide-y divide-white/[0.06]">
              {invoices.map((inv) => (
                <div
                  key={inv.id}
                  className="px-5 py-3.5 flex items-center justify-between hover:bg-white/[0.02] transition-colors cursor-pointer group"
                  onClick={() => navigate('/invoices')}
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-mono-num text-xs font-semibold text-white group-hover:text-indigo-300 transition-colors">
                        {inv.invoiceNumber}
                      </span>
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold uppercase tracking-wider ${
                          inv.status === 'PAID'
                            ? 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-400'
                            : inv.status === 'SENT'
                            ? 'bg-blue-500/10 border border-blue-500/20 text-blue-400'
                            : inv.status === 'OVERDUE'
                            ? 'bg-rose-500/10 border border-rose-500/20 text-rose-400'
                            : inv.status === 'CANCELLED'
                            ? 'bg-orange-500/10 border border-orange-500/20 text-orange-400'
                            : 'bg-zinc-500/10 border border-zinc-500/20 text-zinc-400'
                        }`}
                      >
                        <span className={`w-1.5 h-1.5 rounded-full ${
                          inv.status === 'PAID' ? 'bg-emerald-400' :
                          inv.status === 'SENT' ? 'bg-blue-400' :
                          inv.status === 'OVERDUE' ? 'bg-rose-400' : 'bg-zinc-400'
                        }`} />
                        {inv.status}
                      </span>
                    </div>
                    <span className="text-[11px] font-mono-num text-zinc-500 block">
                      Issued {inv.issueDate} · Due {inv.dueDate}
                    </span>
                  </div>

                  <div className="flex items-center gap-4">
                    <span className="font-bold font-mono-num text-sm text-white">
                      {new Intl.NumberFormat('en-US', { style: 'currency', currency: inv.currency }).format(inv.total)}
                    </span>
                    <ChevronRight className="w-4 h-4 text-zinc-600 group-hover:text-zinc-400 transition-colors" />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
};

export default ClientDetail;
