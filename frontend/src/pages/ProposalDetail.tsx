import React, { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import DashboardLayout from '../components/DashboardLayout';
import { proposalsApi } from '../api/proposals';
import { clientsApi } from '../api/clients';
import type { Invoice } from '../types';
import {
  ArrowLeft, Send, Download, FileText, Trash2, Loader2, AlertCircle, Edit, CheckCircle,
} from 'lucide-react';

const statusStyles: Record<string, string> = {
  DRAFT: 'bg-zinc-500/10 border-zinc-500/20 text-zinc-400',
  SENT: 'bg-blue-500/10 border-blue-500/20 text-blue-400',
  VIEWED: 'bg-purple-500/10 border-purple-500/20 text-purple-400',
  ACCEPTED: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400',
  DECLINED: 'bg-rose-500/10 border-rose-500/20 text-rose-400',
};

const statusDots: Record<string, string> = {
  DRAFT: 'bg-zinc-400',
  SENT: 'bg-blue-400',
  VIEWED: 'bg-purple-400',
  ACCEPTED: 'bg-emerald-400',
  DECLINED: 'bg-rose-400',
};

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);

const ProposalDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const proposalQuery = useQuery({
    queryKey: ['proposal', id],
    queryFn: () => proposalsApi.getProposal(id!),
    enabled: !!id,
  });

  const clientQuery = useQuery({
    queryKey: ['client', proposalQuery.data?.clientId],
    queryFn: () => clientsApi.getClient(proposalQuery.data!.clientId),
    enabled: !!proposalQuery.data?.clientId,
  });

  const proposal = proposalQuery.data;
  const client = clientQuery.data;

  const handleSend = async () => {
    if (!id || !confirm('Send this proposal to the client?')) return;
    setActionLoading('send');
    try {
      await proposalsApi.sendProposal(id);
      queryClient.invalidateQueries({ queryKey: ['proposal', id] });
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to send proposal');
    } finally {
      setActionLoading(null);
    }
  };

  const handleDelete = async () => {
    if (!id || !confirm('Delete this proposal?')) return;
    setActionLoading('delete');
    try {
      await proposalsApi.deleteProposal(id);
      navigate('/proposals');
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Cannot delete proposal');
    } finally {
      setActionLoading(null);
    }
  };

  const handleConvert = async () => {
    if (!id || !confirm('Convert this accepted proposal to an invoice?')) return;
    setActionLoading('convert');
    try {
      const invoice = await proposalsApi.convertToInvoice(id) as Invoice;
      navigate(`/invoices/${invoice.id}`);
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to convert');
    } finally {
      setActionLoading(null);
    }
  };

  const handleDownloadPdf = async () => {
    if (!id) return;
    setActionLoading('pdf');
    try {
      await proposalsApi.downloadPdf(id);
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to download PDF');
    } finally {
      setActionLoading(null);
    }
  };

  if (proposalQuery.isLoading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center py-32 text-zinc-500 text-xs">
          <Loader2 className="w-5 h-5 text-indigo-400 animate-spin mr-2" /> Loading proposal…
        </div>
      </DashboardLayout>
    );
  }

  if (proposalQuery.isError || !proposal) {
    return (
      <DashboardLayout>
        <div className="text-center py-24">
          <AlertCircle className="w-10 h-10 text-rose-400 mx-auto mb-4" />
          <h2 className="text-lg font-semibold text-zinc-200">Proposal not found</h2>
          <Link to="/proposals" className="text-indigo-400 text-sm mt-4 inline-block hover:underline">
            Back to proposals
          </Link>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/proposals')}
              className="p-2 sharp-btn-secondary text-zinc-400 hover:text-white"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <div>
              <div className="flex items-center gap-2.5 flex-wrap">
                <h1 className="text-xl font-bold tracking-tight text-white">{proposal.title}</h1>
                <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider border ${statusStyles[proposal.status] || statusStyles.DRAFT}`}>
                  <span className={`w-1.5 h-1.5 rounded-full ${statusDots[proposal.status] || 'bg-zinc-400'}`} />
                  {proposal.status}
                </span>
              </div>
              <p className="text-xs text-zinc-400 mt-0.5">
                <span className="font-mono-num font-semibold text-zinc-300">{proposal.proposalNumber}</span> · {client?.name || 'Client'}
              </p>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex flex-wrap gap-2">
            {proposal.status === 'DRAFT' && (
              <>
                <button
                  onClick={() => navigate(`/proposals/${id}/edit`)}
                  className="flex items-center gap-1.5 px-3 py-1.5 sharp-btn-secondary text-xs font-semibold text-zinc-200"
                >
                  <Edit className="w-3.5 h-3.5" />
                  Edit
                </button>
                <button
                  onClick={handleSend}
                  disabled={!!actionLoading}
                  className="flex items-center gap-1.5 px-3 py-1.5 sharp-btn-primary text-xs font-semibold text-white shadow-sm disabled:opacity-50"
                >
                  {actionLoading === 'send' ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
                  Send Proposal
                </button>
              </>
            )}

            <button
              onClick={handleDownloadPdf}
              disabled={!!actionLoading}
              className="flex items-center gap-1.5 px-3 py-1.5 sharp-btn-secondary text-xs font-semibold text-zinc-200 disabled:opacity-50"
            >
              {actionLoading === 'pdf' ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Download className="w-3.5 h-3.5" />}
              PDF
            </button>

            {proposal.status === 'ACCEPTED' && !proposal.convertedToInvoiceId && (
              <button
                onClick={handleConvert}
                disabled={!!actionLoading}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-500/15 border border-emerald-500/30 text-emerald-300 text-xs font-semibold hover:bg-emerald-500/25 transition-all disabled:opacity-50"
              >
                {actionLoading === 'convert' ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <FileText className="w-3.5 h-3.5" />}
                Convert to Invoice
              </button>
            )}

            {proposal.status === 'ACCEPTED' && proposal.convertedToInvoiceId && (
              <Link
                to={`/invoices/${proposal.convertedToInvoiceId}`}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-500/15 border border-emerald-500/30 text-emerald-300 text-xs font-semibold hover:bg-emerald-500/25 transition-all"
              >
                <CheckCircle className="w-3.5 h-3.5" /> View Invoice
              </Link>
            )}

            {proposal.status === 'DRAFT' && (
              <button
                onClick={handleDelete}
                disabled={!!actionLoading}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold hover:bg-rose-500/20 transition-all disabled:opacity-50"
              >
                {actionLoading === 'delete' ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Trash2 className="w-3.5 h-3.5" />}
                Delete
              </button>
            )}
          </div>
        </div>

        {/* Narrative Sections */}
        {proposal.overview && (
          <section className="sharp-card p-6 space-y-2">
            <h3 className="sharp-label">Executive Overview</h3>
            <p className="text-xs text-zinc-300 leading-relaxed whitespace-pre-wrap">{proposal.overview}</p>
          </section>
        )}

        {proposal.scope && (
          <section className="sharp-card p-6 space-y-2">
            <h3 className="sharp-label">Scope of Work</h3>
            <p className="text-xs text-zinc-300 leading-relaxed whitespace-pre-wrap">{proposal.scope}</p>
          </section>
        )}

        {proposal.timeline && (
          <section className="sharp-card p-6 space-y-2">
            <h3 className="sharp-label">Project Timeline</h3>
            <p className="text-xs text-zinc-300 leading-relaxed whitespace-pre-wrap">{proposal.timeline}</p>
          </section>
        )}

        {/* Line items & Pricing Breakdown */}
        <section className="sharp-card p-6 space-y-4">
          <h3 className="sharp-label">Line Items & Pricing</h3>
          <div className="rounded-lg overflow-hidden border border-white/[0.06]">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-white/[0.02] text-[11px] font-semibold text-zinc-400 uppercase tracking-wider border-b border-white/[0.06]">
                  <th className="px-4 py-3">Description</th>
                  <th className="px-4 py-3 text-right">Qty</th>
                  <th className="px-4 py-3 text-right">Unit Price</th>
                  <th className="px-4 py-3 text-right">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/[0.06]">
                {proposal.items.map(item => (
                  <tr key={item.id} className="hover:bg-white/[0.015] transition-colors">
                    <td className="px-4 py-3 text-xs text-white">{item.description}</td>
                    <td className="px-4 py-3 text-xs font-mono-num text-zinc-400 text-right">{item.quantity}</td>
                    <td className="px-4 py-3 text-xs font-mono-num text-zinc-400 text-right">{formatCurrency(item.unitPrice ?? 0)}</td>
                    <td className="px-4 py-3 text-xs font-mono-num font-semibold text-white text-right">{formatCurrency(item.total ?? 0)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="pt-4 border-t border-white/[0.08] flex justify-end">
            <div className="w-full max-w-xs space-y-2 text-xs">
              <div className="flex justify-between text-zinc-400">
                <span>Subtotal:</span>
                <span className="font-mono-num text-zinc-200">{formatCurrency(proposal.subtotal)}</span>
              </div>
              {proposal.discountType && (
                <div className="flex justify-between text-rose-400">
                  <span>Discount ({proposal.discountType === 'PERCENT' ? `${proposal.discountValue}%` : 'Fixed'}):</span>
                  <span className="font-mono-num">-{formatCurrency(proposal.discountValue)}</span>
                </div>
              )}
              {proposal.taxRate > 0 && (
                <div className="flex justify-between text-zinc-400">
                  <span>Tax ({proposal.taxRate}%):</span>
                  <span className="font-mono-num text-zinc-200">{formatCurrency(proposal.total - proposal.subtotal)}</span>
                </div>
              )}
              <div className="pt-2 border-t border-white/[0.08] flex justify-between items-baseline">
                <span className="text-sm font-semibold text-white">Total:</span>
                <span className="text-xl font-bold font-mono-num text-white">{formatCurrency(proposal.total)}</span>
              </div>
            </div>
          </div>

          {proposal.pricingNotes && (
            <div className="pt-3 border-t border-white/[0.06]">
              <span className="text-[10px] font-semibold text-zinc-500 uppercase tracking-wider block mb-1">Pricing Terms</span>
              <p className="text-xs text-zinc-400">{proposal.pricingNotes}</p>
            </div>
          )}
        </section>
      </div>
    </DashboardLayout>
  );
};

export default ProposalDetail;
