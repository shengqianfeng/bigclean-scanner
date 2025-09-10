package com.hello.bigclean.spoon.reference;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author hello.sheng
 * @version v1.0
 * @date 2025/4/12-18:38
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class MethodReference extends Reference{
    private String method;
    private String methodDescriptor;
    private Integer methodLines;


    @Override
    public String toString() {
        if (getOriginalClass() != null && method != null) {
            String lineInfo = methodLines != null ? " (方法行数：" + methodLines + ")" : "";
            return getOriginalClass() + "#" + method + lineInfo;
        }
        return "MethodReference{" +
                "class='" + getOriginalClass() + '\'' +
                ", method='" + method + '\'' +
                ", methodDescriptor='" + methodDescriptor + '\'' +
                ", methodLines=" + methodLines +
                '}';
    }
}
