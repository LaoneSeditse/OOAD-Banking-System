-- =====================================================================
-- OOAD Banking System - database schema (H2, MySQL-compatible types)
-- Tables map to the Class Diagram: Customer (Individual/Company),
-- Account (Savings/Investment/Cheque), EmploymentDetails, Transaction,
-- BankAdministrator, plus login sessions (Login sequence diagram).
-- =====================================================================

CREATE SEQUENCE IF NOT EXISTS customer_seq START WITH 1001;
CREATE SEQUENCE IF NOT EXISTS account_seq START WITH 1000001;

CREATE TABLE IF NOT EXISTS customers (
    customer_id           VARCHAR(20)   PRIMARY KEY,
    customer_type         VARCHAR(20)   NOT NULL CHECK (customer_type IN ('INDIVIDUAL', 'COMPANY')),
    username              VARCHAR(50)   NOT NULL UNIQUE,
    password_hash         VARCHAR(128)  NOT NULL,
    date_of_birth         DATE          NOT NULL,
    first_name            VARCHAR(60),
    last_name             VARCHAR(60),
    id_number             VARCHAR(30),
    company_name          VARCHAR(120),
    registration_number   VARCHAR(40),
    postal_address        VARCHAR(200)  NOT NULL,
    physical_address      VARCHAR(200)  NOT NULL,
    social_income         DECIMAL(15,2) DEFAULT 0 NOT NULL,
    is_active             BOOLEAN       DEFAULT TRUE NOT NULL,
    status                VARCHAR(10)   DEFAULT 'ACTIVE' NOT NULL CHECK (status IN ('ACTIVE', 'LOCKED')),
    failed_login_attempts INT           DEFAULT 0 NOT NULL,
    created_at            TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS accounts (
    account_number VARCHAR(20)   PRIMARY KEY,
    customer_id    VARCHAR(20)   NOT NULL,
    account_type   VARCHAR(20)   NOT NULL CHECK (account_type IN ('SAVINGS', 'INVESTMENT', 'CHEQUE')),
    balance        DECIMAL(15,2) DEFAULT 0 NOT NULL,
    branch         VARCHAR(60)   NOT NULL,
    status         VARCHAR(15)   NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'CLOSED')),
    interest_rate  DECIMAL(8,5)  NOT NULL,
    opened_date    DATE          NOT NULL,
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS employment_details (
    account_number  VARCHAR(20)  PRIMARY KEY,
    company_name    VARCHAR(120) NOT NULL,
    company_address VARCHAR(200) NOT NULL,
    position        VARCHAR(80)  NOT NULL,
    CONSTRAINT fk_employment_account FOREIGN KEY (account_number)
        REFERENCES accounts (account_number) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id   VARCHAR(20)   PRIMARY KEY,
    account_number   VARCHAR(20)   NOT NULL,
    transaction_date TIMESTAMP     NOT NULL,
    amount           DECIMAL(15,2) NOT NULL,
    transaction_type VARCHAR(15)   NOT NULL CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'INTEREST')),
    description      VARCHAR(255),
    balance_after    DECIMAL(15,2) NOT NULL,
    status           VARCHAR(15)   NOT NULL CHECK (status IN ('INITIATED', 'VALIDATED', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_number)
        REFERENCES accounts (account_number) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions (account_number);

CREATE TABLE IF NOT EXISTS sessions (
    token       VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_session_customer FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS administrators (
    admin_id      VARCHAR(20)  PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(120) NOT NULL,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL
);
