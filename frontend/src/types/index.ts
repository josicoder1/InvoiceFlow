export interface User {
  id: string;
  email: string;
  fullName: string;
  businessName: string | null;
  businessAddress: string | null;
  businessLogoUrl: string | null;
  taxId: string | null;
  defaultCurrency: string;
  plan: 'FREE' | 'STARTER' | 'PRO';
  chapaConnected: boolean;
  remindersEnabled: boolean;
  reminderDaysBeforeDue: number;
  reminderDaysAfterDue: number;
}

export interface TokenResponse {
  accessToken: string;
}

export interface Client {
  id: string;
  userId: string;
  name: string;
  email: string;
  company: string | null;
  address: string | null;
  currency: string;
  createdAt: string;
  updatedAt: string;
}

export interface ClientRequest {
  name: string;
  email: string;
  company?: string;
  address?: string;
  currency?: string;
}

export interface InvoiceItem {
  id: string;
  invoiceId: string;
  description: string;
  quantity: number;
  unitPrice: number;
  total: number;
  position: number;
}

export interface InvoiceItemRequest {
  description: string;
  quantity: number;
  unitPrice: number;
  position: number;
}

export interface Invoice {
  id: string;
  userId: string;
  clientId: string;
  invoiceNumber: string;
  status: 'DRAFT' | 'SENT' | 'PAID' | 'OVERDUE' | 'CANCELLED';
  issueDate: string;
  dueDate: string;
  currency: string;
  subtotal: number;
  discountType: 'PERCENT' | 'FIXED' | null;
  discountValue: number;
  taxRate: number;
  total: number;
  notes: string | null;
  paymentTerms: string | null;
  chapaPaymentLink: string | null;
  paidAt: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
  items: InvoiceItem[];
}

export interface InvoiceRequest {
  clientId: string;
  issueDate: string;
  dueDate: string;
  currency?: string;
  items: InvoiceItemRequest[];
  discountType?: 'PERCENT' | 'FIXED' | null;
  discountValue?: number;
  taxRate?: number;
  notes?: string;
  paymentTerms?: string;
}

export interface ClientHistory {
  client: Client;
  invoices: Invoice[];
  totalBilled: number;
}

export interface ProposalItem {
  id: string;
  proposalId: string;
  description: string;
  quantity: number | null;
  unitPrice: number | null;
  total: number | null;
  position: number;
}

export interface ProposalItemRequest {
  description: string;
  quantity?: number;
  unitPrice?: number;
  position: number;
}

export interface Proposal {
  id: string;
  userId: string;
  clientId: string;
  proposalNumber: string;
  title: string;
  publicToken: string | null;
  publicLink: string | null;
  status: 'DRAFT' | 'SENT' | 'VIEWED' | 'ACCEPTED' | 'DECLINED';
  overview: string | null;
  scope: string | null;
  timeline: string | null;
  pricingNotes: string | null;
  validUntil: string | null;
  subtotal: number;
  discountType: 'PERCENT' | 'FIXED' | null;
  discountValue: number;
  taxRate: number;
  total: number;
  hasSigned: boolean;
  signedAt: string | null;
  viewedAt: string | null;
  convertedToInvoiceId: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
  items: ProposalItem[];
}

export interface ProposalRequest {
  clientId: string;
  title: string;
  overview?: string;
  scope?: string;
  timeline?: string;
  pricingNotes?: string;
  validUntil?: string;
  items?: ProposalItemRequest[];
  discountType?: 'PERCENT' | 'FIXED' | null;
  discountValue?: number;
  taxRate?: number;
  version?: number;
}

export interface PublicProposalParty {
  name: string;
  email: string;
  address: string | null;
  logoUrl: string | null;
}

export interface PublicProposal {
  proposalNumber: string;
  title: string;
  status: 'DRAFT' | 'SENT' | 'VIEWED' | 'ACCEPTED' | 'DECLINED';
  from: PublicProposalParty;
  to: PublicProposalParty;
  overview: string | null;
  scope: string | null;
  timeline: string | null;
  pricingNotes: string | null;
  items: ProposalItem[];
  subtotal: number;
  discountType: 'PERCENT' | 'FIXED' | null;
  discountValue: number;
  taxRate: number;
  total: number;
  validUntil: string | null;
  isExpired: boolean;
  isActionable: boolean;
  hasSigned: boolean;
}

export interface ProposalTemplate {
  id: string;
  name: string;
  overview: string | null;
  scope: string | null;
  timeline: string | null;
  pricingNotes: string | null;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export interface DashboardSummary {
  totalRevenue: number;
  totalOutstanding: number;
  totalOverdueThisMonth: number;
  activeClientsCount: number;
  conversionRate: number;
}

export interface RevenueDataPoint {
  month: string;
  revenue: number;
}

export interface ActivityEvent {
  eventType: string;
  description: string;
  timestamp: string;
  linkTarget: string;
}

export interface SubscriptionStatus {
  plan: 'FREE' | 'STARTER' | 'PRO';
  invoicesThisMonth: number;
  totalClients: number;
}

export interface PublicInvoiceParty {
  name: string;
  email: string;
  address: string | null;
  logoUrl: string | null;
}

export interface PublicInvoice {
  invoiceNumber: string;
  status: 'DRAFT' | 'SENT' | 'PAID' | 'OVERDUE' | 'CANCELLED';
  from: PublicInvoiceParty;
  to: PublicInvoiceParty;
  issueDate: string;
  dueDate: string;
  currency: string;
  items: ProposalItem[];
  subtotal: number;
  discountType: 'PERCENT' | 'FIXED' | null;
  discountValue: number;
  taxRate: number;
  total: number;
  notes: string | null;
  paymentTerms: string | null;
  canPay: boolean;
  isPaid: boolean;
}
