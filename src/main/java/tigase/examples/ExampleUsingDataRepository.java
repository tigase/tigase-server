/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.examples;

import tigase.component.exceptions.RepositoryException;
import tigase.db.DataRepository;
import tigase.db.DataSourceAware;
import tigase.db.Repository;
import tigase.db.TigaseDBException;

import java.sql.ResultSet;
import java.sql.SQLException;

import static java.lang.System.Logger;
import static java.lang.System.getLogger;

/**
 * This is only and example, sample implementation of DataSourceAware and should not be used!
 */
@Repository.Meta(supportedUris = {"jdbc:[^:]+:.*"})
public class ExampleUsingDataRepository implements DataSourceAware<DataRepository> {

    private static final Logger log = getLogger(ExampleUsingDataRepository.class.getName());

    private static final String GET_DATA = "SELECT field FROM my_custom_table WHERE user_id = ?";

    private DataRepository data_repo = null;

    @Override
    public void setDataSource(DataRepository dataSource) throws RepositoryException {
        this.data_repo = dataSource;
        try {
            data_repo.initPreparedStatement("GET_DATA", GET_DATA);
        } catch (SQLException e) {
            log.log(Logger.Level.WARNING, "Failed to init prepared statement: " + e.getMessage());
        }
    }


    public void getDataFromRepo(String userID) throws TigaseDBException {
        try {
            var getDataQuery = data_repo.getPreparedStatement(null, GET_DATA);
            getDataQuery.setString(1, userID);
            ResultSet rs = null;
            synchronized (getDataQuery) {
                try {
                    rs = getDataQuery.executeQuery();
                    if (rs.next()) {
                        log.log(Logger.Level.INFO, "User data: " + rs.getString(1));
                    }
                } finally {
                    data_repo.release(null, rs);
                }
            }
        } catch (SQLException e) {
            throw new TigaseDBException("Failed to get prepared statement: " + e.getMessage());
        }
    }
}
