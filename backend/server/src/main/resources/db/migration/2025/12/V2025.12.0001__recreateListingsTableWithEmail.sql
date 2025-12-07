DROP TABLE IF EXISTS listing_item_details;
DROP TABLE IF EXISTS listing_event_details;
DROP TABLE IF EXISTS listing_service_details;
DROP TABLE IF EXISTS listings;

CREATE TABLE listings
(
    id               uuid primary key          default uuidv7(),
    created_at       timestamptz      not null default clock_timestamp(),
    publisher_id     uuid             not null,
    publisher_email  text             not null,
    listing_type     listing_type_enum not null,
    title            text             not null,
    description      text,
    price_amount     decimal(19, 4)   not null,
    price_currency   char(3)          not null,
    image_links      text[]           not null default '{}'
);

CREATE TABLE listing_item_details
(
    listing_id uuid primary key,
    constraint fk_listing_item_details_listing_id
        foreign key (listing_id) references listings (id) on delete cascade
);

CREATE TABLE listing_event_details
(
    listing_id uuid primary key,
    constraint fk_listing_event_details_listing_id
        foreign key (listing_id) references listings (id) on delete cascade
);

CREATE TABLE listing_service_details
(
    listing_id uuid primary key,
    constraint fk_listing_service_details_listing_id
        foreign key (listing_id) references listings (id) on delete cascade
);