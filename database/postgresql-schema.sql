create table tig_users (
       uid bigint NOT NULL,

       user_id varchar(128) NOT NULL,

       primary key (uid)
);
create unique index user_id on tig_users ( user_id );

create table tig_nodes (
       nid bigint NOT NULL,
       parent_nid bigint,
       uid bigint NOT NULL,

       node varchar(64) NOT NULL,

       primary key (nid)
);
create unique index tnode on tig_nodes ( parent_nid, node );
create index node on tig_nodes ( node );

create table tig_pairs (
       nid bigint,
       uid bigint NOT NULL,

       pkey varchar(128) NOT NULL,
       pval varchar(65535)
);
create index pkey on tig_pairs ( pkey );

create table tig_max_ids (
       max_uid bigint,
       max_nid bigint
);


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
