ALTER TABLE proposals
    ADD COLUMN proposal_number VARCHAR(50),
    ADD COLUMN discount_type VARCHAR(20),
    ADD COLUMN discount_value NUMERIC(15,2) DEFAULT 0,
    ADD COLUMN tax_rate NUMERIC(5,2) DEFAULT 0,
    ADD COLUMN subtotal NUMERIC(15,2) DEFAULT 0,
    ADD COLUMN total NUMERIC(15,2) DEFAULT 0,
    ADD COLUMN converted_to_invoice_id UUID REFERENCES invoices(id);

UPDATE proposals SET public_token = gen_random_uuid()::text WHERE public_token IS NULL;
ALTER TABLE proposals ALTER COLUMN public_token SET NOT NULL;

CREATE INDEX idx_proposals_proposal_number ON proposals(user_id, proposal_number);
