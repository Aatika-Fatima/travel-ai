create table notification_request(
    id bigserial primary key ,
    notification_id UUID not null unique,
    channel varchar(16) not null,
    template varchar(64) not null,
    recipient varchar(255) not null,
    created_at timestamptz not null default now()
);

create table email_delivery(
    id bigserial primary key,
    email_id uuid not null unique ,
    status varchar(16) not null,
    failure_reason text
);