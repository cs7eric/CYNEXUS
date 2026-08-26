package cn.cccs7.cynexus.market.domain.factorresearch;

public interface EventReplayDomainService {
    EventReplayBO.Result replay(EventReplayBO.Query query);
}
