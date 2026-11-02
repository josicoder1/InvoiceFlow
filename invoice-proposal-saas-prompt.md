# Master Build Prompt — Invoice & Proposal Generator SaaS (Updated)

Use this prompt (or sections of it) when working with an AI coding assistant to build the full project. Weeks 1–5 are already implemented. This document covers the complete spec including original Weeks 1–6 plus all Phase 2 extensions.

---

## Project overview

You are building a full-stack SaaS application called **InvoiceFlow** — an invoice and proposal generator for freelancers and small businesses. The goal is to ship a production-ready MVP that can be listed for sale on Acquire.com or Flippa.

The app must be clean, fast, and fully functional end-to-end. Every feature must work — no stubs, no TODO placeholders in shipped code.

**Current status: Weeks 1–5 COMPLETED.** Week 6 (Dashboard + Billing + Polish) and Phase 2 extensions (Weeks 7–12) are the remaining build scope.

---

## Tech stack

### Backend
- **Java 21** with **Spring Boot 3.x**
- **Hexagonal architecture** (ports and adapters): domain, application, infrastructure layers strictly separated
- **Spring Data JPA** with **PostgreSQL**
- **Flyway** for database migrations
- **Spring Security** with **JWT** (stateless, no sessions)
- **Spring Mail** for email sending (SMTP / SendGrid)
- **iText or OpenPDF** for PDF generation
- **Stripe Java SDK** for payment processing
- **Kafka** (optional for Phase 2) or Spring `@Scheduled` for background jobs
- **Testcontainers + JUnit 5 + Mockito** for testing (no Spring context in unit tests)
- **WebFlux/WebSocket** (Phase 2) for real-time proposal analytics
- **OpenAPI/SpringDoc** for API documentation

### Frontend
- **React 18** with **TypeScript**
- **Vite** as build tool
- **React Router v6** for routing
- **TanStack Query (React Query)** for server state
- **React Hook Form + Zod** for forms and validation
- **Tailwind CSS** for styling
- **shadcn/ui** for UI components
- **React PDF** or iframe for PDF preview
- **Recharts** for dashboard charts
- **Stripe.js** for payment UI
- **Zustand** for client state (Phase 2: real-time notifications)
- **React DnD** (Phase 2: Kanban-style project board)

### Infrastructure
- **Docker + Docker Compose** for local development
- **PostgreSQL** (containerized locally, managed DB in prod)
- **MinIO** (Phase 2) for S3-compatible object storage (receipts, logos)
- **Redis** (Phase 2) for caching and session store
- Environment config via `.env` files, never hardcoded secrets

---

## Project structure

### Backend (`/backend`)

```
src/main/java/com/invoiceflow/
├── domain/
│   ├── model/          # Pure domain entities and value objects
│   ├── event/          # Domain events
│   ├── exception/      # BusinessRuleViolationException and domain exceptions
│   └── port/
│       ├── in/         # Use case interfaces (input ports)
│       └── out/        # Repository and external service interfaces (output ports)
├── application/
│   └── service/        # Use case implementations
├── infrastructure/
│   ├── persistence/    # JPA entities, repositories, mappers
│   ├── web/            # Controllers, DTOs, exception handlers
│   ├── email/          # Email adapter
│   ├── pdf/            # PDF generation adapter
│   ├── payment/        # Stripe adapter
│   ├── security/       # JWT filter, user details service
│   ├── scheduler/      # Background job runners
│   └── websocket/      # WebSocket handlers (Phase 2)
└── InvoiceFlowApplication.java
```

### Frontend (`/frontend`)

```
src/
├── api/           # API client functions (one file per domain)
├── components/    # Shared UI components
├── pages/         # Route-level page components
├── hooks/         # Custom React hooks
├── types/         # TypeScript interfaces mirroring backend DTOs
├── lib/           # Utilities (formatters, validators)
├── store/         # Zustand stores (Phase 2)
└── main.tsx
```

---

## Database schema

Create Flyway migrations in order. All tables use UUID primary keys.

### V1__create_users.sql
```sql
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255),
                       google_id VARCHAR(255),
                       full_name VARCHAR(255) NOT NULL,
                       business_name VARCHAR(255),
                       business_address TEXT,
                       business_logo_url VARCHAR(500),
                       tax_id VARCHAR(100),
                       default_currency VARCHAR(10) DEFAULT 'USD',
                       brand_color VARCHAR(7) DEFAULT '#4F46E5',
                       stripe_account_id VARCHAR(255),
                       stripe_customer_id VARCHAR(255),
                       plan VARCHAR(50) DEFAULT 'FREE',
                       plan_expires_at TIMESTAMP,
                       late_fee_enabled BOOLEAN DEFAULT false,
                       late_fee_type VARCHAR(20),
                       late_fee_value NUMERIC(15,2) DEFAULT 0,
                       late_fee_grace_period_days INTEGER DEFAULT 0,
                       timezone VARCHAR(50) DEFAULT 'UTC',
                       language VARCHAR(10) DEFAULT 'en',
                       created_at TIMESTAMP DEFAULT now(),
                       updated_at TIMESTAMP DEFAULT now()
);
```

### V2__create_clients.sql
```sql
CREATE TABLE clients (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  company VARCHAR(255),
  address TEXT,
  currency VARCHAR(10) DEFAULT 'USD',
  payment_score INTEGER DEFAULT 50,
  avg_days_to_pay INTEGER,
  notes TEXT,
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);
```

### V3__create_invoices.sql
```sql
CREATE TABLE invoices (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  client_id UUID NOT NULL REFERENCES clients(id),
  invoice_number VARCHAR(50) NOT NULL,
  status VARCHAR(50) DEFAULT 'DRAFT',   -- DRAFT, SENT, PAID, OVERDUE, CANCELLED, PENDING_APPROVAL
  issue_date DATE NOT NULL,
  due_date DATE NOT NULL,
  currency VARCHAR(10) DEFAULT 'USD',
  subtotal NUMERIC(15,2) DEFAULT 0,
  discount_type VARCHAR(20),             -- PERCENT or FIXED
  discount_value NUMERIC(15,2) DEFAULT 0,
  tax_rate NUMERIC(5,2) DEFAULT 0,
  total NUMERIC(15,2) DEFAULT 0,
  amount_paid NUMERIC(15,2) DEFAULT 0,
  notes TEXT,
  payment_terms TEXT,
  stripe_payment_link VARCHAR(500),
  paid_at TIMESTAMP,
  approved_by UUID REFERENCES users(id),
  approved_at TIMESTAMP,
  recurring_profile_id UUID,
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now(),
  version INTEGER DEFAULT 0             -- optimistic locking
);

CREATE TABLE invoice_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
  description TEXT NOT NULL,
  quantity NUMERIC(10,2) NOT NULL,
  unit_price NUMERIC(15,2) NOT NULL,
  total NUMERIC(15,2) NOT NULL,
  position INTEGER NOT NULL
);

CREATE INDEX idx_invoices_user_id ON invoices(user_id);
CREATE INDEX idx_invoices_client_id ON invoices(client_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoice_items_invoice_id ON invoice_items(invoice_id);
```

### V4__create_proposals.sql
```sql
CREATE TABLE proposals (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  client_id UUID NOT NULL REFERENCES clients(id),
  proposal_number VARCHAR(20) NOT NULL,
  title VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  public_token VARCHAR(255) UNIQUE NOT NULL,
  overview TEXT,
  scope_of_work TEXT,
  timeline TEXT,
  pricing_notes TEXT,
  subtotal NUMERIC(15,2) NOT NULL DEFAULT 0,
  discount_type VARCHAR(10),
  discount_value NUMERIC(15,2) DEFAULT 0,
  tax_rate NUMERIC(5,2) DEFAULT 0,
  total NUMERIC(15,2) NOT NULL DEFAULT 0,
  valid_until DATE,
  signature_data TEXT,
  signed_at TIMESTAMP,
  viewed_at TIMESTAMP,
  converted_to_invoice_id UUID REFERENCES invoices(id),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  version INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE proposal_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  proposal_id UUID NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
  description TEXT NOT NULL,
  quantity NUMERIC(10,2) NOT NULL,
  unit_price NUMERIC(15,2) NOT NULL,
  total NUMERIC(15,2) NOT NULL,
  position INTEGER NOT NULL
);

CREATE INDEX idx_proposals_user_id ON proposals(user_id);
CREATE INDEX idx_proposals_public_token ON proposals(public_token);
CREATE INDEX idx_proposals_status ON proposals(status);
CREATE INDEX idx_proposal_items_proposal_id ON proposal_items(proposal_id);
```

### V5__create_outbox.sql
```sql
CREATE TABLE outbox_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  aggregate_type VARCHAR(100) NOT NULL,
  aggregate_id UUID NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  payload JSONB NOT NULL,
  published BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_outbox_published ON outbox_events(published, created_at);
```

### V6__seed_proposal_templates.sql
```sql
CREATE TABLE proposal_templates (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  overview TEXT,
  scope_of_work TEXT,
  timeline TEXT,
  pricing_notes TEXT,
  created_at TIMESTAMP DEFAULT now()
);

INSERT INTO proposal_templates (name, title, overview, scope_of_work, timeline, pricing_notes) VALUES
('Web Development Project', 'Web Development Proposal', 
 'We propose a complete web development solution tailored to your business needs. Our team will deliver a modern, responsive, and high-performance website.',
 '— Full-stack web application development
— Responsive UI/UX design
— CMS integration
— SEO optimization
— 30 days post-launch support',
 'Week 1–2: Discovery and wireframing
Week 3–4: UI/UX design
Week 5–8: Development and testing
Week 9: Deployment and handover',
 '50% deposit required to begin work. Remaining 50% due upon project completion.'),

('Design Services', 'Design Services Proposal',
 'We offer comprehensive design services including branding, UI/UX, and marketing materials to elevate your brand identity.',
 '— Brand identity design
— Marketing collateral
— Social media assets
— Print-ready files
— 2 rounds of revisions included',
 'Week 1: Brand discovery and research
Week 2: Concept development
Week 3: Design refinement
Week 4: Final delivery',
 'Payment due within 14 days of invoice. Rush delivery available for additional fee.'),

('Consulting Engagement', 'Consulting Services Proposal',
 'Strategic consulting services to help optimize your business processes, technology stack, and growth strategy.',
 '— Business process analysis
— Technology assessment
— Strategic roadmap development
— Implementation guidance
— Weekly progress reports',
 'Ongoing engagement with weekly deliverables. Initial assessment in first 2 weeks.',
 'Billed monthly in arrears based on hours worked. Minimum 20-hour monthly commitment.');
```

### V7__time_tracking.sql
```sql
CREATE TABLE time_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    client_id UUID REFERENCES clients(id),
    project_id UUID,
    description TEXT NOT NULL,
    duration_minutes INTEGER NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    billable BOOLEAN DEFAULT true,
    invoiced BOOLEAN DEFAULT false,
    invoice_id UUID REFERENCES invoices(id),
    hourly_rate NUMERIC(15,2),
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_time_entries_user_id ON time_entries(user_id);
CREATE INDEX idx_time_entries_client_id ON time_entries(client_id);
CREATE INDEX idx_time_entries_invoiced ON time_entries(invoiced);
```

### V8__recurring_invoices.sql
```sql
CREATE TABLE recurring_invoice_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id),
    frequency VARCHAR(20) NOT NULL, -- WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM
    cron_expression VARCHAR(100),
    start_date DATE NOT NULL,
    end_date DATE,
    next_run_date DATE NOT NULL,
    last_run_date DATE,
    auto_send BOOLEAN DEFAULT false,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, PAUSED, COMPLETED, CANCELLED
    currency VARCHAR(10) DEFAULT 'USD',
    notes TEXT,
    payment_terms TEXT,
    discount_type VARCHAR(20),
    discount_value NUMERIC(15,2) DEFAULT 0,
    tax_rate NUMERIC(5,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE recurring_invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES recurring_invoice_profiles(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    quantity NUMERIC(10,2) NOT NULL,
    unit_price NUMERIC(15,2) NOT NULL,
    position INTEGER NOT NULL
);

CREATE INDEX idx_recurring_profiles_user_id ON recurring_invoice_profiles(user_id);
CREATE INDEX idx_recurring_profiles_next_run ON recurring_invoice_profiles(next_run_date, status);
```

### V9__expenses.sql
```sql
CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL, -- SOFTWARE, TRAVEL, MEALS, OFFICE, MARKETING, OTHER
    amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'USD',
    vendor VARCHAR(255),
    receipt_url VARCHAR(500),
    date DATE NOT NULL,
    billable BOOLEAN DEFAULT false,
    client_id UUID REFERENCES clients(id),
    invoice_id UUID REFERENCES invoices(id),
    notes TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_expenses_user_id ON expenses(user_id);
CREATE INDEX idx_expenses_date ON expenses(date);
CREATE INDEX idx_expenses_category ON expenses(category);
```

### V10__projects.sql
```sql
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    total_budget NUMERIC(15,2),
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, CANCELLED, ON_HOLD
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE project_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    percentage INTEGER NOT NULL CHECK (percentage >= 0 AND percentage <= 100),
    amount NUMERIC(15,2),
    completed_at TIMESTAMP,
    invoiced BOOLEAN DEFAULT false,
    invoice_id UUID REFERENCES invoices(id),
    position INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_projects_user_id ON projects(user_id);
CREATE INDEX idx_projects_client_id ON projects(client_id);
CREATE INDEX idx_milestones_project_id ON project_milestones(project_id);
```

### V11__team_members.sql
```sql
CREATE TABLE team_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES users(id), -- owner acts as workspace root
    user_id UUID REFERENCES users(id), -- null if invited but not registered
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER', -- ADMIN, MANAGER, VIEWER
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, ACTIVE, INACTIVE
    invited_at TIMESTAMP DEFAULT now(),
    joined_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE invoice_approval_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES users(id),
    condition_field VARCHAR(50) NOT NULL, -- TOTAL, DISCOUNT_VALUE
    condition_operator VARCHAR(20) NOT NULL, -- GT, GTE, LT, LTE, EQ
    condition_value NUMERIC(15,2) NOT NULL,
    approver_role VARCHAR(20) NOT NULL, -- ADMIN, MANAGER
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE invoice_approvals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    requested_by UUID NOT NULL REFERENCES users(id),
    approver_id UUID REFERENCES users(id),
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    comment TEXT,
    requested_at TIMESTAMP DEFAULT now(),
    responded_at TIMESTAMP
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES users(id),
    entity_type VARCHAR(50) NOT NULL, -- INVOICE, PROPOSAL, CLIENT, PROJECT
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL, -- CREATED, UPDATED, DELETED, SENT, PAID, APPROVED
    actor_id UUID NOT NULL REFERENCES users(id),
    details JSONB,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_team_members_workspace ON team_members(workspace_id);
CREATE INDEX idx_audit_log_workspace ON audit_log(workspace_id, created_at);
```

### V12__client_portal.sql
```sql
CREATE TABLE client_portal_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients(id),
    access_token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE proposal_views (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proposal_id UUID NOT NULL REFERENCES proposals(id),
    ip_address VARCHAR(45),
    user_agent TEXT,
    section_times JSONB, -- {"overview": 45, "pricing": 120, "scope": 30}
    viewed_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_proposal_views_proposal_id ON proposal_views(proposal_id);
```

### V13__contracts.sql
```sql
CREATE TABLE contract_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);

INSERT INTO contract_templates (name, title, content) VALUES
('Master Service Agreement', 'Master Service Agreement',
 '<h1>Master Service Agreement</h1><p>This agreement sets forth the terms...</p>'),
('Statement of Work', 'Statement of Work',
 '<h1>Statement of Work</h1><p>This SOW describes the services to be performed...</p>'),
('Non-Disclosure Agreement', 'Non-Disclosure Agreement',
 '<h1>Non-Disclosure Agreement</h1><p>The parties agree to maintain confidentiality...</p>');

CREATE TABLE contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES users(id),
    client_id UUID NOT NULL REFERENCES clients(id),
    template_id UUID REFERENCES contract_templates(id),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, SENT, SIGNED, EXPIRED
    public_token VARCHAR(255) UNIQUE NOT NULL,
    signature_data TEXT,
    signed_at TIMESTAMP,
    valid_until DATE,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);
```

### V14__webhooks.sql
```sql
CREATE TABLE webhooks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES users(id),
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(255) NOT NULL,
    events TEXT[] NOT NULL, -- ['invoice.paid', 'proposal.accepted']
    active BOOLEAN DEFAULT true,
    last_delivered_at TIMESTAMP,
    last_delivery_status INTEGER,
    failure_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE webhook_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    webhook_id UUID NOT NULL REFERENCES webhooks(id),
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    response_status INTEGER,
    response_body TEXT,
    delivered_at TIMESTAMP,
    duration_ms INTEGER,
    success BOOLEAN DEFAULT false
);
```

### V15__multi_currency.sql
```sql
CREATE TABLE exchange_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate NUMERIC(15,8) NOT NULL,
    fetched_at TIMESTAMP DEFAULT now(),
    UNIQUE(from_currency, to_currency, fetched_at)
);

CREATE INDEX idx_exchange_rates_from_to ON exchange_rates(from_currency, to_currency, fetched_at);
```

---

## Domain model rules

Apply these rules strictly across all domain entities:

- Use **static factory methods** (`Invoice.create(...)`) instead of public constructors
- All state transitions must be guarded — throw `BusinessRuleViolationException` on invalid transitions
- Domain entities must not depend on any Spring beans or infrastructure
- Use **domain events** published via the outbox pattern
- **Optimistic locking** via `@Version` on Invoice and Proposal JPA entities
- All money values stored as `BigDecimal`, never `double` or `float`
- All totals calculated server-side with `HALF_UP` rounding to 2 decimal places

### Valid state transitions

**Invoice:** `DRAFT → SENT → PAID | OVERDUE | CANCELLED`
**Invoice (with approval):** `DRAFT → PENDING_APPROVAL → SENT → PAID | OVERDUE | CANCELLED`

**Proposal:**
- `DRAFT → SENT`
- `SENT → VIEWED` (on first public link open)
- `SENT → ACCEPTED` (client may accept without triggering VIEWED first)
- `SENT → DECLINED`
- `VIEWED → ACCEPTED`
- `VIEWED → DECLINED`

**Contract:** `DRAFT → SENT → SIGNED | EXPIRED`

**Forbidden transitions:**
- Cannot edit a proposal/contract that is not in DRAFT status
- Cannot send a proposal/contract that is already SENT, ACCEPTED, or DECLINED
- Cannot accept or decline a proposal that is already ACCEPTED or DECLINED
- Cannot delete a proposal that is ACCEPTED (may be linked to an invoice)
- Cannot convert to invoice unless status is ACCEPTED

### Total calculation formula (server-side only)

```
subtotal = sum(item.quantity * item.unitPrice) for all items
discountAmount = discountType == PERCENT
    ? subtotal * (discountValue / 100)
    : discountValue
taxableAmount = subtotal - discountAmount
taxAmount = taxableAmount * (taxRate / 100)
total = taxableAmount + taxAmount
```

All money values use `BigDecimal` with `HALF_UP` rounding to 2 decimal places.

---

## All use cases to implement

### Authentication (UC-01 to UC-06)
- UC-01: Register with email + password (hash with BCrypt)
- UC-02: Log in, return signed JWT (access token 1h, refresh token 7d)
- UC-03: Reset password via email link (time-limited token)
- UC-04: Set up business profile on first login
- UC-05: Update business profile (name, logo, address, tax ID, currency, brand color, timezone, language)
- UC-06: Delete account (cascade delete all data)

### Client management (UC-07 to UC-11)
- UC-07: Add client
- UC-08: Edit client
- UC-09: Delete client (block if active invoices exist)
- UC-10: Search and filter clients (by name, email, company)
- UC-11: View client history (all invoices and proposals)

### Invoice management (UC-12 to UC-23)
- UC-12: Create invoice (assign auto-incremented number INV-XXXX)
- UC-13: Edit invoice (only in DRAFT or PENDING_APPROVAL status)
- UC-14: Delete invoice (only DRAFT or CANCELLED)
- UC-15: Preview invoice (return rendered HTML)
- UC-16: Download invoice as PDF
- UC-17: Send invoice via email (attach PDF, update status to SENT)
- UC-18: Duplicate invoice (copy as new DRAFT)
- UC-19: Mark invoice as paid (manual)
- UC-20: Apply discount (percentage or fixed amount)
- UC-21: Filter and search invoices (status, client, date range, keyword)
- UC-22: Auto-generate invoice number (sequential per user: INV-0001, INV-0002...)
- UC-23: Auto-mark overdue (scheduled job, runs daily at midnight UTC)

### Proposal management (UC-24 to UC-33)
- UC-24: Create proposal
    - System generates unique proposal number (PROP-XXXX, sequential per user)
    - System generates unique public token (UUID) at creation, never changes
    - Total calculated server-side with HALF_UP rounding
    - Proposal with no items can be saved as DRAFT but cannot be sent
- UC-25: Create proposal from template (3 starter templates seeded in DB)
- UC-26: Edit proposal (only in DRAFT status)
    - Total always recalculated on save
    - Version field checked for optimistic locking — stale update returns 409 Conflict
- UC-27: Send proposal to client
    - Cannot send proposal with no items → 422
    - Cannot send proposal not in DRAFT status → 422
    - Public link: https://app.invoiceflow.com/p/{publicToken}
    - Email subject: "Proposal from {businessName}: {proposalTitle}"
- UC-28: Download proposal as PDF
    - Generated on the fly, never stored on disk
    - Available for proposals in any status
    - Uses user's stored brand color (default #4F46E5)
    - Includes signature block (if signed: client name + signed date; if unsigned: blank signature line)
- UC-29: Convert accepted proposal to invoice (one-click, pre-fills invoice from proposal pricing)
    - Can only convert proposal with status ACCEPTED → 422 otherwise
    - Conversion creates new invoice in DRAFT — does not automatically send
    - One proposal can only be converted once
    - All money values copied exactly — no recalculation
    - notes copied from proposal pricingNotes
    - dueDate defaults to today + 30 days
- UC-30: View proposal via public link (no auth required, token-based)
    - No authentication required — token is the only credential
    - If token does not exist → 404 page
    - If proposal is ACCEPTED or DECLINED → show read-only view with outcome, hide action buttons
    - If proposal is past validUntil date → show expired notice, disable Accept button
    - Never expose owner's internal IDs, JWT, or other proposals
    - On first view (status == SENT): record viewedAt, transition SENT → VIEWED, publish ProposalViewedEvent
- UC-31: Accept proposal with e-signature (client signs on public page)
    - Signature required — cannot accept without drawing one
    - signatureData stored as base64 PNG, rendered in PDF signature block
    - Once ACCEPTED, cannot be declined or re-accepted
    - Owner receives email notification via outbox event
    - Client sees success confirmation page
- UC-32: Decline proposal (client action on public page)
    - Reason is optional — do not require it
    - Once DECLINED, cannot be accepted
    - Owner receives email notification immediately
- UC-33: Track proposal viewed (record timestamp on first public link open, notify owner)
    - Viewed notification sent only once
    - If viewedAt is not null, do not update again and do not send notification again
    - Notification sent asynchronously via outbox

### Payments (UC-34 to UC-37)
- UC-34: Connect Stripe account (OAuth connect flow)
- UC-35: Pay invoice online (Stripe Checkout session from public invoice view)
- UC-36: Auto-mark invoice paid (Stripe webhook handler)
- UC-37: Send payment receipt email to client after payment

### Notifications & reminders (UC-38 to UC-42)
- UC-38: Send overdue reminder email to client (scheduled daily)
- UC-39: Notify user when proposal is viewed (email)
- UC-40: Notify user when proposal is accepted
- UC-41: Notify user when invoice is paid
- UC-42: User can configure reminder schedule (days before/after due date)

### Dashboard & reporting (UC-43 to UC-46)
- UC-43: View revenue overview (total earned, outstanding, overdue this month)
- UC-44: Monthly revenue chart data (last 12 months)
- UC-45: Recent activity feed (last 10 events across invoices and proposals)
- UC-46: Export invoices as CSV

### Subscription & billing (UC-47 to UC-50)
- UC-47: Choose plan at signup (FREE, STARTER $12/mo, PRO $29/mo, ENTERPRISE $59/mo)
- UC-48: Upgrade plan (Stripe subscription)
- UC-49: Cancel subscription (downgrade to FREE at period end)
- UC-50: Enforce plan limits (FREE: max 5 invoices/month, 3 clients; STARTER: unlimited invoices, 50 clients, 3 team members; PRO: unlimited everything, 10 team members; ENTERPRISE: unlimited everything, unlimited team, white-label, API access)

---

## PHASE 2: EXTENDED FEATURES (Weeks 7–12)

### Time Tracking (UC-51)
- Start/stop timer per client/project
- Log hours with description → one-click "Bill these hours" creates invoice draft
- Weekly timesheet view
- Hourly rate per client or per time entry
- Idle detection and manual adjustment

### Recurring Invoices & Retainer Billing (UC-52)
- Create recurring invoice profiles (weekly, biweekly, monthly, quarterly, yearly, custom cron)
- Auto-generate and auto-send on schedule
- Retainer management: track hours against prepaid retainer balance
- Auto-generate "true-up" invoice when overage occurs

### Automatic Late Fees (UC-53)
- Configurable per-client or global: percentage or fixed fee
- Auto-applied when invoice transitions to OVERDUE past grace period
- Displayed clearly on PDF and client view
- Late fee line item added to invoice total

### Payment Behavior Scoring (UC-54)
- Score each client 0-100 based on payment history
- Updated whenever invoice transitions to PAID
- Formula: `100 - (avg_days_to_pay * 2)` capped at 0-100
- Display score badge on client list (green 80-100, yellow 50-79, red 0-49)
- Future: AI escalation recommendations

### Expense Tracking & Receipt Capture (UC-55)
- Upload receipt photos (OCR extraction of amount, date, vendor)
- Categorize expenses (software, travel, meals, office, marketing, other)
- Mark as billable to specific client → auto-adds to next invoice
- Simple P&L view: revenue vs expenses by month

### Project-Based Billing with Milestones (UC-56)
- Create projects linked to clients
- Define milestones with deliverables and % of total fee
- Progress invoicing: auto-generate invoice when milestone marked complete
- Project dashboard: % complete, billed vs unbilled, remaining budget
- Kanban-style project board view

### Proposal Engagement Analytics (UC-57)
- Track time spent per section (overview, scope, pricing, timeline)
- Heat map of client attention in owner view
- Drop-off detection
- Real-time notification: "Client is viewing your proposal right now" (WebSocket/SSE)
- Section visibility tracked via IntersectionObserver frontend events

### Internal Approval Workflows (UC-58)
- Route proposals/invoices to manager/finance for review before sending
- Configurable rules: "Invoices over $5K require approval"
- Audit trail: who edited, who approved, when
- Email notifications for pending approvals

### Multi-Currency & Multi-Language Support (UC-59)
- Invoice in 135+ currencies with real-time exchange rates
- Client sees invoice in their currency, you see revenue in your base currency
- PDF generation in client's language (English, Spanish, French, German, Portuguese, Italian, Dutch)
- Exchange rate API integration (frankfurter.app or exchangerate-api.com)

### Client Portal (UC-60)
- Password-protected portal per client (magic link auth, no password)
- View all invoices, proposals, payment history
- Download PDFs, pay outstanding invoices, view project progress
- Styled with owner's brand color and logo

### Team/Workspace Support (UC-61)
- Invite team members (limited by plan)
- Role-based permissions: Admin, Manager, Viewer
- Activity log showing who created/modified what
- All queries scoped to workspace_id instead of user_id

### Contract Templates + E-Sign Integration (UC-62)
- Master Service Agreement, Statement of Work, NDA templates
- Drag-and-drop fields (signature, date, initials)
- Client signs contract → auto-triggers invoice generation workflow
- Public signing page similar to proposals

### Cash Flow Forecasting Dashboard (UC-63)
- Project incoming cash 30/60/90 days based on:
  - Outstanding invoices (probability-weighted by client score)
  - Scheduled recurring invoices
  - Accepted proposals awaiting conversion
- Visual chart: expected vs pessimistic vs optimistic
- Simple projection line chart

### API & Webhooks for Power Users (UC-64)
- REST API for all CRUD operations
- Webhook events: `invoice.paid`, `proposal.accepted`, `client.created`
- Zapier/Make.com integration template
- API key generation for external access (scoped read/write)
- Exponential backoff retry for webhook deliveries

### White-Label / Custom Domain (UC-65)
- Use your own domain for public links: `proposals.yourbrand.com/p/{token}`
- Remove "Powered by InvoiceFlow" branding on PRO/ENTERPRISE plan
- Custom email sending domain (SPF/DKIM setup)
- DNS TXT record verification

---

## REST API design

All endpoints are prefixed with `/api/v1`. All responses follow RFC 7807 `ProblemDetail` for errors.

### Auth
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password
```

### Users
```
GET    /api/v1/users/me
PUT    /api/v1/users/me
DELETE /api/v1/users/me
POST   /api/v1/users/me/logo          (multipart upload)
```

### Clients
```
GET    /api/v1/clients                (paginated, filterable)
POST   /api/v1/clients
GET    /api/v1/clients/{id}
PUT    /api/v1/clients/{id}
DELETE /api/v1/clients/{id}
GET    /api/v1/clients/{id}/history
```

### Invoices
```
GET    /api/v1/invoices               (paginated, filterable)
POST   /api/v1/invoices
GET    /api/v1/invoices/{id}
PUT    /api/v1/invoices/{id}
DELETE /api/v1/invoices/{id}
GET    /api/v1/invoices/{id}/pdf      (stream PDF)
POST   /api/v1/invoices/{id}/send
POST   /api/v1/invoices/{id}/duplicate
POST   /api/v1/invoices/{id}/mark-paid
POST   /api/v1/invoices/{id}/approve
POST   /api/v1/invoices/{id}/reject
GET    /api/v1/invoices/export/csv
```

### Proposals
```
GET    /api/v1/proposals              (paginated, filterable)
POST   /api/v1/proposals
GET    /api/v1/proposals/{id}
PUT    /api/v1/proposals/{id}
DELETE /api/v1/proposals/{id}
GET    /api/v1/proposals/{id}/pdf
POST   /api/v1/proposals/{id}/send
POST   /api/v1/proposals/{id}/convert-to-invoice
GET    /api/v1/proposal-templates     (list 3 starter templates)
```

### Time Tracking
```
GET    /api/v1/time-entries
POST   /api/v1/time-entries/start
POST   /api/v1/time-entries/{id}/stop
POST   /api/v1/time-entries/{id}/bill
DELETE /api/v1/time-entries/{id}
```

### Recurring Invoices
```
GET    /api/v1/recurring-invoices
POST   /api/v1/recurring-invoices
GET    /api/v1/recurring-invoices/{id}
PUT    /api/v1/recurring-invoices/{id}
DELETE /api/v1/recurring-invoices/{id}
POST   /api/v1/recurring-invoices/{id}/pause
POST   /api/v1/recurring-invoices/{id}/resume
```

### Expenses
```
GET    /api/v1/expenses
POST   /api/v1/expenses
GET    /api/v1/expenses/categories
GET    /api/v1/expenses/summary
PUT    /api/v1/expenses/{id}
DELETE /api/v1/expenses/{id}
```

### Projects
```
GET    /api/v1/projects
POST   /api/v1/projects
GET    /api/v1/projects/{id}
PUT    /api/v1/projects/{id}
DELETE /api/v1/projects/{id}
POST   /api/v1/projects/{id}/milestones
POST   /api/v1/projects/{id}/milestones/{milestoneId}/complete
```

### Team
```
GET    /api/v1/team/members
POST   /api/v1/team/invites
PUT    /api/v1/team/members/{id}/role
DELETE /api/v1/team/members/{id}
```

### Contracts
```
GET    /api/v1/contracts
POST   /api/v1/contracts
GET    /api/v1/contracts/{id}
PUT    /api/v1/contracts/{id}
GET    /api/v1/contracts/{id}/pdf
POST   /api/v1/contracts/{id}/send
DELETE /api/v1/contracts/{id}
```

### Webhooks
```
GET    /api/v1/webhooks
POST   /api/v1/webhooks
PUT    /api/v1/webhooks/{id}
DELETE /api/v1/webhooks/{id}
POST   /api/v1/webhooks/{id}/test
```

### Public endpoints (no auth — token-based)
```
GET    /api/v1/public/proposals/{token}         (client views proposal)
POST   /api/v1/public/proposals/{token}/accept  (client accepts + signs)
POST   /api/v1/public/proposals/{token}/decline
GET    /api/v1/public/invoices/{token}          (client views invoice)
POST   /api/v1/public/invoices/{token}/pay      (create Stripe checkout session)
GET    /api/v1/public/contracts/{token}         (client views contract)
POST   /api/v1/public/contracts/{token}/sign    (client signs contract)
```

### Client Portal
```
POST   /api/v1/portal/auth                     (magic link request)
GET    /api/v1/portal/invoices                 (client's invoices)
GET    /api/v1/portal/proposals                  (client's proposals)
GET    /api/v1/portal/projects                   (client's projects)
```

### Payments
```
POST   /api/v1/payments/stripe/connect
GET    /api/v1/payments/stripe/callback
POST   /api/v1/payments/stripe/webhook          (Stripe sends events here)
```

### Dashboard
```
GET    /api/v1/dashboard/summary
GET    /api/v1/dashboard/revenue-chart
GET    /api/v1/dashboard/activity
GET    /api/v1/dashboard/cash-flow-forecast
GET    /api/v1/dashboard/p-and-l
```

### Subscriptions
```
GET    /api/v1/subscription
POST   /api/v1/subscription/upgrade
POST   /api/v1/subscription/cancel
GET    /api/v1/subscription/limits
```

### Analytics
```
GET    /api/v1/analytics/proposals/{id}/engagement
GET    /api/v1/analytics/clients/{id}/payment-score
GET    /api/v1/analytics/revenue/by-client
GET    /api/v1/analytics/revenue/by-project
```

---

## Response DTOs

### ProposalResponse (owner view)
```json
{
  "id": "uuid",
  "proposalNumber": "PROP-0007",
  "title": "Website Redesign for Acme Corp",
  "status": "ACCEPTED",
  "publicLink": "https://app.invoiceflow.com/p/abc123token",
  "client": {
    "id": "uuid",
    "name": "Acme Corp",
    "email": "sarah@acmecorp.com",
    "address": "New York, USA",
    "paymentScore": 85
  },
  "overview": "We propose a complete redesign...",
  "scopeOfWork": "— Full UI/UX design...",
  "timeline": "Week 1–2: Discovery...",
  "pricingNotes": "50% deposit required...",
  "items": [
    {
      "id": "uuid",
      "description": "UI/UX Design (Figma)",
      "quantity": 1,
      "unitPrice": 800.00,
      "total": 800.00,
      "position": 1
    }
  ],
  "subtotal": 3200.00,
  "discountType": "PERCENT",
  "discountValue": 10,
  "taxRate": 15,
  "total": 3312.00,
  "validUntil": "2026-06-30",
  "viewedAt": "2026-06-03T10:42:00Z",
  "signedAt": "2026-06-05T14:17:00Z",
  "convertedToInvoiceId": "uuid-of-invoice",
  "engagement": {
    "totalViews": 3,
    "lastViewedAt": "2026-06-05T09:00:00Z",
    "sectionTimes": {
      "overview": 45,
      "scope": 120,
      "pricing": 180,
      "timeline": 30
    }
  },
  "createdAt": "2026-06-01T08:00:00Z",
  "updatedAt": "2026-06-05T14:17:00Z"
}
```

### PublicProposalResponse (client view — strip owner internals)
```json
{
  "proposalNumber": "PROP-0007",
  "title": "Website Redesign for Acme Corp",
  "status": "SENT",
  "from": {
    "businessName": "John Dev Studio",
    "email": "john@devstudio.com",
    "address": "Addis Ababa, Ethiopia",
    "logoUrl": "https://...",
    "brandColor": "#4F46E5"
  },
  "to": {
    "name": "Acme Corp",
    "email": "sarah@acmecorp.com",
    "address": "New York, USA"
  },
  "overview": "...",
  "scopeOfWork": "...",
  "timeline": "...",
  "pricingNotes": "...",
  "items": [...],
  "subtotal": 3200.00,
  "discountType": "PERCENT",
  "discountValue": 10,
  "taxRate": 15,
  "total": 3312.00,
  "validUntil": "2026-06-30",
  "isExpired": false,
  "isActionable": true,
  "currency": "USD"
}
```

### DashboardSummaryResponse
```json
{
  "totalEarnedThisMonth": 12500.00,
  "totalOutstanding": 8400.00,
  "totalOverdue": 2100.00,
  "totalExpensesThisMonth": 3200.00,
  "netProfitThisMonth": 9300.00,
  "invoiceCountThisMonth": 12,
  "proposalAcceptanceRate": 68.5,
  "averageDaysToPayment": 14,
  "cashFlowForecast": {
    "next30Days": 15200.00,
    "next60Days": 23100.00,
    "next90Days": 28400.00
  }
}
```

### TimeEntryResponse
```json
{
  "id": "uuid",
  "clientId": "uuid",
  "clientName": "Acme Corp",
  "projectId": "uuid",
  "projectName": "Website Redesign",
  "description": "Homepage design iteration",
  "durationMinutes": 240,
  "startedAt": "2026-06-23T09:00:00Z",
  "endedAt": "2026-06-23T13:00:00Z",
  "billable": true,
  "invoiced": false,
  "hourlyRate": 75.00,
  "totalValue": 300.00
}
```

---

## Domain events (outbox pattern)

Write these events to `outbox_events` table. The outbox poller picks them up and sends emails/webhooks.

```java
// Published when: status transitions SENT → VIEWED (first open)
ProposalViewedEvent {
    proposalId, proposalTitle, clientName, clientEmail, ownerEmail
}

// Published when: client accepts and signs
ProposalAcceptedEvent {
    proposalId, proposalTitle, proposalNumber,
    clientName, clientEmail, ownerEmail, signedAt
}

// Published when: client declines
ProposalDeclinedEvent {
    proposalId, proposalTitle, clientName, ownerEmail, reason
}

// Published when: invoice marked paid
InvoicePaidEvent {
    invoiceId, invoiceNumber, clientName, clientEmail,
    ownerEmail, amount, paidAt
}

// Published when: invoice becomes overdue
InvoiceOverdueEvent {
    invoiceId, invoiceNumber, clientName, clientEmail,
    ownerEmail, amount, daysOverdue
}

// Published when: contract signed
ContractSignedEvent {
    contractId, contractTitle, clientName, clientEmail,
    ownerEmail, signedAt
}

// Published when: milestone completed
MilestoneCompletedEvent {
    milestoneId, projectId, projectName, milestoneTitle,
    ownerEmail, amount
}
```

---

## Frontend pages and routes

```
/                         → redirect to /dashboard if logged in, else /login
/login                    → Login page
/register                 → Register page
/forgot-password          → Forgot password
/reset-password           → Reset password (token from email)

/dashboard                → Dashboard (summary cards + chart + activity feed + cash flow forecast)
/clients                  → Client list
/clients/new              → New client form
/clients/:id              → Client detail + history + payment score

/invoices                 → Invoice list with filters
/invoices/new             → Invoice builder
/invoices/:id             → Invoice detail + actions
/invoices/:id/edit        → Edit invoice

/proposals                → Proposal list with filters
/proposals/new            → Proposal builder (with template picker)
/proposals/:id            → Proposal detail + actions + engagement analytics
/proposals/:id/edit       → Edit proposal

/time-tracking            → Timer + timesheet view
/time-tracking/weekly     → Weekly timesheet

/projects                 → Project list (Kanban board)
/projects/new             → New project
/projects/:id             → Project detail + milestones

/expenses                 → Expense list + receipt upload
/expenses/new             → New expense

/contracts                → Contract list
/contracts/new            → Contract builder
/contracts/:id            → Contract detail

/team                     → Team member management
/team/invites             → Pending invites

/settings                 → Business profile, branding, notification preferences
/settings/billing         → Plan selection and subscription management
/settings/webhooks        → Webhook management
/settings/approval-rules  → Invoice approval workflow rules

/reports                  → Reports hub (P&L, revenue by client, revenue by project)
/reports/p-and-l          → Profit & Loss statement
/reports/cash-flow        → Cash flow forecast

/p/:token                 → Public proposal view (no auth)
/inv/:token               → Public invoice view + payment button (no auth)
/c/:token                 → Public contract view + sign (no auth)
/payment-success          → Stripe payment success redirect
/payment-cancelled        → Stripe payment cancelled redirect

/portal                   → Client portal login (magic link)
/portal/invoices          → Client portal invoices
/portal/proposals         → Client portal proposals
```

---

## Security requirements

- All `/api/v1/**` routes (except `/auth/**`, `/public/**`, `/payments/stripe/webhook`, `/portal/**`) require a valid JWT
- JWT signed with RS256 (asymmetric key pair), not HS256
- Refresh token stored as HttpOnly cookie, access token returned in response body
- All user data is scoped to the authenticated user/workspace — never expose another user's data
- Stripe webhook endpoint validates the `Stripe-Signature` header using the webhook secret
- Public proposal/invoice/contract endpoints validate only the public token — no user auth required
- Client portal validates magic link token — no password required
- Rate limit the auth endpoints (5 requests/minute per IP)
- File uploads (logo, receipts) validated for type (JPEG/PNG/PDF only) and size (max 5MB)
- Webhook endpoints validate signature using stored secret
- Team member endpoints enforce role-based access control

---

## Email templates

Create HTML email templates for:
1. **Welcome email** — sent on registration
2. **Email verification** — verify account
3. **Password reset** — with reset link (expires in 1 hour)
4. **Invoice sent** — sent to client, includes PDF attachment and payment link
5. **Proposal sent** — sent to client, includes link to public proposal page
6. **Proposal viewed notification** — sent to owner when client opens proposal
7. **Proposal accepted notification** — sent to owner
8. **Invoice paid notification** — sent to owner
9. **Overdue reminder** — sent to client, polite tone, includes payment link
10. **Payment receipt** — sent to client after successful payment
11. **Contract sent** — sent to client, includes link to sign
12. **Contract signed notification** — sent to owner
13. **Team invite** — sent to invited team member
14. **Invoice approval request** — sent to approver
15. **Invoice approved/rejected** — sent to requester

All templates must be mobile-responsive HTML using inline styles (no external CSS).

---

## PDF generation requirements

Both invoice and proposal PDFs must:
- Include user's business logo (if set)
- Use the user's brand color (configurable, default #4F46E5)
- Be clean and professional — no clip art, no unnecessary decorations
- Include all line items in a table
- Show subtotal, discount, tax, and total clearly
- Include invoice number / proposal title prominently
- Include both parties' details (user business + client)
- Be generated server-side (not from browser print)
- Be streamed directly as `application/pdf` response — not saved to disk
- Support multi-currency symbols and formatting
- Support RTL languages for Arabic/Hebrew clients

**Proposal PDF additional requirements:**
- Include signature block (if signed: client name + signed date; if unsigned: blank signature line)
- Include overview, scope of work, timeline, and pricing notes sections
- Include valid-until date

**Contract PDF additional requirements:**
- Include all contract clauses
- Include signature block with date
- Include initial fields where marked

---

## Testing requirements

### Backend
- Unit tests for every domain entity and use case (no Spring context)
- Use Mockito for all port mocks
- Integration tests for all REST endpoints using `@SpringBootTest` + Testcontainers (real PostgreSQL)
- Test all state transition guards — verify `BusinessRuleViolationException` is thrown correctly
- Test the Stripe webhook handler with mock payloads
- Test WebSocket connections for real-time analytics
- Test webhook delivery with retry logic
- Minimum 80% line coverage on domain and application layers

**Proposal module specific unit tests:**
```java
ProposalTest:
- test_createProposal_assignsCorrectInitialStatus()
- test_sendProposal_transitionsStatusToSent()
- test_sendProposal_throwsWhenAlreadySent()
- test_sendProposal_throwsWhenNoItems()
- test_markAsViewed_transitionsStatusToViewed()
- test_markAsViewed_doesNotUpdateViewedAtIfAlreadyViewed()
- test_acceptProposal_transitionsStatusToAccepted()
- test_acceptProposal_throwsWhenAlreadyAccepted()
- test_acceptProposal_throwsWhenExpired()
- test_declineProposal_transitionsStatusToDeclined()
- test_declineProposal_throwsWhenAlreadyDeclined()
- test_calculateTotal_withPercentDiscount()
- test_calculateTotal_withFixedDiscount()
- test_calculateTotal_withNoDiscount()
- test_convertToInvoice_throwsWhenNotAccepted()
```

**Proposal module specific integration tests:**
```java
ProposalControllerTest:
- POST /proposals → 201 with correct proposal number
- GET /proposals → 200 paginated list
- PUT /proposals/{id} when DRAFT → 200
- PUT /proposals/{id} when SENT → 422
- POST /proposals/{id}/send → 200, status = SENT
- GET /public/proposals/{token} → 200 public view
- GET /public/proposals/{token} (first view) → status transitions to VIEWED
- POST /public/proposals/{token}/accept → 200, status = ACCEPTED
- POST /public/proposals/{token}/accept (already accepted) → 422
- POST /public/proposals/{token}/decline → 200, status = DECLINED
- POST /proposals/{id}/convert-to-invoice when ACCEPTED → 201 invoice created
- POST /proposals/{id}/convert-to-invoice when DRAFT → 422
```

### Frontend
- Unit tests for utility functions and formatters
- Component tests for the invoice builder and proposal builder forms
- E2E happy path: register → create client → create invoice → send → mark paid
- E2E proposal path: create proposal → send → client views → client accepts → convert to invoice
- E2E time tracking: start timer → stop → bill hours → invoice created

---

## Error handling

### Backend
- All exceptions map to RFC 7807 `ProblemDetail` responses
- `BusinessRuleViolationException` → HTTP 422 Unprocessable Entity
- `ResourceNotFoundException` → HTTP 404
- `AccessDeniedException` → HTTP 403
- `MethodArgumentNotValidException` → HTTP 400 with field-level errors
- `OptimisticLockingFailureException` → HTTP 409 Conflict
- `PlanLimitExceededException` → HTTP 402 Payment Required
- Unexpected errors → HTTP 500, log full stack trace, return generic message to client

### Frontend
- All API errors display a toast notification with the error message
- Form validation errors show inline under each field
- Network errors show a retry option
- 401 responses redirect to `/login` and clear stored tokens
- 402 responses show upgrade modal with plan comparison
- 409 Conflict shows "This record was modified by another user. Refresh and try again."

---

## Docker Compose (local dev)

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: invoiceflow
      POSTGRES_USER: invoiceflow
      POSTGRES_PASSWORD: invoiceflow
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data

  mailhog:
    image: mailhog/mailhog
    ports:
      - "8025:8025"   # Web UI to inspect sent emails
      - "1025:1025"   # SMTP

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/invoiceflow
      SPRING_MAIL_HOST: mailhog
      SPRING_REDIS_HOST: redis
      MINIO_ENDPOINT: http://minio:9000
      JWT_PRIVATE_KEY: ${JWT_PRIVATE_KEY}
      JWT_PUBLIC_KEY: ${JWT_PUBLIC_KEY}
      STRIPE_SECRET_KEY: ${STRIPE_SECRET_KEY}
      STRIPE_WEBHOOK_SECRET: ${STRIPE_WEBHOOK_SECRET}
    depends_on:
      - postgres
      - redis
      - minio

  frontend:
    build: ./frontend
    ports:
      - "5173:5173"
    environment:
      VITE_API_BASE_URL: http://localhost:8080
      VITE_STRIPE_PUBLISHABLE_KEY: ${STRIPE_PUBLISHABLE_KEY}
      VITE_WS_URL: ws://localhost:8080
    depends_on:
      - backend

volumes:
  postgres_data:
  minio_data:
```

---

## Build order (12-week plan)

### Week 1 — Foundation ✅ COMPLETED
- Project scaffolding (backend + frontend + Docker Compose)
- Database schema + Flyway migrations V1–V6
- Auth: register, login, JWT (RS256 asymmetric keys), refresh token (HttpOnly cookie), UC-01 to UC-02
- Business profile setup — UC-04, UC-05
- Frontend: login/register pages, auth context, protected routes, dashboard layout, settings page

### Week 2 — Clients + Invoice builder ✅ COMPLETED
- Client CRUD — UC-07 to UC-11
- Invoice domain model + full state machine
- Invoice CRUD + auto-numbering — UC-12 to UC-14, UC-18, UC-22
- Invoice state transition use cases: UC-17 send, UC-19 mark paid, cancel
- PDF generation adapter + email adapter
- Frontend: `/clients`, `/clients/new`, `/clients/:id`, `/invoices`, `/invoices/new`, `/invoices/:id/edit`

### Week 3 — Invoice actions + PDF ✅ COMPLETED
- UC-15 Preview invoice
- UC-16 Download PDF
- UC-17 Send invoice via email
- UC-21 Filter/search invoices
- UC-23 Auto-mark overdue
- Frontend: invoice list side panel with inline actions

### Week 4 — Proposals ✅ COMPLETED
- Proposal domain model + state machine
- Proposal CRUD (UC-24 to UC-28)
- Template system with 3 seeded templates (UC-25)
- Public proposal page (UC-30 to UC-33)
- E-signature capture (base64 PNG)
- Convert proposal to invoice (UC-29)
- Frontend: proposal builder, public proposal page

### Week 5 — Payments + notifications ✅ COMPLETED
- Chapa Connect integration (UC-34) — merchant secret key connect flow
- Invoice payment via Chapa (UC-35 to UC-37) — checkout, webhook auto-mark paid, payment receipt email
- Public invoice view with pay button
- All email notifications including proposal viewed/accepted/declined (UC-38 to UC-41)
- Scheduled jobs: overdue marking, reminder emails
- Frontend: payment success page with confirm + polling, notification settings

### Week 6 — Dashboard + billing + polish
- Dashboard summary + revenue chart (UC-43 to UC-45)
- Cash flow forecasting MVP (UC-63 lite)
- CSV export (UC-46)
- Subscription plans + Stripe billing (UC-47 to UC-50)
- Plan limit enforcement (UC-50)
- Branding settings (logo, color)
- Full test pass + bug fixes
- Production Docker build + README

### Week 7 — Time tracking + recurring invoices + late fees
- Time tracking domain + UI (UC-51)
- Recurring invoice profiles + scheduler (UC-52)
- Automatic late fee engine (UC-53)
- Frontend: timer component, timesheet view, recurring invoice builder

### Week 8 — Intelligence layer
- Payment behavior scoring (UC-54)
- Proposal engagement analytics backend (UC-57)
- Real-time WebSocket notifications
- Frontend: proposal heat map, client score badges

### Week 9 — Operational depth
- Expense tracking + receipt OCR (UC-55)
- Project milestones + progress invoicing (UC-56)
- Internal approval workflows (UC-58)
- Frontend: expense list, project Kanban board, approval inbox

### Week 10 — Scale & reach
- Multi-currency support (UC-59)
- Team/workspace support (UC-61)
- Client portal (UC-60)
- Frontend: team management, portal branding

### Week 11 — Ecosystem & moat
- Contract templates + e-sign (UC-62)
- API & webhooks (UC-64)
- White-label custom domains (UC-65)
- Frontend: contract builder, webhook management, domain settings

### Week 12 — Marketplace preparation
- Full E2E test suite across all features
- Performance optimization (DB indexes, query tuning, caching)
- Security audit + penetration testing
- Demo data seeder (realistic 90-day dataset)
- Acquire.com listing assets (screenshots, MRR projections, tech docs)
- Customer journey video
- Final documentation + handoff README

---

## Subscription Tiers

| Feature | FREE | STARTER ($12/mo) | PRO ($29/mo) | ENTERPRISE ($59/mo) |
|---------|------|------------------|--------------|---------------------|
| Invoices/month | 5 | Unlimited | Unlimited | Unlimited |
| Clients | 3 | 50 | Unlimited | Unlimited |
| Proposals | Unlimited | Unlimited | Unlimited | Unlimited |
| Team members | 1 (solo) | 3 | 10 | Unlimited |
| Time tracking | ❌ | ✅ | ✅ | ✅ |
| Recurring invoices | ❌ | ✅ | ✅ | ✅ |
| Expense tracking | ❌ | ❌ | ✅ | ✅ |
| Multi-currency | ❌ | ❌ | ✅ | ✅ |
| Client portal | ❌ | ✅ | ✅ | ✅ |
| Approval workflows | ❌ | ❌ | ✅ | ✅ |
| White-label domain | ❌ | ❌ | ❌ | ✅ |
| API access | ❌ | ❌ | ❌ | ✅ |
| Webhooks | ❌ | ❌ | ✅ | ✅ |
| Custom contracts | ❌ | ❌ | ✅ | ✅ |
| Priority support | ❌ | ❌ | ✅ | ✅ |

---

## Definition of done

The project is complete when:
- All 65 use cases (UC-01 to UC-65) are implemented and manually verified end-to-end
- All backend unit and integration tests pass
- The app runs fully on Docker Compose with a single `docker compose up`
- A new user can: register → set up profile → add a client → create an invoice → send it → client pays online → owner sees it marked paid
- A new user can: create a proposal from template → send it → client signs it → convert to invoice
- A new user can: track time → bill hours → create recurring invoice → client pays → view cash flow forecast
- No hardcoded secrets anywhere in the codebase
- README includes setup instructions, environment variable reference, architecture overview, and API docs link
- Minimum 80% test coverage on domain and application layers
- Performance: page load < 2s, PDF generation < 3s, API response < 200ms (p95)