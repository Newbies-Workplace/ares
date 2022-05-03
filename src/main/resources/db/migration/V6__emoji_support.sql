ALTER TABLE Users
    MODIFY COLUMN nickname varchar(50) not null COLLATE utf8mb4_unicode_ci,
    MODIFY COLUMN description varchar(255) null COLLATE utf8mb4_unicode_ci;

ALTER TABLE Events
    MODIFY COLUMN title varchar(100) not null COLLATE utf8mb4_unicode_ci,
    MODIFY COLUMN subtitle varchar(100) null COLLATE utf8mb4_unicode_ci,
    MODIFY COLUMN description text null COLLATE utf8mb4_unicode_ci;