-- assume same currency

create table if not exists accounts
(
    id             uuid primary key not null,
    created_at     timestamp without time zone,
    version        integer          not null,
    code           text             not null unique,
    name           text             not null,
    ledger         text             not null
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

create table if not exists pending_transaction_entries
(
    id                  uuid primary key not null,
    created_at          timestamp without time zone,
    account_id          uuid             not null references accounts (id),
    transaction_id      uuid             not null references transactions (id),
    main_transaction_id uuid             not null references transactions (id),
    amount_signed       numeric(10, 2)   not null,
    sequence            integer          not null,
    preceding_entry_id  uuid references pending_transaction_entries (id) on delete cascade,
    type                text             not null,
    ledger              text             not null,
    reverses_entry_id   uuid references pending_transaction_entries (id)
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
);

-- BELOW are triggers and functions that ensure that ledgers are balanced

-- Ensure pending transaction entries are balanced
CREATE FUNCTION validate_pending_transaction_balance() RETURNS TRIGGER AS
$$
BEGIN
    IF (SELECT COALESCE(SUM(abs(amount_signed)), 0)
        FROM pending_transaction_entries
        WHERE transaction_id = NEW.transaction_id
          AND amount_signed < 0) !=
       (SELECT COALESCE(SUM(abs(amount_signed)), 0)
        FROM pending_transaction_entries
        WHERE transaction_id = NEW.transaction_id
          AND amount_signed > 0) THEN
        RAISE EXCEPTION 'Transaction is unbalanced';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_validate_pending_balance
    AFTER INSERT OR UPDATE
    ON pending_transaction_entries
    FOR EACH STATEMENT
EXECUTE FUNCTION validate_pending_transaction_balance();


-- Ensure settled transaction entries are balanced
CREATE FUNCTION validate_settled_transaction_balance() RETURNS TRIGGER AS
$$
BEGIN
    IF (SELECT COALESCE(SUM(abs(amount_signed)), 0)
        FROM settled_transaction_entries
        WHERE transaction_id = NEW.transaction_id
          AND amount_signed < 0) !=
       (SELECT COALESCE(SUM(abs(amount_signed)), 0)
        FROM settled_transaction_entries
        WHERE transaction_id = NEW.transaction_id
          AND amount_signed > 0) THEN
        RAISE EXCEPTION 'Transaction is unbalanced';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_validate_settled_balance
    AFTER INSERT OR UPDATE
    ON settled_transaction_entries
    FOR EACH STATEMENT
EXECUTE FUNCTION validate_settled_transaction_balance();
