import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../components/DashboardLayout';
import { invoicesApi } from '../api/invoices';
import { clientsApi } from '../api/clients';
import type { Client, InvoiceItemRequest } from '../types';
import { Plus, X, AlertCircle, ArrowLeft, Loader2, Save } from 'lucide-react';

const InvoiceNew: React.FC = () => {
  const navigate = useNavigate();

  const [clients, setClients] = useState<Client[]>([]);
  const [formClientId, setFormClientId] = useState('');
  const [formIssueDate, setFormIssueDate] = useState(new Date().toISOString().split('T')[0]);
  const [formDueDate, setFormDueDate] = useState(
    new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  );
  const [formCurrency, setFormCurrency] = useState('USD');
  const [formDiscountType, setFormDiscountType] = useState<'PERCENT' | 'FIXED' | ''>('');
  const [formDiscountValue, setFormDiscountValue] = useState(0);
  const [formTaxRate, setFormTaxRate] = useState(0);
  const [formNotes, setFormNotes] = useState('');
  const [formPaymentTerms, setFormPaymentTerms] = useState('Net 30');
  const [formItems, setFormItems] = useState<InvoiceItemRequest[]>([
    { description: '', quantity: 1, unitPrice: 0, position: 1 },
  ]);
  const [formError, setFormError] = useState<string | null>(null);
  const [formSubmitting, setFormSubmitting] = useState(false);

  useEffect(() => {
    clientsApi.getClients(undefined, 0, 1000).then((data) => {
      setClients(data.content);
      if (data.content.length > 0) {
        setFormClientId(data.content[0].id);
        setFormCurrency(data.content[0].currency);
      }
    });
  }, []);

  const handleClientChange = (clientId: string) => {
    setFormClientId(clientId);
    const selected = clients.find((c) => c.id === clientId);
    if (selected) setFormCurrency(selected.currency);
  };

  const handleAddItem = () => {
    setFormItems([...formItems, { description: '', quantity: 1, unitPrice: 0, position: formItems.length + 1 }]);
  };

  const handleRemoveItem = (index: number) => {
    if (formItems.length === 1) return;
    setFormItems(formItems.filter((_, i) => i !== index).map((item, idx) => ({ ...item, position: idx + 1 })));
  };

  const handleItemChange = (index: number, field: keyof InvoiceItemRequest, value: any) => {
    const newItems = [...formItems];
    newItems[index] = { ...newItems[index], [field]: value };
    setFormItems(newItems);
  };

  const calcSubtotal = () => formItems.reduce((s, item) => s + item.quantity * item.unitPrice, 0);

  const calcTotal = () => {
    const sub = calcSubtotal();
    const discount = formDiscountType === 'PERCENT' ? sub * (formDiscountValue / 100) : formDiscountType === 'FIXED' ? formDiscountValue : 0;
    const discounted = Math.max(0, sub - discount);
    return discounted + discounted * (formTaxRate / 100);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    if (!formClientId) { setFormError('Please select a client'); return; }
    const invalid = formItems.find((item) => !item.description.trim() || item.quantity <= 0 || item.unitPrice < 0);
    if (invalid) { setFormError('Ensure all items have descriptions, positive quantities, and non-negative prices.'); return; }

    setFormSubmitting(true);
    try {
      await invoicesApi.createInvoice({
        clientId: formClientId,
        issueDate: formIssueDate,
        dueDate: formDueDate,
        currency: formCurrency,
        items: formItems,
        discountType: formDiscountType || null,
        discountValue: formDiscountValue || 0,
        taxRate: formTaxRate || 0,
        notes: formNotes || undefined,
        paymentTerms: formPaymentTerms || undefined,
      });
      navigate('/invoices');
    } catch (err: any) {
      setFormError(err.response?.data?.detail || 'An error occurred while saving.');
    } finally {
      setFormSubmitting(false);
    }
  };

  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/invoices')}
              className="p-2 sharp-btn-secondary text-zinc-400 hover:text-white"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-white">
                New Invoice
              </h1>
              <p className="text-zinc-400 text-xs">Create and dispatch a high-precision invoice.</p>
            </div>
          </div>
        </div>

        {/* Form Card */}
        <div className="sharp-card p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            {formError && (
              <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-lg flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{formError}</span>
              </div>
            )}

            {/* Client & Dates */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="space-y-1.5">
                <label className="sharp-label">Bill To *</label>
                <select
                  value={formClientId}
                  onChange={(e) => handleClientChange(e.target.value)}
                  className="w-full sharp-input px-3.5 py-2 text-zinc-200"
                >
                  {clients.length === 0 && <option value="">No clients registered</option>}
                  {clients.map((c) => (
                    <option key={c.id} value={c.id} className="bg-[#0c101d] text-white">
                      {c.name} ({c.company || 'Private'})
                    </option>
                  ))}
                </select>
              </div>
              <div className="space-y-1.5">
                <label className="sharp-label">Issue Date *</label>
                <input
                  type="date"
                  required
                  value={formIssueDate}
                  onChange={(e) => setFormIssueDate(e.target.value)}
                  className="w-full sharp-input px-3.5 py-2 font-mono-num text-zinc-200"
                />
              </div>
              <div className="space-y-1.5">
                <label className="sharp-label">Due Date *</label>
                <input
                  type="date"
                  required
                  value={formDueDate}
                  onChange={(e) => setFormDueDate(e.target.value)}
                  className="w-full sharp-input px-3.5 py-2 font-mono-num text-zinc-200"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="sharp-label">Currency</label>
                <input
                  type="text"
                  readOnly
                  value={`${formCurrency} (inherited from client)`}
                  className="w-full sharp-input px-3.5 py-2 font-mono-num text-zinc-400 bg-white/[0.01] border-white/[0.05] cursor-not-allowed"
                />
              </div>
              <div className="space-y-1.5">
                <label className="sharp-label">Payment Terms</label>
                <select
                  value={formPaymentTerms}
                  onChange={(e) => setFormPaymentTerms(e.target.value)}
                  className="w-full sharp-input px-3.5 py-2 text-zinc-200"
                >
                  <option value="Net 30" className="bg-[#0c101d]">Net 30 (Default)</option>
                  <option value="Net 15" className="bg-[#0c101d]">Net 15</option>
                  <option value="Due on Receipt" className="bg-[#0c101d]">Due on Receipt</option>
                  <option value="Net 7" className="bg-[#0c101d]">Net 7</option>
                </select>
              </div>
            </div>

            {/* Line Items */}
            <div className="space-y-3 pt-2">
              <div className="flex items-center justify-between">
                <h3 className="sharp-label">Line Items</h3>
                <button
                  type="button"
                  onClick={handleAddItem}
                  className="flex items-center gap-1.5 text-xs text-indigo-400 hover:text-indigo-300 font-semibold py-1.5 px-3 rounded-lg bg-indigo-500/10 hover:bg-indigo-500/15 border border-indigo-500/20 transition-all"
                >
                  <Plus className="w-3.5 h-3.5" /> Add Item
                </button>
              </div>

              <div className="space-y-2">
                <div className="grid grid-cols-12 gap-3 px-3 py-1 text-[11px] font-semibold text-zinc-500 uppercase tracking-wider">
                  <div className="col-span-12 sm:col-span-6">Description</div>
                  <div className="col-span-4 sm:col-span-2">Qty</div>
                  <div className="col-span-4 sm:col-span-2">Unit Price</div>
                  <div className="col-span-3 sm:col-span-1 text-right">Total</div>
                  <div className="col-span-1 text-center"></div>
                </div>

                {formItems.map((item, index) => (
                  <div
                    key={index}
                    className="grid grid-cols-12 gap-3 items-center bg-white/[0.015] border border-white/[0.06] p-2.5 rounded-lg"
                  >
                    <div className="col-span-12 sm:col-span-6">
                      <input
                        type="text"
                        required
                        placeholder="Item description or service..."
                        value={item.description}
                        onChange={(e) => handleItemChange(index, 'description', e.target.value)}
                        className="w-full sharp-input px-3 py-1.5 text-xs text-zinc-100"
                      />
                    </div>
                    <div className="col-span-4 sm:col-span-2">
                      <input
                        type="number"
                        required
                        min="0.01"
                        step="any"
                        placeholder="Qty"
                        value={item.quantity}
                        onChange={(e) => handleItemChange(index, 'quantity', parseFloat(e.target.value) || 0)}
                        className="w-full sharp-input px-3 py-1.5 text-xs font-mono-num text-zinc-100"
                      />
                    </div>
                    <div className="col-span-4 sm:col-span-2">
                      <input
                        type="number"
                        required
                        min="0"
                        step="any"
                        placeholder="Price"
                        value={item.unitPrice}
                        onChange={(e) => handleItemChange(index, 'unitPrice', parseFloat(e.target.value) || 0)}
                        className="w-full sharp-input px-3 py-1.5 text-xs font-mono-num text-zinc-100"
                      />
                    </div>
                    <div className="col-span-3 sm:col-span-1 text-right text-xs font-semibold font-mono-num text-zinc-200">
                      {(item.quantity * item.unitPrice).toFixed(2)}
                    </div>
                    <div className="col-span-1 text-center">
                      <button
                        type="button"
                        disabled={formItems.length === 1}
                        onClick={() => handleRemoveItem(index)}
                        className="p-1 text-zinc-500 hover:text-rose-400 disabled:opacity-20 transition-all"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Discount, Tax, Notes & Totals */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-5 border-t border-white/[0.08]">
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-1.5">
                    <label className="sharp-label">Discount Type</label>
                    <select
                      value={formDiscountType}
                      onChange={(e) => { setFormDiscountType(e.target.value as any); setFormDiscountValue(0); }}
                      className="w-full sharp-input px-3 py-2 text-xs text-zinc-200"
                    >
                      <option value="" className="bg-[#0c101d]">None</option>
                      <option value="PERCENT" className="bg-[#0c101d]">Percentage (%)</option>
                      <option value="FIXED" className="bg-[#0c101d]">Fixed Amount</option>
                    </select>
                  </div>
                  <div className="space-y-1.5">
                    <label className="sharp-label">Discount Value</label>
                    <input
                      type="number"
                      min="0"
                      disabled={!formDiscountType}
                      value={formDiscountValue}
                      onChange={(e) => setFormDiscountValue(parseFloat(e.target.value) || 0)}
                      className="w-full sharp-input px-3 py-2 text-xs font-mono-num text-zinc-200 disabled:opacity-40 disabled:cursor-not-allowed"
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="sharp-label">Tax Rate (%)</label>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    value={formTaxRate}
                    onChange={(e) => setFormTaxRate(parseFloat(e.target.value) || 0)}
                    className="w-full sharp-input px-3 py-2 text-xs font-mono-num text-zinc-200"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="sharp-label">Invoice Notes</label>
                  <textarea
                    rows={3}
                    value={formNotes}
                    onChange={(e) => setFormNotes(e.target.value)}
                    placeholder="Payment guidelines, wire details, terms or custom notes..."
                    className="w-full sharp-input px-3 py-2 text-xs text-zinc-200 resize-none"
                  />
                </div>
              </div>

              {/* Summary Card */}
              <div className="bg-white/[0.015] border border-white/[0.06] p-5 rounded-xl flex flex-col justify-between space-y-4">
                <div className="space-y-2.5 text-xs">
                  <div className="flex justify-between text-zinc-400">
                    <span>Subtotal</span>
                    <span className="font-mono-num text-zinc-200">{formCurrency} {calcSubtotal().toFixed(2)}</span>
                  </div>
                  {formDiscountType && (
                    <div className="flex justify-between text-indigo-400">
                      <span>Discount ({formDiscountType === 'PERCENT' ? `${formDiscountValue}%` : 'Fixed'})</span>
                      <span className="font-mono-num">
                        -{formCurrency} {(formDiscountType === 'PERCENT' ? calcSubtotal() * (formDiscountValue / 100) : formDiscountValue).toFixed(2)}
                      </span>
                    </div>
                  )}
                  {formTaxRate > 0 && (
                    <div className="flex justify-between text-zinc-400">
                      <span>Tax ({formTaxRate}%)</span>
                      <span className="font-mono-num text-zinc-200">
                        {formCurrency} {(Math.max(0, calcSubtotal() - (formDiscountType === 'PERCENT' ? calcSubtotal() * (formDiscountValue / 100) : formDiscountValue)) * (formTaxRate / 100)).toFixed(2)}
                      </span>
                    </div>
                  )}
                  <div className="pt-3 border-t border-white/[0.08] flex justify-between items-baseline">
                    <span className="text-sm font-semibold text-white">Total Amount</span>
                    <span className="text-xl font-bold font-mono-num text-white">
                      {formCurrency} {calcTotal().toFixed(2)}
                    </span>
                  </div>
                </div>

                <div className="flex gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => navigate('/invoices')}
                    className="flex-1 sharp-btn-secondary py-2 text-xs"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={formSubmitting}
                    className="flex-1 sharp-btn-primary py-2 text-xs flex items-center justify-center gap-2 text-white"
                  >
                    {formSubmitting ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <>
                        <Save className="w-3.5 h-3.5" />
                        <span>Save Invoice</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          </form>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default InvoiceNew;
