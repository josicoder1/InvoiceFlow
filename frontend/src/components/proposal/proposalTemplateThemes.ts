import type { LucideIcon } from 'lucide-react';
import { Building2, Layers, Rocket, Wand2 } from 'lucide-react';
import type { ProposalTemplate } from '../../types';

export type TemplateLayout = 'classic' | 'creative' | 'corporate' | 'executive';

export interface TemplateTheme {
  badge: string;
  icon: LucideIcon;
  layout: TemplateLayout;
  cardClass: string;
  previewClass: string;
  glow: string;
  chips: string[];
  accentText: string;
  accentBorder: string;
  accentBg: string;
}

const classicTheme: TemplateTheme = {
  badge: 'Classic',
  icon: Layers,
  layout: 'classic',
  cardClass: 'from-indigo-500/20 to-purple-500/10 border-indigo-400/40 hover:border-indigo-300/70',
  previewClass: 'from-indigo-700 to-purple-700',
  glow: 'shadow-indigo-900/40',
  chips: ['Balanced layout', 'Trusted format', 'Quick to edit'],
  accentText: 'text-indigo-300',
  accentBorder: 'border-indigo-500/30',
  accentBg: 'bg-indigo-500/10',
};

const creativeTheme: TemplateTheme = {
  badge: 'Growth',
  icon: Rocket,
  layout: 'creative',
  cardClass: 'from-fuchsia-500/20 to-pink-500/10 border-fuchsia-400/40 hover:border-fuchsia-300/70',
  previewClass: 'from-fuchsia-700 to-pink-700',
  glow: 'shadow-fuchsia-900/40',
  chips: ['Visual-first', 'Story flow', 'Premium sections'],
  accentText: 'text-fuchsia-300',
  accentBorder: 'border-fuchsia-500/30',
  accentBg: 'bg-fuchsia-500/10',
};

const corporateTheme: TemplateTheme = {
  badge: 'Corporate',
  icon: Building2,
  layout: 'corporate',
  cardClass: 'from-cyan-500/20 to-blue-500/10 border-cyan-400/40 hover:border-cyan-300/70',
  previewClass: 'from-cyan-700 to-blue-700',
  glow: 'shadow-cyan-900/40',
  chips: ['Executive tone', 'Clean structure', 'Client-safe'],
  accentText: 'text-cyan-300',
  accentBorder: 'border-cyan-500/30',
  accentBg: 'bg-cyan-500/10',
};

const executiveTheme: TemplateTheme = {
  badge: 'Executive',
  icon: Building2,
  layout: 'executive',
  cardClass: 'from-emerald-500/20 to-teal-500/10 border-emerald-400/40 hover:border-emerald-300/70',
  previewClass: 'from-emerald-700 to-teal-700',
  glow: 'shadow-emerald-900/40',
  chips: ['Boardroom style', 'Risk-aware', 'Decision ready'],
  accentText: 'text-emerald-300',
  accentBorder: 'border-emerald-500/30',
  accentBg: 'bg-emerald-500/10',
};

const growthTheme: TemplateTheme = {
  ...creativeTheme,
  badge: 'Growth',
  icon: Rocket,
  chips: ['Bold cover', 'Conversion focus', 'KPI-ready'],
};

function themeFromName(name: string): TemplateTheme | null {
  const lower = name.toLowerCase();
  if (lower.includes('web') || lower.includes('design')) return growthTheme;
  if (lower.includes('brand') || lower.includes('identity')) return corporateTheme;
  if (lower.includes('retainer') || lower.includes('monthly') || lower.includes('development')) return executiveTheme;
  if (lower.includes('modern') || lower.includes('growth')) return growthTheme;
  if (lower.includes('corporate') || lower.includes('business')) return corporateTheme;
  if (lower.includes('creative')) return { ...creativeTheme, icon: Wand2, badge: 'Creative' };
  return null;
}

export function resolveTemplateTheme(template: ProposalTemplate, index: number): TemplateTheme {
  return themeFromName(template.name) ?? [classicTheme, creativeTheme, executiveTheme][index % 3];
}

export function inferThemeFromTitle(title: string, templates: ProposalTemplate[] = []): TemplateTheme {
  const lower = title.toLowerCase();
  const matched = templates.find(t => {
    const tn = t.name.toLowerCase();
    return lower.includes(tn) || tn.includes(lower);
  });
  if (matched) return resolveTemplateTheme(matched, templates.indexOf(matched));

  const byName = themeFromName(title);
  if (byName) return byName;

  const hash = title.split('').reduce((acc, ch) => acc + ch.charCodeAt(0), 0);
  return [classicTheme, corporateTheme, executiveTheme][hash % 3];
}

export const defaultTheme = classicTheme;
