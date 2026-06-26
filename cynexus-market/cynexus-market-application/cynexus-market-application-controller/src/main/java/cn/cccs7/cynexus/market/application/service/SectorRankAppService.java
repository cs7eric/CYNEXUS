package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;
import cn.cccs7.cynexus.market.domain.valueobject.Sector;
import cn.cccs7.cynexus.market.domain.valueobject.SectorStrength;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SectorRankAppService {

    public List<SectorRankBO> getSectorRank(String tradeDate, Integer topN) {
        int size = topN == null ? 10 : topN;
        List<SectorRankBO> ranks = List.of(
                new SectorRankBO(new Sector("AI", "Artificial Intelligence"),
                        new SectorStrength(new BigDecimal("3.18"), new BigDecimal("1280.50"),
                                new BigDecimal("86.30"))),
                new SectorRankBO(new Sector("SEMICONDUCTOR", "Semiconductor"),
                        new SectorStrength(new BigDecimal("2.42"), new BigDecimal("980.10"),
                                new BigDecimal("54.20"))),
                new SectorRankBO(new Sector("BROKERAGE", "Brokerage"),
                        new SectorStrength(new BigDecimal("1.76"), new BigDecimal("760.00"),
                                new BigDecimal("21.80")))
        );
        return ranks.stream().limit(size).toList();
    }
}
