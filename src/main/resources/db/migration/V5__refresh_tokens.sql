truncate table RefreshTokens;

alter table RefreshTokens
    add column family varchar(36) not null,
    add column isUsed boolean not null,
    add column dateExpired datetime(6) not null;

