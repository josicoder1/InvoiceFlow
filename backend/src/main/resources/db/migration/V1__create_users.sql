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
  stripe_account_id VARCHAR(255),
  plan VARCHAR(50) DEFAULT 'FREE',
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);
