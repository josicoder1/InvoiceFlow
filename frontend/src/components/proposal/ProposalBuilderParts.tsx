import React from 'react';
import { ChevronDown, ChevronUp, Plus, X } from 'lucide-react';
import type { ProposalItemRequest } from '../../types';
import { sectionMeta } from './proposalSections';
import { calcTotal, calcSubtotal, calcDiscountAmount, calcTaxAmount } from './proposalPricing';

interface SectionValues {
  overview: string;
  scope: string;
  timeline: string;
  pricingNotes: string;
}

interface ProposalBuilderSidebarProps {
  title: string;
  clientId: string;
  sections: SectionValues;
  items: ProposalItemRequest[];
  discountType?: 'PERCENT' | 'FIXED' | '' | null;
  discountValue?: number;
  taxRate?: number;
}

const ProposalBuilderSidebar: React.FC<ProposalBuilderSidebarProps> = ({
  title,
  clientId,
  sections,
  items,
  discountType,
  discountValue = 0,
  taxRate = 0,
}) => {
  const completedSections = sectionMeta.filter(({ key }) => sections[key].trim().length > 20).length;
  const completeness = Math.round(
    ((title.trim() ? 1 : 0) + (clientId ? 1 : 0) + completedSections) / (2 + sectionMeta.length) * 100,
  );
  const subtotal = calcSubtotal(items);
  const discount = calcDiscountAmount(subtotal, discountType || null, discountValue);
  const tax = calcTaxAmount(subtotal, discount, taxRate);
  const total = calcTotal(subtotal, discountType || null, discountValue, taxRate);

  return (
    <div className="space-y-4">
      <div className="sharp-card p-5">
        <p className="sharp-label mb-1">Completeness</p>
        <p className="text-3xl font-bold font-mono-num text-white">{completeness}%</p>
        <div className="w-full h-1.5 rounded-full bg-white/[0.08] mt-2 mb-4 overflow-hidden">
          <div className="h-full bg-emerald-400 transition-all duration-300" style={{ width: `${completeness}%` }} />
        </div>
        <div className="space-y-2">
          {sectionMeta.map(({ key, label }) => {
            const filled = sections[key].trim().length > 20;
            return (
              <div key={key} className="flex items-center gap-2 text-xs">
                <span className={`w-1.5 h-1.5 rounded-full ${filled ? 'bg-emerald-400 shadow-sm shadow-emerald-400' : 'bg-zinc-600'}`} />
                <span className={filled ? 'text-zinc-200 font-medium' : 'text-zinc-500'}>{label}</span>
              </div>
            );
          })}
        </div>
      </div>

      <div className="sharp-card p-5">
        <p className="sharp-label mb-3">Pricing Summary</p>
        <div className="space-y-2 text-xs">
          <div className="flex justify-between text-zinc-400">
            <span>Subtotal</span>
            <span className="font-mono-num text-zinc-200">${subtotal.toFixed(2)}</span>
          </div>
          {discount > 0 && (
            <div className="flex justify-between text-rose-400">
              <span>Discount</span>
              <span className="font-mono-num">-${discount.toFixed(2)}</span>
            </div>
          )}
          {tax > 0 && (
            <div className="flex justify-between text-zinc-400">
              <span>Tax</span>
              <span className="font-mono-num text-zinc-200">${tax.toFixed(2)}</span>
            </div>
          )}
          <div className="pt-3 border-t border-white/[0.08] flex justify-between items-baseline text-white">
            <span className="text-sm font-semibold">Total</span>
            <span className="text-lg font-bold font-mono-num">${total.toFixed(2)}</span>
          </div>
        </div>
      </div>
    </div>
  );
};

interface ProposalSectionFieldsProps {
  overview: string;
  scope: string;
  timeline: string;
  pricingNotes: string;
  setOverview: (v: string) => void;
  setScope: (v: string) => void;
  setTimeline: (v: string) => void;
  setPricingNotes: (v: string) => void;
  collapsed: Record<string, boolean>;
  onToggle: (key: string) => void;
}

export const ProposalSectionFields: React.FC<ProposalSectionFieldsProps> = ({
  overview, scope, timeline, pricingNotes,
  setOverview, setScope, setTimeline, setPricingNotes,
  collapsed, onToggle,
}) => {
  const values = { overview, scope, timeline, pricingNotes };
  const setters = { overview: setOverview, scope: setScope, timeline: setTimeline, pricingNotes: setPricingNotes };

  return (
    <>
      {sectionMeta.map(({ key, label, icon: Icon, placeholder }) => {
        const value = values[key];
        const setter = setters[key];
        const isOpen = !collapsed[key];
        const filled = value.trim().length > 20;
        return (
          <div key={key} className="bg-white/[0.015] border border-white/[0.06] rounded-lg overflow-hidden">
            <button
              type="button"
              onClick={() => onToggle(key)}
              className="w-full p-3 flex items-center justify-between hover:bg-white/[0.02] transition-colors"
            >
              <div className="flex items-center gap-2.5">
                <div className="w-6 h-6 rounded-md bg-indigo-500/15 border border-indigo-500/25 flex items-center justify-center">
                  <Icon className="w-3.5 h-3.5 text-indigo-300" />
                </div>
                <span className="text-xs font-semibold text-white tracking-tight">{label}</span>
                {filled && (
                  <span className="text-[10px] px-2 py-0.5 rounded font-mono-num bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                    Complete
                  </span>
                )}
              </div>
              {isOpen ? <ChevronUp className="w-4 h-4 text-zinc-400" /> : <ChevronDown className="w-4 h-4 text-zinc-400" />}
            </button>
            {isOpen && (
              <div className="p-3 pt-0">
                <textarea
                  rows={4}
                  value={value}
                  onChange={e => setter(e.target.value)}
                  placeholder={placeholder}
                  className="w-full sharp-input px-3.5 py-2 text-xs text-zinc-100 placeholder-zinc-500 resize-none"
                />
              </div>
            )}
          </div>
        );
      })}
    </>
  );
};

interface ProposalPricingFieldsProps {
  items: ProposalItemRequest[];
  discountType: 'PERCENT' | 'FIXED' | '';
  discountValue: number;
  taxRate: number;
  setDiscountType: (v: 'PERCENT' | 'FIXED' | '') => void;
  setDiscountValue: (v: number) => void;
  setTaxRate: (v: number) => void;
  onAddItem: () => void;
  onRemoveItem: (i: number) => void;
  onUpdateItem: (i: number, field: keyof ProposalItemRequest, value: unknown) => void;
}

export const ProposalPricingFields: React.FC<ProposalPricingFieldsProps> = ({
  items, discountType, discountValue, taxRate,
  setDiscountType, setDiscountValue, setTaxRate,
  onAddItem, onRemoveItem, onUpdateItem,
}) => (
  <>
    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
      <div className="space-y-1.5">
        <label className="sharp-label">Discount Type</label>
        <select
          value={discountType}
          onChange={e => setDiscountType(e.target.value as 'PERCENT' | 'FIXED' | '')}
          className="w-full sharp-input px-3 py-2 text-xs text-zinc-200"
        >
          <option value="" className="bg-[#0c101d]">None</option>
          <option value="PERCENT" className="bg-[#0c101d]">Percent (%)</option>
          <option value="FIXED" className="bg-[#0c101d]">Fixed Amount</option>
        </select>
      </div>
      <div className="space-y-1.5">
        <label className="sharp-label">Discount Value</label>
        <input
          type="number"
          min="0"
          step="any"
          value={discountValue}
          onChange={e => setDiscountValue(parseFloat(e.target.value) || 0)}
          className="w-full sharp-input px-3 py-2 text-xs font-mono-num text-zinc-200"
        />
      </div>
      <div className="space-y-1.5">
        <label className="sharp-label">Tax Rate (%)</label>
        <input
          type="number"
          min="0"
          step="any"
          value={taxRate}
          onChange={e => setTaxRate(parseFloat(e.target.value) || 0)}
          className="w-full sharp-input px-3 py-2 text-xs font-mono-num text-zinc-200"
        />
      </div>
    </div>

    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="sharp-label">Pricing Items</h3>
        <button
          type="button"
          onClick={onAddItem}
          className="flex items-center gap-1.5 text-xs text-indigo-400 hover:text-indigo-300 font-semibold py-1 px-2.5 rounded-lg bg-indigo-500/10 border border-indigo-500/20 transition-all"
        >
          <Plus className="w-3.5 h-3.5" /> Add Item
        </button>
      </div>
      <div className="space-y-2">
        {items.map((item, idx) => (
          <div key={idx} className="grid grid-cols-12 gap-3 items-center bg-white/[0.015] border border-white/[0.06] p-2.5 rounded-lg">
            <div className="col-span-12 sm:col-span-6">
              <input
                type="text"
                placeholder="Description..."
                value={item.description}
                onChange={e => onUpdateItem(idx, 'description', e.target.value)}
                className="w-full sharp-input px-3 py-1.5 text-xs text-zinc-100"
              />
            </div>
            <div className="col-span-4 sm:col-span-2">
              <input
                type="number"
                min="0"
                step="any"
                placeholder="Qty"
                value={item.quantity ?? ''}
                onChange={e => onUpdateItem(idx, 'quantity', parseFloat(e.target.value) || undefined)}
                className="w-full sharp-input px-3 py-1.5 text-xs font-mono-num text-zinc-100"
              />
            </div>
            <div className="col-span-4 sm:col-span-2">
              <input
                type="number"
                min="0"
                step="any"
                placeholder="Price"
                value={item.unitPrice ?? ''}
                onChange={e => onUpdateItem(idx, 'unitPrice', parseFloat(e.target.value) || undefined)}
                className="w-full sharp-input px-3 py-1.5 text-xs font-mono-num text-zinc-100"
              />
            </div>
            <div className="col-span-3 sm:col-span-1 text-right text-xs font-semibold font-mono-num text-zinc-200">
              {((item.quantity ?? 0) * (item.unitPrice ?? 0)).toFixed(2)}
            </div>
            <div className="col-span-1 text-center">
              <button
                type="button"
                disabled={items.length === 1}
                onClick={() => onRemoveItem(idx)}
                className="p-1 text-zinc-500 hover:text-rose-400 disabled:opacity-20 transition-all"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  </>
);

export default ProposalBuilderSidebar;
