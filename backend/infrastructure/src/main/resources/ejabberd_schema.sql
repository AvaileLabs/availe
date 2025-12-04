CREATE TABLE IF NOT EXISTS users
(
    username       text NOT NULL,
    server_host    text NOT NULL,
    password       text NOT NULL,
    serverkey      text      DEFAULT '',
    salt           text      DEFAULT '',
    iterationcount integer   DEFAULT 0,
    created_at     timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (server_host, username)
);

CREATE TABLE IF NOT EXISTS last
(
    username    text NOT NULL,
    server_host text NOT NULL,
    seconds     text NOT NULL,
    state       text NOT NULL,
    PRIMARY KEY (server_host, username)
);

CREATE TABLE IF NOT EXISTS rosterusers
(
    username     text         NOT NULL,
    server_host  text         NOT NULL,
    jid          text         NOT NULL,
    nick         text         NOT NULL,
    subscription character(1) NOT NULL,
    ask          character(1) NOT NULL,
    askmessage   text         NOT NULL,
    server       character(1) NOT NULL,
    subscribe    text         NOT NULL,
    type         text,
    created_at   timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (server_host, username, jid)
);

CREATE TABLE IF NOT EXISTS rostergroups
(
    username    text NOT NULL,
    server_host text NOT NULL,
    jid         text NOT NULL,
    grp         text NOT NULL,
    PRIMARY KEY (server_host, username, jid, grp)
);

CREATE TABLE IF NOT EXISTS sr_group
(
    name        text NOT NULL,
    server_host text NOT NULL,
    opts        text NOT NULL,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (server_host, name)
);

CREATE TABLE IF NOT EXISTS sr_user
(
    jid         text NOT NULL,
    server_host text NOT NULL,
    grp         text NOT NULL,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (server_host, jid, grp)
);

CREATE TABLE IF NOT EXISTS vcard
(
    username    text NOT NULL,
    server_host text NOT NULL,
    vcard       text NOT NULL,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (server_host, username)
);

CREATE TABLE IF NOT EXISTS vcard_search
(
    username    text NOT NULL,
    lusername   text NOT NULL,
    fn          text NOT NULL,
    lfn         text NOT NULL,
    server_host text NOT NULL,
    jid         text NOT NULL,
    PRIMARY KEY (server_host, lusername)
);

CREATE TABLE IF NOT EXISTS privacy_list_data
(
    id                 bigserial PRIMARY KEY,
    username           text         NOT NULL,
    server_host        text         NOT NULL,
    name               text         NOT NULL,
    t                  character(1) NOT NULL,
    value              text         NOT NULL,
    action             character(1) NOT NULL,
    ord                numeric      NOT NULL,
    match_all          boolean      NOT NULL,
    match_iq           boolean      NOT NULL,
    match_message      boolean      NOT NULL,
    match_presence_in  boolean      NOT NULL,
    match_presence_out boolean      NOT NULL
);

CREATE TABLE IF NOT EXISTS private_storage
(
    username    text NOT NULL,
    server_host text NOT NULL,
    namespace   text NOT NULL,
    data        text NOT NULL,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (server_host, username, namespace)
);

CREATE TABLE IF NOT EXISTS muc_room
(
    name        text NOT NULL,
    host        text NOT NULL,
    server_host text NOT NULL,
    opts        text NOT NULL,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (server_host, name, host)
);

CREATE TABLE IF NOT EXISTS muc_registered
(
    jid         text NOT NULL,
    host        text NOT NULL,
    server_host text NOT NULL,
    nick        text NOT NULL,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (server_host, host, jid)
);

CREATE TABLE IF NOT EXISTS muc_online_room
(
    name        text NOT NULL,
    host        text NOT NULL,
    server_host text NOT NULL,
    node        text NOT NULL,
    pid         text NOT NULL,
    PRIMARY KEY (server_host, name, host)
);

CREATE TABLE IF NOT EXISTS muc_online_users
(
    username    text NOT NULL,
    server      text NOT NULL,
    resource    text NOT NULL,
    name        text NOT NULL,
    host        text NOT NULL,
    server_host text NOT NULL,
    node        text NOT NULL,
    PRIMARY KEY (server_host, username, server, resource, name, host)
);

CREATE TABLE IF NOT EXISTS muc_room_subscribers
(
    room        text NOT NULL,
    host        text NOT NULL,
    server_host text NOT NULL,
    jid         text NOT NULL,
    nick        text NOT NULL,
    nodes       text NOT NULL,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (server_host, room, host, jid)
);

CREATE TABLE IF NOT EXISTS motd
(
    username    text NOT NULL,
    server_host text NOT NULL,
    xml         text,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (server_host, username)
);

CREATE TABLE IF NOT EXISTS caps_features
(
    node       text NOT NULL,
    subnode    text NOT NULL,
    feature    text,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (node, subnode, feature)
);

CREATE TABLE IF NOT EXISTS sm
(
    usec        bigint NOT NULL,
    pid         text   NOT NULL,
    node        text   NOT NULL,
    username    text   NOT NULL,
    server_host text   NOT NULL,
    resource    text   NOT NULL,
    priority    text   NOT NULL,
    info        text   NOT NULL,
    PRIMARY KEY (usec, pid)
);

CREATE TABLE IF NOT EXISTS route
(
    domain      text NOT NULL,
    server_host text NOT NULL,
    node        text NOT NULL,
    pid         text NOT NULL,
    local_hint  text NOT NULL,
    PRIMARY KEY (domain, server_host, node, pid, local_hint)
);

CREATE TABLE IF NOT EXISTS archive
(
    username    text   NOT NULL,
    server_host text   NOT NULL,
    timestamp   bigint NOT NULL,
    peer        text   NOT NULL,
    bare_peer   text   NOT NULL,
    xml         text   NOT NULL,
    txt         text,
    id          bigserial,
    kind        text,
    nick        text,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    origin_id   text      DEFAULT '',
    PRIMARY KEY (server_host, username, timestamp, peer)
);

CREATE TABLE IF NOT EXISTS archive_prefs
(
    username    text NOT NULL,
    server_host text NOT NULL,
    def         text NOT NULL,
    always      text NOT NULL,
    never       text NOT NULL,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (server_host, username)
);