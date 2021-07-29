create table LINK_TO_BE_PROCESSED
(
	ID bigint auto_increment primary key,
	link varchar(1024),
	CREATED_AT timestamp default now() not null,
	MODIFY_AT timestamp default now() not null
);

create table LINK_ALREADY_PROCESSED
(
	ID bigint auto_increment primary key,
	link varchar(1024),
	CREATED_AT timestamp default now() not null,
	MODIFY_AT timestamp default now() not null
);

create table NEWS
(
	ID bigint auto_increment primary key,
	TITLE text,
	CONTENT text,
	URL varchar(1024),
	CREATED_AT timestamp default now() not null,
	MODIFY_AT timestamp default now() not null
);