package cn.cccs7.cynexus.market.domain.repository;

import java.util.List;

public interface MarketCrudRepository<T> {

    List<T> listAll();

    T findById(Long id);

    Long create(T value);

    boolean update(T value);

    boolean delete(Long id);
}
