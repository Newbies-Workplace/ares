alter table Events
    drop column secondaryColor,
    drop column city,
    modify finishDate datetime(6) not null;

alter table Lectures
    modify finishDate datetime(6) not null;
