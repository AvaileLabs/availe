drop table listing_item_details;
drop table listing_event_details;
drop table listing_service_details;
drop table listings;

create table listings
(
    id              uuid primary key           default uuidv7(),
    created_at      timestamptz       not null default clock_timestamp(),
    publisher_id    uuid              not null,
    publisher_email text              not null,
    listing_type    listing_type_enum not null,
    title           text              not null,
    description     text,
    price_amount    decimal(19, 4)    not null,
    price_currency  char(3)           not null,
    image_links     text[]            not null default '{}'
);

create table listing_item_details
(
    listing_id uuid primary key,
    constraint fk_listing_item_details_listing_id
        foreign key (listing_id) references listings (id) on delete cascade
);

create table listing_event_details
(
    listing_id uuid primary key,
    constraint fk_listing_event_details_listing_id
        foreign key (listing_id) references listings (id) on delete cascade
);

create table listing_service_details
(
    listing_id uuid primary key,
    constraint fk_listing_service_details_listing_id
        foreign key (listing_id) references listings (id) on delete cascade
);