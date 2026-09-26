create table webhook_event (
    id bigint generated always as identity primary key,
    provider varchar(32) not null,
    event_id varchar(64) not null,
    event_type varchar(32) not null,
    payment_reference varchar(64) not null,
    amount_minor bigint not null,
    currency char(3) not null,
    -- text keeps the payload bytes exactly as received; jsonb would reorder and reformat them
    raw_payload text not null,
    received_at timestamptz not null,
    status varchar(32) not null,
    constraint uq_webhook_event_provider_event_id unique (provider, event_id)
);
