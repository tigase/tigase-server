create table tig_users (
       uid bigint unsigned NOT NULL,

       user_id varchar(128) NOT NULL,

       primary key (uid),
       unique key user_id (user_id)
)
default character set utf8;

create table tig_nodes (
       nid bigint unsigned NOT NULL,
       parent_nid bigint unsigned,
       uid bigint unsigned NOT NULL,

       node varchar(64) NOT NULL,

       primary key (nid),
       unique key tnode (parent_nid, node),
       key node (node)
)
default character set utf8;

create table tig_pairs (
       nid bigint unsigned,
       uid bigint unsigned NOT NULL,

       pkey varchar(128) NOT NULL,
       pval varchar(65535),

       key pkey (pkey)
)
default character set utf8;

create table tig_max_ids (
       max_uid bigint unsigned,
       max_nid bigint unsigned
)
default character set utf8;

insert into tig_users (uid, user_id)
       values (1, 'user1@hostname');


insert into tig_nodes (nid, parent_nid, uid, node)
       values (1, null, 1, 'root');
insert into tig_nodes (nid, parent_nid, uid, node)
       values (2, 1, 1, 'roster');
insert into tig_nodes (nid, parent_nid, uid, node)
       values (3, 2, 1, 'user2@hostname');

insert into tig_nodes (nid, parent_nid, uid, node)
       values (4, 2, 1, 'user3@hostname');

insert into tig_nodes (nid, parent_nid, uid, node)
       values (5, 1, 1, 'privacy');
insert into tig_nodes (nid, parent_nid, uid, node)
       values (6, 5, 1, 'default');
insert into tig_nodes (nid, parent_nid, uid, node)
       values (7, 6, 1, '24');
insert into tig_nodes (nid, parent_nid, uid, node)
       values (8, 6, 1, '34');
insert into tig_nodes (nid, parent_nid, uid, node)
       values (9, 6, 1, '44');

insert into tig_pairs (nid, uid, pkey, pval)
       values (3, 1, 'subscription', 'both');
insert into tig_pairs (nid, uid, pkey, pval)
       values (3, 1, 'groups', 'job');
insert into tig_pairs (nid, uid, pkey, pval)
       values (3, 1, 'groups', 'friends');
insert into tig_pairs (nid, uid, pkey, pval)
       values (3, 1, 'name', 'user1');
insert into tig_pairs (nid, uid, pkey, pval)
       values (4, 1, 'subscription', 'none');

insert into tig_pairs (nid, uid, pkey, pval)
       values (7, 1, 'type', 'jid');
insert into tig_pairs (nid, uid, pkey, pval)
       values (7, 1, 'value', 'spam@hotmail');
insert into tig_pairs (nid, uid, pkey, pval)
       values (7, 1, 'action', 'deny');

insert into tig_pairs (nid, uid, pkey, pval)
       values (8, 1, 'type', 'jid');
insert into tig_pairs (nid, uid, pkey, pval)
       values (8, 1, 'value', 'user1@hostname');
insert into tig_pairs (nid, uid, pkey, pval)
       values (8, 1, 'action', 'allow');

insert into tig_pairs (nid, uid, pkey, pval)
       values (9, 1, 'type', 'subscription');
insert into tig_pairs (nid, uid, pkey, pval)
       values (9, 1, 'value', 'none');
insert into tig_pairs (nid, uid, pkey, pval)
       values (9, 1, 'action', 'deny');
insert into tig_pairs (nid, uid, pkey, pval)
       values (9, 1, 'stanzas', 'message');
insert into tig_pairs (nid, uid, pkey, pval)
       values (9, 1, 'stanzas', 'iq');

insert into tig_max_ids (max_uid, max_nid) values (2, 10);

-- Get top nodes for the user: user1@hostname
--
-- select nid, node from nodes, users
--   where ('user1@hostname' = user_id)
--     AND (nodes.uid = users.uid)
--     AND (parent_nid is null);

-- Get all subnodes of the node: /privacy/default for user: user1@hostname
--
-- select nid, node from nodes,
-- (
--   select nid as dnid from nodes,
--   (
--     select nid as pnid from nodes, users
--       where ('user1@hostname' = user_id)
--         AND (nodes.uid = users.uid)
--         AND (parent_nid is null)
--         AND (node = 'privacy')
--   ) ptab where (parent_nid = pnid)
--       AND (node = 'default')
-- ) dtab where (parent_nid = dnid);

-- Get all keys (pairs) for the node: /privacy/default/24 for user: user1@hostname
--
-- select  pkey, pval from pairs,
-- (
--   select nid, node from nodes,
--   (
--     select nid as dnid from nodes,
--     (
--       select nid as pnid from nodes, users
--         where ('user1@hostname' = user_id)
--           AND (nodes.uid = users.uid)
--     	  AND (parent_nid is null)
--     	  AND (node = 'privacy')
--     ) ptab where (parent_nid = pnid)
--         AND (node = 'default')
--   ) dtab where (parent_nid = dnid)
-- ) ntab where (pairs.nid = ntab.nid) AND (node = '24');
