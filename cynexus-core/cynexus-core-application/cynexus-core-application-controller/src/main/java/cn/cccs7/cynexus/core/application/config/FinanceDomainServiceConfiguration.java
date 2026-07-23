package cn.cccs7.cynexus.core.application.config;

import cn.cccs7.cynexus.core.domain.finance.policy.FinanceInstallmentPolicy;
import cn.cccs7.cynexus.core.domain.finance.policy.FinanceLedgerDomainPolicy;
import cn.cccs7.cynexus.core.domain.finance.port.FinanceRepository;
import cn.cccs7.cynexus.core.domain.finance.port.ReceiptAiParserPort;
import cn.cccs7.cynexus.core.domain.service.finance.FinanceAiCaptureDomainService;
import cn.cccs7.cynexus.core.domain.service.finance.FinanceInstallmentDomainService;
import cn.cccs7.cynexus.core.domain.service.finance.FinanceLedgerDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FinanceDomainServiceConfiguration {
    @Bean
    public FinanceLedgerDomainService financeLedgerDomainService(FinanceRepository repository,
                                                                  FinanceLedgerDomainPolicy policy) {
        return new FinanceLedgerDomainService(repository, policy);
    }

    @Bean
    public FinanceInstallmentDomainService financeInstallmentDomainService(FinanceRepository repository,
                                                                            FinanceLedgerDomainService ledgerService,
                                                                            FinanceInstallmentPolicy policy) {
        return new FinanceInstallmentDomainService(repository, ledgerService, policy);
    }

    @Bean
    public FinanceAiCaptureDomainService financeAiCaptureDomainService(FinanceRepository repository,
                                                                        ReceiptAiParserPort parser,
                                                                        FinanceLedgerDomainService ledgerService,
                                                                        FinanceInstallmentDomainService installmentService) {
        return new FinanceAiCaptureDomainService(repository, parser, ledgerService, installmentService);
    }
}
