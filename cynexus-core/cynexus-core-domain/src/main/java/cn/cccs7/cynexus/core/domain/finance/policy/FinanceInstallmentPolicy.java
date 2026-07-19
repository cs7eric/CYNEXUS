package cn.cccs7.cynexus.core.domain.finance.policy;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCreditInstallmentBO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FinanceInstallmentPolicy {
    public List<FinanceCreditInstallmentBO> schedule(BigDecimal totalAmount, BigDecimal feeTotal,
                                                      int count, LocalDate firstDueDate, int intervalMonths,
                                                      Long planId) {
        require(totalAmount != null && totalAmount.signum() > 0, "分期总金额必须大于 0");
        require(feeTotal != null && feeTotal.signum() >= 0, "分期费用不能为负数");
        require(count >= 2 && count <= 60, "分期期数必须在 2 到 60 期之间");
        require(firstDueDate != null, "首期还款日不能为空");
        require(intervalMonths >= 1 && intervalMonths <= 12, "分期间隔必须在 1 到 12 个月之间");

        BigDecimal[] principals = split(totalAmount, count);
        BigDecimal[] fees = split(feeTotal, count);
        List<FinanceCreditInstallmentBO> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            result.add(FinanceCreditInstallmentBO.builder().planId(planId).installmentNo(index + 1)
                    .dueDate(firstDueDate.plusMonths((long) index * intervalMonths)).principalAmount(principals[index])
                    .feeAmount(fees[index]).amount(principals[index].add(fees[index])).status("pending").build());
        }
        return result;
    }

    private BigDecimal[] split(BigDecimal total, int count) {
        BigDecimal normalized = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal base = normalized.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal remainder = normalized.subtract(base.multiply(BigDecimal.valueOf(count)));
        BigDecimal[] result = new BigDecimal[count];
        for (int index = 0; index < count; index++) result[index] = base;
        result[count - 1] = base.add(remainder);
        return result;
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new BizException(ErrorCode.BAD_REQUEST, message);
    }
}
