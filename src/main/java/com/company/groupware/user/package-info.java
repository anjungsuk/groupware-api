/**
 * 사용자(User) 도메인 모듈.
 * 외부 모듈은 {@link com.company.groupware.user.User} 와 {@link com.company.groupware.user.SystemRole}
 * 만 사용 가능하며, 영속성 계층은 internal 패키지로 캡슐화된다.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "User"
)
package com.company.groupware.user;
