alter table EventTags
    drop constraint fk_EventTags_event__id,
    drop constraint fk_EventTags_tag__id;

alter table EventTags
    add constraint fk_EventTags_event__id
        foreign key (event) references Events (id)
            on delete cascade,
    add constraint fk_EventTags_tag__id
        foreign key (tag) references Tags (id)
            on delete cascade;
