package cn.cccs7.cynexus.market.infra.factorresearch;
import cn.cccs7.cynexus.market.domain.factorresearch.*;
import cn.cccs7.cynexus.market.infra.factorresearch.entity.MarketFactorSetPO;
import cn.cccs7.cynexus.market.infra.factorresearch.mapper.MarketFactorSetMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository public class MarketFactorSetRepositoryAdapter implements MarketFactorSetRepository {
 private final MarketFactorSetMapper mapper; private final ObjectMapper json;
 public MarketFactorSetRepositoryAdapter(MarketFactorSetMapper mapper,ObjectMapper json){this.mapper=mapper;this.json=json;}
 public Optional<MarketFactorSetBO.Definition> find(String t,String o,String c){return Optional.ofNullable(mapper.find(t,o,c)).map(this::domain);}
 public List<MarketFactorSetBO.Definition> list(String t,String o,int off,int lim){return mapper.list(t,o,off,lim).stream().map(this::domain).toList();}
 @Transactional public boolean insertDefinition(MarketFactorSetBO.Definition d){MarketFactorSetPO p=base(d); return mapper.insertDefinition(p)==1;}
 @Transactional public boolean updateDefinition(MarketFactorSetBO.Definition d,long e){return mapper.updateDefinition(base(d),e)==1;}
 @Transactional public boolean insertVersion(long id,MarketFactorSetBO.Version v){MarketFactorSetPO p=new MarketFactorSetPO();p.setVersionId(v.id());p.setVersionNo(v.versionNo());p.setSourceStudyId(v.sourceStudyId());p.setExpression(write(v.expression()));p.setValidationSnapshot(write(v.validationSnapshot()));p.setVersionStatus(v.status().name()); return mapper.insertVersion(p,id)==1;}
 public boolean retireDraftVersions(long id){return true;}
 public boolean updateVersionStatus(long id,int no,MarketFactorSetBO.VersionStatus s,Map<String,Object> snap){return mapper.updateVersionStatus(id,no,s.name(),write(snap))==1;}
 @Transactional public boolean activateVersion(long id,String tenantId,String ownerId,long versionId){return mapper.activateVersion(id,tenantId,ownerId,versionId)==1;}
 private MarketFactorSetPO base(MarketFactorSetBO.Definition d){MarketFactorSetPO p=new MarketFactorSetPO();p.setId(d.id());p.setFactorSetCode(d.code());p.setTenantId(d.tenantId());p.setOwnerId(d.ownerId());p.setName(d.name());p.setStatus(d.status().name());p.setActiveVersionId(d.activeVersionId());p.setVersion(d.version());return p;}
 private MarketFactorSetBO.Definition domain(MarketFactorSetPO p){MarketFactorSetBO.Version v=p.getVersionId()==null?null:new MarketFactorSetBO.Version(p.getVersionId(),p.getVersionNo(),p.getSourceStudyId(),read(p.getExpression()),read(p.getValidationSnapshot()),MarketFactorSetBO.VersionStatus.valueOf(p.getVersionStatus()),p.getCreatedAt()); return new MarketFactorSetBO.Definition(p.getId(),p.getFactorSetCode(),p.getTenantId(),p.getOwnerId(),p.getName(),MarketFactorSetBO.DefinitionStatus.valueOf(p.getStatus()),p.getActiveVersionId(),p.getVersion()==null?0:p.getVersion(),v);}
 private String write(Map<String,Object> m){try{return json.writeValueAsString(m==null?Map.of():m);}catch(Exception e){throw new IllegalStateException(e);}}
 private Map<String,Object> read(String s){try{return s==null?Map.of():json.readValue(s,new TypeReference<>(){});}catch(Exception e){return Map.of();}}
}
