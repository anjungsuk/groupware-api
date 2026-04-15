package com.company.groupware.common.config;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

// springdoc 2.8.x 의 QuerydslPredicateOperationCustomizer 는 Spring Data 3 의
// TypeInformation 클래스를 참조하지만 Spring Data 4(Spring Boot 4)에서 제거되어
// 기동 시 NoClassDefFoundError 가 발생한다. 해당 빈을 레지스트리 단계에서 제거한다.
@Component
public class SpringDocQuerydslExcluder implements BeanDefinitionRegistryPostProcessor {

    private static final String TARGET_BEAN = "queryDslQuerydslPredicateOperationCustomizer";

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition(TARGET_BEAN)) {
            registry.removeBeanDefinition(TARGET_BEAN);
        }
    }
}
