package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAccountBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAiCaptureBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCategoryBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceOverviewBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceTransactionBO;
import cn.cccs7.cynexus.core.domain.finance.port.FinanceRepository;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FinanceRepositoryImpl implements FinanceRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SnowflakeIdGenerator idGenerator;

    public FinanceRepositoryImpl(JdbcTemplate jdbcTemplate, SnowflakeIdGenerator idGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.idGenerator = idGenerator;
    }

    @Override
    public List<FinanceAccountBO> listAccounts(String ownerKey, boolean includeArchived) {
        return jdbcTemplate.query("""
                SELECT a.*, a.opening_balance + COALESCE(SUM(CASE
                    WHEN t.transaction_type = 'expense' THEN CASE WHEN a.nature = 'asset' THEN -t.amount ELSE t.amount END
                    WHEN t.transaction_type = 'income' THEN CASE WHEN a.nature = 'asset' THEN t.amount ELSE -t.amount END
                    WHEN t.transaction_type = 'transfer' AND t.account_id = a.id THEN CASE WHEN a.nature = 'asset' THEN -t.amount ELSE t.amount END
                    WHEN t.transaction_type = 'transfer' AND t.to_account_id = a.id THEN CASE WHEN a.nature = 'asset' THEN t.amount ELSE -t.amount END
                    ELSE 0 END
                ), 0) AS calculated_balance
                  FROM finance_accounts a
                  LEFT JOIN finance_transactions t
                    ON (t.account_id = a.id OR t.to_account_id = a.id)
                   AND t.deleted_at IS NULL AND t.is_archived = false
                 WHERE a.owner_key = ? AND a.deleted_at IS NULL
                   AND (? = true OR a.is_archived = false)
                 GROUP BY a.id
                 ORDER BY a.is_archived, a.nature, a.created_at DESC
                """, this::accountRow, ownerKey, includeArchived);
    }

    @Override
    public FinanceAccountBO findAccount(String ownerKey, Long id) {
        return jdbcTemplate.query("""
                SELECT a.*, a.opening_balance + COALESCE(SUM(CASE
                    WHEN t.transaction_type = 'expense' THEN CASE WHEN a.nature = 'asset' THEN -t.amount ELSE t.amount END
                    WHEN t.transaction_type = 'income' THEN CASE WHEN a.nature = 'asset' THEN t.amount ELSE -t.amount END
                    WHEN t.transaction_type = 'transfer' AND t.account_id = a.id THEN CASE WHEN a.nature = 'asset' THEN -t.amount ELSE t.amount END
                    WHEN t.transaction_type = 'transfer' AND t.to_account_id = a.id THEN CASE WHEN a.nature = 'asset' THEN t.amount ELSE -t.amount END
                    ELSE 0 END
                ), 0) AS calculated_balance
                  FROM finance_accounts a
                  LEFT JOIN finance_transactions t ON (t.account_id = a.id OR t.to_account_id = a.id)
                   AND t.deleted_at IS NULL AND t.is_archived = false
                 WHERE a.owner_key = ? AND a.id = ? AND a.deleted_at IS NULL
                 GROUP BY a.id
                """, this::accountRow, ownerKey, id).stream().findFirst().orElse(null);
    }

    @Override
    public Long saveAccount(FinanceAccountBO account) {
        long id = idGenerator.nextId();
        jdbcTemplate.update("""
                INSERT INTO finance_accounts (id, owner_key, name, account_type, nature, currency, opening_balance,
                    credit_limit, billing_day, due_day, color, notes, is_archived)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false)
                """, id, account.getOwnerKey(), account.getName(), account.getAccountType(), account.getNature(),
                value(account.getCurrency(), "CNY"), value(account.getOpeningBalance(), BigDecimal.ZERO), account.getCreditLimit(),
                account.getBillingDay(), account.getDueDay(), account.getColor(), account.getNotes());
        return id;
    }

    @Override
    public boolean updateAccount(FinanceAccountBO account) {
        return jdbcTemplate.update("""
                UPDATE finance_accounts SET name = ?, account_type = ?, nature = ?, currency = ?, opening_balance = ?,
                    credit_limit = ?, billing_day = ?, due_day = ?, color = ?, notes = ?, updated_at = now()
                 WHERE id = ? AND owner_key = ? AND deleted_at IS NULL
                """, account.getName(), account.getAccountType(), account.getNature(), value(account.getCurrency(), "CNY"),
                value(account.getOpeningBalance(), BigDecimal.ZERO), account.getCreditLimit(), account.getBillingDay(), account.getDueDay(),
                account.getColor(), account.getNotes(), account.getId(), account.getOwnerKey()) > 0;
    }

    @Override
    public boolean archiveAccount(String ownerKey, Long id) {
        return jdbcTemplate.update("UPDATE finance_accounts SET is_archived = true, updated_at = now() WHERE id = ? AND owner_key = ? AND deleted_at IS NULL", id, ownerKey) > 0;
    }

    @Override
    public List<FinanceCategoryBO> listCategories(String ownerKey, boolean includeArchived) {
        return jdbcTemplate.query("""
                SELECT * FROM finance_categories
                 WHERE owner_key = ? AND deleted_at IS NULL AND (? = true OR is_archived = false)
                 ORDER BY direction, sort_order, name
                """, this::categoryRow, ownerKey, includeArchived);
    }

    @Override
    public Long saveCategory(FinanceCategoryBO category) {
        long id = idGenerator.nextId();
        jdbcTemplate.update("""
                INSERT INTO finance_categories (id, owner_key, parent_id, name, direction, icon, color, sort_order, is_system)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, false)
                """, id, category.getOwnerKey(), category.getParentId(), category.getName(), category.getDirection(),
                category.getIcon(), category.getColor(), value(category.getSortOrder(), 0));
        return id;
    }

    @Override
    public Long saveTransaction(FinanceTransactionBO transaction) {
        long id = idGenerator.nextId();
        jdbcTemplate.update("""
                INSERT INTO finance_transactions (id, owner_key, transaction_type, account_id, to_account_id, category_id,
                    amount, currency, occurred_at, merchant, note, source, source_ref)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, transaction.getOwnerKey(), transaction.getTransactionType(), transaction.getAccountId(),
                transaction.getToAccountId(), transaction.getCategoryId(), transaction.getAmount(), value(transaction.getCurrency(), "CNY"),
                transaction.getOccurredAt(), transaction.getMerchant(), transaction.getNote(), value(transaction.getSource(), "manual"), transaction.getSourceRef());
        return id;
    }

    @Override
    public boolean updateTransaction(FinanceTransactionBO transaction) {
        return jdbcTemplate.update("""
                UPDATE finance_transactions SET transaction_type = ?, account_id = ?, to_account_id = ?, category_id = ?,
                    amount = ?, currency = ?, occurred_at = ?, merchant = ?, note = ?, updated_at = now()
                 WHERE id = ? AND owner_key = ? AND deleted_at IS NULL
                """, transaction.getTransactionType(), transaction.getAccountId(), transaction.getToAccountId(), transaction.getCategoryId(),
                transaction.getAmount(), value(transaction.getCurrency(), "CNY"), transaction.getOccurredAt(), transaction.getMerchant(),
                transaction.getNote(), transaction.getId(), transaction.getOwnerKey()) > 0;
    }

    @Override
    public boolean archiveTransaction(String ownerKey, Long id) {
        return jdbcTemplate.update("UPDATE finance_transactions SET is_archived = true, updated_at = now() WHERE id = ? AND owner_key = ? AND deleted_at IS NULL", id, ownerKey) > 0;
    }

    @Override
    public FinanceTransactionBO findTransaction(String ownerKey, Long id) {
        return jdbcTemplate.query(transactionSql(" AND t.id = ?"), this::transactionRow, ownerKey, id).stream().findFirst().orElse(null);
    }

    @Override
    public List<FinanceTransactionBO> listTransactions(String ownerKey, LocalDate start, LocalDate end, int limit) {
        return jdbcTemplate.query(transactionSql(" AND t.occurred_at >= ? AND t.occurred_at < ? ORDER BY t.occurred_at DESC, t.id DESC LIMIT ?"),
                this::transactionRow, ownerKey, atStart(start), atStart(end.plusDays(1)), Math.max(1, Math.min(limit, 200)));
    }

    @Override
    public FinanceOverviewBO overview(String ownerKey, LocalDate start, LocalDate end) {
        List<FinanceTransactionBO> transactions = listTransactions(ownerKey, start, end, 200);
        BigDecimal[] totals = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount) FILTER (WHERE transaction_type = 'income'), 0),
                       COALESCE(SUM(amount) FILTER (WHERE transaction_type = 'expense'), 0)
                  FROM finance_transactions
                 WHERE owner_key = ? AND deleted_at IS NULL AND is_archived = false
                   AND occurred_at >= ? AND occurred_at < ?
                """, (rs, rowNum) -> new BigDecimal[]{rs.getBigDecimal(1), rs.getBigDecimal(2)},
                ownerKey, atStart(start), atStart(end.plusDays(1)));
        BigDecimal income = totals == null ? BigDecimal.ZERO : value(totals[0], BigDecimal.ZERO);
        BigDecimal expense = totals == null ? BigDecimal.ZERO : value(totals[1], BigDecimal.ZERO);
        BigDecimal total = income.add(expense);
        List<FinanceOverviewBO.CategoryStatBO> categories = jdbcTemplate.query("""
                SELECT c.id, c.name, t.transaction_type, SUM(t.amount) amount
                  FROM finance_transactions t JOIN finance_categories c ON c.id = t.category_id
                 WHERE t.owner_key = ? AND t.deleted_at IS NULL AND t.is_archived = false
                   AND t.occurred_at >= ? AND t.occurred_at < ? AND t.transaction_type IN ('expense', 'income')
                 GROUP BY c.id, c.name, t.transaction_type ORDER BY amount DESC
                """, (rs, rowNum) -> FinanceOverviewBO.CategoryStatBO.builder().categoryId(rs.getLong("id"))
                .categoryName(rs.getString("name")).direction(rs.getString("transaction_type")).amount(rs.getBigDecimal("amount"))
                .percentage(percent(rs.getBigDecimal("amount"), total)).build(), ownerKey, atStart(start), atStart(end.plusDays(1)));
        List<FinanceOverviewBO.DailyStatBO> daily = jdbcTemplate.query("""
                SELECT occurred_at::date AS day_date,
                       COALESCE(SUM(amount) FILTER (WHERE transaction_type = 'income'), 0) income,
                       COALESCE(SUM(amount) FILTER (WHERE transaction_type = 'expense'), 0) expense
                  FROM finance_transactions
                 WHERE owner_key = ? AND deleted_at IS NULL AND is_archived = false
                   AND occurred_at >= ? AND occurred_at < ?
                 GROUP BY occurred_at::date ORDER BY day_date
                """, (rs, rowNum) -> FinanceOverviewBO.DailyStatBO.builder().date(rs.getObject("day_date", LocalDate.class))
                .income(rs.getBigDecimal("income")).expense(rs.getBigDecimal("expense")).build(), ownerKey, atStart(start), atStart(end.plusDays(1)));
        return FinanceOverviewBO.builder().startDate(start).endDate(end).income(income).expense(expense).net(income.subtract(expense))
                .accounts(listAccounts(ownerKey, false)).categories(categories).daily(daily).recentTransactions(transactions.stream().limit(12).toList()).build();
    }

    @Override
    public Long createCapture(FinanceAiCaptureBO capture) {
        long id = idGenerator.nextId();
        jdbcTemplate.update("INSERT INTO finance_ai_captures (id, owner_key, status, source_count, idempotency_key) VALUES (?, ?, ?, ?, ?)", id, capture.getOwnerKey(), capture.getStatus(), value(capture.getSourceCount(), 0), capture.getIdempotencyKey());
        return id;
    }

    @Override
    public FinanceAiCaptureBO findCaptureByIdempotencyKey(String ownerKey, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return null;
        return jdbcTemplate.query("SELECT * FROM finance_ai_captures WHERE owner_key = ? AND idempotency_key = ? AND deleted_at IS NULL ORDER BY created_at DESC LIMIT 1",
                (rs, rowNum) -> FinanceAiCaptureBO.builder().id(rs.getLong("id")).ownerKey(rs.getString("owner_key"))
                        .status(rs.getString("status")).sourceCount(rs.getInt("source_count")).modelCode(rs.getString("model_code"))
                        .rawResponse(rs.getString("raw_response")).errorMessage(rs.getString("error_message"))
                        .idempotencyKey(rs.getString("idempotency_key")).createdAt(rs.getObject("created_at", OffsetDateTime.class))
                        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class)).build(), ownerKey, idempotencyKey).stream().findFirst().map(capture -> {
                            capture.setItems(jdbcTemplate.query("""
                                    SELECT i.*, c.name category_name, a.name account_name
                                      FROM finance_ai_capture_items i
                                      LEFT JOIN finance_categories c ON c.id = i.category_id
                                      LEFT JOIN finance_accounts a ON a.id = i.account_id
                                     WHERE i.capture_id = ? AND i.deleted_at IS NULL ORDER BY i.item_index, i.id
                                    """, (rs, rowNum) -> itemRow(rs), capture.getId()));
                            return capture;
                        }).orElse(null);
    }

    @Override
    public boolean updateCapture(FinanceAiCaptureBO capture) {
        return jdbcTemplate.update("UPDATE finance_ai_captures SET status = ?, source_count = ?, model_code = ?, raw_response = CAST(? AS jsonb), error_message = ?, updated_at = now() WHERE id = ? AND owner_key = ? AND deleted_at IS NULL",
                capture.getStatus(), value(capture.getSourceCount(), 0), capture.getModelCode(), value(capture.getRawResponse(), "{}"), capture.getErrorMessage(), capture.getId(), capture.getOwnerKey()) > 0;
    }

    @Override
    public FinanceAiCaptureBO findCapture(String ownerKey, Long captureId) {
        FinanceAiCaptureBO capture = jdbcTemplate.query("SELECT * FROM finance_ai_captures WHERE id = ? AND owner_key = ? AND deleted_at IS NULL", (rs, rowNum) -> FinanceAiCaptureBO.builder().id(rs.getLong("id")).ownerKey(rs.getString("owner_key"))
                .status(rs.getString("status")).sourceCount(rs.getInt("source_count")).modelCode(rs.getString("model_code")).rawResponse(rs.getString("raw_response")).errorMessage(rs.getString("error_message")).idempotencyKey(rs.getString("idempotency_key"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class)).updatedAt(rs.getObject("updated_at", OffsetDateTime.class)).build(), captureId, ownerKey).stream().findFirst().orElse(null);
        if (capture == null) return null;
        capture.setItems(jdbcTemplate.query("""
                SELECT i.*, c.name category_name, a.name account_name
                  FROM finance_ai_capture_items i
                  LEFT JOIN finance_categories c ON c.id = i.category_id
                  LEFT JOIN finance_accounts a ON a.id = i.account_id
                 WHERE i.capture_id = ? AND i.deleted_at IS NULL ORDER BY i.item_index, i.id
                """, (rs, rowNum) -> itemRow(rs), captureId));
        return capture;
    }

    @Override
    public boolean archiveCapture(String ownerKey, Long captureId) {
        return jdbcTemplate.update("UPDATE finance_ai_captures SET status = 'archived', updated_at = now() WHERE id = ? AND owner_key = ? AND deleted_at IS NULL", captureId, ownerKey) > 0;
    }

    @Override
    public Long createCaptureImage(Long captureId, Integer imageIndex, String fileName, String contentType, long imageSize, String sha256, byte[] imageData) {
        long id = idGenerator.nextId();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO finance_ai_capture_images (id, capture_id, image_index, file_name, content_type, image_size, content_sha256, image_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            statement.setLong(1, id); statement.setLong(2, captureId); statement.setInt(3, imageIndex); statement.setString(4, fileName);
            statement.setString(5, contentType); statement.setLong(6, imageSize); statement.setString(7, sha256); statement.setBytes(8, imageData); return statement;
        });
        return id;
    }

    @Override
    public Long createCaptureItem(FinanceAiCaptureBO.Item item) {
        long id = idGenerator.nextId();
        jdbcTemplate.update("""
                INSERT INTO finance_ai_capture_items (id, capture_id, image_id, item_index, transaction_type, occurred_at, amount, currency,
                    merchant, category_hint, category_id, account_id, note, confidence, raw_text, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, item.getCaptureId(), item.getImageId(), value(item.getItemIndex(), 0), value(item.getTransactionType(), "expense"), item.getOccurredAt(),
                item.getAmount(), value(item.getCurrency(), "CNY"), item.getMerchant(), item.getCategoryHint(), item.getCategoryId(), item.getAccountId(),
                item.getNote(), item.getConfidence(), item.getRawText(), value(item.getStatus(), "draft"));
        return id;
    }

    @Override
    public boolean updateCaptureItem(FinanceAiCaptureBO.Item item) {
        return jdbcTemplate.update("""
                UPDATE finance_ai_capture_items SET transaction_type = ?, occurred_at = ?, amount = ?, currency = ?, merchant = ?, category_id = ?,
                    account_id = ?, note = ?, status = ?, transaction_id = ?, updated_at = now()
                 WHERE id = ? AND capture_id = ? AND deleted_at IS NULL
                """, item.getTransactionType(), item.getOccurredAt(), item.getAmount(), value(item.getCurrency(), "CNY"), item.getMerchant(),
                item.getCategoryId(), item.getAccountId(), item.getNote(), item.getStatus(), item.getTransactionId(), item.getId(), item.getCaptureId()) > 0;
    }

    private String transactionSql(String suffix) {
        return """
                SELECT t.*, a.name account_name, a.nature account_nature, ta.name to_account_name, c.name category_name,
                       (SELECT count(*) FROM finance_ai_capture_items ai WHERE ai.transaction_id = t.id AND ai.deleted_at IS NULL) attachment_count
                  FROM finance_transactions t
                  JOIN finance_accounts a ON a.id = t.account_id
                  LEFT JOIN finance_accounts ta ON ta.id = t.to_account_id
                  LEFT JOIN finance_categories c ON c.id = t.category_id
                 WHERE t.owner_key = ? AND t.deleted_at IS NULL AND t.is_archived = false
                """ + suffix;
    }

    private FinanceAccountBO accountRow(ResultSet rs, int rowNum) throws SQLException {
        return FinanceAccountBO.builder().id(rs.getLong("id")).ownerKey(rs.getString("owner_key")).name(rs.getString("name"))
                .accountType(rs.getString("account_type")).nature(rs.getString("nature")).currency(rs.getString("currency"))
                .openingBalance(rs.getBigDecimal("opening_balance")).creditLimit(rs.getBigDecimal("credit_limit"))
                .billingDay((Integer) rs.getObject("billing_day")).dueDay((Integer) rs.getObject("due_day")).color(rs.getString("color"))
                .notes(rs.getString("notes")).archived(rs.getBoolean("is_archived")).balance(rs.getBigDecimal("calculated_balance"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class)).updatedAt(rs.getObject("updated_at", OffsetDateTime.class)).build();
    }

    private FinanceCategoryBO categoryRow(ResultSet rs, int rowNum) throws SQLException {
        return FinanceCategoryBO.builder().id(rs.getLong("id")).ownerKey(rs.getString("owner_key")).parentId((Long) rs.getObject("parent_id"))
                .name(rs.getString("name")).direction(rs.getString("direction")).icon(rs.getString("icon")).color(rs.getString("color"))
                .sortOrder(rs.getInt("sort_order")).system(rs.getBoolean("is_system")).archived(rs.getBoolean("is_archived"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class)).updatedAt(rs.getObject("updated_at", OffsetDateTime.class)).build();
    }

    private FinanceTransactionBO transactionRow(ResultSet rs, int rowNum) throws SQLException {
        return FinanceTransactionBO.builder().id(rs.getLong("id")).ownerKey(rs.getString("owner_key")).transactionType(rs.getString("transaction_type"))
                .accountId(rs.getLong("account_id")).accountName(rs.getString("account_name")).accountNature(rs.getString("account_nature"))
                .toAccountId((Long) rs.getObject("to_account_id")).toAccountName(rs.getString("to_account_name"))
                .categoryId((Long) rs.getObject("category_id")).categoryName(rs.getString("category_name")).amount(rs.getBigDecimal("amount"))
                .currency(rs.getString("currency")).occurredAt(rs.getObject("occurred_at", OffsetDateTime.class)).merchant(rs.getString("merchant"))
                .note(rs.getString("note")).source(rs.getString("source")).sourceRef(rs.getString("source_ref")).archived(rs.getBoolean("is_archived"))
                .attachmentCount(rs.getInt("attachment_count")).createdAt(rs.getObject("created_at", OffsetDateTime.class)).updatedAt(rs.getObject("updated_at", OffsetDateTime.class)).build();
    }

    private FinanceAiCaptureBO.Item itemRow(ResultSet rs) throws SQLException {
        return FinanceAiCaptureBO.Item.builder().id(rs.getLong("id")).captureId(rs.getLong("capture_id")).imageId((Long) rs.getObject("image_id"))
                .itemIndex(rs.getInt("item_index")).transactionType(rs.getString("transaction_type")).occurredAt(rs.getObject("occurred_at", OffsetDateTime.class))
                .amount(rs.getBigDecimal("amount")).currency(rs.getString("currency")).merchant(rs.getString("merchant")).categoryHint(rs.getString("category_hint"))
                .categoryId((Long) rs.getObject("category_id")).categoryName(rs.getString("category_name")).accountId((Long) rs.getObject("account_id"))
                .accountName(rs.getString("account_name")).note(rs.getString("note")).confidence(rs.getBigDecimal("confidence")).rawText(rs.getString("raw_text"))
                .status(rs.getString("status")).transactionId((Long) rs.getObject("transaction_id")).build();
    }

    private OffsetDateTime atStart(LocalDate date) { return date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime(); }
    private BigDecimal sum(List<FinanceTransactionBO> rows, String type) { return rows.stream().filter(row -> type.equals(row.getTransactionType())).map(FinanceTransactionBO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private BigDecimal percent(BigDecimal value, BigDecimal total) { return total.signum() == 0 ? BigDecimal.ZERO : value.multiply(BigDecimal.valueOf(100)).divide(total, 2, java.math.RoundingMode.HALF_UP); }
    private static String value(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static <T> T value(T value, T fallback) { return value == null ? fallback : value; }
    private static String sha256(byte[] value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); } catch (Exception e) { throw new IllegalStateException(e); } }
}
