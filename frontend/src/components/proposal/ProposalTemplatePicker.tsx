import React from 'react';
import { Check, X } from 'lucide-react';
import type { ProposalTemplate } from '../../types';
import { resolveTemplateTheme } from './proposalTemplateThemes';

interface ProposalTemplatePickerProps {
  templates: ProposalTemplate[];
  selectedTemplateId: string | null;
  onSelect: (template: ProposalTemplate) => void;
  onClose: () => void;
}

const ProposalTemplatePicker: React.FC<ProposalTemplatePickerProps> = ({
  templates,
  selectedTemplateId,
  onSelect,
  onClose,
}) => (
  <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
    <div className="w-full max-w-2xl bg-[#0c101f] border border-white/[0.05] rounded-2xl shadow-2xl overflow-hidden">
      <div className="px-6 py-4 border-b border-white/[0.05] flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold text-zinc-200">Choose a Template</h2>
          <p className="text-xs text-zinc-500 mt-0.5">Each template uses a distinct layout style for your client preview.</p>
        </div>
        <button onClick={onClose} className="p-1.5 hover:bg-white/[0.05] rounded-lg text-zinc-500 hover:text-zinc-200 transition-all">
          <X className="w-5 h-5" />
        </button>
      </div>
      <div className="p-6">
        <div className="grid gap-3 sm:grid-cols-2">
          {templates.map((t, index) => {
            const theme = resolveTemplateTheme(t, index);
            const ThemeIcon = theme.icon;
            const active = selectedTemplateId === t.id;
            return (
              <button
                key={t.id}
                onClick={() => onSelect(t)}
                className={`group w-full text-left p-4 bg-gradient-to-b ${theme.cardClass} border rounded-xl transition-all shadow-xl ${theme.glow} ${active ? 'ring-2 ring-white/25 scale-[1.01]' : 'hover:scale-[1.01]'}`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <span className="font-semibold text-zinc-100 block">{t.name}</span>
                    <div className="mt-2 inline-flex items-center gap-1.5 text-[11px] px-2 py-1 rounded-full bg-white/10 text-white border border-white/20">
                      <ThemeIcon className="w-3 h-3" />
                      {theme.badge} · {theme.layout}
                    </div>
                  </div>
                  <span className={`transition-opacity text-white ${active ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'}`}>
                    <Check className="w-4 h-4" />
                  </span>
                </div>
                <div className="mt-3 text-xs text-zinc-200/90">
                  {t.overview ? <p className="line-clamp-3">{t.overview}</p> : <p className="italic text-zinc-300/80">No overview preview available.</p>}
                </div>
                <div className="mt-3 flex flex-wrap gap-1.5">
                  {theme.chips.map(chip => (
                    <span key={chip} className="text-[10px] px-2 py-0.5 rounded-full bg-black/20 border border-white/20 text-white/90">{chip}</span>
                  ))}
                </div>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  </div>
);

export default ProposalTemplatePicker;
