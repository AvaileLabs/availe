drop table event_rsvps cascade;
drop table listing_service_details cascade;
drop table listing_event_details cascade;
drop table listing_item_details cascade;
drop table listing_interactions cascade;
drop table listings cascade;
drop table users cascade;

create table users
(
    id           uuid primary key,
    email        text                     not null unique,
    display_name text                     not null,
    avatar_url   text,
    jid          text                     not null unique,
    created_at   timestamp with time zone not null default clock_timestamp()
);

create table listings
(
    id             uuid primary key                  default uuidv7(),
    created_at     timestamp with time zone not null default clock_timestamp(),
    publisher_id   uuid                     not null,
    listing_type   listing_type_enum        not null,
    title          text                     not null,
    description    text,
    price_amount   numeric(19, 4)           not null,
    price_currency char(3)                  not null,
    image_links    text[]                   not null default '{}',
    location       text                     not null,
    constraint foreign_key_listings_publisher_identifier
        foreign key (publisher_id) references users (id) on delete cascade
);

create table listing_interactions
(
    user_id          uuid                     not null,
    listing_id       uuid                     not null,
    interaction_type interaction_type_enum    not null,
    created_at       timestamp with time zone not null default clock_timestamp(),
    primary key (user_id, listing_id, interaction_type),
    constraint foreign_key_listing_interactions_user_identifier
        foreign key (user_id) references users (id) on delete cascade,
    constraint foreign_key_listing_interactions_listing_identifier
        foreign key (listing_id) references listings (id) on delete cascade
);

create table listing_item_details
(
    listing_id uuid primary key,
    constraint foreign_key_listing_item_details_listing_identifier
        foreign key (listing_id) references listings (id) on delete cascade
);

create table listing_event_details
(
    listing_id uuid primary key,
    start_time timestamp with time zone not null,
    constraint foreign_key_listing_event_details_listing_identifier
        foreign key (listing_id) references listings (id) on delete cascade
);

create table listing_service_details
(
    listing_id uuid primary key,
    constraint foreign_key_listing_service_details_listing_identifier
        foreign key (listing_id) references listings (id) on delete cascade
);

create table event_rsvps
(
    user_id    uuid                     not null,
    listing_id uuid                     not null,
    created_at timestamp with time zone not null default clock_timestamp(),
    primary key (user_id, listing_id),
    constraint foreign_key_event_rsvps_user_identifier
        foreign key (user_id) references users (id) on delete cascade,
    constraint foreign_key_event_rsvps_listing_identifier
        foreign key (listing_id) references listing_event_details (listing_id) on delete cascade
);