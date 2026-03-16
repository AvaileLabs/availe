drop table if exists event_rsvps;

drop table if exists listing_event_details;

create table listing_event_details
(
    listing_id           uuid                        not null
        primary key
        constraint foreign_key_listing_event_details_listing_identifier
            references listings
            on delete cascade,
    start_local_time     timestamp without time zone not null,
    end_local_time       timestamp without time zone not null,
    start_instant        timestamp with time zone    not null,
    end_instant          timestamp with time zone    not null,
    time_zone_identifier text                        not null
);

create table event_rsvps
(
    user_id    uuid                                               not null
        constraint foreign_key_event_rsvps_user_identifier
            references users
            on delete cascade,
    listing_id uuid                                               not null
        constraint foreign_key_event_rsvps_listing_identifier
            references listing_event_details
            on delete cascade,
    created_at timestamp with time zone default clock_timestamp() not null,
    primary key (user_id, listing_id)
);