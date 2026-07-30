package cn.cccs7.cynexus.market.domain.service.futures;

import cn.cccs7.cynexus.market.domain.entity.futures.FuturesPositionQueryBO;
import cn.cccs7.cynexus.market.domain.policy.FuturesPositionDomainPolicy;
import cn.cccs7.cynexus.market.domain.repository.FuturesPositionRepository;
import java.time.LocalDate;
import java.util.List;

public class FuturesPositionQueryDomainServiceImpl implements FuturesPositionQueryDomainService {
    private final FuturesPositionRepository repository;
    private final FuturesPositionDomainPolicy policy = new FuturesPositionDomainPolicy();

    public FuturesPositionQueryDomainServiceImpl(FuturesPositionRepository repository) {
        this.repository = repository;
    }

    @Override
    public FuturesPositionQueryBO.Latest latest(String tradeDate, String scopeType, String exchangeCode,
                                                String varietyCode, int top, String sort, String sortDir,
                                                Integer page, Integer pageSize) {
        return query(tradeDate, scopeType, exchangeCode, varietyCode, top, sort, sortDir, page, pageSize);
    }

    @Override
    public FuturesPositionQueryBO.Latest instruments(String tradeDate, String scopeType, String exchangeCode,
                                                     String varietyCode, int top, String sort, String sortDir,
                                                     Integer page, Integer pageSize) {
        return query(tradeDate, scopeType, exchangeCode, varietyCode, top, sort, sortDir, page, pageSize);
    }

    private FuturesPositionQueryBO.Latest query(String tradeDate, String scopeType, String exchangeCode,
                                                String varietyCode, int top, String sort, String sortDir,
                                                Integer page, Integer pageSize) {
        LocalDate requested = policy.parseDate(tradeDate).orElse(null);
        String scope = policy.scope(scopeType);
        String exchange = policy.exchange(exchangeCode);
        String variety = policy.textCode(varietyCode);
        int safeTop = policy.top(top);
        FuturesPositionDomainPolicy.Page slice = policy.page(page, pageSize);
        LocalDate serving = repository.findLatestTradeDate(requested).orElse(null);
        if (serving == null) {
            return new FuturesPositionQueryBO.Latest(false, "futures position data is empty", requested, null,
                    false, scope, exchange, variety, safeTop,
                    new FuturesPositionQueryBO.Page(slice.number(), slice.size(), slice.offset()), 0, List.of(),
                    emptyAggregate(), new FuturesPositionQueryBO.Coverage(null, List.of()), List.of());
        }
        List<FuturesPositionQueryBO.Instrument> items = repository.listInstruments(
                serving, scope, exchange, variety, safeTop, policy.sort(sort), policy.sortDir(sortDir), slice.offset(), slice.size());
        boolean fallback = requested != null && !requested.equals(serving);
        List<String> warnings = fallback ? List.of("requested date has no selected batch; latest prior trade date is served") : List.of();
        return new FuturesPositionQueryBO.Latest(true, null, requested, serving, fallback, scope, exchange, variety,
                safeTop, new FuturesPositionQueryBO.Page(slice.number(), slice.size(), slice.offset()),
                repository.countInstruments(serving, scope, exchange, variety), items,
                repository.aggregate(serving, scope, exchange, variety, safeTop),
                new FuturesPositionQueryBO.Coverage(serving, repository.listCoverage(serving)), warnings);
    }

    @Override
    public List<FuturesPositionQueryBO.Member> members(String tradeDate, String scopeType, String exchangeCode,
                                                       String instrumentCode, String rankType, int top) {
        LocalDate date = repository.findLatestTradeDate(policy.parseDate(tradeDate).orElse(null)).orElse(null);
        if (date == null) return List.of();
        String safeRankType = rankType == null || rankType.isBlank() ? null : rankType.trim().toUpperCase();
        if (safeRankType != null && !List.of("VOLUME", "LONG", "SHORT").contains(safeRankType)) {
            throw new IllegalArgumentException("rankType must be VOLUME, LONG, or SHORT");
        }
        return repository.listMembers(date, policy.scope(scopeType), policy.exchange(exchangeCode),
                policy.textCode(instrumentCode), safeRankType, policy.top(top));
    }

    @Override
    public List<FuturesPositionQueryBO.History> history(String scopeType, String exchangeCode,
                                                         String instrumentCode, int top, int limit) {
        return repository.listHistory(policy.scope(scopeType), policy.exchange(exchangeCode),
                policy.textCode(instrumentCode), policy.top(top), policy.limit(limit, 120));
    }

    @Override
    public FuturesPositionQueryBO.Coverage coverage(String tradeDate) {
        LocalDate date = repository.findLatestTradeDate(policy.parseDate(tradeDate).orElse(null)).orElse(null);
        return new FuturesPositionQueryBO.Coverage(date, date == null ? List.of() : repository.listCoverage(date));
    }

    private FuturesPositionQueryBO.Aggregate emptyAggregate() {
        return new FuturesPositionQueryBO.Aggregate(null, null, null, null, null, null, null);
    }
}
