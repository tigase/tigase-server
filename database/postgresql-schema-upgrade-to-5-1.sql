
select NOW(), ' - Installing missing stored procedures';

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


-- QUERY START:
select TigPutDBProperty('schema-version', '5.1');
-- QUERY END:

select NOW(), ' - All done, database ver 5.1 ready to use!';