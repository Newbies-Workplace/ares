# rename old event foreign key
alter table Events
    drop foreign key fk_Lectures_author__id,
    add constraint fk_Events_author__id
    foreign key (author) references Users (id);

create table Lectures
(
    id             varchar(36)                  not null
        primary key,
    event          varchar(36)                  not null,
    title          varchar(100) collate utf8mb4_unicode_ci not null,
    description    text collate utf8mb4_unicode_ci null,
    author         varchar(36)                  not null,
    startDate      datetime(6)                  not null,
    finishDate     datetime(6)                  null,
    createDate     datetime(6)                  not null,
    updateDate     datetime(6)                  not null,
    constraint fk_Lectures_author__id
        foreign key (author) references Users (id),
    constraint fk_Lectures_event__id
        foreign key (event) references Events (id)
);

create table LectureSpeakers
(
    lecture varchar(36) not null,
    user    varchar(36) not null,
    primary key (lecture, user),
    constraint fk_LectureSpeakers_lecture__id
        foreign key (lecture) references Lectures (id)
            on delete cascade,
    constraint fk_LectureSpeakers_user__id
        foreign key (user) references Users (id)
);