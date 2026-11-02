import React, { useState, useEffect } from 'react';
import DashboardLayout from '../components/DashboardLayout';
import { clientsApi } from '../api/clients';
import type { Client, PageResponse, ClientHistory } from '../types';
import { Plus, Search, Trash2, Edit, Eye, X, Mail, MapPin, DollarSign, AlertCircle } from 'lucide-react';

const Clients: React.FC = () => {
  const [clientsPage, setClientsPage] = useState<PageResponse<Client> | null>(null);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Selected Client for Details/History
  const [selectedClientId, setSelectedClientId] = useState<string | null>(null);
  const [clientHistory, setClientHistory] = useState<ClientHistory | null>(null);
  const [historyLoading, setHistoryLoading] = useState(false);

  // Add/Edit Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingClient, setEditingClient] = useState<Client | null>(null);
  const [formName, setFormName] = useState('');
  const [formEmail, setFormEmail] = useState('');
  const [formCompany, setFormCompany] = useState('');
  const [formAddress, setFormAddress] = useState('');
  const [formCurrency, setFormCurrency] = useState('USD');
  const [formError, setFormError] = useState<string | null>(null);
  const [formSubmitting, setFormSubmitting] = useState(false);

  // Fetch Clients list
  const fetchClients = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await clientsApi.getClients(search, page, 8);
      setClientsPage(data);
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to load clients');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClients();
  }, [search, page]);

  // Fetch Client History when selected
  useEffect(() => {
    if (!selectedClientId) {
      setClientHistory(null);
      return;
    }
    const fetchHistory = async () => {
      setHistoryLoading(true);
      try {
        const historyData = await clientsApi.getClientHistory(selectedClientId);
        setClientHistory(historyData);
      } catch (err: any) {
        console.error('Failed to load history', err);
      } finally {
        setHistoryLoading(false);
      }
    };
    fetchHistory();
  }, [selectedClientId]);

  const handleOpenAddModal = () => {
    setEditingClient(null);
    setFormName('');
    setFormEmail('');
    setFormCompany('');
    setFormAddress('');
    setFormCurrency('USD');
    setFormError(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (client: Client) => {
    setEditingClient(client);
    setFormName(client.name);
    setFormEmail(client.email);
    setFormCompany(client.company || '');
    setFormAddress(client.address || '');
    setFormCurrency(client.currency);
    setFormError(null);
    setIsModalOpen(true);
  };

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setFormSubmitting(true);

    const payload = {
      name: formName,
      email: formEmail,
      company: formCompany || undefined,
      address: formAddress || undefined,
      currency: formCurrency,
    };

    try {
      if (editingClient) {
        await clientsApi.editClient(editingClient.id, payload);
      } else {
        await clientsApi.addClient(payload);
      }
      setIsModalOpen(false);
      fetchClients();
      if (selectedClientId && editingClient && selectedClientId === editingClient.id) {
        // Refresh details
        const historyData = await clientsApi.getClientHistory(selectedClientId);
        setClientHistory(historyData);
      }
    } catch (err: any) {
      setFormError(err.response?.data?.detail || 'An error occurred during submission.');
    } finally {
      setFormSubmitting(false);
    }
  };

  const handleDeleteClient = async (id: string) => {
    if (!confirm('Are you sure you want to delete this client?')) return;
    try {
      await clientsApi.deleteClient(id);
      fetchClients();
      if (selectedClientId === id) {
        setSelectedClientId(null);
      }
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Cannot delete client. Ensure they do not have active invoices.');
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              Client Directory
            </h1>
            <p className="text-zinc-400 text-xs sm:text-sm mt-0.5">Manage your client relationships, billing addresses, and payment histories.</p>
          </div>
          <button
            onClick={handleOpenAddModal}
            className="flex items-center justify-center gap-2 px-4 py-2.5 bg-gradient-to-r from-indigo-500 via-indigo-600 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-semibold text-xs rounded-xl shadow-lg shadow-indigo-500/20 active:scale-[0.98] transition-all"
          >
            <Plus className="w-4 h-4" />
            Add Client
          </button>
        </div>

        {/* Content Split Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
          {/* List panel */}
          <div className="lg:col-span-2 bg-[#0a0e1c] border border-white/[0.08] rounded-2xl overflow-hidden flex flex-col shadow-sm">
            {/* Search Bar */}
            <div className="p-4 border-b border-white/[0.06] flex items-center gap-3">
              <div className="relative flex-1">
                <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
                <input
                  type="text"
                  placeholder="Search clients by name, email, or company..."
                  value={search}
                  onChange={(e) => {
                    setSearch(e.target.value);
                    setPage(0);
                  }}
                  className="w-full bg-white/[0.02] hover:bg-white/[0.03] border border-white/[0.08] rounded-xl pl-10 pr-4 py-2 text-xs text-zinc-200 placeholder-zinc-500 focus:outline-none focus:border-indigo-500/60 focus:bg-white/[0.04] transition-all"
                />
              </div>
            </div>

            {/* Table */}
            {loading && !clientsPage ? (
              <div className="p-12 text-center text-zinc-500 text-xs">Loading clients...</div>
            ) : error ? (
              <div className="p-12 text-center text-rose-400 text-xs flex flex-col items-center gap-2">
                <AlertCircle className="w-7 h-7" />
                <span>{error}</span>
              </div>
            ) : clientsPage?.content.length === 0 ? (
              <div className="p-12 text-center text-zinc-500 text-xs">
                No clients found. Click "Add Client" to register one.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-white/[0.06] bg-white/[0.015] text-[11px] font-bold text-zinc-400 uppercase tracking-wider">
                      <th className="p-4">Name</th>
                      <th className="p-4">Company</th>
                      <th className="p-4">Email</th>
                      <th className="p-4">Currency</th>
                      <th className="p-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/[0.04]">
                    {clientsPage?.content.map((client) => {
                      const isSelected = selectedClientId === client.id;
                      return (
                        <tr
                          key={client.id}
                          onClick={() => setSelectedClientId(client.id)}
                          className={`hover:bg-white/[0.02] cursor-pointer transition-all ${
                            isSelected ? 'bg-indigo-500/10 border-l-2 border-indigo-400' : 'border-l-2 border-transparent'
                          }`}
                        >
                          <td className="p-4 font-semibold text-zinc-100 text-xs">{client.name}</td>
                          <td className="p-4 text-xs text-zinc-300">{client.company || '—'}</td>
                          <td className="p-4 text-xs text-zinc-400 font-mono-num">{client.email}</td>
                          <td className="p-4 text-xs">
                            <span className="text-[10px] px-2 py-0.5 bg-white/[0.03] border border-white/[0.08] rounded-md text-zinc-300 font-mono-num font-bold uppercase">
                              {client.currency}
                            </span>
                          </td>
                          <td className="p-4 text-right" onClick={(e) => e.stopPropagation()}>
                            <div className="flex items-center justify-end gap-1">
                              <button
                                onClick={() => setSelectedClientId(client.id)}
                                className="p-1.5 text-zinc-400 hover:text-indigo-400 hover:bg-white/[0.05] rounded-lg transition-all"
                                title="View details"
                              >
                                <Eye className="w-3.5 h-3.5" />
                              </button>
                              <button
                                onClick={() => handleOpenEditModal(client)}
                                className="p-1.5 text-zinc-400 hover:text-purple-400 hover:bg-white/[0.05] rounded-lg transition-all"
                                title="Edit Client"
                              >
                                <Edit className="w-3.5 h-3.5" />
                              </button>
                              <button
                                onClick={() => handleDeleteClient(client.id)}
                                className="p-1.5 text-zinc-400 hover:text-rose-400 hover:bg-white/[0.05] rounded-lg transition-all"
                                title="Delete Client"
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </button>
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
            {clientsPage && clientsPage.totalPages > 1 && (
              <div className="p-4 border-t border-white/[0.06] flex items-center justify-between gap-4">
                <span className="text-xs text-zinc-400 font-mono-num">
                  Page {clientsPage.number + 1} of {clientsPage.totalPages}
                </span>
                <div className="flex gap-2">
                  <button
                    disabled={clientsPage.number === 0}
                    onClick={() => setPage((p) => p - 1)}
                    className="px-3 py-1.5 bg-white/[0.02] border border-white/[0.08] rounded-xl text-xs font-semibold hover:bg-white/[0.05] active:scale-[0.98] disabled:opacity-40 disabled:pointer-events-none transition-all"
                  >
                    Previous
                  </button>
                  <button
                    disabled={clientsPage.number === clientsPage.totalPages - 1}
                    onClick={() => setPage((p) => p + 1)}
                    className="px-3 py-1.5 bg-white/[0.02] border border-white/[0.08] rounded-xl text-xs font-semibold hover:bg-white/[0.05] active:scale-[0.98] disabled:opacity-40 disabled:pointer-events-none transition-all"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Details / History Panel */}
          <div className="bg-[#0a0e1c] border border-white/[0.08] rounded-2xl p-6 space-y-6 shadow-sm">
            {!selectedClientId ? (
              <div className="text-center py-12 text-zinc-500 flex flex-col items-center gap-3">
                <div className="w-12 h-12 rounded-2xl bg-white/[0.02] border border-white/[0.08] flex items-center justify-center">
                  <Eye className="w-5 h-5 text-zinc-400" />
                </div>
                <div>
                  <h3 className="font-bold text-zinc-200 text-sm">No client selected</h3>
                  <p className="text-xs text-zinc-500 mt-1 max-w-xs leading-relaxed">Select a client from the list to view their detailed billing profile, total invoiced, and historic documents.</p>
                </div>
              </div>
            ) : historyLoading ? (
              <div className="text-center py-12 text-zinc-500 text-xs">Loading client history...</div>
            ) : clientHistory ? (
              <div className="space-y-6">
                {/* Profile header */}
                <div className="flex items-start justify-between pb-3 border-b border-white/[0.06]">
                  <div>
                    <h3 className="text-lg font-bold text-white">{clientHistory.client.name}</h3>
                    {clientHistory.client.company && (
                      <p className="text-xs text-zinc-400 mt-0.5">{clientHistory.client.company}</p>
                    )}
                  </div>
                  <button
                    onClick={() => setSelectedClientId(null)}
                    className="p-1.5 hover:bg-white/[0.06] rounded-lg transition-all text-zinc-500 hover:text-zinc-200"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>

                {/* Info List */}
                <div className="space-y-3 border-b border-white/[0.06] pb-4 text-xs">
                  <div className="flex items-center gap-3 text-zinc-300">
                    <Mail className="w-4 h-4 shrink-0 text-indigo-400" />
                    <span className="truncate font-mono-num">{clientHistory.client.email}</span>
                  </div>
                  {clientHistory.client.address && (
                    <div className="flex items-start gap-3 text-zinc-300">
                      <MapPin className="w-4 h-4 shrink-0 text-purple-400 mt-0.5" />
                      <span>{clientHistory.client.address}</span>
                    </div>
                  )}
                  <div className="flex items-center gap-3 text-zinc-300">
                    <DollarSign className="w-4 h-4 shrink-0 text-emerald-400" />
                    <span>Currency: <strong className="font-mono-num">{clientHistory.client.currency}</strong></span>
                  </div>
                </div>

                {/* Billed stats card */}
                <div className="bg-gradient-to-r from-indigo-500/10 via-purple-500/10 to-indigo-500/5 border border-indigo-500/25 p-4 rounded-xl flex items-center justify-between">
                  <div>
                    <span className="text-[10px] text-zinc-400 font-bold uppercase tracking-wider block">Total Billed</span>
                    <span className="text-xl font-extrabold text-white mt-1 block font-mono-num">
                      {new Intl.NumberFormat('en-US', { style: 'currency', currency: clientHistory.client.currency }).format(clientHistory.totalBilled)}
                    </span>
                  </div>
                  <div className="text-right">
                    <span className="text-[10px] text-zinc-400 font-bold uppercase tracking-wider block">Invoices</span>
                    <span className="text-xl font-extrabold text-white mt-1 block font-mono-num">{clientHistory.invoices.length}</span>
                  </div>
                </div>

                {/* Invoice list */}
                <div className="space-y-3">
                  <h4 className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">Invoices History</h4>
                  {clientHistory.invoices.length === 0 ? (
                    <p className="text-xs text-zinc-500 py-4 text-center">No invoices issued for this client yet.</p>
                  ) : (
                    <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
                      {clientHistory.invoices.map((inv) => (
                        <div
                          key={inv.id}
                          className="p-3 bg-white/[0.02] border border-white/[0.06] hover:border-white/[0.12] rounded-xl flex items-center justify-between transition-all text-xs"
                        >
                          <div>
                            <span className="font-bold text-zinc-200 block font-mono-num">{inv.invoiceNumber}</span>
                            <span className="text-zinc-500 text-[10px] block mt-0.5 font-mono-num">{inv.issueDate}</span>
                          </div>
                          <div className="text-right space-y-1">
                            <span className="font-bold text-white block font-mono-num">
                              {new Intl.NumberFormat('en-US', { style: 'currency', currency: inv.currency }).format(inv.total)}
                            </span>
                            <span
                              className={`inline-block px-2 py-0.5 rounded-full text-[9px] font-bold ${
                                inv.status === 'PAID'
                                  ? 'bg-emerald-500/10 border border-emerald-500/25 text-emerald-400'
                                  : inv.status === 'SENT'
                                  ? 'bg-blue-500/10 border border-blue-500/25 text-blue-400'
                                  : inv.status === 'OVERDUE'
                                  ? 'bg-rose-500/10 border border-rose-500/25 text-rose-400'
                                  : 'bg-zinc-500/10 border border-zinc-500/25 text-zinc-400'
                              }`}
                            >
                              {inv.status}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ) : null}
          </div>
        </div>

        {/* Add/Edit Modal */}
        {isModalOpen && (
          <div className="fixed inset-0 bg-[#070a13]/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <div className="w-full max-w-md bg-[#0c101f] border border-white/[0.05] rounded-2xl shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
              {/* Modal header */}
              <div className="px-6 py-4 border-b border-white/[0.05] flex items-center justify-between">
                <h2 className="text-lg font-bold text-zinc-200">
                  {editingClient ? 'Edit Client Details' : 'Register New Client'}
                </h2>
                <button
                  onClick={() => setIsModalOpen(false)}
                  className="p-1.5 hover:bg-white/[0.05] rounded-lg text-zinc-500 hover:text-zinc-200 transition-all"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Form */}
              <form onSubmit={handleFormSubmit} className="p-6 space-y-4">
                {formError && (
                  <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 shrink-0" />
                    <span>{formError}</span>
                  </div>
                )}

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">Client Name *</label>
                  <input
                    type="text"
                    required
                    value={formName}
                    onChange={(e) => setFormName(e.target.value)}
                    placeholder="e.g. John Doe, Acme Corp"
                    className="w-full bg-white/[0.02] border border-white/[0.05] rounded-xl px-4 py-2.5 text-sm text-zinc-200 placeholder-zinc-600 focus:outline-none focus:border-indigo-500/50 transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">Email Address *</label>
                  <input
                    type="email"
                    required
                    value={formEmail}
                    onChange={(e) => setFormEmail(e.target.value)}
                    placeholder="client@company.com"
                    className="w-full bg-white/[0.02] border border-white/[0.05] rounded-xl px-4 py-2.5 text-sm text-zinc-200 placeholder-zinc-600 focus:outline-none focus:border-indigo-500/50 transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">Company Name</label>
                  <input
                    type="text"
                    value={formCompany}
                    onChange={(e) => setFormCompany(e.target.value)}
                    placeholder="Acme International Ltd"
                    className="w-full bg-white/[0.02] border border-white/[0.05] rounded-xl px-4 py-2.5 text-sm text-zinc-200 placeholder-zinc-600 focus:outline-none focus:border-indigo-500/50 transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">Billing Address</label>
                  <textarea
                    value={formAddress}
                    onChange={(e) => setFormAddress(e.target.value)}
                    placeholder="Street, City, Zip, Country"
                    rows={2}
                    className="w-full bg-white/[0.02] border border-white/[0.05] rounded-xl px-4 py-2.5 text-sm text-zinc-200 placeholder-zinc-600 focus:outline-none focus:border-indigo-500/50 transition-all resize-none"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">Default Billing Currency</label>
                  <select
                    value={formCurrency}
                    onChange={(e) => setFormCurrency(e.target.value)}
                    className="w-full bg-white/[0.02] border border-white/[0.05] rounded-xl px-4 py-2.5 text-sm text-zinc-200 focus:outline-none focus:border-indigo-500/50 transition-all"
                  >
                    <option value="USD">USD ($)</option>
                    <option value="EUR">EUR (€)</option>
                    <option value="GBP">GBP (£)</option>
                    <option value="AUD">AUD (A$)</option>
                    <option value="CAD">CAD (C$)</option>
                  </select>
                </div>

                {/* Actions */}
                <div className="pt-4 flex gap-3">
                  <button
                    type="button"
                    onClick={() => setIsModalOpen(false)}
                    className="flex-1 py-2.5 bg-white/[0.02] border border-white/[0.05] hover:bg-white/[0.05] text-zinc-300 font-medium rounded-xl text-sm transition-all"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={formSubmitting}
                    className="flex-1 py-2.5 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium rounded-xl text-sm shadow-lg shadow-indigo-500/10 active:scale-[0.98] disabled:opacity-55 transition-all"
                  >
                    {formSubmitting ? 'Submitting...' : editingClient ? 'Save Changes' : 'Create Client'}
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

export default Clients;
