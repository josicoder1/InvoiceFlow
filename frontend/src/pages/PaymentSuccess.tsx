import React, { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { CheckCircle, Loader2, AlertCircle, Clock, ArrowRight } from 'lucide-react';
import { publicInvoicesApi } from '../api/publicInvoices';
import type { PublicInvoice } from '../types';

type Status = 'confirming' | 'polling' | 'paid' | 'pending' | 'error';

const PaymentSuccessPage: React.FC = () => {
  const { token } = useParams<{ token: string }>();
  const [searchParams] = useSearchParams();
  const txRef = searchParams.get('tx_ref') || searchParams.get('trx_ref') || searchParams.get('session_id');

  const [status, setStatus] = useState<Status>(txRef ? 'confirming' : 'polling');
  const [invoice, setInvoice] = useState<PublicInvoice | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;

    let cancelled = false;
    let attempts = 0;
    const maxAttempts = 15;

    const pollInvoice = async () => {
      try {
        const data = await publicInvoicesApi.getInvoice(token);
        if (cancelled) return;
        setInvoice(data);
        if (data.isPaid) {
          setStatus('paid');
          return true;
        }
        return false;
      } catch {
        if (!cancelled) setError('Unable to load invoice status.');
        setStatus('error');
        return true;
      }
    };

    const run = async () => {
      if (txRef) {
        try {
          const confirmed = await publicInvoicesApi.confirmPayment(token, txRef);
          if (cancelled) return;
          setInvoice(confirmed);
          if (confirmed.isPaid) {
            setStatus('paid');
            return;
          }
        } catch {
          // Webhook may have already processed — fall through to polling
        }
      }

      setStatus('polling');
      while (!cancelled && attempts < maxAttempts) {
        const done = await pollInvoice();
        if (done) return;
        attempts++;
        await new Promise(r => setTimeout(r, 2000));
      }

      if (!cancelled) {
        const data = await publicInvoicesApi.getInvoice(token).catch(() => null);
        if (data?.isPaid) {
          setInvoice(data);
          setStatus('paid');
        } else {
          setStatus('pending');
        }
      }
    };

    run();
    return () => { cancelled = true; };
  }, [token, txRef]);

  const sym = invoice?.currency === 'EUR' ? '€' : invoice?.currency === 'GBP' ? '£' : '$';

  return (
    <div className="min-h-screen bg-[#060810] flex items-center justify-center p-4 selection:bg-indigo-500/30">
      <div className="sharp-card p-10 max-w-md w-full text-center space-y-6">
        {(status === 'confirming' || status === 'polling') && (
          <>
            <div className="w-14 h-14 rounded-full bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center mx-auto">
              <Loader2 className="w-7 h-7 text-indigo-400 animate-spin" />
            </div>
            <div className="space-y-1.5">
              <h1 className="text-xl font-bold text-white tracking-tight">Verifying Payment</h1>
              <p className="text-zinc-400 text-xs leading-relaxed">
                Confirming transaction status with the payment processor…
              </p>
            </div>
          </>
        )}

        {status === 'paid' && (
          <>
            <div className="w-14 h-14 rounded-full bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center mx-auto">
              <CheckCircle className="w-7 h-7 text-emerald-400" />
            </div>
            <div className="space-y-1.5">
              <h1 className="text-xl font-bold text-white tracking-tight">Payment Completed</h1>
              <p className="text-zinc-400 text-xs leading-relaxed">
                {invoice
                  ? `Invoice ${invoice.invoiceNumber} for ${sym}${invoice.total.toFixed(2)} has been successfully paid.`
                  : 'Your payment was successfully received and recorded.'}
              </p>
            </div>
          </>
        )}

        {status === 'pending' && (
          <>
            <div className="w-14 h-14 rounded-full bg-amber-500/10 border border-amber-500/20 flex items-center justify-center mx-auto">
              <Clock className="w-7 h-7 text-amber-400" />
            </div>
            <div className="space-y-1.5">
              <h1 className="text-xl font-bold text-white tracking-tight">Payment Pending Confirmation</h1>
              <p className="text-zinc-400 text-xs leading-relaxed">
                Your payment was received and will be confirmed momentarily. A receipt will be sent to your email.
              </p>
            </div>
          </>
        )}

        {status === 'error' && (
          <>
            <div className="w-14 h-14 rounded-full bg-rose-500/10 border border-rose-500/20 flex items-center justify-center mx-auto">
              <AlertCircle className="w-7 h-7 text-rose-400" />
            </div>
            <div className="space-y-1.5">
              <h1 className="text-xl font-bold text-white tracking-tight">Verification Incomplete</h1>
              <p className="text-zinc-400 text-xs leading-relaxed">{error || 'Unable to confirm payment status at this moment.'}</p>
            </div>
          </>
        )}

        {token && (
          <div className="pt-2">
            <Link
              to={`/inv/${token}`}
              className="inline-flex items-center gap-1.5 px-4 py-2 sharp-btn-secondary text-xs font-semibold text-zinc-200"
            >
              <span>View Invoice Receipt</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        )}

        <p className="text-[11px] font-mono-num text-zinc-500 pt-2">Powered by InvoiceFlow</p>
      </div>
    </div>
  );
};

export default PaymentSuccessPage;
