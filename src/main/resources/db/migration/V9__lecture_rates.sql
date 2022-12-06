alter table Lectures
    drop constraint fk_Lectures_author__id,
    drop constraint fk_Lectures_event__id;

alter table Lectures
    add constraint fk_Lectures_author__id
        foreign key (author) references Users (id)
            on delete cascade,
    add constraint fk_Lectures_event__id
        foreign key (event) references Events (id)
            on delete cascade;

create table LectureRates
(
    id varchar(36) not null primary key,
    lecture varchar(36) not null,
    topicRate integer not null,
    presentationRate integer not null,
    opinion text collate utf8mb4_unicode_ci null,
    createDate datetime(6) not null,
    constraint fk_LectureRates_lecture__id
        foreign key (lecture) references Lectures (id)
);