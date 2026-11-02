import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../components/DashboardLayout';
import { proposalsApi } from '../api/proposals';
import { clientsApi } from '../api/clients';
import type { Proposal, Client, PageResponse } from '../types';
import {
  Plus, Search, Trash2, Edit, Eye, X, AlertCircle,
  Send, CheckCircle, Copy, FileText, Download, Loader2
} from 'lucide-react';

const statusStyles: Record<string, { badge: string; dot: string }> = {
  DRAFT: { badge: 'bg-zinc-500/10 border border-zinc-500/25 text-zinc-400', dot: 'bg-zinc-400' },
  SENT: { badge: 'bg-blue-500/10 border border-blue-500/25 text-blue-400', dot: 'bg-blue-400 shadow-sm shadow-blue-400' },
  VIEWED: { badge: 'bg-purple-500/10 border border-purple-500/25 text-purple-400', dot: 'bg-purple-400 shadow-sm shadow-purple-400' },
  ACCEPTED: { badge: 'bg-emerald-500/10 border border-emerald-500/25 text-emerald-400', dot: 'bg-emerald-400 shadow-sm shadow-emerald-400' },
  DECLINED: { badge: 'bg-rose-500/10 border border-rose-500/25 text-rose-400', dot: 'bg-rose-400 shadow-sm shadow-rose-400' },
};

const Proposals: React.FC = () => {
  const navigate = useNavigate();
  const [proposalsPage, setProposalsPage] = useState<PageResponse<Proposal> | null>(null);
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [statusFilter, setStatusFilter] = useState('');
  const [clientFilter, setClientFilter] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const [selectedProposal, setSelectedProposal] = useState<Proposal | null>(null);
  const [pdfLoading, setPdfLoading] = useState(false);

  const fetchProposals = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await proposalsApi.getProposals({
        page, size: 8,
        status: statusFilter || undefined,
        clientId: clientFilter || undefined,
        search: search || undefined,
      });
      setProposalsPage(data);
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to load proposals');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchProposals(); }, [statusFilter, clientFilter, search, page]);
  useEffect(() => {
    clientsApi.getClients(undefined, 0, 1000).then(d => setClients(d.content));
  }, []);

  const handleSend = async (id: string) => {
    if (!confirm('Send this proposal? An email with a public link will be sent to the client.')) return;
    try {
      const updated = await proposalsApi.sendProposal(id);
      fetchProposals();
      if (selectedProposal?.id === id) setSelectedProposal(updated);
    } catch (err: any) { alert(err.response?.data?.detail || 'Failed to send'); }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Delete this proposal?')) return;
    try {
      await proposalsApi.deleteProposal(id);
      fetchProposals();
      if (selectedProposal?.id === id) setSelectedProposal(null);
    } catch (err: any) { alert(err.response?.data?.detail || 'Cannot delete'); }
  };

  const handleConvert = async (id: string) => {
    if (!confirm('Convert this accepted proposal to an invoice?')) return;
    try {
      await proposalsApi.convertToInvoice(id);
      navigate('/invoices');
    } catch (err: any) { alert(err.response?.data?.detail || 'Failed to convert'); }
  };

  const handleDownloadPdf = async (id: string) => {
    setPdfLoading(true);
    try {
      await proposalsApi.downloadPdf(id);
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to download PDF');
    } finally {
      setPdfLoading(false);
    }
  };

  const clientName = (clientId: string) => clients.find(c => c.id === clientId)?.name || '—';
  const fmt = (d: string | null) => d ? new Date(d).toLocaleDateString() : '—';

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">Proposals</h1>
            <p className="text-zinc-400 text-xs sm:text-sm mt-0.5">Author, dispatch, and convert professional client bids into active invoices.</p>
          </div>
          <button
            onClick={() => navigate('/proposals/new')}
            className="flex items-center gap-2 px-4 py-2.5 bg-gradient-to-r from-indigo-500 via-indigo-600 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-semibold text-xs rounded-xl shadow-lg shadow-indigo-500/20 active:scale-[0.98] transition-all"
          >
            <Plus className="w-4 h-4" /> Create Proposal
          </button>
        </div>

        {/* Filters */}
        <div className="bg-[#0a0e1c] border border-white/[0.08] p-4 rounded-2xl grid grid-cols-1 sm:grid-cols-3 gap-3 shadow-sm">
          <div className="space-y-1.5 sm:col-span-1">
            <label className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">Search</label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-zinc-500" />
              <input
                type="text"
                placeholder="Title or client..."
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="w-full bg-white/[0.02] hover:bg-white/[0.03] border border-white/[0.08] rounded-xl pl-9 pr-3 py-2 text-xs text-zinc-200 placeholder-zinc-500 focus:outline-none focus:border-indigo-500/60 transition-all"
              />
            </div>
          </div>
          <div className="space-y-1.5">
            <label className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">Status</label>
            <select
              value={statusFilter}
              onChange={e => setStatusFilter(e.target.value)}
              className="w-full bg-[#0a0e1c] border border-white/[0.08] rounded-xl px-3 py-2 text-xs text-zinc-200 focus:outline-none focus:border-indigo-500/60 transition-all"
            >
              <option value="">All Statuses</option>
              {['DRAFT','SENT','VIEWED','ACCEPTED','DECLINED'].map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          <div className="space-y-1.5">
            <label className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">Client</label>
            <select
              value={clientFilter}
              onChange={e => setClientFilter(e.target.value)}
              className="w-full bg-[#0a0e1c] border border-white/[0.08] rounded-xl px-3 py-2 text-xs text-zinc-200 focus:outline-none focus:border-indigo-500/60 transition-all"
            >
              <option value="">All Clients</option>
              {clients.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
        </div>

        {/* Split layout */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
          {/* Table */}
          <div className="lg:col-span-2 bg-[#0a0e1c] border border-white/[0.08] rounded-2xl overflow-hidden flex flex-col shadow-sm">
            {loading && !proposalsPage ? (
              <div className="p-12 text-center text-zinc-500 text-xs">Loading proposals...</div>
            ) : error ? (
              <div className="p-12 text-center text-rose-400 text-xs flex flex-col items-center gap-2">
                <AlertCircle className="w-7 h-7" /><span>{error}</span>
              </div>
            ) : proposalsPage?.content.length === 0 ? (
              <div className="p-12 text-center text-zinc-500 text-xs">No proposals yet. Click "Create Proposal" to get started.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-white/[0.06] bg-white/[0.015] text-[11px] font-bold text-zinc-400 uppercase tracking-wider">
                      <th className="p-4">Proposal #</th>
                      <th className="p-4">Title</th>
                      <th className="p-4">Client</th>
                      <th className="p-4">Valid Until</th>
                      <th className="p-4">Status</th>
                      <th className="p-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/[0.04]">
                    {proposalsPage?.content.map(p => {
                      const isSelected = selectedProposal?.id === p.id;
                      const sStyle = statusStyles[p.status] || { badge: 'bg-zinc-500/10 text-zinc-400', dot: 'bg-zinc-400' };
                      return (
                        <tr
                          key={p.id}
                          onClick={() => setSelectedProposal(p)}
                          className={`hover:bg-white/[0.02] cursor-pointer transition-all ${
                            isSelected ? 'bg-indigo-500/10 border-l-2 border-indigo-400' : 'border-l-2 border-transparent'
                          }`}
                        >
                          <td className="p-4 text-zinc-200 font-mono-num font-bold text-xs">{p.proposalNumber}</td>
                          <td className="p-4 font-semibold text-zinc-100 text-xs max-w-[200px] truncate">{p.title}</td>
                          <td className="p-4 text-xs text-zinc-300 font-medium">{clientName(p.clientId)}</td>
                          <td className="p-4 text-xs text-zinc-400 font-mono-num">{p.validUntil || '—'}</td>
                          <td className="p-4">
                            <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold ${sStyle.badge}`}>
                              <span className={`w-1.5 h-1.5 rounded-full ${sStyle.dot}`} />
                              {p.status}
                            </span>
                          </td>
                          <td className="p-4 text-right" onClick={e => e.stopPropagation()}>
                            <div className="flex items-center justify-end gap-1">
                              <button onClick={() => setSelectedProposal(p)} className="p-1.5 text-zinc-400 hover:text-indigo-400 hover:bg-white/[0.05] rounded-lg transition-all" title="View"><Eye className="w-3.5 h-3.5" /></button>
                              {p.status === 'DRAFT' && <>
                                <button onClick={() => navigate(`/proposals/${p.id}/edit`)} className="p-1.5 text-zinc-400 hover:text-purple-400 hover:bg-white/[0.05] rounded-lg transition-all" title="Edit"><Edit className="w-3.5 h-3.5" /></button>
                                <button onClick={() => handleSend(p.id)} className="p-1.5 text-zinc-400 hover:text-blue-400 hover:bg-white/[0.05] rounded-lg transition-all" title="Send"><Send className="w-3.5 h-3.5" /></button>
                              </>}
                              {p.status === 'ACCEPTED' && (
                                <button onClick={() => handleConvert(p.id)} className="p-1.5 text-zinc-400 hover:text-emerald-400 hover:bg-white/[0.05] rounded-lg transition-all" title="Convert to Invoice"><CheckCircle className="w-3.5 h-3.5" /></button>
                              )}
                              {(p.status === 'DRAFT' || p.status === 'DECLINED') && (
                                <button onClick={() => handleDelete(p.id)} className="p-1.5 text-zinc-400 hover:text-rose-400 hover:bg-white/[0.05] rounded-lg transition-all" title="Delete"><Trash2 className="w-3.5 h-3.5" /></button>
                              )}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
            {proposalsPage && proposalsPage.totalPages > 1 && (
              <div className="p-4 border-t border-white/[0.06] flex items-center justify-between">
                <span className="text-xs text-zinc-400 font-mono-num">Page {proposalsPage.number + 1} of {proposalsPage.totalPages}</span>
                <div className="flex gap-2">
                  <button disabled={proposalsPage.number === 0} onClick={() => setPage(p => p - 1)}
                    className="px-3 py-1.5 bg-white/[0.02] border border-white/[0.08] rounded-xl text-xs disabled:opacity-40 hover:bg-white/[0.05] transition-all">Previous</button>
                  <button disabled={proposalsPage.number === proposalsPage.totalPages - 1} onClick={() => setPage(p => p + 1)}
                    className="px-3 py-1.5 bg-white/[0.02] border border-white/[0.08] rounded-xl text-xs disabled:opacity-40 hover:bg-white/[0.05] transition-all">Next</button>
                </div>
              </div>
            )}
          </div>

          {/* Detail panel */}
          <div className="bg-[#0a0e1c] border border-white/[0.08] rounded-2xl p-6 space-y-5 shadow-sm">
            {!selectedProposal ? (
              <div className="text-center py-12 flex flex-col items-center gap-3 text-zinc-500">
                <div className="w-12 h-12 rounded-full bg-white/[0.02] border border-white/[0.05] flex items-center justify-center">
                  <FileText className="w-5 h-5 text-zinc-400" />
                </div>
                <div>
                  <h3 className="font-semibold text-zinc-300">No proposal selected</h3>
                  <p className="text-xs mt-1 max-w-xs">Click a row to inspect its details, status, and available actions.</p>
                </div>
              </div>
            ) : (
              <div className="space-y-5">
                {/* Header */}
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="text-base font-bold text-zinc-200 leading-snug">{selectedProposal.title}</h3>
                    <p className="text-zinc-500 text-xs font-mono mt-0.5">{selectedProposal.proposalNumber}</p>
                    <span className={`inline-block px-2 py-0.5 rounded-full text-[9px] font-bold mt-1 uppercase ${statusStyles[selectedProposal.status] || ''}`}>
                      {selectedProposal.status}
                    </span>
                  </div>
                  <button onClick={() => setSelectedProposal(null)} className="p-1 hover:bg-white/[0.05] rounded-lg text-zinc-500 hover:text-zinc-200 transition-all">
                    <X className="w-4 h-4" />
                  </button>
                </div>

                {/* Actions */}
                <div className="flex gap-2 flex-wrap">
                  <button onClick={() => handleDownloadPdf(selectedProposal.id)} disabled={pdfLoading}
                    className="flex-1 flex items-center justify-center gap-1.5 py-1.5 bg-white/[0.02] border border-white/[0.05] hover:bg-white/[0.05] text-zinc-300 font-medium rounded-lg text-xs transition-all">
                    {pdfLoading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Download className="w-3.5 h-3.5" />} PDF
                  </button>
                  {selectedProposal.status === 'DRAFT' && <>
                    <button onClick={() => navigate(`/proposals/${selectedProposal.id}/edit`)}
                      className="flex-1 flex items-center justify-center gap-1.5 py-1.5 bg-purple-500/10 border border-purple-500/20 hover:bg-purple-500/20 text-purple-400 font-medium rounded-lg text-xs transition-all">
                      <Edit className="w-3.5 h-3.5" /> Edit
                    </button>
                    <button onClick={() => handleSend(selectedProposal.id)}
                      className="flex-1 flex items-center justify-center gap-1.5 py-1.5 bg-blue-500/10 border border-blue-500/20 hover:bg-blue-500/20 text-blue-400 font-medium rounded-lg text-xs transition-all">
                      <Send className="w-3.5 h-3.5" /> Send
                    </button>
                  </>}
                  {selectedProposal.status === 'ACCEPTED' && !selectedProposal.convertedToInvoiceId && (
                    <button onClick={() => handleConvert(selectedProposal.id)}
                      className="flex-1 flex items-center justify-center gap-1.5 py-1.5 bg-emerald-500/10 border border-emerald-500/20 hover:bg-emerald-500/20 text-emerald-400 font-medium rounded-lg text-xs transition-all">
                      <CheckCircle className="w-3.5 h-3.5" /> Convert to Invoice
                    </button>
                  )}
                  {selectedProposal.convertedToInvoiceId && (
                    <button onClick={() => navigate(`/invoices/${selectedProposal.convertedToInvoiceId}`)}
                      className="flex-1 flex items-center justify-center gap-1.5 py-1.5 bg-emerald-500/10 border border-emerald-500/20 hover:bg-emerald-500/20 text-emerald-400 font-medium rounded-lg text-xs transition-all">
                      <CheckCircle className="w-3.5 h-3.5" /> View Invoice
                    </button>
                  )}
                  {selectedProposal.publicLink && (
                    <button onClick={() => { navigator.clipboard.writeText(selectedProposal.publicLink!); }}
                      className="flex-1 flex items-center justify-center gap-1.5 py-1.5 bg-white/[0.02] border border-white/[0.05] hover:bg-white/[0.05] text-zinc-300 font-medium rounded-lg text-xs transition-all">
                      <Copy className="w-3.5 h-3.5" /> Copy Link
                    </button>
                  )}
                  {(selectedProposal.status === 'DRAFT' || selectedProposal.status === 'DECLINED') && (
                    <button onClick={() => handleDelete(selectedProposal.id)}
                      className="flex-1 flex items-center justify-center gap-1.5 py-1.5 bg-red-500/10 border border-red-500/20 hover:bg-red-500/20 text-red-400 font-medium rounded-lg text-xs transition-all">
                      <Trash2 className="w-3.5 h-3.5" /> Delete
                    </button>
                  )}
                </div>

                {/* Meta */}
                <div className="grid grid-cols-2 gap-3 border-t border-white/[0.05] pt-4 text-xs">
                  <div><span className="text-zinc-500 uppercase text-[9px] font-semibold block">Client</span><span className="text-zinc-300 font-medium">{clientName(selectedProposal.clientId)}</span></div>
                  <div><span className="text-zinc-500 uppercase text-[9px] font-semibold block">Valid Until</span><span className="text-zinc-300 font-medium">{selectedProposal.validUntil || '—'}</span></div>
                  <div><span className="text-zinc-500 uppercase text-[9px] font-semibold block">Total</span><span className="text-zinc-300 font-medium">{new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(selectedProposal.total)}</span></div>
                  {selectedProposal.viewedAt && <div><span className="text-zinc-500 uppercase text-[9px] font-semibold block">Viewed</span><span className="text-zinc-300 font-medium">{fmt(selectedProposal.viewedAt)}</span></div>}
                  {selectedProposal.signedAt && <div><span className="text-zinc-500 uppercase text-[9px] font-semibold block">Signed</span><span className="text-zinc-300 font-medium">{fmt(selectedProposal.signedAt)}</span></div>}
                </div>

                {/* Content preview */}
                {selectedProposal.overview && (
                  <div className="border-t border-white/[0.05] pt-4 space-y-1">
                    <span className="text-zinc-500 uppercase text-[9px] font-semibold block">Overview</span>
                    <p className="text-zinc-400 text-xs leading-relaxed">{selectedProposal.overview}</p>
                  </div>
                )}

                {/* Items */}
                {selectedProposal.items.length > 0 && (
                  <div className="border-t border-white/[0.05] pt-4 space-y-2">
                    <span className="text-zinc-500 uppercase text-[9px] font-semibold block">Pricing Items</span>
                    <div className="space-y-1.5 max-h-40 overflow-y-auto pr-1">
                      {selectedProposal.items.map(item => (
                        <div key={item.id} className="p-2.5 bg-white/[0.01] border border-white/[0.03] rounded-lg flex justify-between items-center text-xs">
                          <span className="text-zinc-300 font-medium">{item.description}</span>
                          {item.total != null && (
                            <span className="text-zinc-400 font-semibold">
                              {new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(item.total)}
                            </span>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default Proposals;
