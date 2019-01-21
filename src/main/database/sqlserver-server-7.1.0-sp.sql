--
-- Tigase XMPP Server - The instant messaging server
-- Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
--
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Affero General Public License as published by
-- the Free Software Foundation, version 3 of the License.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU Affero General Public License for more details.
--
-- You should have received a copy of the GNU Affero General Public License
-- along with this program. Look for COPYING file in the top folder.
-- If not, see http://www.gnu.org/licenses/.
--

-- QUERY START:
SET QUOTED_IDENTIFIER ON
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigPutDBProperty')
DROP PROCEDURE TigPutDBProperty
-- QUERY END:
GO


-- QUERY START:
-- Database properties set - procedure
create procedure dbo.TigPutDBProperty
	@_tkey nvarchar(255),
	@_tval ntext
	AS
	begin
		Declare @_nid int;
		Declare @_uid int;
		Declare @_count int;
		if exists (select 1 from dbo.tig_pairs, dbo.tig_users
					where (sha1_user_id = HASHBYTES('SHA1', LOWER(N'db-properties')))
						AND (dbo.tig_users.uid = dbo.tig_pairs.uid)  AND (pkey = @_tkey))
			begin
				select @_nid = dbo.tig_pairs.nid, @_uid = dbo.tig_pairs.uid from dbo.tig_pairs, dbo.tig_users
					where (sha1_user_id = HASHBYTES('SHA1', LOWER(N'db-properties')))
						AND (dbo.tig_users.uid = dbo.tig_pairs.uid)  AND (pkey = @_tkey);
				update dbo.tig_pairs set pval = @_tval
					where (@_uid = uid) AND (pkey = @_tkey) ;
			end
		else
			begin
			    if not exists (select 1 from tig_users where user_id = 'db-properties')
                    exec dbo.TigAddUserPlainPw 'db-properties', NULL;


				select @_nid = dbo.tig_pairs.nid, @_uid = dbo.tig_pairs.uid from dbo.tig_pairs, dbo.tig_users
					where (sha1_user_id = HASHBYTES('SHA1', LOWER(N'db-properties')))
						AND (dbo.tig_users.uid = dbo.tig_pairs.uid)  AND (pkey = @_tkey);
				insert into dbo.tig_pairs (pkey, pval, uid, nid)
					select @_tkey, @_tval, tu.uid, tn.nid from dbo.tig_users tu  left join tig_nodes tn on tn.uid=tu.uid
						where (sha1_user_id = HASHBYTES('SHA1', LOWER(N'db-properties')) and tn.node='root');
						
			end
	end
-- QUERY END:
GO

