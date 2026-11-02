import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import DashboardLayout from '../components/DashboardLayout';
import { proposalsApi } from '../api/proposals';
import { clientsApi } from '../api/clients';
import type { Client, ProposalTemplate, ProposalItemRequest } from '../types';
import { ArrowLeft, AlertCircle, Sparkles, Eye, Loader2, Save } from 'lucide-react';
import ProposalBuilderSidebar, { ProposalSectionFields, ProposalPricingFields } from '../components/proposal/ProposalBuilderParts';
import ProposalPreview from '../components/proposal/ProposalPreview';
import ProposalTemplatePicker from '../components/proposal/ProposalTemplatePicker';
import { inferThemeFromTitle, resolveTemplateTheme } from '../components/proposal/proposalTemplateThemes';

const ProposalEdit: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [clients, setClients] = useState<Client[]>([]);
  const [templates, setTemplates] = useState<ProposalTemplate[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const [clientId, setClientId] = useState('');
  const [title, setTitle] = useState('');
  const [overview, setOverview] = useState('');
  const [scope, setScope] = useState('');
  const [timeline, setTimeline] = useState('');
  const [pricingNotes, setPricingNotes] = useState('');
  const [validUntil, setValidUntil] = useState('');
  const [items, setItems] = useState<ProposalItemRequest[]>([]);
  const [discountType, setDiscountType] = useState<'PERCENT' | 'FIXED' | ''>('');
  const [discountValue, setDiscountValue] = useState(0);
  const [taxRate, setTaxRate] = useState(0);
  const [version, setVersion] = useState(0);

  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [templatePickerOpen, setTemplatePickerOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<'build' | 'preview'>('build');
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});
  const [selectedTemplateId, setSelectedTemplateId] = useState<string | null>(null);

  useEffect(() => {
    proposalsApi.getTemplates().then(setTemplates);
  }, []);

  useEffect(() => {
    if (!id) return;
    const load = async () => {
      try {
        const [proposal, clientsPage] = await Promise.all([
          proposalsApi.getProposal(id),
          clientsApi.getClients(undefined, 0, 1000),
        ]);
        if (proposal.status !== 'DRAFT') { setLoadError('Only DRAFT proposals can be edited.'); return; }
        setClients(clientsPage.content);
        setClientId(proposal.clientId);
        setTitle(proposal.title);
        setOverview(proposal.overview || '');
        setScope(proposal.scope || '');
        setTimeline(proposal.timeline || '');
        setPricingNotes(proposal.pricingNotes || '');
        setValidUntil(proposal.validUntil || '');
        setDiscountType(proposal.discountType || '');
        setDiscountValue(proposal.discountValue ?? 0);
        setTaxRate(proposal.taxRate ?? 0);
        setVersion(proposal.version);
        setItems(proposal.items.map(i => ({
          description: i.description,
          quantity: i.quantity ?? undefined,
          unitPrice: i.unitPrice ?? undefined,
          position: i.position,
        })));
        if (proposal.items.length === 0) setItems([{ description: '', quantity: 1, unitPrice: 0, position: 1 }]);
      } catch (err: unknown) {
        const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
        setLoadError(detail || 'Failed to load proposal');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  useEffect(() => {
    if (!title || templates.length === 0 || selectedTemplateId) return;
    const matched = templates.find(t => {
      const tn = t.name.toLowerCase();
      const tl = title.toLowerCase();
      return tl.includes(tn) || tn.includes(tl);
    });
    if (matched) setSelectedTemplateId(matched.id);
  }, [title, templates, selectedTemplateId]);

  const applyTemplate = (t: ProposalTemplate) => {
    setOverview(t.overview || overview);
    setScope(t.scope || scope);
    setTimeline(t.timeline || timeline);
    setPricingNotes(t.pricingNotes || pricingNotes);
    setSelectedTemplateId(t.id);
    setTemplatePickerOpen(false);
  };

  const toggle = (key: string) => setCollapsed(prev => ({ ...prev, [key]: !prev[key] }));

  const addItem = () => setItems([...items, { description: '', quantity: 1, unitPrice: 0, position: items.length + 1 }]);
  const removeItem = (i: number) => {
    if (items.length > 1) setItems(items.filter((_, idx) => idx !== i).map((it, idx) => ({ ...it, position: idx + 1 })));
  };
  const updateItem = (i: number, field: keyof ProposalItemRequest, value: unknown) => {
    const n = [...items];
    n[i] = { ...n[i], [field]: value };
    setItems(n);
  };

  const selectedTemplate = templates.find(t => t.id === selectedTemplateId) ?? null;
  const selectedTemplateTheme = selectedTemplate
    ? resolveTemplateTheme(selectedTemplate, templates.findIndex(t => t.id === selectedTemplate.id))
    : inferThemeFromTitle(title, templates);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    try {
      await proposalsApi.editProposal(id!, {
        clientId, title, overview: overview || undefined, scope: scope || undefined,
        timeline: timeline || undefined, pricingNotes: pricingNotes || undefined,
        validUntil: validUntil || undefined, items: items.filter(i => i.description.trim()),
        discountType: discountType || undefined, discountValue: discountValue || undefined,
        taxRate: taxRate || undefined, version,
      });
      navigate('/proposals');
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setFormError(detail || 'Failed to save');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center py-32 text-zinc-500 text-xs">
          <Loader2 className="w-5 h-5 animate-spin mr-2" /> Loading proposal...
        </div>
      </DashboardLayout>
    );
  }

  if (loadError) {
    return (
      <DashboardLayout>
        <div className="max-w-3xl mx-auto space-y-4">
          <button
            onClick={() => navigate('/proposals')}
            className="p-2 sharp-btn-secondary text-zinc-400 hover:text-white"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div className="p-4 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-lg flex items-center gap-3">
            <AlertCircle className="w-5 h-5 shrink-0" />{loadError}
          </div>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-6">
        <div className="flex items-center justify-between flex-wrap gap-4">
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/proposals')}
              className="p-2 sharp-btn-secondary text-zinc-400 hover:text-white"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-white">Edit Proposal</h1>
              <p className="text-zinc-400 text-xs">Modify draft proposal sections, pricing, or terms.</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="bg-white/[0.03] border border-white/[0.08] rounded-lg p-1 flex">
              <button
                onClick={() => setActiveTab('build')}
                className={`px-3 py-1 rounded-md text-xs font-semibold transition-all ${activeTab === 'build' ? 'bg-indigo-600 text-white shadow-sm' : 'text-zinc-400 hover:text-white'}`}
              >
                Editor
              </button>
              <button
                onClick={() => setActiveTab('preview')}
                className={`px-3 py-1 rounded-md text-xs font-semibold transition-all flex items-center gap-1.5 ${activeTab === 'preview' ? 'bg-indigo-600 text-white shadow-sm' : 'text-zinc-400 hover:text-white'}`}
              >
                <Eye className="w-3.5 h-3.5" /> Preview
              </button>
            </div>

            {templates.length > 0 && (
              <button
                onClick={() => setTemplatePickerOpen(true)}
                className="flex items-center gap-2 px-3 py-1.5 sharp-btn-secondary text-indigo-300 text-xs font-semibold"
              >
                <Sparkles className="w-3.5 h-3.5 text-indigo-400" /> Templates
              </button>
            )}
          </div>
        </div>

        {activeTab === 'build' ? (
          <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
            <div className="xl:col-span-2 sharp-card p-6">
              <form onSubmit={handleSubmit} className="space-y-6">
                {formError && (
                  <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-lg flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 shrink-0" /><span>{formError}</span>
                  </div>
                )}

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <label className="sharp-label">Client *</label>
                    <select
                      value={clientId}
                      onChange={e => setClientId(e.target.value)}
                      className="w-full sharp-input px-3.5 py-2 text-zinc-200"
                    >
                      {clients.map(c => <option key={c.id} value={c.id} className="bg-[#0c101d] text-white">{c.name}{c.company ? ` (${c.company})` : ''}</option>)}
                    </select>
                  </div>
                  <div className="space-y-1.5">
                    <label className="sharp-label">Valid Until</label>
                    <input
                      type="date"
                      value={validUntil}
                      onChange={e => setValidUntil(e.target.value)}
                      className="w-full sharp-input px-3.5 py-2 font-mono-num text-zinc-200"
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="sharp-label">Proposal Title *</label>
                  <input
                    type="text"
                    required
                    value={title}
                    onChange={e => setTitle(e.target.value)}
                    className="w-full sharp-input px-3.5 py-2 text-zinc-100 placeholder-zinc-500"
                  />
                </div>

                <ProposalSectionFields
                  overview={overview} scope={scope} timeline={timeline} pricingNotes={pricingNotes}
                  setOverview={setOverview} setScope={setScope} setTimeline={setTimeline} setPricingNotes={setPricingNotes}
                  collapsed={collapsed} onToggle={toggle}
                />

                <ProposalPricingFields
                  items={items} discountType={discountType} discountValue={discountValue} taxRate={taxRate}
                  setDiscountType={setDiscountType} setDiscountValue={setDiscountValue} setTaxRate={setTaxRate}
                  onAddItem={addItem} onRemoveItem={removeItem} onUpdateItem={updateItem}
                />

                <div className="pt-4 border-t border-white/[0.08] flex gap-3">
                  <button
                    type="button"
                    onClick={() => navigate('/proposals')}
                    className="flex-1 sharp-btn-secondary py-2 text-xs"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    className="flex-1 sharp-btn-primary py-2 text-xs flex items-center justify-center gap-2 text-white"
                  >
                    {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-3.5 h-3.5" />}
                    <span>{submitting ? 'Saving...' : 'Save Changes'}</span>
                  </button>
                </div>
              </form>
            </div>

            <ProposalBuilderSidebar
              title={title} clientId={clientId}
              sections={{ overview, scope, timeline, pricingNotes }}
              items={items} discountType={discountType} discountValue={discountValue} taxRate={taxRate}
            />
          </div>
        ) : (
          <div className="max-w-4xl mx-auto sharp-card p-6 sm:p-8">
            <ProposalPreview
              theme={selectedTemplateTheme}
              variant="dark"
              data={{
                title, validUntil, overview, scope, timeline, pricingNotes, items,
                discountType: discountType || null, discountValue, taxRate,
                templateName: selectedTemplate?.name,
              }}
            />
          </div>
        )}
      </div>

      {templatePickerOpen && (
        <ProposalTemplatePicker
          templates={templates}
          selectedTemplateId={selectedTemplateId}
          onSelect={applyTemplate}
          onClose={() => setTemplatePickerOpen(false)}
        />
      )}
    </DashboardLayout>
  );
};

export default ProposalEdit;
