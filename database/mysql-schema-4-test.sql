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

select 'Initializing database..';
call TigInitdb();

-- Possible encodings are:
-- - 'MD5-USERID-PASSWORD'
-- - 'MD5-PASSWORD'
-- - 'PLAIN'
-- More can be added if needed.
call TigPutDBProperty('password-encoding', 'PLAIN');
call TigPutDBProperty('schema-version', '4.0');

select 'Adding new user with PlainPw: ', 'test_user', 'test_password';
call TigTestAddUser('test_user', 'test_passwd', 'SUCCESS - adding new user',
		 'ERROR - adding new user');

call TigUserLogin('test_user', 'wrong_passwd', @res_user_id);
select @res_user_id as user_id\g
select if(@res_user_id is NULL,
         'SUCCESS - User login failed as expected, used UserLogin',
			 	 'ERROR - User login succeeded as NOT expected');

call TigUserLoginPlainPw('test_user', 'wrong_passwd', @res_user_id);
select if(@res_user_id is NULL,
			   'SUCCESS - User login failed as expected, used wrong password',
			 	 'ERROR - User login succeeded as NOT expected');

call TigUserLoginPlainPw('test_user', 'test_passwd', @res_user_id);
select if(@res_user_id is not NULL,
			   'SUCCESS - User login OK as expected, used UserLoginPlainPw',
			 	 'ERROR - User login failed as NOT expected');

call TigUserLogout('test_user');
call TigUserLogout('test_user');
select online_status into @res_online_status from tig_users
  where user_id = 'test_user';
select if(@res_online_status = 0,
			   'SUCCESS - online status OK after 2 logouts',
			 	 'ERROR - online status incorrect after 2 logouts');

select 'Changing password using UpdatePassword';
call TigUpdatePassword('test_user', 'new_password');
call TigUserLoginPlainPw('test_user', 'new_password', @res_user_id);
select if(@res_user_id is NULL,
			   'SUCCESS - User login failed as expected, password incorrectly changed',
			 	 'ERROR - User login succeeded as NOT expected');

select 'Changing password using UpdatePasswordPlainPw';
call TigUpdatePasswordPlainPw('test_user', 'new_password');
call TigUserLoginPlainPw('test_user', 'new_password', @res_user_id);
select if(@res_user_id is not NULL,
			   'SUCCESS - User login OK as expected, password updated with PlainPw',
			 	 'ERROR - User login failed as NOT expected');

call TigUserLogout('test_user');
select 'Disabling user account';
call TigDisableAccount('test_user');
call TigUserLoginPlainPw('test_user', 'new_password', @res_user_id);
select if(@res_user_id is NULL,
			   'SUCCESS - User login failed as expected, account disabled',
			 	 'ERROR - User login succeeded as NOT expected');

select 'Enabling user account';
call TigEnableAccount('test_user');
call TigUserLoginPlainPw('test_user', 'new_password', @res_user_id);
select if(@res_user_id is not NULL,
			   'SUCCESS - User login OK as expected, account enabled',
			 	 'ERROR - User login failed as NOT expected');

call TigUserLogout('test_user');

select 'Adding new user with PlainPw: ', 'test_user_2', 'test_password_2';
call TigTestAddUser('test_user_2', 'test_passwd_2', 'SUCCESS - adding new user',
		 'ERROR - adding new user');

select 'Adding a user with the same user_id: ', 'test_user', 'test_password_2';
call TigTestAddUser('test_user', 'test_password_2', 'ERROR, that was duplicate entry insertion and it should fail.', 'SUCCESS - user adding failure as expected as that was duplicate entry insertion attempt');

call TigRemoveUser('test_user');
call TigRemoveUser('test_user_2');
call TigOnlineUsers();
call TigOfflineUsers();

