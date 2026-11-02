import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { publicInvoicesApi } from '../api/publicInvoices';
import type { PublicInvoice } from '../types';
import { AlertCircle, CheckCircle, CreditCard, Loader2, Sparkles, ShieldCheck } from 'lucide-react';

const PublicInvoicePage: React.FC = () => {
  const { token } = useParams<{ token: string }>();
  const [invoice, setInvoice] = useState<PublicInvoice | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [paying, setPaying] = useState(false);
  const [payError, setPayError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    publicInvoicesApi.getInvoice(token)
      .then(setInvoice)
      .catch(err => setError(err.response?.data?.detail || 'Invoice not found.'))
      .finally(() => setLoading(false));
  }, [token]);

  const handlePay = async () => {
    if (!token) return;
    setPaying(true);
    setPayError(null);
    try {
      const { checkoutUrl } = await publicInvoicesApi.payInvoice(token);
      window.location.href = checkoutUrl;
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setPayError(detail || 'Unable to start payment. Please try again.');
      setPaying(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#060810] flex items-center justify-center text-zinc-400 text-xs">
        <Loader2 className="w-5 h-5 animate-spin mr-2.5 text-indigo-400" /> Loading invoice…
      </div>
    );
  }

  if (error || !invoice) {
    return (
      <div className="min-h-screen bg-[#060810] flex items-center justify-center p-4">
        <div className="sharp-card p-8 max-w-md w-full text-center space-y-3">
          <AlertCircle className="w-10 h-10 text-rose-400 mx-auto" />
          <h2 className="text-lg font-bold text-white">Invoice Not Found</h2>
          <p className="text-zinc-400 text-xs leading-relaxed">{error}</p>
        </div>
      </div>
    );
  }

  const sym = invoice.currency === 'EUR' ? '€' : invoice.currency === 'GBP' ? '£' : '$';

  return (
    <div className="min-h-screen bg-[#060810] py-12 px-4 selection:bg-indigo-500/30 selection:text-indigo-200">
      <div className="max-w-3xl mx-auto space-y-6">
        {/* Invoice Header Card */}
        <div className="sharp-card overflow-hidden">
          <div className="p-6 sm:p-8 border-b border-white/[0.08] bg-gradient-to-br from-indigo-950/40 via-[#0c101d] to-[#0c101d]">
            <div className="flex items-center justify-between flex-wrap gap-4">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <div className="w-6 h-6 rounded-md bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center">
                    <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
                  </div>
                  <span className="sharp-label text-indigo-300">
                    Invoice from {invoice.from.name}
                  </span>
                </div>
                <h1 className="text-2xl font-bold font-mono-num text-white tracking-tight">{invoice.invoiceNumber}</h1>
              </div>

              <div className="text-right">
                <span className="text-[11px] font-mono-num text-zinc-400 block">Due Date</span>
                <span className="font-mono-num text-sm font-semibold text-white">{invoice.dueDate}</span>
              </div>
            </div>
          </div>

          <div className="p-6 sm:p-8 grid sm:grid-cols-2 gap-6 text-xs">
            <div className="p-4 rounded-lg bg-white/[0.015] border border-white/[0.06] space-y-1">
              <p className="sharp-label mb-1">From</p>
              <p className="font-semibold text-white text-sm">{invoice.from.name}</p>
              <p className="text-zinc-400">{invoice.from.email}</p>
            </div>
            <div className="p-4 rounded-lg bg-white/[0.015] border border-white/[0.06] space-y-1">
              <p className="sharp-label mb-1">Billed To</p>
              <p className="font-semibold text-white text-sm">{invoice.to.name}</p>
              <p className="text-zinc-400">{invoice.to.email}</p>
            </div>
          </div>
        </div>

        {/* Paid Banner */}
        {invoice.isPaid && (
          <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/25 flex items-center gap-3">
            <CheckCircle className="w-6 h-6 text-emerald-400 shrink-0" />
            <div>
              <h3 className="font-bold text-emerald-300 text-sm">Invoice Paid in Full</h3>
              <p className="text-emerald-400/80 text-xs mt-0.5">Thank you — payment has been completed and verified.</p>
            </div>
          </div>
        )}

        {/* Line Items Card */}
        <div className="sharp-card p-6 space-y-4">
          <h2 className="sharp-label">Itemized Breakdown</h2>
          <div className="rounded-lg overflow-hidden border border-white/[0.06]">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-white/[0.02] text-[11px] font-semibold text-zinc-400 uppercase tracking-wider border-b border-white/[0.06]">
                  <th className="px-4 py-3">Description</th>
                  <th className="px-4 py-3 text-right">Qty</th>
                  <th className="px-4 py-3 text-right">Unit Price</th>
                  <th className="px-4 py-3 text-right">Amount</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/[0.06]">
                {invoice.items.map((item, idx) => (
                  <tr key={idx} className="hover:bg-white/[0.015] transition-colors">
                    <td className="px-4 py-3 text-xs text-white">{item.description}</td>
                    <td className="px-4 py-3 text-xs font-mono-num text-zinc-400 text-right">{item.quantity ?? '—'}</td>
                    <td className="px-4 py-3 text-xs font-mono-num text-zinc-400 text-right">
                      {item.unitPrice != null ? `${sym}${item.unitPrice.toFixed(2)}` : '—'}
                    </td>
                    <td className="px-4 py-3 text-xs font-mono-num font-semibold text-white text-right">
                      {item.total != null ? `${sym}${item.total.toFixed(2)}` : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="pt-3 border-t border-white/[0.08] flex justify-end">
            <div className="flex items-baseline gap-4">
              <span className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">Total Due</span>
              <span className="text-2xl font-bold font-mono-num text-white">
                {sym}{invoice.total.toFixed(2)}
              </span>
            </div>
          </div>

          {invoice.notes && (
            <div className="pt-3 border-t border-white/[0.06]">
              <span className="sharp-label block mb-1">Notes</span>
              <p className="text-xs text-zinc-400 leading-relaxed">{invoice.notes}</p>
            </div>
          )}
        </div>

        {/* Online Payment Action Card */}
        {invoice.canPay && (
          <div className="sharp-card p-6 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-sm font-bold text-white">Instant Online Checkout</h2>
                <p className="text-zinc-400 text-xs mt-0.5 flex items-center gap-1.5">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" /> Encrypted & secure payment processing
                </p>
              </div>
            </div>

            {payError && (
              <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-lg flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{payError}</span>
              </div>
            )}

            <button
              onClick={handlePay}
              disabled={paying}
              className="w-full flex items-center justify-center gap-2 py-3 sharp-btn-primary text-white text-sm font-semibold shadow-lg shadow-indigo-500/20 active:scale-[0.99] disabled:opacity-50"
            >
              {paying ? <Loader2 className="w-4 h-4 animate-spin" /> : <CreditCard className="w-4 h-4" />}
              {paying ? 'Redirecting to payment gateway…' : `Pay ${sym}${invoice.total.toFixed(2)} Now`}
            </button>
          </div>
        )}

        <p className="text-center text-[11px] font-mono-num text-zinc-500 pt-2 pb-6">
          Powered by InvoiceFlow Precision Invoicing
        </p>
      </div>
    </div>
  );
};

export default PublicInvoicePage;
