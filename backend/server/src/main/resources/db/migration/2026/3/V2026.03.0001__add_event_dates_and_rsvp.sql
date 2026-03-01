truncate table listings cascade;

drop table listing_event_details cascade;

create table listing_event_details
(
    listing_id uuid not null
        primary key
        constraint fk_listing_event_details_listing_id
            references listings
            on delete cascade,
    start_time timestamp with time zone not null
);

create table event_rsvps
(
    user_id uuid not null
        constraint fk_event_rsvps_user_id
            references users
            on delete cascade,
    listing_id uuid not null
        constraint fk_event_rsvps_listing_id
            references listing_event_details
            on delete cascade,
    created_at timestamp with time zone default clock_timestamp() not null,
    primary key (user_id, listing_id)
);