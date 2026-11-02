CREATE TABLE proposals (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  client_id UUID NOT NULL REFERENCES clients(id),
  title VARCHAR(255) NOT NULL,
  status VARCHAR(50) DEFAULT 'DRAFT',   -- DRAFT, SENT, VIEWED, ACCEPTED, DECLINED
  public_token VARCHAR(255) UNIQUE,     -- used for the shareable link
  overview TEXT,
  scope TEXT,
  timeline TEXT,
  pricing_notes TEXT,
  valid_until DATE,
  signature_data TEXT,                  -- base64 e-signature image
  signed_at TIMESTAMP,
  viewed_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now(),
  version INTEGER DEFAULT 0
);

CREATE TABLE proposal_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  proposal_id UUID NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
  description TEXT NOT NULL,
  quantity NUMERIC(10,2),
  unit_price NUMERIC(15,2),
  total NUMERIC(15,2),
  position INTEGER NOT NULL
);
