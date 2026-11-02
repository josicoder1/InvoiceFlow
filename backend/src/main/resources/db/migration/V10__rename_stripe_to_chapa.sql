ALTER TABLE users RENAME COLUMN stripe_account_id TO chapa_merchant_key;
ALTER TABLE invoices RENAME COLUMN stripe_payment_link TO chapa_payment_link;
ALTER TABLE invoices RENAME COLUMN stripe_checkout_session_id TO chapa_checkout_session_id;
ALTER TABLE invoices RENAME COLUMN stripe_payment_intent_id TO chapa_tx_ref;
