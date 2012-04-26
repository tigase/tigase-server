-- QUERY START:
CREATE procedure TigUpdatePairs(nid bigint, uid bigint, tkey varchar(255), tval varchar(32672)) 
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUpdatePairs';
-- QUERY END:

call TigPutDBProperty('schema-version','5.1');