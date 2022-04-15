alter table Events
    add column vanityUrl varchar(50) not null default 'title',
    modify place varchar(100) null;