package com.hello.bigclean.spoon.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author hello.sheng
 * @version v1.0
 * @date 2025/4/3-14:13
 */
public class StrategyFactory {
    private static final Map<String, ScanStrategy> scanStrategyMap = new HashMap<>();
    static {
        scanStrategyMap.put("SCAN_ALL_CLASS", new ScanAllClassStrategy());
        scanStrategyMap.put("SCAN_ALL_METHOD", new ScanAllMethodStrategy());
    }
    public static ScanStrategy obtainScanner(String mode){
        return scanStrategyMap.get(mode);
    }

    public static boolean isClassMode(String mode){
        return Objects.equals("SCAN_ALL_CLASS", mode);
    }
    public static boolean isMethodMode(String mode){
        return Objects.equals("SCAN_ALL_METHOD", mode);
    }
}
