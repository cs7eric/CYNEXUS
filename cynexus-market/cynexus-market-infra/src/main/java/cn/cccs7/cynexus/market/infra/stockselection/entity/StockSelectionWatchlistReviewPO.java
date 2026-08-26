package cn.cccs7.cynexus.market.infra.stockselection.entity;
import java.time.OffsetDateTime; import lombok.Data;
@Data public class StockSelectionWatchlistReviewPO { private Long id; private Long watchlistItemId; private String tenantId; private String ownerId; private String decision; private String note; private OffsetDateTime reviewedAt; }
