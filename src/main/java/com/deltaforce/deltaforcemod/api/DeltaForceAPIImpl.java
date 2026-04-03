package com.deltaforce.deltaforcemod.api;

/**
 * API 信息类，其他模组可以通过此类检查 API 版本
 */
public class DeltaForceAPIImpl {
    public static final String MOD_ID = "deltaforcemod";
    public static final String API_VERSION = "1.0.0";
    public static final String MOD_NAME = "Delta Force System";

    /**
     * 获取 API 版本
     */
    public static String getAPIVersion() {
        return API_VERSION;
    }

    /**
     * 检查模组是否已加载
     */
    public static boolean isModLoaded() {
        return true;
    }
}