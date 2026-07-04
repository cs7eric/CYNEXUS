package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.TdxSectorQuoteDetailSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.TdxSectorQuoteDetailSnapshotMapper;
import cn.cccs7.cynexus.market.infra.basic.service.TdxSectorQuoteDetailSnapshotBasicService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TdxSectorQuoteDetailSnapshotBasicServiceImpl extends AbstractCrudBasicService<TdxSectorQuoteDetailSnapshotPO>
        implements TdxSectorQuoteDetailSnapshotBasicService {

    private final TdxSectorQuoteDetailSnapshotMapper mapper;

    public TdxSectorQuoteDetailSnapshotBasicServiceImpl(TdxSectorQuoteDetailSnapshotMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.mapper = mapper;
    }

    @Override
    public Optional<TdxSectorQuoteDetailSnapshotPO> findLatestByCode6(String code6) {
        return findLatestByCode6OnOrBefore(code6, null);
    }

    @Override
    public Optional<TdxSectorQuoteDetailSnapshotPO> findLatestByCode6OnOrBefore(String code6, LocalDate snapshotDate) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<TdxSectorQuoteDetailSnapshotPO>()
                .eq(TdxSectorQuoteDetailSnapshotPO::getCode6, code6)
                .le(snapshotDate != null, TdxSectorQuoteDetailSnapshotPO::getSnapshotDate, snapshotDate)
                .orderByDesc(TdxSectorQuoteDetailSnapshotPO::getSnapshotDate)
                .orderByDesc(TdxSectorQuoteDetailSnapshotPO::getId)
                .last("limit 1")));
    }
}
