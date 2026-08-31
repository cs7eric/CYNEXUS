package cn.cccs7.cynexus.market.infra.portfolio;
import cn.cccs7.cynexus.market.domain.portfolio.*; import java.util.*; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Repository;
@Repository public class PortfolioCashFlowRepositoryAdapter implements PortfolioCashFlowRepository {
 private final JdbcTemplate jdbc; private final PortfolioIdGenerator ids; public PortfolioCashFlowRepositoryAdapter(JdbcTemplate jdbc, PortfolioIdGenerator ids){this.jdbc=jdbc;this.ids=ids;}
 public List<PortfolioCashFlowBO> list(String p,String t){return jdbc.query("select id,occurred_at,amount,flow_type,note from cynexus_appl_market.portfolio_account_cash_flow where principal_key=? and tenant_key=? order by occurred_at,id",(r,n)->new PortfolioCashFlowBO(r.getLong(1),r.getObject(2,java.time.OffsetDateTime.class),r.getBigDecimal(3),r.getString(4),r.getString(5)),p,t);}
 public PortfolioCashFlowBO save(String p,String t,PortfolioCashFlowBO f){long id=ids.nextId();jdbc.update("insert into cynexus_appl_market.portfolio_account_cash_flow(id,principal_key,tenant_key,occurred_at,amount,flow_type,note) values(?,?,?,?,?,?,?)",id,p,t,f.occurredAt(),f.amount(),f.flowType(),f.note());return new PortfolioCashFlowBO(id,f.occurredAt(),f.amount(),f.flowType(),f.note());}
}
