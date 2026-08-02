-- ══════════════════════════════════════════════════════════════════════════
-- Esquema explícito para tests (H2). Espejo manual de:
--   banquito-infra/db/04-schema-accountingdb.sql
--   banquito-infra/db/13-schema-correspondent-bank.sql
-- accounting_rule / accounting_rule_line / accounting_parameter no tienen
-- todavía un .sql de infra (hoy nacen de ddl-auto=update en producción);
-- se declaran aquí replicando exactamente las columnas de sus @Entity.
-- ddl-auto=none: Hibernate ya NO genera tablas, solo hace ORM sobre estas.
-- ══════════════════════════════════════════════════════════════════════════

CREATE TABLE accounting_account (
    account_code         VARCHAR(20)   PRIMARY KEY,
    name                 VARCHAR(100)  NOT NULL,
    account_class        VARCHAR(15)   NOT NULL,
    account_type         VARCHAR(15)   NOT NULL CHECK (account_type IN ('ESTRUCTURAL','DETALLE')),
    parent_account_code  VARCHAR(20)   REFERENCES accounting_account(account_code),
    current_balance      DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    creation_date        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_aa_parent ON accounting_account(parent_account_code);

CREATE TABLE journal_entry (
    id                   BIGINT        AUTO_INCREMENT PRIMARY KEY,
    entry_uuid           VARCHAR(36)   NOT NULL UNIQUE,
    description          VARCHAR(255)  NOT NULL,
    source_account_number      VARCHAR(20),
    destination_account_number VARCHAR(20),
    beneficiary_name           VARCHAR(150),
    entry_date           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status               VARCHAR(15)   NOT NULL DEFAULT 'REGISTRADO' CHECK (status IN ('REGISTRADO','ANULADO')),
    reversal_of_entry_id BIGINT        REFERENCES journal_entry(id)
);

CREATE INDEX idx_je_uuid ON journal_entry(entry_uuid);
CREATE INDEX idx_je_date ON journal_entry(entry_date);
CREATE INDEX idx_je_reversal ON journal_entry(reversal_of_entry_id);

CREATE TABLE journal_entry_line (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    journal_entry_id BIGINT       NOT NULL,
    account_code     VARCHAR(20)  NOT NULL,
    movement_type    VARCHAR(10)  NOT NULL CHECK (movement_type IN ('DEBITO','CREDITO')),
    amount           DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    reference        VARCHAR(255)
);

CREATE INDEX idx_jel_entry   ON journal_entry_line(journal_entry_id);
CREATE INDEX idx_jel_account ON journal_entry_line(account_code);

CREATE TABLE accounting_parameter (
    param_key    VARCHAR(50)   PRIMARY KEY,
    param_value  VARCHAR(100)  NOT NULL
);

CREATE TABLE accounting_rule (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    operation_type VARCHAR(50)   NOT NULL,
    description    VARCHAR(200),
    effective_from DATE          NOT NULL,
    effective_to   DATE
);

CREATE TABLE accounting_rule_line (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    rule_id          BIGINT       NOT NULL REFERENCES accounting_rule(id),
    line_order       INT          NOT NULL,
    account_code     VARCHAR(20)  NOT NULL,
    movement_type    VARCHAR(10)  NOT NULL,
    amount_component VARCHAR(25)  NOT NULL,
    skip_if_zero     BOOLEAN      NOT NULL DEFAULT TRUE
);

-- RF Nostro/Vostro — Fase 1
CREATE SEQUENCE correspondent_bank_account_seq START WITH 1;

CREATE TABLE correspondent_bank (
    bank_code            VARCHAR(10)   PRIMARY KEY,
    bank_name            VARCHAR(100)  NOT NULL,
    nostro_account_code  VARCHAR(20)   NOT NULL UNIQUE REFERENCES accounting_account(account_code),
    vostro_account_code  VARCHAR(20)   NOT NULL UNIQUE REFERENCES accounting_account(account_code),
    currency             VARCHAR(3)    NOT NULL DEFAULT 'USD',
    status               VARCHAR(15)   NOT NULL DEFAULT 'ACTIVO' CHECK (status IN ('ACTIVO','INACTIVO')),
    creation_date        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cb_status ON correspondent_bank(status);
