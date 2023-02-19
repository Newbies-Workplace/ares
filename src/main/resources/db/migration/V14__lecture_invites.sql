create table LectureInvites
(
    id varchar(36) not null primary key,
    lecture varchar(36) not null,
    name varchar(50) collate utf8mb4_unicode_ci not null,
    createDate datetime(6) not null,
    constraint fk_LectureInvites_lecture__id
        foreign key (lecture) references Lectures (id)
            on delete cascade
);