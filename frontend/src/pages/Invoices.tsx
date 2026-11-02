import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../components/DashboardLayout';
import { invoicesApi } from '../api/invoices';
import type { InvoiceFilters } from '../api/invoices';
import { clientsApi } from '../api/clients';
import type { Invoice, Client, InvoiceItemRequest, PageResponse } from '../types';
import {
  Plus, Search, Trash2, Edit, Copy, CheckCircle, Eye, X,
  AlertCircle, FileText, Send, Ban, Download, Loader2
} from 'lucide-react';

const Invoices: React.FC = () => {
  const navigate = useNavigate();
  const [invoicesPage, setInvoicesPage] = useState<PageResponse<Invoice> | null>(null);
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Filters State
  const [statusFilter, setStatusFilter] = useState('');
  const [clientFilter, setClientFilter] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  // Detail View State
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);

  // Preview modal state
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewHtml, setPreviewHtml] = useState<string | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [pdfLoading, setPdfLoading] = useState(false);

  // Invoice Form Modal State
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingInvoice, setEditingInvoice] = useState<Invoice | null>(null);
  const [formClientId, setFormClientId] = useState('');
  const [formIssueDate, setFormIssueDate] = useState(new Date().toISOString().split('T')[0]);
  const [formDueDate, setFormDueDate] = useState(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]);
  const [formCurrency, setFormCurrency] = useState('USD');
  const [formDiscountType, setFormDiscountType] = useState<'PERCENT' | 'FIXED' | ''>('');
  const [formDiscountValue, setFormDiscountValue] = useState(0);
  const [formTaxRate, setFormTaxRate] = useState(0);
  const [formNotes, setFormNotes] = useState('');
  const [formPaymentTerms, setFormPaymentTerms] = useState('Net 30');
  const [formItems, setFormItems] = useState<InvoiceItemRequest[]>([
    { description: '', quantity: 1, unitPrice: 0, position: 1 }
  ]);
  const [formError, setFormError] = useState<string | null>(null);
  const [formSubmitting, setFormSubmitting] = useState(false);

  const fetchInvoices = async () => {
    setLoading(true);
    setError(null);
    try {
      const filters: InvoiceFilters = {
        page,
        size: 8,
        status: statusFilter || undefined,
        clientId: clientFilter || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        search: search || undefined,
      };
      const data = await invoicesApi.getInvoices(filters);
      setInvoicesPage(data);
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to load invoices');
    } finally {
      setLoading(false);
    }
  };

  const fetchClients = async () => {
    try {
      const data = await clientsApi.getClients(undefined, 0, 1000);
      setClients(data.content);
    } catch (err) {
      console.error('Failed to load clients', err);
    }
  };

  useEffect(() => {
    fetchInvoices();
  }, [statusFilter, clientFilter, startDate, endDate, search, page]);

  useEffect(() => {
    fetchClients();
  }, []);

  const handleOpenCreateForm = () => {
    setEditingInvoice(null);
    setFormClientId(clients[0]?.id || '');
    setFormIssueDate(new Date().toISOString().split('T')[0]);
    setFormDueDate(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]);
    setFormCurrency(clients[0]?.currency || 'USD');
    setFormDiscountType('');
    setFormDiscountValue(0);
    setFormTaxRate(0);
    setFormNotes('');
    setFormPaymentTerms('Net 30');
    setFormItems([{ description: '', quantity: 1, unitPrice: 0, position: 1 }]);
    setFormError(null);
    setIsFormOpen(true);
  };

  const handleOpenEditForm = (invoice: Invoice) => {
    setEditingInvoice(invoice);
    setFormClientId(invoice.clientId);
    setFormIssueDate(invoice.issueDate);
    setFormDueDate(invoice.dueDate);
    setFormCurrency(invoice.currency);
    setFormDiscountType(invoice.discountType || '');
    setFormDiscountValue(invoice.discountValue);
    setFormTaxRate(invoice.taxRate);
    setFormNotes(invoice.notes || '');
    setFormPaymentTerms(invoice.paymentTerms || 'Net 30');
    setFormItems(invoice.items.map(item => ({
      description: item.description,
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      position: item.position
    })));
    setFormError(null);
    setIsFormOpen(true);
  };

  // Auto-set currency when Client changes in Form
  const handleClientChange = (clientId: string) => {
    setFormClientId(clientId);
    const selected = clients.find(c => c.id === clientId);
    if (selected) {
      setFormCurrency(selected.currency);
    }
  };

  const handleAddItem = () => {
    setFormItems([
      ...formItems,
      { description: '', quantity: 1, unitPrice: 0, position: formItems.length + 1 }
    ]);
  };

  const handleRemoveItem = (index: number) => {
    if (formItems.length === 1) return;
    const newItems = formItems.filter((_, i) => i !== index).map((item, idx) => ({
      ...item,
      position: idx + 1
    }));
    setFormItems(newItems);
  };

  const handleItemChange = (index: number, field: keyof InvoiceItemRequest, value: any) => {
    const newItems = [...formItems];
    newItems[index] = {
      ...newItems[index],
      [field]: value
    };
    setFormItems(newItems);
  };

  // Calculate live totals for the Form
  const calculateFormSubtotal = () => {
    return formItems.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0);
  };

  const calculateFormTotal = () => {
    const subtotal = calculateFormSubtotal();
    let discount = 0;
    if (formDiscountType === 'PERCENT') {
      discount = subtotal * (formDiscountValue / 100);
    } else if (formDiscountType === 'FIXED') {
      discount = formDiscountValue;
    }
    const discounted = Math.max(0, subtotal - discount);
    const tax = discounted * (formTaxRate / 100);
    return discounted + tax;
  };

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setFormSubmitting(true);

    // Basic Validations
    if (!formClientId) {
      setFormError('Please select a client');
      setFormSubmitting(false);
      return;
    }

    const invalidItem = formItems.find(item => !item.description.trim() || item.quantity <= 0 || item.unitPrice < 0);
    if (invalidItem) {
      setFormError('Ensure all items have descriptions, positive quantities, and non-negative unit prices.');
      setFormSubmitting(false);
      return;
    }

    const payload = {
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
    };

    try {
      if (editingInvoice) {
        await invoicesApi.editInvoice(editingInvoice.id, payload);
      } else {
        await invoicesApi.createInvoice(payload);
      }
      setIsFormOpen(false);
      fetchInvoices();
      if (selectedInvoice) {
        // Refresh details
        const updated = await invoicesApi.getInvoice(selectedInvoice.id);
        setSelectedInvoice(updated);
      }
    } catch (err: any) {
      setFormError(err.response?.data?.detail || 'An error occurred while saving.');
    } finally {
      setFormSubmitting(false);
    }
  };

  const handleMarkPaid = async (id: string) => {
    try {
      const updated = await invoicesApi.markInvoicePaid(id);
      fetchInvoices();
      if (selectedInvoice && selectedInvoice.id === id) {
        setSelectedInvoice(updated);
      }
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Could not mark paid');
    }
  };

  const handleSend = async (id: string) => {
    if (!confirm('Mark this invoice as SENT? This will lock it from further edits.')) return;
    try {
      const updated = await invoicesApi.markInvoiceSent(id);
      fetchInvoices();
      if (selectedInvoice && selectedInvoice.id === id) {
        setSelectedInvoice(updated);
      }
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Could not mark as sent');
    }
  };

  const handleCancel = async (id: string) => {
    if (!confirm('Cancel this invoice? This action cannot be undone for paid invoices.')) return;
    try {
      const updated = await invoicesApi.cancelInvoice(id);
      fetchInvoices();
      if (selectedInvoice && selectedInvoice.id === id) {
        setSelectedInvoice(updated);
      }
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Could not cancel invoice');
    }
  };

  const handleDuplicate = async (id: string) => {
    try {
      await invoicesApi.duplicateInvoice(id);
      fetchInvoices();
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Could not duplicate');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this invoice?')) return;
    try {
      await invoicesApi.deleteInvoice(id);
      fetchInvoices();
      if (selectedInvoice && selectedInvoice.id === id) {
        setSelectedInvoice(null);
      }
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Cannot delete invoice (only DRAFT or CANCELLED allowed)');
    }
  };

  const handlePreview = async (id: string) => {
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

  const handleDownloadPdf = async (id: string) => {
    setPdfLoading(true);
    try {
      await invoicesApi.downloadPdf(id);
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to download PDF');
    } finally {
      setPdfLoading(false);
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
              Invoices
            </h1>
            <p className="text-zinc-400 text-xs mt-0.5">Build, send, track, and manage customer billing and payment statuses.</p>
          </div>
          <button
            onClick={handleOpenCreateForm}
            className="flex items-center justify-center gap-1.5 px-3.5 py-2 sharp-btn-primary text-white font-semibold text-xs rounded-lg active:scale-[0.98] transition-all"
          >
            <Plus className="w-3.5 h-3.5" />
            Create Invoice
          </button>
        </div>

        {/* Filters Panel */}
        <div className="sharp-card p-3.5 rounded-xl grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-3 items-end">
          <div className="space-y-1 col-span-1 sm:col-span-2 md:col-span-1">
            <label className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">Search</label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-zinc-500" />
              <input
                type="text"
                placeholder="Number or Client..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full bg-white/[0.02] hover:bg-white/[0.03] border border-white/[0.08] rounded-lg pl-9 pr-3 py-1.5 text-xs text-zinc-200 placeholder-zinc-500 focus:outline-none focus:border-indigo-500/70 transition-all"
              />
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">Status</label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full bg-[#0c101d] border border-white/[0.08] rounded-lg px-2.5 py-1.5 text-xs text-zinc-200 focus:outline-none focus:border-indigo-500/70 transition-all"
            >
              <option value="">All Statuses</option>
              <option value="DRAFT">DRAFT</option>
              <option value="SENT">SENT</option>
              <option value="PAID">PAID</option>
              <option value="OVERDUE">OVERDUE</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">Client</label>
            <select
              value={clientFilter}
              onChange={(e) => setClientFilter(e.target.value)}
              className="w-full bg-[#0c101d] border border-white/[0.08] rounded-lg px-2.5 py-1.5 text-xs text-zinc-200 focus:outline-none focus:border-indigo-500/70 transition-all"
            >
              <option value="">All Clients</option>
              {clients.map(c => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">From Date</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full bg-white/[0.02] border border-white/[0.08] rounded-lg px-2.5 py-1.5 text-xs text-zinc-200 focus:outline-none focus:border-indigo-500/70 transition-all"
            />
          </div>

          <div className="space-y-1">
            <label className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">To Date</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="w-full bg-white/[0.02] border border-white/[0.08] rounded-lg px-2.5 py-1.5 text-xs text-zinc-200 focus:outline-none focus:border-indigo-500/70 transition-all"
            />
          </div>
        </div>

        {/* Dashboard Content Split Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-5 items-start">
          {/* Table Panel */}
          <div className="lg:col-span-2 sharp-card rounded-xl overflow-hidden flex flex-col">
            {loading && !invoicesPage ? (
              <div className="p-12 text-center text-zinc-500 text-xs">Loading invoices...</div>
            ) : error ? (
              <div className="p-12 text-center text-rose-400 text-xs flex flex-col items-center gap-2">
                <AlertCircle className="w-7 h-7" />
                <span>{error}</span>
              </div>
            ) : invoicesPage?.content.length === 0 ? (
              <div className="p-12 text-center text-zinc-500 text-xs">
                No invoices found matching criteria. Click "Create Invoice" to build one.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-white/[0.06] bg-white/[0.015] text-[11px] font-bold text-zinc-400 uppercase tracking-wider">
                      <th className="p-4">Invoice #</th>
                      <th className="p-4">Client</th>
                      <th className="p-4">Issue Date</th>
                      <th className="p-4">Total</th>
                      <th className="p-4">Status</th>
                      <th className="p-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/[0.04]">
                    {invoicesPage?.content.map((inv) => {
                      const client = clients.find(c => c.id === inv.clientId);
                      const isSelected = selectedInvoice?.id === inv.id;
                      return (
                        <tr
                          key={inv.id}
                          onClick={() => setSelectedInvoice(inv)}
                          className={`hover:bg-white/[0.02] cursor-pointer transition-all ${
                            isSelected ? 'bg-indigo-500/10 border-l-2 border-indigo-400' : 'border-l-2 border-transparent'
                          }`}
                        >
                          <td className="p-4 font-mono-num font-bold text-zinc-100 text-xs">{inv.invoiceNumber}</td>
                          <td className="p-4 text-xs text-zinc-300 font-medium">{client?.name || 'Loading client...'}</td>
                          <td className="p-4 text-xs text-zinc-400 font-mono-num">{inv.issueDate}</td>
                          <td className="p-4 font-mono-num font-bold text-white text-xs">
                            {new Intl.NumberFormat('en-US', { style: 'currency', currency: inv.currency }).format(inv.total)}
                          </td>
                          <td className="p-4">
                            <span
                              className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                                inv.status === 'PAID'
                                  ? 'bg-emerald-500/10 border border-emerald-500/25 text-emerald-400'
                                  : inv.status === 'SENT'
                                  ? 'bg-blue-500/10 border border-blue-500/25 text-blue-400'
                                  : inv.status === 'OVERDUE'
                                  ? 'bg-rose-500/10 border border-rose-500/25 text-rose-400'
                                  : 'bg-zinc-500/10 border border-zinc-500/25 text-zinc-400'
                              }`}
                            >
                              <span
                                className={`w-1.5 h-1.5 rounded-full ${
                                  inv.status === 'PAID'
                                    ? 'bg-emerald-400 shadow-sm shadow-emerald-400'
                                    : inv.status === 'SENT'
                                    ? 'bg-blue-400 shadow-sm shadow-blue-400'
                                    : inv.status === 'OVERDUE'
                                    ? 'bg-rose-400 shadow-sm shadow-rose-400'
                                    : 'bg-zinc-400'
                                }`}
                              />
                              {inv.status}
                            </span>
                          </td>
                          <td className="p-4 text-right" onClick={(e) => e.stopPropagation()}>
                            <div className="flex items-center justify-end gap-1">
                              <button
                                onClick={() => setSelectedInvoice(inv)}
                                className="p-1.5 text-zinc-400 hover:text-indigo-400 hover:bg-white/[0.05] rounded-lg transition-all"
                                title="View Details"
                              >
                                <Eye className="w-3.5 h-3.5" />
                              </button>
                              <button
                                onClick={() => handleDuplicate(inv.id)}
                                className="p-1.5 text-zinc-400 hover:text-blue-400 hover:bg-white/[0.05] rounded-lg transition-all"
                                title="Duplicate Invoice"
                              >
                                <Copy className="w-3.5 h-3.5" />
                              </button>
                              {inv.status === 'DRAFT' && (
                                <button
                                  onClick={() => handleSend(inv.id)}
                                  className="p-1.5 text-zinc-400 hover:text-blue-400 hover:bg-white/[0.05] rounded-lg transition-all"
                                  title="Mark as Sent"
                                >
                                  <Send className="w-3.5 h-3.5" />
                                </button>
                              )}
                              {inv.status === 'DRAFT' && (
                                <button
                                  onClick={() => handleOpenEditForm(inv)}
                                  className="p-1.5 text-zinc-400 hover:text-purple-400 hover:bg-white/[0.05] rounded-lg transition-all"
                                  title="Edit Invoice"
                                >
                                  <Edit className="w-3.5 h-3.5" />
                                </button>
                              )}
                              {(inv.status === 'DRAFT' || inv.status === 'SENT' || inv.status === 'OVERDUE') && (
                                <button
                                  onClick={() => handleCancel(inv.id)}
                                  className="p-1.5 text-zinc-400 hover:text-amber-400 hover:bg-white/[0.05] rounded-lg transition-all"
                                  title="Cancel Invoice"
                                >
                                  <Ban className="w-3.5 h-3.5" />
                                </button>
                              )}
                              {(inv.status === 'DRAFT' || inv.status === 'CANCELLED') && (
                                <button
                                  onClick={() => handleDelete(inv.id)}
                                  className="p-1.5 text-zinc-400 hover:text-rose-400 hover:bg-white/[0.05] rounded-lg transition-all"
                                  title="Delete"
                                >
                                  <Trash2 className="w-3.5 h-3.5" />
                                </button>
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

            {/* Pagination */}
            {invoicesPage && invoicesPage.totalPages > 1 && (
              <div className="p-3.5 border-t border-white/[0.08] flex items-center justify-between gap-4">
                <span className="text-xs text-zinc-400 font-mono-num">
                  Page {invoicesPage.number + 1} of {invoicesPage.totalPages}
                </span>
                <div className="flex gap-2">
                  <button
                    disabled={invoicesPage.number === 0}
                    onClick={() => setPage((p) => p - 1)}
                    className="px-2.5 py-1 bg-white/[0.02] border border-white/[0.08] rounded-lg text-xs font-semibold hover:bg-white/[0.05] active:scale-[0.98] disabled:opacity-40 disabled:pointer-events-none transition-all"
                  >
                    Previous
                  </button>
                  <button
                    disabled={invoicesPage.number === invoicesPage.totalPages - 1}
                    onClick={() => setPage((p) => p + 1)}
                    className="px-2.5 py-1 bg-white/[0.02] border border-white/[0.08] rounded-lg text-xs font-semibold hover:bg-white/[0.05] active:scale-[0.98] disabled:opacity-40 disabled:pointer-events-none transition-all"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Details Panel */}
          <div className="sharp-card rounded-xl p-5 space-y-5">
            {!selectedInvoice ? (
              <div className="text-center py-12 text-zinc-500 flex flex-col items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-white/[0.02] border border-white/[0.08] flex items-center justify-center">
                  <FileText className="w-4 h-4 text-zinc-400" />
                </div>
                <div>
                  <h3 className="font-bold text-zinc-200 text-xs">No invoice selected</h3>
                  <p className="text-[11px] text-zinc-500 mt-1 max-w-xs leading-relaxed">Select an invoice from the list to inspect line items, due dates, actions, and client details.</p>
                </div>
              </div>
            ) : (
              <div className="space-y-5">
                {/* Header detail */}
                <div className="flex items-start justify-between pb-3 border-b border-white/[0.08]">
                  <div>
                    <h3 className="text-base font-bold text-white font-mono-num">{selectedInvoice.invoiceNumber}</h3>
                    <span
                      className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md text-[10px] font-bold mt-1.5 uppercase ${
                        selectedInvoice.status === 'PAID'
                          ? 'bg-emerald-500/10 border border-emerald-500/25 text-emerald-400'
                          : selectedInvoice.status === 'SENT'
                          ? 'bg-blue-500/10 border border-blue-500/25 text-blue-400'
                          : selectedInvoice.status === 'OVERDUE'
                          ? 'bg-rose-500/10 border border-rose-500/25 text-rose-400'
                          : 'bg-zinc-500/10 border border-zinc-500/25 text-zinc-400'
                      }`}
                    >
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${
                          selectedInvoice.status === 'PAID'
                            ? 'bg-emerald-400 shadow-sm shadow-emerald-400'
                            : selectedInvoice.status === 'SENT'
                            ? 'bg-blue-400 shadow-sm shadow-blue-400'
                            : selectedInvoice.status === 'OVERDUE'
                            ? 'bg-rose-400 shadow-sm shadow-rose-400'
                            : 'bg-zinc-400'
                        }`}
                      />
                      {selectedInvoice.status}
                    </span>
                  </div>
                  <button
                    onClick={() => setSelectedInvoice(null)}
                    className="p-1 hover:bg-white/[0.06] rounded-md transition-all text-zinc-500 hover:text-zinc-200"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>

                {/* Quick actions for details */}
                <div className="grid grid-cols-2 gap-2">
                  <button
                    onClick={() => handlePreview(selectedInvoice.id)}
                    className="flex items-center justify-center gap-1.5 py-1.5 bg-white/[0.02] hover:bg-white/[0.05] border border-white/[0.08] text-zinc-200 font-medium rounded-lg text-xs transition-all"
                  >
                    <Eye className="w-3.5 h-3.5" />
                    Preview
                  </button>
                  <button
                    onClick={() => handleDownloadPdf(selectedInvoice.id)}
                    disabled={pdfLoading}
                    className="flex items-center justify-center gap-1.5 py-1.5 bg-white/[0.02] hover:bg-white/[0.05] border border-white/[0.08] text-zinc-200 font-medium rounded-lg text-xs transition-all disabled:opacity-50"
                  >
                    {pdfLoading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Download className="w-3.5 h-3.5" />}
                    Download PDF
                  </button>
                  {selectedInvoice.status === 'DRAFT' && (
                    <button
                      onClick={() => handleSend(selectedInvoice.id)}
                      className="flex items-center justify-center gap-1.5 py-1.5 bg-blue-500/10 border border-blue-500/25 hover:bg-blue-500/20 text-blue-300 font-medium rounded-lg text-xs transition-all"
                    >
                      <Send className="w-3.5 h-3.5" />
                      Send
                    </button>
                  )}
                  {(selectedInvoice.status === 'SENT' || selectedInvoice.status === 'OVERDUE') && (
                    <button
                      onClick={() => handleMarkPaid(selectedInvoice.id)}
                      className="flex items-center justify-center gap-1.5 py-1.5 bg-emerald-500/10 border border-emerald-500/25 hover:bg-emerald-500/20 text-emerald-300 font-medium rounded-lg text-xs transition-all"
                    >
                      <CheckCircle className="w-3.5 h-3.5" />
                      Mark Paid
                    </button>
                  )}
                  {selectedInvoice.status === 'DRAFT' && (
                    <button
                      onClick={() => navigate(`/invoices/${selectedInvoice.id}/edit`)}
                      className="flex items-center justify-center gap-1.5 py-1.5 bg-purple-500/10 border border-purple-500/25 hover:bg-purple-500/20 text-purple-300 font-medium rounded-lg text-xs transition-all"
                    >
                      <Edit className="w-3.5 h-3.5" />
                      Edit
                    </button>
                  )}
                  <button
                    onClick={() => handleDuplicate(selectedInvoice.id)}
                    className="flex items-center justify-center gap-1.5 py-1.5 bg-white/[0.02] hover:bg-white/[0.05] border border-white/[0.08] text-zinc-200 font-medium rounded-lg text-xs transition-all"
                  >
                    <Copy className="w-3.5 h-3.5" />
                    Duplicate
                  </button>
                  {(selectedInvoice.status === 'DRAFT' || selectedInvoice.status === 'SENT' || selectedInvoice.status === 'OVERDUE') && (
                    <button
                      onClick={() => handleCancel(selectedInvoice.id)}
                      className="flex items-center justify-center gap-1.5 py-1.5 bg-amber-500/10 border border-amber-500/25 hover:bg-amber-500/20 text-amber-300 font-medium rounded-lg text-xs transition-all"
                    >
                      <Ban className="w-3.5 h-3.5" />
                      Cancel
                    </button>
                  )}
                  {(selectedInvoice.status === 'DRAFT' || selectedInvoice.status === 'CANCELLED') && (
                    <button
                      onClick={() => handleDelete(selectedInvoice.id)}
                      className="flex items-center justify-center gap-1.5 py-1.5 bg-rose-500/10 border border-rose-500/25 hover:bg-rose-500/20 text-rose-300 font-medium rounded-lg text-xs transition-all"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                      Delete
                    </button>
                  )}
                </div>

                {/* Dates & Terms */}
                <div className="grid grid-cols-2 gap-3.5 border-t border-white/[0.08] pt-3 text-xs">
                  <div>
                    <span className="text-zinc-500 uppercase tracking-wider font-bold block text-[9px]">Issue Date</span>
                    <span className="text-zinc-200 font-mono-num font-medium mt-0.5 block">{selectedInvoice.issueDate}</span>
                  </div>
                  <div>
                    <span className="text-zinc-500 uppercase tracking-wider font-bold block text-[9px]">Due Date</span>
                    <span className="text-zinc-200 font-mono-num font-medium mt-0.5 block">{selectedInvoice.dueDate}</span>
                  </div>
                  <div>
                    <span className="text-zinc-500 uppercase tracking-wider font-bold block text-[9px]">Currency</span>
                    <span className="text-zinc-200 font-semibold mt-0.5 block uppercase font-mono-num">{selectedInvoice.currency}</span>
                  </div>
                  <div>
                    <span className="text-zinc-500 uppercase tracking-wider font-bold block text-[9px]">Payment Terms</span>
                    <span className="text-zinc-200 font-medium mt-0.5 block">{selectedInvoice.paymentTerms || '—'}</span>
                  </div>
                </div>

                {/* Line items details */}
                <div className="border-t border-white/[0.08] pt-3 space-y-2">
                  <span className="text-zinc-500 uppercase tracking-wider font-bold block text-[9px]">Line Items</span>
                  <div className="space-y-1.5 max-h-48 overflow-y-auto pr-1">
                    {selectedInvoice.items.map((item) => (
                      <div key={item.id} className="p-2.5 bg-white/[0.015] border border-white/[0.06] rounded-lg flex justify-between items-center text-xs">
                        <div className="min-w-0 pr-2">
                          <span className="font-semibold text-zinc-100 block truncate">{item.description}</span>
                          <span className="text-zinc-400 font-mono-num text-[11px] block mt-0.5">
                            {item.quantity} × {new Intl.NumberFormat('en-US', { style: 'currency', currency: selectedInvoice.currency }).format(item.unitPrice)}
                          </span>
                        </div>
                        <span className="font-bold font-mono-num text-white shrink-0">
                          {new Intl.NumberFormat('en-US', { style: 'currency', currency: selectedInvoice.currency }).format(item.total)}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Calculations details */}
                <div className="border-t border-white/[0.08] pt-3 space-y-2 text-xs font-mono-num">
                  <div className="flex justify-between text-zinc-400">
                    <span className="font-sans">Subtotal</span>
                    <span>{new Intl.NumberFormat('en-US', { style: 'currency', currency: selectedInvoice.currency }).format(selectedInvoice.subtotal)}</span>
                  </div>
                  {selectedInvoice.discountValue > 0 && (
                    <div className="flex justify-between text-emerald-400">
                      <span className="font-sans">Discount ({selectedInvoice.discountType})</span>
                      <span>
                        {selectedInvoice.discountType === 'PERCENT'
                          ? `-${selectedInvoice.discountValue}%`
                          : `-${new Intl.NumberFormat('en-US', { style: 'currency', currency: selectedInvoice.currency }).format(selectedInvoice.discountValue)}`}
                      </span>
                    </div>
                  )}
                  {selectedInvoice.taxRate > 0 && (
                    <div className="flex justify-between text-zinc-400">
                      <span className="font-sans">Tax ({selectedInvoice.taxRate}%)</span>
                      <span>
                        {new Intl.NumberFormat('en-US', { style: 'currency', currency: selectedInvoice.currency }).format((selectedInvoice.total - (selectedInvoice.subtotal - (selectedInvoice.discountType === 'PERCENT' ? selectedInvoice.subtotal * selectedInvoice.discountValue / 100 : selectedInvoice.discountValue))))}
                      </span>
                    </div>
                  )}
                  <div className="flex justify-between text-sm font-extrabold text-white border-t border-white/[0.08] pt-2.5">
                    <span className="font-sans text-xs font-bold text-zinc-200">Total Due</span>
                    <span className="text-indigo-400">{new Intl.NumberFormat('en-US', { style: 'currency', currency: selectedInvoice.currency }).format(selectedInvoice.total)}</span>
                  </div>
                </div>

                {/* Notes details */}
                {selectedInvoice.notes && (
                  <div className="border-t border-white/[0.08] pt-3 text-xs">
                    <span className="text-zinc-500 uppercase tracking-wider font-bold block text-[9px]">Notes & Terms</span>
                    <p className="text-zinc-300 mt-1 whitespace-pre-wrap leading-relaxed bg-white/[0.015] border border-white/[0.06] p-2.5 rounded-lg">{selectedInvoice.notes}</p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Preview Modal */}
        {previewOpen && (
          <div className="fixed inset-0 z-50 flex flex-col bg-black/80 backdrop-blur-sm">
            <div className="flex items-center justify-between px-6 py-4 bg-[#0e1224] border-b border-white/[0.06]">
              <div className="flex items-center gap-3">
                <FileText className="w-5 h-5 text-indigo-400" />
                <span className="font-semibold text-zinc-200">
                  Invoice Preview{selectedInvoice ? ` — ${selectedInvoice.invoiceNumber}` : ''}
                </span>
              </div>
              <div className="flex items-center gap-2">
                {selectedInvoice && (
                  <button
                    onClick={() => handleDownloadPdf(selectedInvoice.id)}
                    disabled={pdfLoading}
                    className="flex items-center gap-1.5 px-3 py-1.5 bg-indigo-500/10 border border-indigo-500/20 hover:bg-indigo-500/20 text-indigo-400 rounded-lg text-sm transition-all disabled:opacity-50"
                  >
                    {pdfLoading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Download className="w-3.5 h-3.5" />}
                    Download PDF
                  </button>
                )}
                <button
                  onClick={() => setPreviewOpen(false)}
                  className="p-1.5 hover:bg-white/[0.05] rounded-lg text-zinc-400 hover:text-zinc-200 transition-all"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>
            <div className="flex-1 overflow-hidden">
              {previewLoading ? (
                <div className="flex items-center justify-center h-full text-zinc-500">
                  <Loader2 className="w-6 h-6 animate-spin mr-3" />
                  Generating preview…
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

        {/* Invoice Builder Form Modal */}
        {isFormOpen && (          <div className="fixed inset-0 bg-[#070a13]/90 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <div className="w-full max-w-3xl bg-[#0c101f] border border-white/[0.05] rounded-2xl shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200 flex flex-col max-h-[90vh]">
              {/* Header */}
              <div className="px-6 py-4 border-b border-white/[0.05] flex items-center justify-between">
                <h2 className="text-lg font-bold text-zinc-200">
                  {editingInvoice ? `Edit Invoice ${editingInvoice.invoiceNumber}` : 'Create New Invoice'}
                </h2>
                <button
                  onClick={() => setIsFormOpen(false)}
                  className="p-1.5 hover:bg-white/[0.05] rounded-lg text-zinc-500 hover:text-zinc-200 transition-all"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Scrollable Form Body */}
              <form onSubmit={handleFormSubmit} className="flex-1 overflow-y-auto p-6 space-y-6">
                {formError && (
                  <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 shrink-0" />
                    <span>{formError}</span>
                  </div>
                )}

                {/* Block 1: Client & Dates */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">Bill To *</label>
                    <select
                      value={formClientId}
                      onChange={(e) => handleClientChange(e.target.value)}
                      className="w-full bg-white/[0.02] border border-white/[0.05] rounded-xl px-4 py-2.5 text-sm text-zinc-200 focus:outline-none focus:border-indigo-500/50 transition-all"
                    >
                      {clients.length === 0 && <option value="">No clients registered</option>}
                      {clients.map(c => (
                        <option key={c.id} value={c.id}>{c.name} ({c.company || 'Private'})</option>
                      ))}
                    </select>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">Issue Date *</label>
                    <input
                      type="date"
                      required
                      value={formIssueDate}
                      onChange={(e) => setFormIssueDate(e.target.value)}
                      className="w-full bg-white/[0.02] border border-white/[0.05] rounded-xl px-4 py-2.5 text-sm text-zinc-200 focus:outline-none focus:border-indigo-500/50 transition-all"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">Due Date *</label>
                    <input
                      type="date"
                      required
                      value={formDueDate}
                      onChange={(e) => setFormDueDate(e.target.value)}
                      className="w-full bg-white/[0.02] border border-white/[0.05] rounded-xl px-4 py-2.5 text-sm text-zinc-200 focus:outline-none focus:border-indigo-500/50 transition-all"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">Currency</label>
                    <input
                      type="text"
                      readOnly
                      value={`${formCurrency} (Inherited from client profile)`}
                      className="w-full bg-white/[0.01] border border-white/[0.03] rounded-xl px-4 py-2.5 text-sm text-zinc-500 cursor-not-allowed focus:outline-none"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">Payment Terms</label>
                    <select
                      value={formPaymentTerms}
                      onChange={(e) => setFormPaymentTerms(e.target.value)}
                      className="w-full bg-white/[0.02] border border-white/[0.05] rounded-xl px-4 py-2.5 text-sm text-zinc-200 focus:outline-none focus:border-indigo-500/50 transition-all"
                    >
                      <option value="Net 30">Net 30</option>
                      <option value="Net 15">Net 15</option>
                      <option value="Due on Receipt">Due on Receipt</option>
                      <option value="Net 7">Net 7</option>
                    </select>
                  </div>
                </div>

                {/* Block 2: Line Items builder */}
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">Invoice Items</h3>
                    <button
                      type="button"
                      onClick={handleAddItem}
                      className="flex items-center gap-1 text-xs text-indigo-400 hover:text-indigo-300 font-medium py-1 px-2.5 rounded-lg bg-indigo-500/10 hover:bg-indigo-500/15 border border-indigo-500/20 transition-all"
                    >
                      <Plus className="w-3.5 h-3.5" />
                      Add Item
                    </button>
                  </div>

                  <div className="space-y-2">
                    {formItems.map((item, index) => (
                      <div key={index} className="grid grid-cols-12 gap-3 items-center bg-white/[0.01] border border-white/[0.03] p-3 rounded-xl">
                        <div className="col-span-12 sm:col-span-6 space-y-1">
                          <input
                            type="text"
                            required
                            placeholder="Line item description..."
                            value={item.description}
                            onChange={(e) => handleItemChange(index, 'description', e.target.value)}
                            className="w-full bg-white/[0.02] border border-white/[0.05] rounded-lg px-3 py-1.5 text-xs text-zinc-200 focus:outline-none focus:border-indigo-500/50"
                          />
                        </div>

                        <div className="col-span-4 sm:col-span-2 space-y-1">
                          <input
                            type="number"
                            required
                            min="0.01"
                            step="any"
                            placeholder="Qty"
                            value={item.quantity}
                            onChange={(e) => handleItemChange(index, 'quantity', parseFloat(e.target.value) || 0)}
                            className="w-full bg-white/[0.02] border border-white/[0.05] rounded-lg px-3 py-1.5 text-xs text-zinc-200 focus:outline-none focus:border-indigo-500/50"
                          />
                        </div>

                        <div className="col-span-4 sm:col-span-2 space-y-1">
                          <input
                            type="number"
                            required
                            min="0"
                            step="any"
                            placeholder="Price"
                            value={item.unitPrice}
                            onChange={(e) => handleItemChange(index, 'unitPrice', parseFloat(e.target.value) || 0)}
                            className="w-full bg-white/[0.02] border border-white/[0.05] rounded-lg px-3 py-1.5 text-xs text-zinc-200 focus:outline-none focus:border-indigo-500/50"
                          />
                        </div>

                        <div className="col-span-3 sm:col-span-1 text-center text-xs font-semibold text-zinc-400">
                          {(item.quantity * item.unitPrice).toFixed(2)}
                        </div>

                        <div className="col-span-1 text-right">
                          <button
                            type="button"
                            disabled={formItems.length === 1}
                            onClick={() => handleRemoveItem(index)}
                            className="p-1 text-zinc-500 hover:text-red-400 disabled:opacity-30 disabled:hover:text-zinc-500 transition-all"
                          >
                            <X className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Block 3: Discount & Tax + Calculations */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4 border-t border-white/[0.05]">
                  <div className="space-y-4">
                    <div className="grid grid-cols-2 gap-3">
                      <div className="space-y-1.5">
                        <label className="text-[10px] font-semibold text-zinc-500 uppercase tracking-wider">Discount Type</label>
                        <select
                          value={formDiscountType}
                          onChange={(e) => {
                            setFormDiscountType(e.target.value as any);
                            setFormDiscountValue(0);
                          }}
                          className="w-full bg-white/[0.02] border border-white/[0.05] rounded-xl px-3 py-2 text-xs text-zinc-200 focus:outline-none"
                        >
                          <option value="">None</option>
                          <option value="PERCENT">PERCENT (%)</option>
                          <option value="FIXED">FIXED amount</option>
                        </select>
                      </div>

                      <div className="space-y-1.5">
                        <label className="text-[10px] font-semibold text-zinc-500 uppercase tracking-wider">Discount Value</label>
                        <input
                          type="number"
                          min="0"
                          disabled={!formDiscountType}
                          value={formDiscountValue}
                          onChange={(e) => setFormDiscountValue(parseFloat(e.target.value) || 0)}
                          className="w-full bg-white/[0.02] border border-white/[0.05] rounded-xl px-3 py-2 text-xs text-zinc-200 focus:outline-none disabled:opacity-40 disabled:cursor-not-allowed"
                        />
                      </div>
                    </div>

                    <div className="space-y-1.5">
                      <label className="text-[10px] font-semibold text-zinc-500 uppercase tracking-wider">Tax Rate (%)</label>
                      <input
                        type="number"
                        min="0"
                        max="100"
                        value={formTaxRate}
                        onChange={(e) => setFormTaxRate(parseFloat(e.target.value) || 0)}
                        className="w-full bg-white/[0.02] border border-white/[0.05] rounded-xl px-3 py-2 text-xs text-zinc-200 focus:outline-none"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <label className="text-[10px] font-semibold text-zinc-500 uppercase tracking-wider">Invoice Notes</label>
                      <textarea
                        rows={2}
                        value={formNotes}
                        onChange={(e) => setFormNotes(e.target.value)}
                        placeholder="Provide payment guidelines, wiring details, or custom notes..."
                        className="w-full bg-white/[0.02] border border-white/[0.05] rounded-xl px-3 py-2 text-xs text-zinc-200 focus:outline-none resize-none"
                      />
                    </div>
                  </div>

                  {/* Calculations panel */}
                  <div className="bg-white/[0.01] border border-white/[0.03] p-4 rounded-xl flex flex-col justify-center space-y-3 text-xs">
                    <div className="flex justify-between text-zinc-400">
                      <span>Subtotal</span>
                      <span className="font-semibold">{calculateFormSubtotal().toFixed(2)} {formCurrency}</span>
                    </div>

                    {formDiscountValue > 0 && formDiscountType && (
                      <div className="flex justify-between text-zinc-400">
                        <span>Discount ({formDiscountType})</span>
                        <span className="font-semibold text-rose-400">
                          {formDiscountType === 'PERCENT'
                            ? `-${formDiscountValue}% (-${(calculateFormSubtotal() * formDiscountValue / 100).toFixed(2)} ${formCurrency})`
                            : `-${formDiscountValue.toFixed(2)} ${formCurrency}`}
                        </span>
                      </div>
                    )}

                    {formTaxRate > 0 && (
                      <div className="flex justify-between text-zinc-400">
                        <span>Tax Rate ({formTaxRate}%)</span>
                        <span className="font-semibold text-indigo-400">
                          +{((Math.max(0, calculateFormSubtotal() - (formDiscountType === 'PERCENT' ? calculateFormSubtotal() * formDiscountValue / 100 : formDiscountValue))) * formTaxRate / 100).toFixed(2)} {formCurrency}
                        </span>
                      </div>
                    )}

                    <div className="flex justify-between text-base font-extrabold text-zinc-200 border-t border-white/[0.05] pt-3">
                      <span>Total Amount</span>
                      <span className="text-xl bg-gradient-to-r from-emerald-400 to-teal-400 bg-clip-text text-transparent">
                        {calculateFormTotal().toFixed(2)} {formCurrency}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Actions */}
                <div className="pt-4 border-t border-white/[0.05] flex gap-3">
                  <button
                    type="button"
                    onClick={() => setIsFormOpen(false)}
                    className="flex-1 py-2.5 bg-white/[0.02] border border-white/[0.05] hover:bg-white/[0.05] text-zinc-300 font-medium rounded-xl text-sm transition-all"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={formSubmitting}
                    className="flex-1 py-2.5 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium rounded-xl text-sm shadow-lg shadow-indigo-500/10 active:scale-[0.98] disabled:opacity-55 transition-all"
                  >
                    {formSubmitting ? 'Saving...' : editingInvoice ? 'Save Changes' : 'Create Invoice'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
};

export default Invoices;
