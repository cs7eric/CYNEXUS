package cn.cccs7.cynexus.market.domain.information;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class MarketInformationClusterer {
    private static final Duration WINDOW = Duration.ofHours(48);
    private static final double SIMILARITY_THRESHOLD = 0.62;
    private static final Pattern ENTITY_PATTERN = Pattern.compile("[\\p{IsHan}A-Za-z0-9]{3,16}");
    private static final Pattern DOCUMENT_PATTERN = Pattern.compile("《([^》]{4,48})》");
    private static final Set<String> STOP_WORDS = Set.of("有限公司", "股份有限公司", "关于", "公告", "消息", "表示", "市场", "今日", "最新");
    private static final Set<String> MATERIAL_FACT_KEYWORDS = Set.of(
            "业绩预增", "业绩快报", "扭亏", "中标", "订单", "合同", "回购", "增持", "减持", "并购", "重组",
            "收购", "出售", "获批", "批准", "立案", "处罚", "停产", "复产", "扩产", "投产", "签署", "签订");
    private static final Set<String> POLICY_ACTION_KEYWORDS = Set.of(
            "降准", "降息", "利率", "税率", "补贴", "专项债", "规划", "实施方案", "指导意见", "管理办法",
            "改革措施", "支持政策", "政策措施", "正式发布", "国务院常务会议", "货币政策", "财政政策");
    private static final Set<String> LEADER_KEYWORDS = Set.of("龙头", "央企", "行业第一", "全球领先", "核心供应商");
    private static final Set<String> MATERIALITY_SIGNAL_KEYWORDS = Set.of(
            "重大", "占上年度营业收入", "占最近一期", "亿元", "创历史新高", "首次", "独家", "核心资产");

    public List<Cluster> cluster(List<Candidate> input) {
        List<Candidate> ordered = input.stream()
                .filter(item -> item.title() != null && !item.title().isBlank() && item.publishedAt() != null)
                .sorted(Comparator.comparing(Candidate::publishedAt).thenComparing(Candidate::identity))
                .toList();
        List<MutableCluster> clusters = new ArrayList<>();
        for (Candidate candidate : ordered) {
            String normalized = normalize(candidate.title());
            Set<String> entities = entities(safe(candidate.issuer()) + " " + candidate.title() + " " + safe(candidate.summary()));
            Set<String> documentKeys = documentKeys(candidate.title() + " " + safe(candidate.summary()));
            MutableCluster best = null;
            double bestScore = 0;
            for (MutableCluster cluster : clusters) {
                if (Duration.between(cluster.lastPublishedAt, candidate.publishedAt()).abs().compareTo(WINDOW) > 0) continue;
                boolean sameDocument = overlap(cluster.documentKeys, documentKeys) > 0;
                if (!categoryCompatible(cluster.category, candidate.category(), sameDocument)) continue;
                if (!cluster.subjectCompatible(candidate.subjectKey())) continue;
                double titleScore = bigramSimilarity(cluster.normalizedTitle, normalized);
                double entityScore = overlap(cluster.entities, entities);
                double score = titleScore * 0.82 + entityScore * 0.18;
                if (sameDocument) score = Math.max(score, 0.9);
                boolean sameSubject = cluster.hasSubject(candidate.subjectKey());
                boolean exactTitleCompatible = normalized.equals(cluster.normalizedTitle)
                        && (sameSubject || (!cluster.hasSubjects() && safe(candidate.subjectKey()).isBlank()) || entityScore > 0);
                if ((sameDocument || exactTitleCompatible || score >= SIMILARITY_THRESHOLD) && score >= bestScore) {
                    best = cluster;
                    bestScore = score;
                }
            }
            if (best == null) {
                best = new MutableCluster(candidate, normalized, entities, documentKeys);
                clusters.add(best);
                bestScore = 1;
            } else {
                best.add(candidate, entities, documentKeys);
            }
            best.members.add(new Member(candidate, bestScore));
        }
        return clusters.stream().map(MutableCluster::freeze).toList();
    }

    public String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}\\p{S}\\s]+", "")
                .replaceAll("(公告|快讯|最新|突发)$", "");
    }

    public double bigramSimilarity(String left, String right) {
        if (left.equals(right)) return 1;
        Set<String> a = bigrams(left);
        Set<String> b = bigrams(right);
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    private Set<String> bigrams(String value) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i + 1 < value.length(); i++) result.add(value.substring(i, i + 2));
        return result;
    }

    private Set<String> entities(String value) {
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = ENTITY_PATTERN.matcher(value);
        while (matcher.find() && result.size() < 20) {
            String token = matcher.group();
            if (!STOP_WORDS.contains(token)) result.add(token);
        }
        return result;
    }

    private Set<String> documentKeys(String value) {
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = DOCUMENT_PATTERN.matcher(value);
        while (matcher.find()) {
            String key = normalize(matcher.group(1));
            if (!key.isBlank()) result.add(key);
        }
        return result;
    }

    private double overlap(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return (double) intersection.size() / Math.min(left.size(), right.size());
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash market information cluster", exception);
        }
    }

    public record Candidate(String source, String sourceRecordId, String title, String summary, String category,
                            OffsetDateTime publishedAt, String url, String subjectKey, String issuer,
                            String contentQuality) {
        public String identity() { return source + ":" + sourceRecordId; }
    }

    public record Member(Candidate candidate, double similarity) { }

    public record Cluster(long id, String key, String displayTitle, String normalizedTitle, String category,
                          String representativeSource, String representativeRecordId, Set<String> entities,
                          OffsetDateTime firstPublishedAt, OffsetDateTime lastPublishedAt, int sourceCount,
                          double priorityScore, boolean highValue, String contentHash, List<Member> members) { }

    private static final class MutableCluster {
        private final Candidate representative;
        private final String normalizedTitle;
        private String category;
        private final Set<String> entities = new LinkedHashSet<>();
        private final Set<String> documentKeys = new LinkedHashSet<>();
        private final Set<String> sources = new HashSet<>();
        private final Set<String> subjectKeys = new HashSet<>();
        private final List<Member> members = new ArrayList<>();
        private OffsetDateTime firstPublishedAt;
        private OffsetDateTime lastPublishedAt;

        private MutableCluster(Candidate candidate, String normalizedTitle, Set<String> entities, Set<String> documentKeys) {
            this.representative = candidate;
            this.normalizedTitle = normalizedTitle;
            this.category = candidate.category();
            this.entities.addAll(entities);
            this.documentKeys.addAll(documentKeys);
            this.sources.add(candidate.source());
            if (!safe(candidate.subjectKey()).isBlank()) this.subjectKeys.add(candidate.subjectKey());
            this.firstPublishedAt = candidate.publishedAt();
            this.lastPublishedAt = candidate.publishedAt();
        }

        private void add(Candidate candidate, Set<String> candidateEntities, Set<String> candidateDocumentKeys) {
            entities.addAll(candidateEntities);
            documentKeys.addAll(candidateDocumentKeys);
            if ("market wire".equalsIgnoreCase(category) && !"market wire".equalsIgnoreCase(safe(candidate.category()))) {
                category = candidate.category();
            }
            sources.add(candidate.source());
            if (!safe(candidate.subjectKey()).isBlank()) subjectKeys.add(candidate.subjectKey());
            if (candidate.publishedAt().isBefore(firstPublishedAt)) firstPublishedAt = candidate.publishedAt();
            if (candidate.publishedAt().isAfter(lastPublishedAt)) lastPublishedAt = candidate.publishedAt();
        }

        private boolean subjectCompatible(String candidateSubject) {
            String subject = safe(candidateSubject);
            return subject.isBlank() || subjectKeys.isEmpty() || subjectKeys.contains(subject);
        }

        private boolean hasSubject(String candidateSubject) {
            String subject = safe(candidateSubject);
            return !subject.isBlank() && subjectKeys.contains(subject);
        }

        private boolean hasSubjects() {
            return !subjectKeys.isEmpty();
        }

        private Cluster freeze() {
            String anchor = members.stream().map(item -> item.candidate().identity()).min(String::compareTo)
                    .orElse(representative.identity());
            String key = hash(category + "|" + anchor).substring(0, 32);
            long id = Long.parseUnsignedLong(hash("cluster|" + key).substring(0, 15), 16);
            String contentHash = hash(members.stream().map(item -> item.candidate().identity() + "|" + item.candidate().title()
                    + "|" + safe(item.candidate().summary())).sorted().reduce("", (a, b) -> a + "\n" + b));
            double score = priorityScore();
            return new Cluster(id, key, representative.title(), normalizedTitle, category, representative.source(),
                    representative.sourceRecordId(), Set.copyOf(entities), firstPublishedAt, lastPublishedAt,
                    sources.size(), score, score >= 5.2, contentHash, List.copyOf(members));
        }

        private double priorityScore() {
            String text = members.stream()
                    .map(item -> item.candidate().title() + " " + safe(item.candidate().summary()) + " " + safe(item.candidate().issuer()))
                    .reduce("", (left, right) -> left + " " + right);
            boolean materialFact = containsAny(text, MATERIAL_FACT_KEYWORDS);
            boolean policyAction = containsAny(text, POLICY_ACTION_KEYWORDS);
            boolean companySpecific = !subjectKeys.isEmpty();
            boolean fullText = members.stream().anyMatch(item -> Set.of("full_text", "ocr").contains(
                    safe(item.candidate().contentQuality()).toLowerCase(Locale.ROOT)));

            double score = switch (category.toLowerCase(Locale.ROOT)) {
                case "policy" -> 2.4;
                case "macro" -> 2.2;
                case "industry" -> 1.6;
                case "regulation" -> 1.2;
                default -> 0.8;
            };
            if (sources.stream().anyMatch(MarketInformationClusterer::officialSource)) score += 1.2;
            score += Math.min(2.2, Math.max(0, sources.size() - 1) * 0.9);
            if (members.size() >= 3) score += 0.5;
            if (!entities.isEmpty()) score += 0.2;
            if (!companySpecific && Set.of("policy", "macro", "industry").contains(category.toLowerCase(Locale.ROOT))) score += 0.8;
            if (policyAction) score += 1.4;
            if (materialFact) score += 1.8;
            if (materialFact && fullText && containsAny(text, MATERIALITY_SIGNAL_KEYWORDS)) score += 0.7;
            if (companySpecific && materialFact && (sources.size() > 1 || containsAny(text, LEADER_KEYWORDS))) score += 0.6;
            if (fullText) score += 0.6;
            else score -= 0.6;
            score += recencyScore(lastPublishedAt);
            // A single-source routine notice should remain conservative, but a full-text
            // material fact with an explicit materiality signal is already sufficiently
            // evidenced to avoid that routine-notice discount.
            if (companySpecific && sources.size() == 1
                    && !(materialFact && fullText && containsAny(text, MATERIALITY_SIGNAL_KEYWORDS))) score -= 0.7;
            score -= routinePenalty(text, materialFact || policyAction);
            return Math.round(Math.max(0, score) * 100.0) / 100.0;
        }
    }

    private static boolean officialSource(String source) {
        return safe(source).matches("(gov\\.cn\\.policy|pboc\\.news|stats\\.gov\\.cn\\.release|mof\\.policy|ndrc\\.news|csrc\\.news|szse\\.disclosure|cninfo\\..*)");
    }

    private static boolean containsAny(String value, Set<String> keywords) {
        return keywords.stream().anyMatch(value::contains);
    }

    private static boolean categoryCompatible(String left, String right, boolean sameDocument) {
        if (safe(left).equalsIgnoreCase(safe(right))) return true;
        return sameDocument && ("market wire".equalsIgnoreCase(safe(left)) || "market wire".equalsIgnoreCase(safe(right)));
    }

    private static double routinePenalty(String text, boolean materialAction) {
        if (text.contains("股票交易异常波动") || text.contains("严重异常波动")) return 3.6;
        if (text.contains("问询函") && (text.contains("回复") || text.contains("核查意见"))) return 2.8;
        if (text.contains("风险提示公告")) return 2.2;
        if (!materialAction && text.contains("公告")) return 1.2;
        return 0;
    }

    private static double recencyScore(OffsetDateTime publishedAt) {
        if (publishedAt == null) return 0;
        long ageHours = Math.max(0, Duration.between(publishedAt, OffsetDateTime.now(publishedAt.getOffset())).toHours());
        if (ageHours <= 48) return 0.6;
        if (ageHours <= 24 * 7) return 0.3;
        return 0;
    }
}
