\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_core.finance_categories
    (id, owner_key, parent_id, name, direction, icon, color, sort_order, is_system)
VALUES
    (920010000000001001, 'default', NULL, '早餐', 'expense', 'coffee', 'orange', 11, true),
    (920010000000001002, 'default', NULL, '午餐', 'expense', 'utensils', 'orange', 12, true),
    (920010000000001003, 'default', NULL, '晚餐', 'expense', 'utensils', 'orange', 13, true),
    (920010000000001004, 'default', NULL, '水果零食', 'expense', 'apple', 'orange', 14, true),
    (920010000000001005, 'default', NULL, '饮料酒水', 'expense', 'glass-water', 'orange', 15, true),
    (920010000000001006, 'default', NULL, '公交地铁', 'expense', 'train-front', 'blue', 21, true),
    (920010000000001007, 'default', NULL, '打车', 'expense', 'car-front', 'blue', 22, true),
    (920010000000001008, 'default', NULL, '加油', 'expense', 'fuel', 'blue', 23, true),
    (920010000000001009, 'default', NULL, '停车', 'expense', 'parking-circle', 'blue', 24, true),
    (920010000000001010, 'default', NULL, '爱车养车', 'expense', 'wrench', 'blue', 25, true),
    (920010000000001011, 'default', NULL, '日用品', 'expense', 'shopping-basket', 'pink', 31, true),
    (920010000000001012, 'default', NULL, '服饰鞋包', 'expense', 'shirt', 'pink', 32, true),
    (920010000000001013, 'default', NULL, '数码电器', 'expense', 'smartphone', 'pink', 33, true),
    (920010000000001014, 'default', NULL, '家居家装', 'expense', 'lamp-ceiling', 'pink', 34, true),
    (920010000000001015, 'default', NULL, '宠物', 'expense', 'paw-print', 'pink', 35, true),
    (920010000000001016, 'default', NULL, '房租', 'expense', 'building-2', 'violet', 41, true),
    (920010000000001017, 'default', NULL, '水电煤', 'expense', 'zap', 'violet', 42, true),
    (920010000000001018, 'default', NULL, '物业', 'expense', 'building', 'violet', 43, true),
    (920010000000001019, 'default', NULL, '家庭维护', 'expense', 'house', 'violet', 44, true),
    (920010000000001020, 'default', NULL, '运动健身', 'expense', 'dumbbell', 'cyan', 51, true),
    (920010000000001021, 'default', NULL, '电影演出', 'expense', 'ticket', 'cyan', 52, true),
    (920010000000001022, 'default', NULL, '游戏', 'expense', 'gamepad-2', 'cyan', 53, true),
    (920010000000001023, 'default', NULL, '旅行', 'expense', 'plane', 'cyan', 54, true),
    (920010000000001024, 'default', NULL, '药品', 'expense', 'pill', 'rose', 61, true),
    (920010000000001025, 'default', NULL, '挂号就诊', 'expense', 'stethoscope', 'rose', 62, true),
    (920010000000001026, 'default', NULL, '保健', 'expense', 'heart-pulse', 'rose', 63, true),
    (920010000000001027, 'default', NULL, '书籍资料', 'expense', 'book-open', 'indigo', 71, true),
    (920010000000001028, 'default', NULL, '课程培训', 'expense', 'graduation-cap', 'indigo', 72, true),
    (920010000000001029, 'default', NULL, '手机话费', 'expense', 'smartphone', 'slate', 81, true),
    (920010000000001030, 'default', NULL, '网络宽带', 'expense', 'wifi', 'slate', 82, true),
    (920010000000001031, 'default', NULL, '礼物红包', 'expense', 'gift', 'amber', 91, true),
    (920010000000001032, 'default', NULL, '聚会社交', 'expense', 'users', 'amber', 92, true),
    (920010000000001033, 'default', NULL, '保险', 'expense', 'shield-check', 'teal', 93, true),
    (920010000000001034, 'default', NULL, '税费', 'expense', 'receipt', 'teal', 94, true),
    (920010000000001035, 'default', NULL, '手续费', 'expense', 'coins', 'teal', 95, true),
    (920010000000001036, 'default', NULL, '兼职劳务', 'income', 'briefcase-business', 'emerald', 11, true),
    (920010000000001037, 'default', NULL, '报销', 'income', 'receipt-text', 'emerald', 12, true),
    (920010000000001038, 'default', NULL, '退款', 'income', 'undo-2', 'emerald', 13, true),
    (920010000000001039, 'default', NULL, '利息', 'income', 'percent', 'teal', 14, true),
    (920010000000001040, 'default', NULL, '租金收入', 'income', 'building-2', 'teal', 15, true),
    (920010000000001041, 'default', NULL, '补贴', 'income', 'badge-dollar-sign', 'amber', 16, true),
    (920010000000001042, 'default', NULL, '红包收入', 'income', 'gift', 'amber', 17, true),
    (920010000000001043, 'default', NULL, '养老金', 'income', 'landmark', 'indigo', 18, true)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    direction = EXCLUDED.direction,
    icon = EXCLUDED.icon,
    color = EXCLUDED.color,
    sort_order = EXCLUDED.sort_order,
    updated_at = now(),
    deleted_at = NULL,
    is_archived = false;

COMMIT;
