package cn.cccs7.cynexus.common.db.service;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import java.util.List;

public abstract class AbstractCrudBasicService<T extends BasePO> implements CrudBasicService<T> {

    private final BaseMapperX<T> mapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    protected AbstractCrudBasicService(BaseMapperX<T> mapper, SnowflakeIdGenerator snowflakeIdGenerator) {
        this.mapper = mapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public List<T> listAll() {
        return mapper.selectList(null);
    }

    @Override
    public T getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public Long create(T po) {
        if (po.getId() == null) {
            po.setId(snowflakeIdGenerator.nextId());
        }
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public boolean update(T po) {
        return mapper.updateById(po) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return mapper.deleteById(id) > 0;
    }
}
