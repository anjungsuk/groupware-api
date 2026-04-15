/**
 * 모든 도메인 모듈이 자유롭게 의존할 수 있는 공용 모듈.
 * 보안, 예외, 응답, 인프라(redis, storage), 공통 설정을 포함한다.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.company.groupware.common;
