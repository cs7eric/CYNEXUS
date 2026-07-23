package cn.cccs7.cynexus.core.domain.finance.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class FinanceAiCaptureItemUpdate {
    private Long id;
    private String status;
    private String transactionType;
    private OffsetDateTime occurredAt;
    private BigDecimal amount;
    private String currency;
    private String merchant;
    private Long categoryId;
    private Long accountId;
    private Integer installmentCount;
    private BigDecimal installmentFee;
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { this.installmentCount = installmentCount; }
    public BigDecimal getInstallmentFee() { return installmentFee; }
    public void setInstallmentFee(BigDecimal installmentFee) { this.installmentFee = installmentFee; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
