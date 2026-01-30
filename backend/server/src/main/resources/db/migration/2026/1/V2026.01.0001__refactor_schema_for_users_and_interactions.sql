drop table listing_item_details;
drop table listing_event_details;
drop table listing_service_details;
drop table listings;

create table users
(
    id           uuid primary key,
    email        text        not null unique,
    display_name text        not null,
    avatar_url   text,
    jid          text,
    created_at   timestamptz not null default clock_timestamp()
);

create type interaction_type_enum as enum ('favorite');

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
    image_links    text[]            not null default '{}',
    location       text              not null,
    constraint fk_listings_publisher_id
        foreign key (publisher_id) references users (id) on delete cascade
);

create table listing_interactions
(
    user_id          uuid                  not null,
    listing_id       uuid                  not null,
    interaction_type interaction_type_enum not null,
    created_at       timestamptz           not null default clock_timestamp(),
    primary key (user_id, listing_id, interaction_type),
    constraint fk_listing_interactions_user_id
        foreign key (user_id) references users (id) on delete cascade,
    constraint fk_listing_interactions_listing_id
        foreign key (listing_id) references listings (id) on delete cascade
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