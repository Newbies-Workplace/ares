alter table Events
    add column vanityUrl varchar(50) not null;

alter table Events
    add unique index vanityUrl_unique(vanityUrl);