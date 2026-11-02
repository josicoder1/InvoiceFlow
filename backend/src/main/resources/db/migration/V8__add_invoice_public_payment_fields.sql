ALTER TABLE invoices
  ADD COLUMN public_token VARCHAR(255) UNIQUE,
  ADD COLUMN stripe_checkout_session_id VARCHAR(255),
  ADD COLUMN stripe_payment_intent_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_invoices_public_token ON invoices(public_token);
