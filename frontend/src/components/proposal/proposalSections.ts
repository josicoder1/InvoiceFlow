import { BarChart2, Layers, Settings, Users } from 'lucide-react';

export const sectionMeta = [
  { key: 'overview', label: 'Project Overview', icon: Users, placeholder: 'Describe what you are proposing...' },
  { key: 'scope', label: 'Scope of Work', icon: Layers, placeholder: 'List deliverables, exclusions, and assumptions...' },
  { key: 'timeline', label: 'Timeline', icon: BarChart2, placeholder: 'Week 1: Discovery, Week 2: Delivery...' },
  { key: 'pricingNotes', label: 'Pricing Notes', icon: Settings, placeholder: 'Payment terms, milestones, and billing notes...' },
] as const;

export type SectionKey = (typeof sectionMeta)[number]['key'];
