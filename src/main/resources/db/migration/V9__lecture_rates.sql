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