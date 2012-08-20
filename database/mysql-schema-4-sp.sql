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
--

-- Database stored procedures and functions for Tigase schema version 4.0.0

-- QUERY START:
drop procedure if exists TigInitdb;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigAddUser;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigAddUserPlainPw;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigGetUserDBUid;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigRemoveUser;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUpdatePassword;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUpdatePasswordPlainPw;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUpdatePasswordPlainPwRev;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigGetPassword;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUserLogin;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUserLoginPlainPw;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUserLogout;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigOnlineUsers;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigOfflineUsers;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigAllUsers;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigAllUsersCount;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigDisableAccount;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigEnableAccount;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigActiveAccounts;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigDisabledAccounts;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigAddNode;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigTestAddUser;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUsers2Ver4Convert;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigPutDBProperty;
-- QUERY END:
-- QUERY START:
drop function if exists TigGetDBProperty;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUpdatePairs;
-- QUERY END:

delimiter //

-- QUERY START:
-- Database properties get - function
create function TigGetDBProperty(_tkey varchar(255) CHARSET utf8) returns mediumtext CHARSET utf8
READS SQL DATA
begin
	declare _result mediumtext CHARSET utf8;

	select pval into _result from tig_pairs, tig_users
		where (pkey = _tkey) AND (sha1_user_id = sha1(lower('db-properties')))
					AND (tig_pairs.uid = tig_users.uid);

	return (_result);
end //
-- QUERY END:

-- QUERY START:
-- Database properties set - procedure
create procedure TigPutDBProperty(_tkey varchar(255) CHARSET utf8, _tval mediumtext CHARSET utf8)
begin
  if exists( select 1 from tig_pairs, tig_users where
    (sha1_user_id = sha1(lower('db-properties'))) AND (tig_users.uid = tig_pairs.uid)
    AND (pkey = _tkey))
  then
    update tig_pairs, tig_users set pval = _tval
    where (sha1_user_id = sha1(lower('db-properties'))) AND (tig_users.uid = tig_pairs.uid)
      AND (pkey = _tkey);
  else
    insert into tig_pairs (pkey, pval, uid)
      select _tkey, _tval, uid from tig_users
        where (sha1_user_id = sha1(lower('db-properties')));
  end if;
end //
-- QUERY END:

-- QUERY START:
-- The initialization of the database.
-- The procedure should be called manually somehow before starting the
-- server. In theory the server could call the procedure automatically
-- at the startup time but I don't know yet how to solve the problem
-- with multiple cluster nodes starting at later time when the server
-- is already running.
create procedure TigInitdb()
begin
  update tig_users set online_status = 0;
end //
-- QUERY END:

-- QUERY START:
-- Add a new user to the database assuming the user password is already
-- encoded properly according to the database settings.
-- If password is not encoded TigAddUserPlainPw should be used instead.
create procedure TigAddUser(_user_id varchar(2049) CHARSET utf8, _user_pw varchar(255) CHARSET utf8)
begin
	declare res_uid bigint unsigned;

	insert into tig_users (user_id, sha1_user_id, user_pw)
		values (_user_id, sha1(lower(_user_id)), _user_pw);

	select LAST_INSERT_ID() into res_uid;

	insert into tig_nodes (parent_nid, uid, node)
		values (NULL, res_uid, 'root');

	if _user_pw is NULL then
		update tig_users set account_status = -1 where uid = res_uid;
	end if;

	select res_uid as uid;
end //
-- QUERY END:

-- QUERY START:
-- Takes plain text user password and converts it to internal representation
-- and creates a new user account.
create procedure TigAddUserPlainPw(_user_id varchar(2049) CHARSET utf8, _user_pw varchar(255) CHARSET utf8)
begin
	case TigGetDBProperty('password-encoding')
		when 'MD5-PASSWORD' then
			call TigAddUser(_user_id, MD5(_user_pw));
		when 'MD5-USERID-PASSWORD' then
			call TigAddUser(_user_id, MD5(CONCAT(_user_id, _user_pw)));
		when 'MD5-USERNAME-PASSWORD' then
			call TigAddUser(_user_id, MD5(CONCAT(substring_index(_user_id, '@', 1), _user_pw)));
		else
			call TigAddUser(_user_id, _user_pw);
		end case;
end //
-- QUERY END:

-- QUERY START:
-- Low level database user id as big number. Used only for performance reasons
-- and save database space. Besides JID is too large to server as UID
create procedure TigGetUserDBUid(_user_id varchar(2049) CHARSET utf8)
begin
	select uid from tig_users where sha1_user_id = sha1(lower(_user_id));
end //
-- QUERY END:

-- QUERY START:
-- Removes a user from the database
create procedure TigRemoveUser(_user_id varchar(2049) CHARSET utf8)
begin
	declare res_uid bigint unsigned;

	select uid into res_uid from tig_users where sha1_user_id = sha1(lower(_user_id));

	delete from tig_pairs where uid = res_uid;
	delete from tig_nodes where uid = res_uid;
	delete from tig_users where uid = res_uid;
end //
-- QUERY END:

-- QUERY START:
-- Returns user's password from the database
create procedure TigGetPassword(_user_id varchar(2049) CHARSET utf8)
begin
	select user_pw from tig_users where sha1_user_id = sha1(lower(_user_id));
end //
-- QUERY END:

-- QUERY START:
-- Takes plain text user password and converts it to internal representation
create procedure TigUpdatePasswordPlainPw(_user_id varchar(2049) CHARSET utf8, _user_pw varchar(255) CHARSET utf8)
begin
	case TigGetDBProperty('password-encoding')
		when 'MD5-PASSWORD' then
			call TigUpdatePassword(_user_id, MD5(_user_pw));
		when 'MD5-USERID-PASSWORD' then
			call TigUpdatePassword(_user_id, MD5(CONCAT(_user_id, _user_pw)));
		when 'MD5-USERNAME-PASSWORD' then
			call TigUpdatePassword(_user_id, MD5(CONCAT(substring_index(_user_id, '@', 1), _user_pw)));
		else
			call TigUpdatePassword(_user_id, _user_pw);
		end case;
end //
-- QUERY END:

-- QUERY START:
-- Variant of TigUpdatePasswordPlainPw SP with parameters in reverse order.
-- Some implementations require the parameters to be in the same order as
-- the update query.
create procedure TigUpdatePasswordPlainPwRev(_user_pw varchar(255) CHARSET utf8, _user_id varchar(2049) CHARSET utf8)
begin
	call TigUpdatePasswordPlainPw(_user_id, _user_pw);
end //
-- QUERY END:

-- QUERY START:
-- Update user password
create procedure TigUpdatePassword(_user_id varchar(2049) CHARSET utf8, _user_pw varchar(255) CHARSET utf8)
begin
	update tig_users set user_pw = _user_pw where sha1_user_id = sha1(lower(_user_id));
end //
-- QUERY END:

-- QUERY START:
-- List all online users
create procedure TigOnlineUsers()
begin
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from tig_users where online_status > 0;
end //
-- QUERY END:

-- QUERY START:
-- List all offline users
create procedure TigOfflineUsers()
begin
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from tig_users where online_status = 0;
end //
-- QUERY END:

-- QUERY START:
-- List of all users in database
create procedure TigAllUsers()
begin
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from tig_users;
end //
-- QUERY END:

-- QUERY START:
-- All users count
create procedure TigAllUsersCount()
begin
	select count(*) from tig_users;
end //
-- QUERY END:

-- QUERY START:
-- Performs user login for a plain text password, converting it to an internal
-- representation if necessary
create procedure TigUserLoginPlainPw(_user_id varchar(2049) CHARSET utf8, _user_pw varchar(255) CHARSET utf8)
begin
	case TigGetDBProperty('password-encoding')
		when 'MD5-PASSWORD' then
			call TigUserLogin(_user_id, MD5(_user_pw));
		when 'MD5-USERID-PASSWORD' then
			call TigUserLogin(_user_id, MD5(CONCAT(_user_id, _user_pw)));
		when 'MD5-USERNAME-PASSWORD' then
			call TigUserLogin(_user_id, MD5(CONCAT(substring_index(_user_id, '@', 1), _user_pw)));
		else
			call TigUserLogin(_user_id, _user_pw);
		end case;
end //
-- QUERY END:

-- QUERY START:
-- Perforrm user login. It returns user_id uppon success and NULL
-- on failure.
-- If the login is successful it also increases online_status and sets
-- last_login time to the current timestamp
create procedure TigUserLogin(_user_id varchar(2049) CHARSET utf8, _user_pw varchar(255) CHARSET utf8)
begin
	if exists(select 1 from tig_users
		where (account_status > 0) AND (sha1_user_id = sha1(lower(_user_id))) AND (user_pw = _user_pw) AND (user_id = _user_id))
	then
		update tig_users
			set online_status = online_status + 1, last_login = CURRENT_TIMESTAMP
			where sha1_user_id = sha1(lower(_user_id));
		select _user_id as user_id;
	else
		update tig_users set failed_logins = failed_logins + 1 where sha1_user_id = sha1(lower(_user_id));
		select NULL as user_id;
	end if;
end //
-- QUERY END:

-- QUERY START:
-- It decreases online_status and sets last_logout time to the current timestamp
create procedure TigUserLogout(_user_id varchar(2049) CHARSET utf8)
begin
	update tig_users
		set online_status = greatest(online_status - 1, 0),
			last_logout = CURRENT_TIMESTAMP
		where user_id = _user_id;
end //
-- QUERY END:

-- QUERY START:
-- Disable user account
create procedure TigDisableAccount(_user_id varchar(2049) CHARSET utf8)
begin
	update tig_users set account_status = 0 where sha1_user_id = sha1(lower(_user_id));
end //
-- QUERY END:

-- QUERY START:
-- Enable user account
create procedure TigEnableAccount(_user_id varchar(2049) CHARSET utf8)
begin
	update tig_users set account_status = 1 where sha1_user_id = sha1(lower(_user_id));
end //
-- QUERY END:

-- QUERY START:
-- Get list of all active user accounts
create procedure TigActiveAccounts()
begin
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from tig_users where account_status > 0;
end //
-- QUERY END:

-- QUERY START:
-- Get list of all disabled user accounts
create procedure TigDisabledAccounts()
begin
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from tig_users where account_status = 0;
end //
-- QUERY END:

-- QUERY START:
-- Helper procedure for adding a new node
create procedure TigAddNode(_parent_nid bigint, _uid bigint, _node varchar(255) CHARSET utf8)
begin
	insert into tig_nodes (parent_nid, uid, node)
		values (_parent_nid, _uid, _node);
	select LAST_INSERT_ID() as nid;
end //
-- QUERY END:

-- QUERY START:
-- Procedure to efficiently and safely update data in tig_pairs table
create procedure TigUpdatePairs(_nid bigint, _uid bigint, _tkey varchar(255) CHARSET utf8, _tval mediumtext CHARSET utf8)
begin
  if exists(SELECT 1 FROM tig_pairs WHERE nid = _nid AND uid = _uid AND pkey = _tkey)
  then
    UPDATE tig_pairs SET pval = _tval WHERE nid = _nid AND uid = _uid AND pkey = _tkey;
  ELSE
    INSERT INTO tig_pairs (nid, uid, pkey, pval) VALUES (_nid, _uid, _tkey, _tval);
  END IF;
end //
-- QUERY END:

-- QUERY START:
-- For testing only:
create procedure TigTestAddUser(_user_id varchar(2049) CHARSET utf8, _user_passwd varchar(255) CHARSET utf8,
			 success_text text CHARSET utf8, failure_text text CHARSET utf8)
begin
	declare insert_status int default 0;
	DECLARE CONTINUE HANDLER FOR 1062 SET insert_status=1;
	call TigAddUserPLainPw(_user_id, _user_passwd);
	if insert_status = 0 then
		 select success_text;
 	else
		 select failure_text;
	end if;
end //
-- QUERY END:

-- QUERY START:
create procedure TigUsers2Ver4Convert()
begin

	declare _user_id varchar(2049) CHARSET utf8;
	declare _password varchar(255) CHARSET utf8;
	declare _parent_nid bigint;
	declare _uid bigint;
	declare _node varchar(255) CHARSET utf8;
	declare l_last_row_fetched int default 0;

	DECLARE cursor_users CURSOR FOR
		select user_id, pval as password
			from tig_users, tig_pairs
			where tig_users.uid = tig_pairs.uid and pkey = 'password';
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET l_last_row_fetched=1;

	START TRANSACTION;

		SET l_last_row_fetched=0;

		OPEN cursor_users;
			cursor_loop:LOOP
				FETCH cursor_users INTO _user_id, _password;
    		IF l_last_row_fetched=1 THEN
      		LEAVE cursor_loop;
    		END IF;
				call TigUpdatePasswordPlainPw(_user_id, _password);
			END LOOP cursor_loop;
		CLOSE cursor_users;

		SET l_last_row_fetched=0;

	COMMIT;

end //
-- QUERY END:

delimiter ;

