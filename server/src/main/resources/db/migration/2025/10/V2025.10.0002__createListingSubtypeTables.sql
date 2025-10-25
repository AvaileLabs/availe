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