--
--  Tigase Jabber/XMPP Server
--  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
--
--  This program is free software: you can redistribute it and/or modify
--  it under the terms of the GNU Affero General Public License as published by
--  the Free Software Foundation, either version 3 of the License.
--
--  This program is distributed in the hope that it will be useful,
--  but WITHOUT ANY WARRANTY; without even the implied warranty of
--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
--  GNU Affero General Public License for more details.
--
--  You should have received a copy of the GNU Affero General Public License
--  along with this program. Look for COPYING file in the top folder.
--  If not, see http://www.gnu.org/licenses/.
--
--  $Rev: $
--  Last modified by $Author: $
--  $Date: $

-- Database stored procedures and fucntions for Tigase schema version 4.0.0

-- QUERY START:
CREATE OR REPLACE FUNCTION public.create_plpgsql_language ()
        RETURNS TEXT
        AS $$
            CREATE LANGUAGE plpgsql;
            SELECT 'language plpgsql created'::TEXT;
        $$
LANGUAGE 'sql';

SELECT CASE WHEN
              (SELECT true::BOOLEAN
                 FROM pg_language
                WHERE lanname='plpgsql')
            THEN
              (SELECT 'language already installed'::TEXT)
            ELSE
              (SELECT public.create_plpgsql_language())
            END;

DROP FUNCTION public.create_plpgsql_language ();
-- QUERY END:

-- QUERY START:
-- Database properties get - function
create or replace function TigGetDBProperty(varchar(255)) returns text as '
declare
  _result text;
  _tkey alias for $1;
begin

	select pval into _result from tig_pairs, tig_users
		where (pkey = _tkey) AND (lower(user_id) = lower(''db-properties''))
					AND (tig_pairs.uid = tig_users.uid);

	return _result;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Database properties set - procedure
create or replace function TigPutDBProperty(varchar(255), text) returns void as '
declare
  _tkey alias for $1;
  _tval alias for $2;
begin
  if exists( select pval from tig_pairs, tig_users where
		(lower(user_id) = lower(''db-properties'')) AND (tig_users.uid = tig_pairs.uid)
		AND (pkey = _tkey))
  then
	  update tig_pairs set pval = _tval from tig_users
      where (lower(tig_users.user_id) = lower(''db-properties''))
        AND (tig_users.uid = tig_pairs.uid)
        AND (pkey = _tkey);
  else
    insert into tig_pairs (pkey, pval, uid)
		  select _tkey, _tval, uid from tig_users
			  where (lower(user_id) = lower(''db-properties''));
  end if;
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- The initialization of the database.
-- The procedure should be called manually somehow before starting the
-- server. In theory the server could call the procedure automatically
-- at the startup time but I don't know yet how to solve the problem
-- with multiple cluster nodes starting at later time when the server
-- is already running.
create or replace function TigInitdb() returns void as '
begin
	update tig_users set online_status = 0;
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Add a new user to the database assuming the user password is already
-- encoded properly according to the database settings.
-- If password is not encoded TigAddUserPlainPw should be used instead.
create or replace function TigAddUser(varchar(2049), varchar(255))
  returns bigint as '
declare
  _user_id alias for $1;
  _user_pw alias for $2;
  _res_uid bigint;
begin
	if exists( select uid from tig_users where
		(lower(user_id) = lower(_user_id)) AND (user_pw = _user_pw) )
	then
		return null;
	else
		insert into tig_users (user_id, user_pw)
			values (_user_id, _user_pw);
		select currval(''tig_users_uid_seq'') into _res_uid;

		insert into tig_nodes (parent_nid, uid, node)
		values (NULL, _res_uid, ''root'');

		return _res_uid as uid;
	end if;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Takes plain text user password and converts it to internal representation
-- and creates a new user account.
create or replace function TigAddUserPlainPw(varchar(2049), varchar(255))
  returns bigint as '
declare
  _user_id alias for $1;
  _user_pw alias for $2;
  _enc text;
  _res_uid bigint;
begin
	select TigGetDBProperty(''password-encoding'') into _enc;
  select
    case _enc
 		when ''MD5-PASSWORD'' then TigAddUser(_user_id, MD5(_user_pw))
	  when ''MD5-USERID-PASSWORD'' then TigAddUser(_user_id, MD5(_user_id || _user_pw))
	  when ''MD5-USERNAME-PASSWORD'' then
      TigAddUser(_user_id, MD5(split_part(_user_id, ''@'', 1) || _user_pw))
	  else TigAddUser(_user_id, _user_pw)
	  end into _res_uid;
	return _res_uid as uid;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Low level database user id as big number. Used only for performance reasons
-- and save database space. Besides JID is too large to server as UID
create or replace function TigGetUserDBUid(varchar(2049)) returns bigint as '
declare
  _user_id alias for $1;
  res_uid bigint;
begin
	select uid into res_uid from tig_users where lower(user_id) = lower(_user_id);
  return res_uid;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Removes a user from the database
create or replace function TigRemoveUser(varchar(2049)) returns void as '
declare
  _user_id alias for $1;
  res_uid bigint;
begin
	select uid into res_uid from tig_users where lower(user_id) = lower(_user_id);

	delete from tig_pairs where uid = res_uid;
	delete from tig_nodes where uid = res_uid;
	delete from tig_users where uid = res_uid;
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Returns user's password from the database
create or replace function TigGetPassword(varchar(2049)) returns varchar(255) as '
declare
  _user_id alias for $1;
  res_pw varchar(255);
begin
	select user_pw into res_pw from tig_users where lower(user_id) = lower(_user_id);
  return res_pw;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Takes plain text user password and converts it to internal representation
create or replace function TigUpdatePasswordPlainPw(varchar(2049), varchar(255))
  returns void as '
declare
  _user_id alias for $1;
  _user_pw alias for $2;
  _enc text;
begin
	select TigGetDBProperty(''password-encoding'') into _enc;
  perform
    case _enc
		when ''MD5-PASSWORD'' then TigUpdatePassword(_user_id, MD5(_user_pw))
		when ''MD5-USERID-PASSWORD'' then
      TigUpdatePassword(_user_id, MD5(_user_id || _user_pw))
	  when ''MD5-USERNAME-PASSWORD'' then
      TigUpdatePassword(_user_id, MD5(split_part(_user_id, ''@'', 1) || _user_pw))
		else TigUpdatePassword(_user_id, _user_pw)
		end;
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Variant of TigUpdatePasswordPlainPw SP with parameters in reverse order.
-- Some implementations require the parameters to be in the same order as
-- the update query.
create or replace function TigUpdatePasswordPlainPwRev(varchar(255), varchar(2049))
  returns void as '
declare
  _user_pw alias for $1;
  _user_id alias for $2;
begin
  perform TigUpdatePasswordPlainPw(_user_id, _user_pw);
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Update user password
create or replace function TigUpdatePassword(varchar(2049), varchar(255))
  returns void as '
declare
  _user_id alias for $1;
  _user_pw alias for $2;
begin
	update tig_users set user_pw = _user_pw where lower(user_id) = lower(_user_id);
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- List all online users
create or replace function TigOnlineUsers() returns void as '
begin
  return;
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from tig_users where online_status > 0;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- List all offline users
create or replace function TigOfflineUsers() returns void as '
begin
  return;
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from tig_users where online_status = 0;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- List of all users in database
create or replace function TigAllUsers() returns setof varchar(2049) as
	'select user_id from tig_users;'
LANGUAGE 'sql';
-- create or replace function TigAllUsers() returns void as '
-- begin
--  return;
--	select user_id, last_login, last_logout, online_status, failed_logins, account_status
--		from tig_users;
-- end;
-- ' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- All users count
create or replace function TigAllUsersCount() returns bigint as '
declare
  res_cnt bigint;
begin
	select count(*) into res_cnt from tig_users;
  return res_cnt;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Performs user login for a plain text password, converting it to an internal
-- representation if necessary
create or replace function TigUserLoginPlainPw(varchar(2049), varchar(255))
			 					  returns varchar(2049) as '
declare
  _user_id alias for $1;
  _user_pw alias for $2;
  res_user_id varchar(2049);
  _enc text;
begin
	select TigGetDBProperty(''password-encoding'') into _enc;
  select
    case _enc
		when ''MD5-PASSWORD'' then TigUserLogin(_user_id, MD5(_user_pw))
		when ''MD5-USERID-PASSWORD'' then
      TigUserLogin(_user_id, MD5(_user_id || _user_pw))
	  when ''MD5-USERNAME-PASSWORD'' then
      TigUserLogin(_user_id, MD5(split_part(_user_id, ''@'', 1) || _user_pw))
		else TigUserLogin(_user_id, _user_pw)
		end into res_user_id;
  return res_user_id;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Perforrm user login. It returns user_id uppon success and NULL
-- on failure.
-- If the login is successful it also increases online_status and sets
-- last_login time to the current timestamp
create or replace function TigUserLogin(varchar(2049), varchar(255))
			 					  returns varchar(2049) as '
declare
  _user_id alias for $1;
  _user_pw alias for $2;
  res_user_id varchar(2049);
begin
	if exists(select user_id from tig_users
		where (account_status > 0) AND (lower(user_id) = lower(_user_id)) AND (user_pw = _user_pw))
	then
		update tig_users
			set online_status = online_status + 1, last_login = now()
			where lower(user_id) = lower(_user_id);
    select _user_id into res_user_id;
	else
		update tig_users set failed_logins = failed_logins + 1 where lower(user_id) = lower(_user_id);
    select NULL into res_user_id;
	end if;
  return res_user_id;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- It decreases online_status and sets last_logout time to the current timestamp
create or replace function TigUserLogout(varchar(2049)) returns void as '
declare
  _user_id alias for $1;
begin
	update tig_users
		set online_status = greatest(online_status - 1, 0),
			last_logout = now()
		where lower(user_id) = lower(_user_id);
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Disable user account
create or replace function TigDisableAccount(varchar(2049)) returns void as '
declare
  _user_id alias for $1;
begin
	update tig_users set account_status = 0 where lower(user_id) = lower(_user_id);
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Enable user account
create or replace function TigEnableAccount(varchar(2049)) returns void as '
declare
  _user_id alias for $1;
begin
	update tig_users set account_status = 1 where lower(user_id) = lower(_user_id);
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Get list of all active user accounts
create or replace function TigActiveAccounts() returns void as '
begin
  return;
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from tig_users where account_status > 0;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Get list of all disabled user accounts
create or replace function TigDisabledAccounts() returns table(user_id varchar(2049), last_login timestamp, last_logout timestamp, online_status int, failed_logins int, account_status int) as '
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from tig_users where account_status = 0;
' LANGUAGE 'sql';
-- QUERY END:

-- QUERY START:
-- Helper procedure for adding a new node
create or replace function TigAddNode(bigint, bigint, varchar(255))
  returns bigint as '
declare
  _parent_nid alias for $1;
  _uid alias for $2;
  _node alias for $3;
  res_nid bigint;
begin
	insert into tig_nodes (parent_nid, uid, node)
		values (_parent_nid, _uid, _node);
  select currval(''tig_nodes_nid_seq'') into res_nid;
  return res_nid;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function TigUsers2Ver4Convert() returns void as '
declare
  user_row RECORD;
begin

  for user_row in
		select user_id, pval as password
			from tig_users, tig_pairs
			where tig_users.uid = tig_pairs.uid and pkey = ''password'' loop
		perform TigUpdatePasswordPlainPw(user_row.user_id, user_row.password);
	END LOOP;
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function TigUpdatePairs(bigint, bigint, varchar(255), text) returns void as '
declare
  _nid alias for $1;
  _uid alias for $2;
  _tkey alias for $3;
  _tval alias for $4;
begin
  if exists(select 1 from tig_pairs where nid = _nid and uid = _uid and pkey = _tkey)
  then
        update tig_pairs set pval = _tval where nid = _nid and uid = _uid and pkey = _tkey;
  else
        insert into tig_pairs (nid, uid, pkey, pval) values (_nid, _uid, _tkey, _tval);
  end if;
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:
