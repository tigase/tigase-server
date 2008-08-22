-- Database schema upgrade for Tigase version 4.0.0

alter table tig_users modify user_id varchar(256) not null;
alter table tig_nodes modify node varchar(128) not null;
alter table tig_pairs modify pval mediumtext;

