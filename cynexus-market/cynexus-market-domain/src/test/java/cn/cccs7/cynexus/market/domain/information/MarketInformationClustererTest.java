package cn.cccs7.cynexus.market.domain.information;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketInformationClustererTest {
    private final MarketInformationClusterer clusterer = new MarketInformationClusterer();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-07-14T08:00:00+08:00");

    @Test
    void mergesSimilarCrossSourceEventsWithin48Hours() {
        var clusters = clusterer.cluster(List.of(
                candidate("cls.telegraph", "1", "央行下调政策利率支持实体经济", "Macro", now),
                candidate("pboc.news", "2", "央行下调政策利率 支持实体经济发展", "Macro", now.plusHours(2))));

        assertEquals(1, clusters.size());
        assertEquals(2, clusters.getFirst().sourceCount());
        assertEquals(2, clusters.getFirst().members().size());
    }

    @Test
    void keepsEventsOutside48HourWindowSeparate() {
        var clusters = clusterer.cluster(List.of(
                candidate("cls.telegraph", "1", "监管发布资本市场改革措施", "Regulation", now),
                candidate("csrc.news", "2", "监管发布资本市场改革措施", "Regulation", now.plusHours(49))));

        assertEquals(2, clusters.size());
    }

    @Test
    void retainsActiveClusterWhenRecentMembersExtendItsWindow() {
        var clusters = clusterer.cluster(List.of(
                candidate("cls.telegraph", "1", "监管发布资本市场改革措施", "Regulation", now),
                candidate("xinhua.finance", "2", "监管发布资本市场改革措施", "Regulation", now.plusHours(47)),
                candidate("csrc.news", "3", "监管发布资本市场改革措施", "Regulation", now.plusHours(49))));

        assertEquals(1, clusters.size());
        assertEquals(3, clusters.getFirst().members().size());
    }

    @Test
    void stableIdentityDoesNotDependOnInputOrderAndContentChangesInvalidateHash() {
        var first = candidate("gov.cn.policy", "policy-1", "国务院发布产业支持政策", "Policy", now);
        var confirmation = candidate("xinhua.finance", "news-2", "国务院发布产业支持政策", "Policy", now.plusHours(1));
        var normal = clusterer.cluster(List.of(first, confirmation)).getFirst();
        var reversed = clusterer.cluster(List.of(confirmation, first)).getFirst();
        var changed = clusterer.cluster(List.of(first, new MarketInformationClusterer.Candidate(
                confirmation.source(), confirmation.sourceRecordId(), confirmation.title(), "新增政策执行细则",
                confirmation.category(), confirmation.publishedAt(), confirmation.url(),
                confirmation.subjectKey(), confirmation.issuer(), confirmation.contentQuality()))).getFirst();

        assertEquals(normal.id(), reversed.id());
        assertEquals(normal.key(), reversed.key());
        assertNotEquals(normal.contentHash(), changed.contentHash());
        assertTrue(normal.highValue());
    }

    @Test
    void keepsGenericCompanyAnnouncementsWithDifferentSubjectsSeparate() {
        var clusters = clusterer.cluster(List.of(
                companyCandidate("cninfo.regulation", "a1", "股票交易异常波动公告", "000001", "平安银行"),
                companyCandidate("cninfo.regulation", "a2", "股票交易异常波动公告", "600000", "浦发银行")));

        assertEquals(2, clusters.size());
    }

    @Test
    void mergesCrossSourceCompanyEventWhenSubjectOrEntityMatches() {
        var clusters = clusterer.cluster(List.of(
                companyCandidate("cninfo.regulation", "a1", "平安银行股票交易异常波动公告", "000001", "平安银行"),
                new MarketInformationClusterer.Candidate("cls.telegraph", "n1", "平安银行回应股票交易异常波动",
                        "平安银行发布说明", "Regulation", now.plusHours(1), null, null, "平安银行", "summary_only")));

        assertEquals(1, clusters.size());
        assertEquals(2, clusters.getFirst().sourceCount());
    }

    @Test
    void routineSingleCompanyNoticeIsNotPromotedByCnInfoSourceAlone() {
        var cluster = clusterer.cluster(List.of(companyCandidate(
                "cninfo.regulation", "routine-1", "关于公司股票交易异常波动的公告", "600001", "示例公司"))).getFirst();

        assertFalse(cluster.highValue());
        assertTrue(cluster.priorityScore() < 3.0);
    }

    @Test
    void materialCompanyFactWithFullTextCanBecomeHighValue() {
        var cluster = clusterer.cluster(List.of(new MarketInformationClusterer.Candidate(
                "cninfo.regulation", "material-1", "示例公司签订重大订单公告",
                "示例公司签订金额占上年度营业收入45%的重大订单", "Regulation", now,
                "https://static.cninfo.com.cn/material.pdf", "600001", "示例公司", "full_text"))).getFirst();

        assertTrue(cluster.highValue());
        assertTrue(cluster.priorityScore() >= 5.2);
    }

    @Test
    void inquiryReplyWithConnectingWordsRemainsRoutine() {
        var cluster = clusterer.cluster(List.of(new MarketInformationClusterer.Candidate(
                "cninfo.regulation", "inquiry-1", "关于2025年年度报告信息披露监管问询函的回复公告",
                "示例公司 600001", "Regulation", now, "https://static.cninfo.com.cn/inquiry.pdf",
                "600001", "示例公司", "full_text"))).getFirst();

        assertFalse(cluster.highValue());
    }

    @Test
    void mergesDifferentHeadlinesReferencingTheSameNamedPolicy() {
        var clusters = clusterer.cluster(List.of(
                candidate("gov.cn.policy", "policy-1", "国务院关于印发《扩大消费十五五规划》的通知", "Policy", now),
                candidate("xinhua.finance", "policy-2", "《扩大消费十五五规划》明确重点任务", "Policy", now.plusHours(2))));

        assertEquals(1, clusters.size());
        assertEquals(2, clusters.getFirst().sourceCount());
    }

    @Test
    void namedPolicyPromotesMediaWireToPolicyCluster() {
        var clusters = clusterer.cluster(List.of(
                candidate("cls.telegraph", "wire-1", "国务院批复《扩大消费十五五规划》", "Market Wire", now),
                candidate("gov.cn.policy", "policy-1", "《扩大消费十五五规划》明确重点任务", "Policy", now.plusHours(2))));

        assertEquals(1, clusters.size());
        assertEquals("Policy", clusters.getFirst().category());
        assertEquals(2, clusters.getFirst().sourceCount());
    }

    private MarketInformationClusterer.Candidate candidate(String source, String id, String title,
                                                            String category, OffsetDateTime publishedAt) {
        return new MarketInformationClusterer.Candidate(source, id, title, title, category, publishedAt, null, null, null, "full_text");
    }

    private MarketInformationClusterer.Candidate companyCandidate(String source, String id, String title,
                                                                   String code, String issuer) {
        return new MarketInformationClusterer.Candidate(source, id, title, issuer + " " + code,
                "Regulation", now, null, code, issuer, "summary_only");
    }
}
