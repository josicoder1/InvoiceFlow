import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import DashboardLayout from '../components/DashboardLayout';
import { invoicesApi } from '../api/invoices';
import { clientsApi } from '../api/clients';
import type { Invoice, Client } from '../types';
import {
  ArrowLeft, Download, Eye, Send, CheckCircle, Copy, Ban,
  Trash2, Edit, X, AlertCircle, Loader2, FileText
} from 'lucide-react';

const statusStyles: Record<string, string> = {
  DRAFT: 'bg-zinc-500/10 border-zinc-500/20 text-zinc-400',
  SENT: 'bg-blue-500/10 border-blue-500/20 text-blue-400',
  PAID: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400',
  OVERDUE: 'bg-rose-500/10 border-rose-500/20 text-rose-400',
  CANCELLED: 'bg-orange-500/10 border-orange-500/20 text-orange-400',
};

const statusDots: Record<string, string> = {
  DRAFT: 'bg-zinc-400',
  SENT: 'bg-blue-400',
  PAID: 'bg-emerald-400',
  OVERDUE: 'bg-rose-400',
  CANCELLED: 'bg-orange-400',
};

const InvoiceDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [client, setClient] = useState<Client | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Preview modal
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewHtml, setPreviewHtml] = useState<string | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);

  // Action loading states
  const [sendLoading, setSendLoading] = useState(false);
  const [sendConfirmOpen, setSendConfirmOpen] = useState(false);
  const [pdfLoading, setPdfLoading] = useState(false);

  const fetchInvoice = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const data = await invoicesApi.getInvoice(id);
      setInvoice(data);
      const clientData = await clientsApi.getClient(data.clientId);
      setClient(clientData);
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to load invoice');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchInvoice();
  }, [fetchInvoice]);

  const handleDownloadPdf = async () => {
    if (!id) return;
    setPdfLoading(true);
    try {
      await invoicesApi.downloadPdf(id);
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to download PDF');
    } finally {
      setPdfLoading(false);
    }
  };

  const handlePreview = async () => {
    if (!id) return;
    setPreviewOpen(true);
    setPreviewHtml(null);
    setPreviewLoading(true);
    try {
      const html = await invoicesApi.previewInvoice(id);
      setPreviewHtml(html);
    } catch (err: any) {
      setPreviewHtml(`<p style="color:red;font-family:sans-serif;padding:24px;">Failed to load preview: ${err.response?.data?.detail || err.message}</p>`);
    } finally {
      setPreviewLoading(false);
    }
  };

  const handleSendConfirm = async () => {
    if (!id) return;
    setSendLoading(true);
    try {
      const updated = await invoicesApi.markInvoiceSent(id);
      setInvoice(updated);
      setSendConfirmOpen(false);
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to send invoice');
    } finally {
      setSendLoading(false);
    }
  };

  const handleMarkPaid = async () => {
    if (!id) return;
    try {
      const updated = await invoicesApi.markInvoicePaid(id);
      setInvoice(updated);
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to mark as paid');
    }
  };

  const handleCancel = async () => {
    if (!id || !confirm('Are you sure you want to cancel this invoice?')) return;
    try {
      const updated = await invoicesApi.cancelInvoice(id);
      setInvoice(updated);
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to cancel invoice');
    }
  };

  const handleDelete = async () => {
    if (!id || !confirm('Are you sure you want to delete this invoice? This cannot be undone.')) return;
    try {
      await invoicesApi.deleteInvoice(id);
      navigate('/invoices');
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to delete invoice');
    }
  };

  const handleDuplicate = async () => {
    if (!invoice) return;
    try {
      const duplicated = await invoicesApi.createInvoice({
        clientId: invoice.clientId,
        issueDate: new Date().toISOString().split('T')[0],
        dueDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
        currency: invoice.currency,
        items: invoice.items.map((i) => ({
          description: i.description,
          quantity: i.quantity,
          unitPrice: i.unitPrice,
          position: i.position,
        })),
        discountType: invoice.discountType,
        discountValue: invoice.discountValue,
        taxRate: invoice.taxRate,
        notes: invoice.notes || undefined,
        paymentTerms: invoice.paymentTerms || undefined,
      });
      navigate(`/invoices/${duplicated.id}`);
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to duplicate invoice');
    }
  };

  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(amount);
  };

  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center py-32 text-zinc-500 text-xs">
          <Loader2 className="w-5 h-5 animate-spin mr-2" /> Loading invoice…
        </div>
      </DashboardLayout>
    );
  }

  if (error || !invoice) {
    return (
      <DashboardLayout>
        <div className="flex flex-col items-center justify-center py-32 gap-3 text-rose-400">
          <AlertCircle className="w-8 h-8" />
          <p className="text-sm">{error || 'Invoice not found'}</p>
          <button
            onClick={() => navigate('/invoices')}
            className="mt-2 px-4 py-2 sharp-btn-secondary text-xs"
          >
            Back to Invoices
          </button>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="space-y-6 max-w-5xl mx-auto">
        {/* Back + header row */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/invoices')}
              className="p-2 sharp-btn-secondary text-zinc-400 hover:text-white"
              title="Back to Invoices"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <div>
              <div className="flex items-center gap-2.5">
                <h1 className="text-xl font-bold tracking-tight text-white font-mono-num">
                  {invoice.invoiceNumber}
                </h1>
                <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider border ${statusStyles[invoice.status] || statusStyles.DRAFT}`}>
                  <span className={`w-1.5 h-1.5 rounded-full ${statusDots[invoice.status] || 'bg-zinc-400'}`} />
                  {invoice.status}
                </span>
              </div>
              <p className="text-zinc-400 text-xs mt-0.5">
                Issued to {client?.name || 'Client'} · Created {new Date(invoice.createdAt).toLocaleDateString()}
              </p>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex flex-wrap gap-2">
            <button
              onClick={handlePreview}
              className="flex items-center gap-1.5 px-3 py-1.5 sharp-btn-secondary text-xs font-semibold text-zinc-200"
            >
              <Eye className="w-3.5 h-3.5" />
              Preview
            </button>

            <button
              onClick={handleDownloadPdf}
              disabled={pdfLoading}
              className="flex items-center gap-1.5 px-3 py-1.5 sharp-btn-secondary text-xs font-semibold text-zinc-200 disabled:opacity-60"
            >
              {pdfLoading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Download className="w-3.5 h-3.5" />}
              PDF
            </button>

            {invoice.status === 'DRAFT' && (
              <button
                onClick={() => setSendConfirmOpen(true)}
                className="flex items-center gap-1.5 px-3 py-1.5 sharp-btn-primary text-xs font-semibold text-white shadow-sm"
              >
                <Send className="w-3.5 h-3.5" />
                Send Invoice
              </button>
            )}

            {(invoice.status === 'SENT' || invoice.status === 'OVERDUE') && (
              <button
                onClick={handleMarkPaid}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-500/15 border border-emerald-500/30 hover:bg-emerald-500/25 text-emerald-300 text-xs font-semibold transition-all"
              >
                <CheckCircle className="w-3.5 h-3.5" />
                Mark Paid
              </button>
            )}

            {invoice.status === 'DRAFT' && (
              <button
                onClick={() => navigate(`/invoices/${id}/edit`)}
                className="flex items-center gap-1.5 px-3 py-1.5 sharp-btn-secondary text-xs font-semibold text-zinc-200"
              >
                <Edit className="w-3.5 h-3.5" />
                Edit
              </button>
            )}

            <button
              onClick={handleDuplicate}
              className="flex items-center gap-1.5 px-3 py-1.5 sharp-btn-secondary text-xs font-semibold text-zinc-200"
            >
              <Copy className="w-3.5 h-3.5" />
              Duplicate
            </button>

            {(invoice.status === 'DRAFT' || invoice.status === 'SENT' || invoice.status === 'OVERDUE') && (
              <button
                onClick={handleCancel}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-orange-500/10 border border-orange-500/20 hover:bg-orange-500/20 text-orange-400 text-xs font-semibold transition-all"
              >
                <Ban className="w-3.5 h-3.5" />
                Cancel
              </button>
            )}

            {(invoice.status === 'DRAFT' || invoice.status === 'CANCELLED') && (
              <button
                onClick={handleDelete}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-rose-500/10 border border-rose-500/20 hover:bg-rose-500/20 text-rose-400 text-xs font-semibold transition-all"
              >
                <Trash2 className="w-3.5 h-3.5" />
                Delete
              </button>
            )}
          </div>
        </div>

        {/* Main content grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left: details */}
          <div className="lg:col-span-2 space-y-6">
            {/* Parties + dates card */}
            <div className="sharp-card p-6 space-y-6">
              {/* From / To */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="p-4 rounded-lg bg-white/[0.015] border border-white/[0.06] space-y-1">
                  <p className="sharp-label">Bill To</p>
                  {client ? (
                    <>
                      <p className="font-semibold text-white pt-1">{client.name}</p>
                      {client.company && <p className="text-xs text-zinc-400">{client.company}</p>}
                      {client.address && <p className="text-xs text-zinc-500">{client.address}</p>}
                      <p className="text-xs text-indigo-400 pt-0.5">{client.email}</p>
                    </>
                  ) : (
                    <p className="text-zinc-500 text-xs">Loading client…</p>
                  )}
                </div>
                <div className="p-4 rounded-lg bg-white/[0.015] border border-white/[0.06] space-y-3">
                  <p className="sharp-label">Dates & Terms</p>
                  <div className="grid grid-cols-2 gap-3 text-xs">
                    <div>
                      <span className="text-zinc-500 block text-[11px]">Issue Date</span>
                      <span className="text-white font-mono-num font-medium">{invoice.issueDate}</span>
                    </div>
                    <div>
                      <span className="text-zinc-500 block text-[11px]">Due Date</span>
                      <span className="text-white font-mono-num font-medium">{invoice.dueDate}</span>
                    </div>
                    <div>
                      <span className="text-zinc-500 block text-[11px]">Currency</span>
                      <span className="text-white font-mono-num font-medium uppercase">{invoice.currency}</span>
                    </div>
                    <div>
                      <span className="text-zinc-500 block text-[11px]">Payment Terms</span>
                      <span className="text-white font-medium">{invoice.paymentTerms || 'Net 30'}</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Line items table */}
              <div>
                <p className="sharp-label mb-3">Line Items</p>
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
                      {invoice.items.map((item) => (
                        <tr key={item.id} className="hover:bg-white/[0.015] transition-colors">
                          <td className="px-4 py-3 text-xs text-white">{item.description}</td>
                          <td className="px-4 py-3 text-xs font-mono-num text-zinc-400 text-right">{item.quantity}</td>
                          <td className="px-4 py-3 text-xs font-mono-num text-zinc-400 text-right">
                            {formatCurrency(item.unitPrice, invoice.currency)}
                          </td>
                          <td className="px-4 py-3 text-xs font-mono-num font-semibold text-white text-right">
                            {formatCurrency(item.total, invoice.currency)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Notes */}
              {invoice.notes && (
                <div className="bg-white/[0.015] border border-white/[0.06] rounded-lg p-4">
                  <p className="sharp-label mb-1">Notes & Instructions</p>
                  <p className="text-xs text-zinc-300 leading-relaxed">{invoice.notes}</p>
                </div>
              )}
            </div>
          </div>

          {/* Right: totals + meta */}
          <div className="space-y-4">
            {/* Totals card */}
            <div className="sharp-card p-6 space-y-4">
              <p className="sharp-label">Financial Breakdown</p>

              <div className="space-y-2.5 text-xs">
                <div className="flex justify-between text-zinc-400">
                  <span>Subtotal</span>
                  <span className="font-mono-num text-zinc-200">{formatCurrency(invoice.subtotal, invoice.currency)}</span>
                </div>

                {invoice.discountValue > 0 && (
                  <div className="flex justify-between text-zinc-400">
                    <span>
                      Discount
                      {invoice.discountType === 'PERCENT' && ` (${invoice.discountValue}%)`}
                    </span>
                    <span className="text-rose-400 font-mono-num">
                      −{invoice.discountType === 'PERCENT'
                        ? formatCurrency((invoice.subtotal * invoice.discountValue) / 100, invoice.currency)
                        : formatCurrency(invoice.discountValue, invoice.currency)}
                    </span>
                  </div>
                )}

                {invoice.taxRate > 0 && (
                  <div className="flex justify-between text-zinc-400">
                    <span>Tax ({invoice.taxRate}%)</span>
                    <span className="font-mono-num text-zinc-200">{formatCurrency(
                      ((invoice.subtotal - (invoice.discountType === 'PERCENT'
                        ? (invoice.subtotal * invoice.discountValue) / 100
                        : invoice.discountValue)) * invoice.taxRate) / 100,
                      invoice.currency
                    )}</span>
                  </div>
                )}
              </div>

              <div className="pt-3 border-t border-white/[0.08] flex justify-between items-baseline">
                <span className="text-sm font-semibold text-white">Total Amount</span>
                <span className="text-xl font-bold font-mono-num text-white">
                  {formatCurrency(invoice.total, invoice.currency)}
                </span>
              </div>

              {invoice.paidAt && (
                <div className="p-2.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-xs text-emerald-400 flex items-center gap-2">
                  <CheckCircle className="w-4 h-4 shrink-0" />
                  <span>Paid on {new Date(invoice.paidAt).toLocaleDateString()}</span>
                </div>
              )}
            </div>

            {/* Meta card */}
            <div className="sharp-card p-6 space-y-3">
              <p className="sharp-label">Invoice Metadata</p>
              <div className="space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-zinc-500">Invoice Identifier</span>
                  <span className="text-zinc-200 font-mono-num font-semibold">{invoice.invoiceNumber}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">Created Date</span>
                  <span className="text-zinc-300 font-mono-num">{new Date(invoice.createdAt).toLocaleDateString()}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">Last Modified</span>
                  <span className="text-zinc-300 font-mono-num">{new Date(invoice.updatedAt).toLocaleDateString()}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">Schema Version</span>
                  <span className="text-zinc-400 font-mono-num">v{invoice.version}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Send Confirmation Modal */}
      {sendConfirmOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm">
          <div className="sharp-card p-6 w-full max-w-md shadow-2xl space-y-4">
            <div className="flex items-start gap-3">
              <div className="w-10 h-10 rounded-lg bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center shrink-0">
                <Send className="w-5 h-5 text-indigo-400" />
              </div>
              <div>
                <h3 className="font-bold text-white text-base">Send Invoice</h3>
                <p className="text-xs text-zinc-400 mt-1 leading-relaxed">
                  Generate PDF and dispatch via email to{' '}
                  <span className="text-white font-semibold">{client?.email}</span>.
                  The invoice will be locked from draft editing.
                </p>
              </div>
            </div>

            <div className="bg-white/[0.02] border border-white/[0.06] rounded-lg p-3 text-xs space-y-1.5">
              <div className="flex justify-between">
                <span className="text-zinc-500">Invoice:</span>
                <span className="text-zinc-200 font-mono-num font-semibold">{invoice.invoiceNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">Recipient:</span>
                <span className="text-zinc-200">{client?.name}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">Total Due:</span>
                <span className="text-white font-mono-num font-bold">{formatCurrency(invoice.total, invoice.currency)}</span>
              </div>
            </div>

            <div className="flex gap-3 pt-2">
              <button
                onClick={() => setSendConfirmOpen(false)}
                disabled={sendLoading}
                className="flex-1 py-2 sharp-btn-secondary text-xs disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                onClick={handleSendConfirm}
                disabled={sendLoading}
                className="flex-1 flex items-center justify-center gap-2 py-2 sharp-btn-primary text-xs text-white disabled:opacity-50"
              >
                {sendLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
                {sendLoading ? 'Sending…' : 'Send Invoice'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Preview Modal */}
      {previewOpen && (
        <div className="fixed inset-0 z-50 flex flex-col bg-black/85 backdrop-blur-md">
          <div className="flex items-center justify-between px-6 py-3.5 bg-[#0a0e1c] border-b border-white/[0.08]">
            <div className="flex items-center gap-3">
              <FileText className="w-4 h-4 text-indigo-400" />
              <span className="font-semibold text-xs text-white font-mono-num">
                Preview — {invoice.invoiceNumber}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={handleDownloadPdf}
                disabled={pdfLoading}
                className="flex items-center gap-1.5 px-3 py-1.5 sharp-btn-secondary text-xs font-semibold text-white"
              >
                {pdfLoading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Download className="w-3.5 h-3.5" />}
                Download PDF
              </button>
              <button
                onClick={() => setPreviewOpen(false)}
                className="p-1.5 hover:bg-white/[0.05] rounded-lg text-zinc-400 hover:text-white transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>
          <div className="flex-1 overflow-hidden bg-[#101424]">
            {previewLoading ? (
              <div className="flex items-center justify-center h-full text-zinc-500 text-xs">
                <Loader2 className="w-6 h-6 animate-spin mr-3 text-indigo-400" />
                Rendering preview document…
              </div>
            ) : previewHtml ? (
              <iframe
                srcDoc={previewHtml}
                className="w-full h-full border-0"
                title="Invoice Preview"
                sandbox="allow-same-origin"
              />
            ) : null}
          </div>
        </div>
      )}
    </DashboardLayout>
  );
};

export default InvoiceDetail;
