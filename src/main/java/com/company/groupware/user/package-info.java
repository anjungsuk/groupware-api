/**
 * 사용자(User) 도메인 모듈.
 * 외부 모듈은 {@link com.company.groupware.user.User} 만 사용 가능하며,
 * 영속성 계층은 internal 패키지로 캡슐화된다.
 * 권한 enum {@link com.company.groupware.common.security.SystemRole} 은 공용 모듈에서 제공한다.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "User"
)
package com.company.groupware.user;
