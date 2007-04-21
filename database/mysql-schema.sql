create table xmpp_stanza (
			 id bigint unsigned NOT NULL auto_increment,
			 stanza text NOT NULL,

			 primary key (id)
)
default character set utf8;

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

insert into tig_max_ids (max_uid, max_nid) values (1, 1);

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
