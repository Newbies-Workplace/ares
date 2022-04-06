create table RefreshTokens
(
    id          varchar(36) not null
        primary key,
    userId      varchar(36) not null,
    dateCreated datetime(6) not null
);

create table Tags
(
    id   varchar(36)                 not null
        primary key,
    name varchar(50) charset utf8mb3 not null,
    constraint Tags_name_unique
        unique (name)
);

create table Users
(
    id              varchar(36)                  not null
        primary key,
    nickname        varchar(50) charset utf8mb3  not null,
    description     varchar(255) charset utf8mb3 null,
    githubId        varchar(30)                  null,
    contactGithub   varchar(50) charset utf8mb3  null,
    contactLinkedin varchar(50) charset utf8mb3  null,
    contactMail     varchar(50) charset utf8mb3  null,
    contactTwitter  varchar(50) charset utf8mb3  null,
    createDate      datetime(6)                  not null,
    updateDate      datetime(6)                  not null,
    devGithubId     varchar(30)                  null
);

create table Events
(
    id             varchar(36)                  not null
        primary key,
    title          varchar(100) charset utf8mb3 not null,
    subtitle       varchar(100) charset utf8mb3 null,
    author         varchar(36)                  not null,
    startDate      datetime(6)                  not null,
    finishDate     datetime(6)                  null,
    city           varchar(50) charset utf8mb3  null,
    place          varchar(50) charset utf8mb3  null,
    latitude       double                       null,
    longitude      double                       null,
    createDate     datetime(6)                  not null,
    updateDate     datetime(6)                  not null,
    primaryColor   varchar(7)                   null,
    secondaryColor varchar(7)                   null,
    image          varchar(100)                 null,
    constraint fk_Lectures_author__id
        foreign key (author) references Users (id)
);

create table EventFollows
(
    id         varchar(36) not null
        primary key,
    event    varchar(36) not null,
    user       varchar(36) not null,
    followDate datetime(6) not null,
    constraint EventFollows_event_user_unique
        unique (event, user),
    constraint fk_EventFollows_event__id
        foreign key (event) references Events (id),
    constraint fk_EventFollows_user__id
        foreign key (user) references Users (id)
);

create table EventTags
(
    event varchar(36) not null,
    tag     varchar(36) not null,
    primary key (event, tag),
    constraint fk_EventTags_event__id
        foreign key (event) references Events (id),
    constraint fk_EventTags_tag__id
        foreign key (tag) references Tags (id)
);

create table FollowedTags
(
    id   varchar(36) not null
        primary key,
    user varchar(36) not null,
    tag  varchar(36) not null,
    constraint fk_FollowedTags_tag__id
        foreign key (tag) references Tags (id)
            on delete cascade,
    constraint fk_FollowedTags_user__id
        foreign key (user) references Users (id)
);

