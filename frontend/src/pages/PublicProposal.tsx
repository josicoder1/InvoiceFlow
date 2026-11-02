import React, { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { proposalsApi } from '../api/proposals';
import type { PublicProposal } from '../types';
import { CheckCircle, XCircle, AlertCircle, Loader2, PenLine, Sparkles } from 'lucide-react';
import ProposalPreview from '../components/proposal/ProposalPreview';
import { inferThemeFromTitle } from '../components/proposal/proposalTemplateThemes';

const PublicProposalPage: React.FC = () => {
  const { token } = useParams<{ token: string }>();
  const [proposal, setProposal] = useState<PublicProposal | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [hasSig, setHasSig] = useState(false);
  const [signatureStep, setSignatureStep] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    proposalsApi.getPublicProposal(token)
      .then(setProposal)
      .catch(err => setError(err.response?.data?.detail || 'Proposal not found or link expired.'))
      .finally(() => setLoading(false));
  }, [token]);

  const getPos = (e: React.MouseEvent | React.TouchEvent, canvas: HTMLCanvasElement) => {
    const rect = canvas.getBoundingClientRect();
    if ('touches' in e) {
      return { x: e.touches[0].clientX - rect.left, y: e.touches[0].clientY - rect.top };
    }
    return { x: (e as React.MouseEvent).clientX - rect.left, y: (e as React.MouseEvent).clientY - rect.top };
  };

  const startDraw = (e: React.MouseEvent | React.TouchEvent) => {
    const canvas = canvasRef.current; if (!canvas) return;
    const ctx = canvas.getContext('2d'); if (!ctx) return;
    const pos = getPos(e, canvas);
    ctx.beginPath(); ctx.moveTo(pos.x, pos.y);
    setIsDrawing(true);
  };

  const draw = (e: React.MouseEvent | React.TouchEvent) => {
    if (!isDrawing) return;
    const canvas = canvasRef.current; if (!canvas) return;
    const ctx = canvas.getContext('2d'); if (!ctx) return;
    e.preventDefault();
    const pos = getPos(e, canvas);
    ctx.lineWidth = 2; ctx.lineCap = 'round'; ctx.strokeStyle = '#818cf8';
    ctx.lineTo(pos.x, pos.y); ctx.stroke();
    setHasSig(true);
  };

  const stopDraw = () => setIsDrawing(false);

  const clearSig = () => {
    const canvas = canvasRef.current; if (!canvas) return;
    canvas.getContext('2d')?.clearRect(0, 0, canvas.width, canvas.height);
    setHasSig(false);
  };

  const handleAccept = async () => {
    if (!token || !canvasRef.current || !hasSig) return;
    setSubmitting(true); setActionError(null);
    try {
      const sigData = canvasRef.current.toDataURL('image/png');
      const updated = await proposalsApi.acceptProposal(token, sigData);
      setProposal(updated);
      setSignatureStep(false);
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setActionError(detail || 'Failed to accept');
    } finally { setSubmitting(false); }
  };

  const handleDecline = async () => {
    if (!token) return;
    if (!confirm('Are you sure you want to decline this proposal?')) return;
    setSubmitting(true); setActionError(null);
    try {
      const updated = await proposalsApi.declineProposal(token);
      setProposal(updated);
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setActionError(detail || 'Failed to decline');
    } finally { setSubmitting(false); }
  };

  if (loading) return (
    <div className="min-h-screen bg-[#060810] flex items-center justify-center text-zinc-400 text-xs">
      <Loader2 className="w-5 h-5 animate-spin mr-2.5 text-indigo-400" /> Loading proposal…
    </div>
  );

  if (error || !proposal) return (
    <div className="min-h-screen bg-[#060810] flex items-center justify-center p-4">
      <div className="sharp-card p-8 max-w-md w-full text-center space-y-3">
        <AlertCircle className="w-10 h-10 text-rose-400 mx-auto" />
        <h2 className="text-lg font-bold text-white">Proposal Not Found</h2>
        <p className="text-zinc-400 text-xs leading-relaxed">{error || 'This link may be invalid or expired.'}</p>
      </div>
    </div>
  );

  const isTerminal = proposal.status === 'ACCEPTED' || proposal.status === 'DECLINED';
  const canAct = proposal.isActionable;
  const theme = inferThemeFromTitle(proposal.title);

  return (
    <div className="min-h-screen bg-[#060810] py-12 px-4 selection:bg-indigo-500/30 selection:text-indigo-200">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Status header strip */}
        <div className="sharp-card px-5 py-3.5 flex items-center justify-between flex-wrap gap-4 text-xs">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 rounded-md bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center">
              <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
            </div>
            <span className="text-zinc-300 font-semibold">{proposal.proposalNumber}</span>
          </div>

          <div className="flex items-center gap-4">
            {proposal.validUntil && (
              <div className="text-zinc-400">
                <span className="sharp-label mr-1.5">Valid Until:</span>
                <span className="font-mono-num text-zinc-200">{proposal.validUntil}</span>
              </div>
            )}
            <div className="flex items-center gap-1.5">
              <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider border ${
                proposal.status === 'ACCEPTED' ? 'bg-emerald-500/10 border-emerald-500/25 text-emerald-400' :
                proposal.status === 'DECLINED' ? 'bg-rose-500/10 border-rose-500/25 text-rose-400' :
                'bg-indigo-500/10 border-indigo-500/25 text-indigo-400'
              }`}>
                <span className={`w-1.5 h-1.5 rounded-full ${
                  proposal.status === 'ACCEPTED' ? 'bg-emerald-400' :
                  proposal.status === 'DECLINED' ? 'bg-rose-400' :
                  'bg-indigo-400'
                }`} />
                {proposal.status}
              </span>
            </div>
          </div>
        </div>

        {proposal.isExpired && !isTerminal && (
          <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center gap-3">
            <AlertCircle className="w-6 h-6 text-amber-400 shrink-0" />
            <div>
              <h3 className="font-bold text-amber-300 text-sm">Proposal Expired</h3>
              <p className="text-amber-400/80 text-xs mt-0.5">This proposal expired on {proposal.validUntil}. Action is no longer available.</p>
            </div>
          </div>
        )}

        {proposal.status === 'ACCEPTED' && (
          <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center gap-3">
            <CheckCircle className="w-6 h-6 text-emerald-400 shrink-0" />
            <div>
              <h3 className="font-bold text-emerald-300 text-sm">Proposal Accepted</h3>
              <p className="text-emerald-400/80 text-xs mt-0.5">Thank you — this proposal has been digitally signed and accepted.</p>
            </div>
          </div>
        )}

        {proposal.status === 'DECLINED' && (
          <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center gap-3">
            <XCircle className="w-6 h-6 text-rose-400 shrink-0" />
            <div>
              <h3 className="font-bold text-rose-300 text-sm">Proposal Declined</h3>
              <p className="text-rose-400/80 text-xs mt-0.5">This proposal was declined.</p>
            </div>
          </div>
        )}

        {/* Proposal Document Render */}
        <div className="sharp-card p-6 sm:p-8">
          <ProposalPreview
            variant="dark"
            theme={theme}
            data={{
              title: proposal.title,
              proposalNumber: proposal.proposalNumber,
              validUntil: proposal.validUntil,
              fromName: proposal.from.name,
              overview: proposal.overview,
              scope: proposal.scope,
              timeline: proposal.timeline,
              pricingNotes: proposal.pricingNotes,
              items: proposal.items,
              subtotal: proposal.subtotal,
              discountType: proposal.discountType,
              discountValue: proposal.discountValue,
              taxRate: proposal.taxRate,
              total: proposal.total,
            }}
          />
        </div>

        {canAct && !signatureStep && (
          <div className="sharp-card p-6 space-y-4">
            <h2 className="text-sm font-bold text-white">Your Decision</h2>
            {actionError && (
              <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-lg flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{actionError}</span>
              </div>
            )}
            <div className="flex gap-3">
              <button
                onClick={() => setSignatureStep(true)}
                disabled={submitting}
                className="flex-1 flex items-center justify-center gap-2 py-2.5 sharp-btn-primary text-white text-xs font-semibold shadow-sm disabled:opacity-50"
              >
                <PenLine className="w-3.5 h-3.5" /> Accept & Digitally Sign
              </button>
              <button
                onClick={handleDecline}
                disabled={submitting}
                className="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-rose-500/10 border border-rose-500/20 hover:bg-rose-500/20 text-rose-400 text-xs font-semibold transition-all disabled:opacity-50"
              >
                <XCircle className="w-3.5 h-3.5" /> Decline Proposal
              </button>
            </div>
          </div>
        )}

        {canAct && signatureStep && (
          <div className="sharp-card p-6 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-sm font-bold text-white">Digital Signature</h2>
                <p className="text-zinc-400 text-xs mt-0.5">Draw your signature in the field below to confirm acceptance.</p>
              </div>
            </div>

            {actionError && (
              <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-lg flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{actionError}</span>
              </div>
            )}

            <canvas
              ref={canvasRef}
              width={600}
              height={180}
              onMouseDown={startDraw}
              onMouseMove={draw}
              onMouseUp={stopDraw}
              onMouseLeave={stopDraw}
              onTouchStart={startDraw}
              onTouchMove={draw}
              onTouchEnd={stopDraw}
              className="w-full border border-dashed border-indigo-500/30 rounded-lg cursor-crosshair bg-white/[0.015] touch-none"
              style={{ height: '150px' }}
            />

            <div className="flex gap-3">
              <button
                type="button"
                onClick={clearSig}
                className="px-4 py-2 sharp-btn-secondary text-xs font-semibold"
              >
                Clear
              </button>
              <button
                type="button"
                onClick={() => setSignatureStep(false)}
                className="px-4 py-2 sharp-btn-secondary text-xs font-semibold"
              >
                Back
              </button>
              <button
                onClick={handleAccept}
                disabled={!hasSig || submitting}
                className="flex-1 flex items-center justify-center gap-2 py-2 sharp-btn-primary text-white text-xs font-semibold shadow-sm disabled:opacity-50"
              >
                {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle className="w-4 h-4" />}
                {submitting ? 'Submitting Signature…' : 'Confirm & Accept Proposal'}
              </button>
            </div>
          </div>
        )}

        <p className="text-center text-[11px] font-mono-num text-zinc-500 pt-2 pb-6">
          Powered by InvoiceFlow Precision Invoicing
        </p>
      </div>
    </div>
  );
};

export default PublicProposalPage;
