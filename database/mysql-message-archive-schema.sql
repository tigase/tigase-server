-- Message logging implementation. As this is quite resource consuming
-- functionality the message logging database can be located in a different
-- area or on a different database server.
-- The table schema for Messages archiving is optimized to store and, search
-- and retrieve archived messages.

-- Here is an example piece of data stored in the database.
-- The example is taken fom the XEP-0136
-- <iq type='set' id='up1'>
--  <save xmlns='http://www.xmpp.org/extensions/xep-0136.html#ns'>
--   <chat with='juliet@capulet.com/chamber'
--         start='1469-07-21T02:56:15Z'
--         thread='damduoeg08'
--         subject='She speaks!'>
--    <from secs='0'><body>Art thou not Romeo, and a Montague?</body></from>
--    <to secs='11'><body>Neither, fair saint, if either thee dislike.</body></to>
--    <from secs='7'><body>How cam'st thou hither, tell me, and wherefore?</body></from>
--    <note utc='1469-07-21T03:04:35Z'>I think she might fancy me.</note>
--   </chat>
--  </save>
-- </iq>

-- Table keeping JIDs for message logs
create table tig_ma_jid (
  -- Automatic record ID
  ma_j_id    bigint unsigned NOT NULL auto_increment,
  -- Buddy JID the chat is with
  jid     varchar(128) NOT NULL,
  primary key(ma_j_id),
  unique key jid (jid)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;

-- Table keeping chat threads and subjects if set
create table tig_ma_thread_subject (
  -- Automatic record ID
  ma_ts_id    bigint unsigned NOT NULL auto_increment,
  -- Chat thread, if set
  thread     varchar(128),
	-- Chat subject if set
	subject    varchar(255),
  primary key(ma_ts_id),
  unique key thread (thread),
  unique key subject (subject)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;

-- Table keeping actual message body
create table tig_ma_message (
  -- Automatic record ID
  ma_m_id    bigint unsigned NOT NULL auto_increment,
  -- Automaticly generated timestamp and automaticly updated on change
  utc        timestamp,
	-- From address of the message, the vaulue refers to the tig_ma_jid table
	from_jid 	bigint unsigned NOT NULL,
	-- To address of the message, the vaulue refers to the tig_ma_jid table
	to_jid		bigint unsigned NOT NULL,
	-- Thread and Subject ID reference to the table tig_ma_thread_subject
	ts_id 		bigint unsigned,
	-- This field is supposed to lower database storage consumption. The idea is
	-- to not store the same message 2 times which may happen if 2 users
	-- on the same server chat with each other. With all the informations in this
	-- table it should be enough to store such message only once.
	body_hash  char(32) NOT NULL,
	-- The body of the message
	body       varchar(4096) NOT NULL,
  primary key(ma_m_id),
  key utc (utc),
	key from_jid (from_jid),
	key to_jid (to_jid),
	key ts_id (ts_id),
	key body_hash (body_hash),
  key body (body)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;
