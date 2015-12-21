package tigase.map;

/**
 * Created by bmalkow on 04.12.2015.
 */
public interface DMapListener<K, V> {

	void onAddItem(K key, V value);

	void onClear();

	void onRemove(K key, V value);

}
