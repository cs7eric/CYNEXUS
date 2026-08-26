package cn.cccs7.cynexus.market.infra.factorresearch;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorSetIdGenerator;
import org.springframework.stereotype.Component;
@Component public class MarketFactorSetIdGeneratorAdapter implements MarketFactorSetIdGenerator { private final SnowflakeIdGenerator ids; public MarketFactorSetIdGeneratorAdapter(SnowflakeIdGenerator ids){this.ids=ids;} public long nextId(){return ids.nextId();} }
