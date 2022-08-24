alter table EventFollows
    drop constraint fk_EventFollows_event__id,
    drop constraint fk_EventFollows_user__id;

alter table EventFollows
    add constraint fk_EventFollows_event__id
        foreign key (event) references Events (id)
            on delete cascade,
    add constraint fk_EventFollows_user__id
        foreign key (user) references Users (id)
            on delete cascade;