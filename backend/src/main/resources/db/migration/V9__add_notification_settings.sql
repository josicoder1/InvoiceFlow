ALTER TABLE users
  ADD COLUMN reminder_days_before_due INTEGER NOT NULL DEFAULT 3,
  ADD COLUMN reminder_days_after_due INTEGER NOT NULL DEFAULT 7,
  ADD COLUMN reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE invoices
  ADD COLUMN last_reminder_sent_at TIMESTAMP;
