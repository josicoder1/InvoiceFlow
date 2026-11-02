import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../components/DashboardLayout';
import { clientsApi } from '../api/clients';
import { AlertCircle, ArrowLeft, Loader2, UserPlus } from 'lucide-react';

const ClientNew: React.FC = () => {
  const navigate = useNavigate();

  const [formName, setFormName] = useState('');
  const [formEmail, setFormEmail] = useState('');
  const [formCompany, setFormCompany] = useState('');
  const [formAddress, setFormAddress] = useState('');
  const [formCurrency, setFormCurrency] = useState('USD');
  const [formError, setFormError] = useState<string | null>(null);
  const [formSubmitting, setFormSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setFormSubmitting(true);
    try {
      await clientsApi.addClient({
        name: formName,
        email: formEmail,
        company: formCompany || undefined,
        address: formAddress || undefined,
        currency: formCurrency,
      });
      navigate('/clients');
    } catch (err: any) {
      setFormError(err.response?.data?.detail || 'An error occurred during submission.');
    } finally {
      setFormSubmitting(false);
    }
  };

  return (
    <DashboardLayout>
      <div className="max-w-xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/clients')}
            className="p-2 sharp-btn-secondary text-zinc-400 hover:text-white"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div>
            <h1 className="text-xl font-bold tracking-tight text-white">
              New Client
            </h1>
            <p className="text-zinc-400 text-xs">Register a new client profile for invoicing & proposals.</p>
          </div>
        </div>

        {/* Form Card */}
        <div className="sharp-card p-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            {formError && (
              <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-lg flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{formError}</span>
              </div>
            )}

            <div className="space-y-1.5">
              <label className="sharp-label">Client Name *</label>
              <input
                type="text"
                required
                value={formName}
                onChange={(e) => setFormName(e.target.value)}
                placeholder="e.g. Acme Corporation, Sarah Jenkins"
                className="w-full sharp-input px-3.5 py-2 text-zinc-100 placeholder-zinc-500"
              />
            </div>

            <div className="space-y-1.5">
              <label className="sharp-label">Email Address *</label>
              <input
                type="email"
                required
                value={formEmail}
                onChange={(e) => setFormEmail(e.target.value)}
                placeholder="billing@company.com"
                className="w-full sharp-input px-3.5 py-2 text-zinc-100 placeholder-zinc-500"
              />
            </div>

            <div className="space-y-1.5">
              <label className="sharp-label">Company Name</label>
              <input
                type="text"
                value={formCompany}
                onChange={(e) => setFormCompany(e.target.value)}
                placeholder="Acme Global Inc."
                className="w-full sharp-input px-3.5 py-2 text-zinc-100 placeholder-zinc-500"
              />
            </div>

            <div className="space-y-1.5">
              <label className="sharp-label">Billing Address</label>
              <textarea
                value={formAddress}
                onChange={(e) => setFormAddress(e.target.value)}
                placeholder="Street Address, City, State/Province, Postal Code, Country"
                rows={2}
                className="w-full sharp-input px-3.5 py-2 text-zinc-100 placeholder-zinc-500 resize-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="sharp-label">Billing Currency</label>
              <select
                value={formCurrency}
                onChange={(e) => setFormCurrency(e.target.value)}
                className="w-full sharp-input px-3.5 py-2 text-zinc-200"
              >
                <option value="USD" className="bg-[#0c101d]">USD ($) — US Dollar</option>
                <option value="EUR" className="bg-[#0c101d]">EUR (€) — Euro</option>
                <option value="GBP" className="bg-[#0c101d]">GBP (£) — British Pound</option>
                <option value="AUD" className="bg-[#0c101d]">AUD (A$) — Australian Dollar</option>
                <option value="CAD" className="bg-[#0c101d]">CAD (C$) — Canadian Dollar</option>
              </select>
            </div>

            <div className="pt-4 flex gap-3">
              <button
                type="button"
                onClick={() => navigate('/clients')}
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
                    <UserPlus className="w-3.5 h-3.5" />
                    <span>Create Client</span>
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default ClientNew;
