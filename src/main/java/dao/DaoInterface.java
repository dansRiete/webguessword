package dao;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Aleks on 28.03.2017.
 */
public interface DaoInterface<T, Id extends Serializable> {

    public void persist(T entity);

    public void update(T entity);

    public T findById(Id id);

    public void delete (T entity);

    public List<T> fetchAll();

    public void deleteAll();

}
