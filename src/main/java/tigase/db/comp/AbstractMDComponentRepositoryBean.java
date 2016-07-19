/*
 * AbstractMDComponentRepositoryBean.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */
package tigase.db.comp;

import tigase.db.DBInitException;
import tigase.db.DataSource;
import tigase.db.TigaseDBException;
import tigase.db.beans.MDRepositoryBean;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Class implements ComponentRepository interfaces and extends MDRepositoryBean
 * and is designed to be based bean used by other classes responsible for loading
 * proper implementation of ComponentRepository depending on used implementation
 * of DataSource.
 *
 * Created by andrzej on 18.03.2016.
 */
public abstract class AbstractMDComponentRepositoryBean<Item extends RepositoryItem> extends MDRepositoryBean<ComponentRepositoryDataSourceAware<Item, DataSource>> implements ComponentRepository<Item> {

	protected ComponentRepositoryDataSourceAware<Item,DataSource> repo = null;

	@Override
	protected void updateDataSource(String domain, DataSource newDS, DataSource oldDS) {
		if (!dataSourceBean.getDefaultAlias().equals(domain))
			return;

		ComponentRepositoryDataSourceAware<Item,DataSource> repo_old = repo;
		super.updateDataSource(domain, newDS, oldDS);
		repo = getRepository(dataSourceBean.getDefaultAlias());
		if (repo_old != repo && repo_old != null) {
			repo_old.destroy();
		}
	}


	@Override
	public void addRepoChangeListener(RepositoryChangeListenerIfc<Item> repoChangeListener) {
		repo.addRepoChangeListener(repoChangeListener);
	}

	@Override
	public void removeRepoChangeListener(RepositoryChangeListenerIfc<Item> repoChangeListener) {
		repo.removeRepoChangeListener(repoChangeListener);

	}

	@Override
	public void addItem(Item item) throws TigaseDBException {
		repo.addItem(item);

	}

	@Override
	public void addItemNoStore(Item item) {
		repo.addItemNoStore(item);
	}

	@Override
	public Collection<Item> allItems() throws TigaseDBException {
		return repo.allItems();
	}

	@Override
	public boolean contains(String key) {
		return repo.contains(key);
	}

	@Override
	public void destroy() {
		repo.destroy();
	}

	@Deprecated
	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
		//repo.getDefaults(defs, params);
	}

	@Override
	public Item getItem(String key) {
		return repo.getItem(key);
	}

	@Override
	public Item getItemInstance() {
		return repo.getItemInstance();
	}

	@Override
	public void reload() throws TigaseDBException {
		repo.reload();
	}

	@Override
	public void removeItem(String key) throws TigaseDBException {
		repo.removeItem(key);
	}

	@Deprecated
	@Override
	public void setProperties(Map<String, Object> properties) {
		//repo.setProperties(properties);
	}

	@Override
	public int size() {
		return repo.size();
	}

	@Override
	public void store() throws TigaseDBException {
		repo.store();
	}

	@Override
	public String validateItem(Item item) {
		return repo.validateItem(item);
	}

	@Override
	public void setAutoloadTimer(long delay) {
		//repo.setAutoloadTimer(delay);
	}

	@Override
	public Iterator<Item> iterator() {
		return repo.iterator();
	}

	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		repo.initRepository(resource_uri, params);
	}

}
