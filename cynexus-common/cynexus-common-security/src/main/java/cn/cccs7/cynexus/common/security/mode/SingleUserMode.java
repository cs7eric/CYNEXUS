package cn.cccs7.cynexus.common.security.mode;

import cn.cccs7.cynexus.common.core.constant.OperatorConstants;

public final class SingleUserMode {

    private SingleUserMode() {
    }

    public static String currentActor() {
        return OperatorConstants.ACTOR;
    }
}
