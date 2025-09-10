package com.hello.bigclean.spoon.reference;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author hello.sheng
 * @version v1.0
 * @date 2025/4/2-19:51
 */
@Data
@NoArgsConstructor
public class Reference {
    private String originalClass;
    private Set<String> referenceClasses;

    public Reference(String originalClass, Set<String> referenceClasses) {
        this.originalClass = originalClass;
        this.referenceClasses = referenceClasses;
    }

    public void append(String ref){
        if(Objects.isNull(referenceClasses)){
            referenceClasses = new HashSet<>();
        }
        referenceClasses.add(ref);
    }
    
    @Override
    public String toString() {
        return originalClass != null ? originalClass : "未知类";
    }
}
