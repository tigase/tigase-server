
select NOW(), ' - Installing missing stored procedures';

-- QUERY START:
drop procedure if exists TigUpdatePairs;
-- QUERY END:
 
delimiter //

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

delimiter ;


-- QUERY START:
call TigPutDBProperty('schema-version', '5.1');
-- QUERY END:

select NOW(), ' - All done, database ver 5.1 ready to use!';