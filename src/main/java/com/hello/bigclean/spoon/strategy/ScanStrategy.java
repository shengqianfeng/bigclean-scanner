package com.hello.bigclean.spoon.strategy;


import com.hello.bigclean.spoon.reference.Reference;

import java.util.List;

/**
 * @author hello.sheng
 * @version v1.0
 * @date 2025/4/3-14:14
 */
public interface ScanStrategy {
    List<? extends Reference>  scan(String path) throws InterruptedException;
}
