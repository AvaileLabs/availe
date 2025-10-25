create type listing_type_enum as enum (
    'item',
    'event',
    'service'
    );

create table listings
(
    id             uuid primary key           default uuidv7(),
    created_at     timestamptz       not null default clock_timestamp(),
    publisher_id   uuid              not null,
    listing_type   listing_type_enum not null,
    title          text              not null,
    description    text,
    price_amount   decimal(19, 4)    not null,
    price_currency char(3)           not null,
    image_links    text[]            not null default '{}'
)