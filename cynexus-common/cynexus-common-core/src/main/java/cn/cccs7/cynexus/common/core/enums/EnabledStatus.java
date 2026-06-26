package cn.cccs7.cynexus.common.core.enums;

public enum EnabledStatus implements BaseEnum<Integer> {
    DISABLED(0, "disabled"),
    ENABLED(1, "enabled");

    private final Integer code;
    private final String desc;

    EnabledStatus(Integer code, String message) {
        this.code = code;
        this.desc = message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
