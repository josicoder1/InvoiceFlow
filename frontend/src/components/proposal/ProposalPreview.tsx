import React from 'react';
import type { PricedItem } from './proposalPricing';
import { calcDiscountAmount, calcSubtotal, calcTaxAmount, calcTotal } from './proposalPricing';
import { sectionMeta } from './proposalSections';
import type { TemplateTheme } from './proposalTemplateThemes';
import { defaultTheme } from './proposalTemplateThemes';

export interface ProposalPreviewData {
  title: string;
  validUntil?: string | null;
  overview?: string | null;
  scope?: string | null;
  timeline?: string | null;
  pricingNotes?: string | null;
  items: PricedItem[];
  subtotal?: number | null;
  discountType?: 'PERCENT' | 'FIXED' | string | null;
  discountValue?: number | null;
  taxRate?: number | null;
  total?: number | null;
  templateName?: string | null;
  fromName?: string | null;
  proposalNumber?: string | null;
}

interface ProposalPreviewProps {
  data: ProposalPreviewData;
  theme?: TemplateTheme;
  variant?: 'dark' | 'light';
}

function filledItems(items: PricedItem[]) {
  return items.filter(i => i.description?.trim());
}

function lineTotal(item: PricedItem) {
  if (item.total != null) return item.total;
  return (item.quantity ?? 0) * (item.unitPrice ?? 0);
}

function TimelineSteps({ text, accent }: { text: string; accent: string }) {
  const lines = text.split('\n').filter(l => l.trim());
  return (
    <div className="space-y-3">
      {lines.map((line, i) => (
        <div key={i} className="flex gap-3">
          <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${accent}`}>
            {i + 1}
          </div>
          <p className="text-sm leading-relaxed pt-0.5">{line}</p>
        </div>
      ))}
    </div>
  );
}

function PricingTable({ items, subtotal, discount, tax, total, variant }: {
  items: PricedItem[];
  subtotal: number;
  discount: number;
  tax: number;
  total: number;
  variant: 'dark' | 'light';
}) {
  const row = variant === 'dark' ? 'border-white/[0.05] text-zinc-300' : 'border-gray-100 text-gray-800';
  const muted = variant === 'dark' ? 'text-zinc-500' : 'text-gray-500';
  const strong = variant === 'dark' ? 'text-zinc-100' : 'text-gray-900';
  const listed = filledItems(items);
  if (listed.length === 0) return null;

  return (
    <div className="space-y-3">
      <div className={`border rounded-xl overflow-hidden ${variant === 'dark' ? 'border-white/[0.05]' : 'border-gray-200'}`}>
        {listed.map((item, idx) => (
          <div key={idx} className={`flex items-center justify-between px-4 py-2.5 text-sm border-b last:border-0 ${row}`}>
            <span>{item.description}</span>
            <span className={`font-medium ${strong}`}>${lineTotal(item).toFixed(2)}</span>
          </div>
        ))}
      </div>
      <div className={`space-y-1 text-sm ${muted}`}>
        <div className="flex justify-between"><span>Subtotal</span><span>${subtotal.toFixed(2)}</span></div>
        {discount > 0 && <div className="flex justify-between"><span>Discount</span><span>-${discount.toFixed(2)}</span></div>}
        {tax > 0 && <div className="flex justify-between"><span>Tax</span><span>${tax.toFixed(2)}</span></div>}
        <div className={`flex justify-between text-base font-semibold pt-2 border-t ${variant === 'dark' ? 'border-white/[0.05] text-zinc-100' : 'border-gray-200 text-gray-900'}`}>
          <span>Total</span><span>${total.toFixed(2)}</span>
        </div>
      </div>
    </div>
  );
}

function ClassicLayout({ data, theme, variant }: { data: ProposalPreviewData; theme: TemplateTheme; variant: 'dark' | 'light' }) {
  const text = variant === 'dark' ? 'text-zinc-200' : 'text-gray-600';
  const heading = variant === 'dark' ? 'text-zinc-400' : 'text-gray-500';
  const subtotal = data.subtotal ?? calcSubtotal(data.items);
  const discount = calcDiscountAmount(subtotal, data.discountType, data.discountValue);
  const tax = calcTaxAmount(subtotal, discount, data.taxRate);
  const total = data.total ?? calcTotal(subtotal, data.discountType, data.discountValue, data.taxRate);

  return (
    <div className="space-y-6">
      {sectionMeta.map(({ key, label }, idx) => {
        const value = data[key];
        if (!value?.trim()) return null;
        return (
          <div key={key}>
            <div className="flex items-center gap-2 mb-2">
              <span className={`w-6 h-6 rounded-md text-xs font-bold flex items-center justify-center ${theme.accentBg} ${theme.accentText}`}>{idx + 1}</span>
              <h3 className={`text-xs font-semibold uppercase tracking-wider ${heading}`}>{label}</h3>
            </div>
            <p className={`text-sm whitespace-pre-line leading-relaxed ${text}`}>{value}</p>
          </div>
        );
      })}
      {filledItems(data.items).length > 0 && (
        <div>
          <h3 className={`text-xs font-semibold uppercase tracking-wider mb-3 ${heading}`}>Pricing</h3>
          <PricingTable items={data.items} subtotal={subtotal} discount={discount} tax={tax} total={total} variant={variant} />
          {data.pricingNotes?.trim() && <p className={`text-sm mt-3 ${text}`}>{data.pricingNotes}</p>}
        </div>
      )}
    </div>
  );
}

function CreativeLayout({ data, theme, variant }: { data: ProposalPreviewData; theme: TemplateTheme; variant: 'dark' | 'light' }) {
  const card = variant === 'dark' ? 'bg-white/[0.03] border-white/[0.06]' : 'bg-gradient-to-br from-fuchsia-50 to-pink-50 border-fuchsia-100';
  const text = variant === 'dark' ? 'text-zinc-200' : 'text-gray-700';
  const heading = variant === 'dark' ? 'text-fuchsia-300' : 'text-fuchsia-700';
  const subtotal = data.subtotal ?? calcSubtotal(data.items);
  const total = data.total ?? calcTotal(subtotal, data.discountType, data.discountValue, data.taxRate);
  const listed = filledItems(data.items);

  return (
    <div className="space-y-5">
      <div className="grid md:grid-cols-2 gap-4">
        {data.overview?.trim() && (
          <div className={`md:col-span-2 rounded-2xl border p-5 ${card}`}>
            <h3 className={`text-sm font-bold mb-2 ${heading}`}>Overview</h3>
            <p className={`text-sm whitespace-pre-line leading-relaxed ${text}`}>{data.overview}</p>
          </div>
        )}
        {data.scope?.trim() && (
          <div className={`rounded-2xl border p-5 ${card}`}>
            <h3 className={`text-sm font-bold mb-2 ${heading}`}>Scope</h3>
            <p className={`text-sm whitespace-pre-line leading-relaxed ${text}`}>{data.scope}</p>
          </div>
        )}
        {data.timeline?.trim() && (
          <div className={`rounded-2xl border p-5 ${card}`}>
            <h3 className={`text-sm font-bold mb-3 ${heading}`}>Timeline</h3>
            <TimelineSteps text={data.timeline} accent={`${theme.accentBg} ${theme.accentText}`} />
          </div>
        )}
      </div>
      {listed.length > 0 && (
        <div>
          <h3 className={`text-sm font-bold mb-3 ${heading}`}>Investment</h3>
          <div className="grid sm:grid-cols-2 gap-3">
            {listed.map((item, idx) => (
              <div key={idx} className={`rounded-xl border p-4 ${card}`}>
                <p className={`text-sm font-medium ${text}`}>{item.description}</p>
                <p className={`text-lg font-bold mt-2 ${variant === 'dark' ? 'text-white' : 'text-fuchsia-800'}`}>
                  ${lineTotal(item).toFixed(2)}
                </p>
              </div>
            ))}
          </div>
          <div className={`mt-4 rounded-xl border p-4 flex justify-between items-center ${card}`}>
            <span className={`font-semibold ${text}`}>Total</span>
            <span className={`text-xl font-bold ${variant === 'dark' ? 'text-white' : 'text-fuchsia-800'}`}>${total.toFixed(2)}</span>
          </div>
          {data.pricingNotes?.trim() && <p className={`text-sm mt-3 ${text}`}>{data.pricingNotes}</p>}
        </div>
      )}
    </div>
  );
}

function CorporateLayout({ data, variant }: { data: ProposalPreviewData; theme: TemplateTheme; variant: 'dark' | 'light' }) {
  const border = variant === 'dark' ? 'border-l-cyan-400/60' : 'border-l-cyan-600';
  const text = variant === 'dark' ? 'text-zinc-200' : 'text-gray-700';
  const heading = variant === 'dark' ? 'text-cyan-300' : 'text-cyan-800';
  const subtotal = data.subtotal ?? calcSubtotal(data.items);
  const discount = calcDiscountAmount(subtotal, data.discountType, data.discountValue);
  const tax = calcTaxAmount(subtotal, discount, data.taxRate);
  const total = data.total ?? calcTotal(subtotal, data.discountType, data.discountValue, data.taxRate);

  return (
    <div className="space-y-5">
      {sectionMeta.map(({ key, label }) => {
        const value = data[key];
        if (!value?.trim()) return null;
        return (
          <div key={key} className={`border-l-4 pl-4 ${border}`}>
            <h3 className={`text-xs font-bold uppercase tracking-widest mb-2 ${heading}`}>{label}</h3>
            <p className={`text-sm whitespace-pre-line leading-relaxed ${text}`}>{value}</p>
          </div>
        );
      })}
      {filledItems(data.items).length > 0 && (
        <div className={`rounded-xl border overflow-hidden ${variant === 'dark' ? 'border-white/[0.06]' : 'border-gray-200'}`}>
          <div className={`px-4 py-2 text-xs font-bold uppercase tracking-wider ${variant === 'dark' ? 'bg-cyan-900/30 text-cyan-200' : 'bg-cyan-700 text-white'}`}>
            Pricing Schedule
          </div>
          <div className="p-4">
            <PricingTable items={data.items} subtotal={subtotal} discount={discount} tax={tax} total={total} variant={variant} />
            {data.pricingNotes?.trim() && <p className={`text-sm mt-3 ${text}`}>{data.pricingNotes}</p>}
          </div>
        </div>
      )}
    </div>
  );
}

function ExecutiveLayout({ data, theme, variant }: { data: ProposalPreviewData; theme: TemplateTheme; variant: 'dark' | 'light' }) {
  const text = variant === 'dark' ? 'text-zinc-300' : 'text-gray-600';
  const strong = variant === 'dark' ? 'text-zinc-100' : 'text-gray-900';
  const subtotal = data.subtotal ?? calcSubtotal(data.items);
  const discount = calcDiscountAmount(subtotal, data.discountType, data.discountValue);
  const tax = calcTaxAmount(subtotal, discount, data.taxRate);
  const total = data.total ?? calcTotal(subtotal, data.discountType, data.discountValue, data.taxRate);

  return (
    <div className="grid lg:grid-cols-3 gap-6">
      <div className="lg:col-span-2 space-y-6">
        {data.overview?.trim() && (
          <blockquote className={`text-lg font-medium leading-relaxed border-l-4 pl-5 ${theme.accentBorder} ${strong}`}>
            {data.overview}
          </blockquote>
        )}
        {data.scope?.trim() && (
          <div>
            <h3 className={`text-xs font-bold uppercase tracking-widest mb-2 ${theme.accentText}`}>Deliverables</h3>
            <div className={`rounded-xl border p-4 text-sm whitespace-pre-line leading-relaxed ${variant === 'dark' ? 'border-white/[0.06] bg-white/[0.02]' : 'border-gray-200 bg-gray-50'} ${text}`}>
              {data.scope}
            </div>
          </div>
        )}
        {data.timeline?.trim() && (
          <div>
            <h3 className={`text-xs font-bold uppercase tracking-widest mb-3 ${theme.accentText}`}>Milestones</h3>
            <div className={`flex flex-wrap gap-2`}>
              {data.timeline.split('\n').filter(l => l.trim()).map((line, i) => (
                <span key={i} className={`text-xs px-3 py-1.5 rounded-full border ${theme.accentBorder} ${theme.accentBg} ${theme.accentText}`}>
                  {line}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>
      {filledItems(data.items).length > 0 && (
        <div className={`rounded-2xl border p-5 h-fit ${variant === 'dark' ? 'border-white/[0.06] bg-white/[0.02]' : 'border-emerald-200 bg-emerald-50'}`}>
          <h3 className={`text-xs font-bold uppercase tracking-widest mb-4 ${theme.accentText}`}>Summary</h3>
          <PricingTable items={data.items} subtotal={subtotal} discount={discount} tax={tax} total={total} variant={variant} />
          {data.pricingNotes?.trim() && <p className={`text-xs mt-4 leading-relaxed ${text}`}>{data.pricingNotes}</p>}
        </div>
      )}
    </div>
  );
}

const ProposalPreview: React.FC<ProposalPreviewProps> = ({ data, theme = defaultTheme, variant = 'dark' }) => {
  const shell = variant === 'dark'
    ? 'bg-[#0c101f] border-white/[0.05]'
    : 'bg-white border-gray-200 shadow-sm';
  const subtitle = variant === 'dark' ? 'text-white/80' : 'text-white/90';

  const body = (() => {
    switch (theme.layout) {
      case 'creative': return <CreativeLayout data={data} theme={theme} variant={variant} />;
      case 'corporate': return <CorporateLayout data={data} theme={theme} variant={variant} />;
      case 'executive': return <ExecutiveLayout data={data} theme={theme} variant={variant} />;
      default: return <ClassicLayout data={data} theme={theme} variant={variant} />;
    }
  })();

  return (
    <div className={`rounded-2xl border p-6 space-y-6 ${shell}`}>
      <div className={`rounded-2xl p-6 bg-gradient-to-r ${theme.previewClass} shadow-2xl`}>
        <div className="flex items-center justify-between gap-3 flex-wrap">
          <div>
            {data.fromName && <p className="text-xs uppercase tracking-wider text-white/75">Proposal from {data.fromName}</p>}
            {!data.fromName && <p className="text-xs uppercase tracking-wider text-white/75">Proposal</p>}
            <h2 className="text-2xl font-bold text-white mt-1">{data.title || 'Untitled Proposal'}</h2>
            <p className={`text-sm mt-1 ${subtitle}`}>
              {data.proposalNumber && <span className="mr-3">{data.proposalNumber}</span>}
              Valid until: {data.validUntil || 'Not set'}
            </p>
          </div>
          {data.templateName && (
            <span className="text-xs px-3 py-1 rounded-full border border-white/40 text-white bg-white/10">
              {data.templateName}
            </span>
          )}
        </div>
      </div>
      {body}
    </div>
  );
};

export default ProposalPreview;
