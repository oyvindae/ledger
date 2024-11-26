create table if not exists bananas
(
    id    bigserial primary key not null,
    title TEXT
);

-- TODO: One API for pending and one for settled transactions

-- assume only customer ledger for now, not considering organizations, payment gateways etc

-- assume same region/currency for now

-- TODO: Triggers and functions for checking integrity
-- TODO: ADding a ledger now is just adding a name and you get one pending and one settled
-- create table if not exists accounts
-- (
--     id         uuid primary key not null,
--     created_at timestamp without time zone,
--     version    integer          not null,
--     code       text             not null unique,
--     name       text             not null
--
-- --     type              text             not null,
-- );
--
-- create table if not exists ledger_accounts
-- (
--     id         uuid primary key not null,
--     created_at timestamp without time zone,
--     version    integer          not null,
--     account_id uuid             not null references accounts (id),
--     pending    boolean          not null,
--     credit     numeric(10, 2)   not null,
--     debit      numeric(10, 2)   not null,
--     ledger     text             not null,
--     unique (account_id, ledger, pending)
-- );

create table if not exists accounts
(
    id             uuid primary key not null,
    created_at     timestamp without time zone,
    version        integer          not null,
    code           text             not null unique,
    name           text             not null,
    ledger         text             not null,
    pending_credit numeric(10, 2)   not null,
    pending_debit  numeric(10, 2)   not null,
    settled_credit numeric(10, 2)   not null,
    settled_debit  numeric(10, 2)   not null
);


create table if not exists transactions
(
    id                      uuid primary key not null,
    created_at              timestamp without time zone,
    version                 integer          not null,
    reversal                boolean,
    settled                 boolean          not null,
    external_transaction_id text             not null UNIQUE,
    parent_transaction_id   uuid references transactions (id)
);

-- assume reversals have transaction id

create table if not exists pending_transaction_entries
(
    id                 uuid primary key not null,
    created_at         timestamp without time zone,
    account_id         uuid             not null references accounts (id),
    transaction_id     uuid             not null references transactions (id),
    amount_signed      numeric(10, 2)   not null,
    sequence           integer          not null,
    preceding_entry_id uuid references pending_transaction_entries (id) on delete cascade,
    type               text             not null,
    ledger             text             not null,
    reverses_entry_id  uuid references pending_transaction_entries (id)
);

create table if not exists settled_transaction_entries
(
    id                 uuid primary key not null,
    created_at         timestamp without time zone,
    account_id         uuid             not null references accounts (id),
    transaction_id     uuid             not null references transactions (id),
    amount_signed      numeric(10, 2)   not null,
    sequence           integer          not null,
    preceding_entry_id uuid references settled_transaction_entries (id),
    type               text             not null,
    ledger             text             not null
)
--
-- CREATE TABLE otp_credentials
-- (
--     id                UUID  DEFAULT gen_random_uuid()              NOT NULL,
--     login_identity_id UUID references login_identities (id)
--         CONSTRAINT unique_otp_credentials_login_identity_id UNIQUE NOT NULL,
--     enabled           BOOLEAN                                      NOT NULL,
--     created_at        timestamp without time zone                  NOT NULL,
--     updated_at        timestamp without time zone                  NOT NULL,
--     encrypted_secret  jsonb DEFAULT '{}'::jsonb                    NOT NULL,
--     PRIMARY KEY (id)
-- );
