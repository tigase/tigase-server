-- Database schema upgrade for Tigase version 4.0.0

alter table tig_users drop index user_id;
alter table tig_users add index user_id (user_id(765));
alter table tig_users modify user_id varchar(2048) not null;
alter table tig_nodes modify node varchar(128) not null;
alter table tig_pairs modify pval mediumtext;

create table if not exists tig_properties (
       tkey varchar(255) NOT NULL,
       tval text NOT NULL,

       primary key (tkey)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;

insert ignore into tig_properties (tkey, tval) values ('schema-version', '4.0');
