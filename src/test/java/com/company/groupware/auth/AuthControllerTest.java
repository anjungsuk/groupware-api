package com.company.groupware.auth;

import com.company.groupware.common.exception.BusinessException;
import com.company.groupware.common.exception.ErrorCode;
import com.company.groupware.common.exception.FieldValidationException;
import com.company.groupware.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 프론트(groupware-front)와 맞춘 인증 API 계약을 고정한다.
 * 대응: src/mocks/handlers.ts · docs/04_인증_API_명세.md
 *
 * Spring Boot 4 에서 @WebMvcTest 슬라이스는 별도 아티팩트로 분리되었으므로,
 * 의존성을 늘리지 않고 standaloneSetup 으로 컨트롤러·검증·예외처리만 검증한다.
 */
class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final String VALID_SIGNUP = """
            {
              "name": "김철수",
              "email": "kim@company.co.kr",
              "password": "password1",
              "birthDate": "1990-05-14",
              "zipCode": "06236",
              "address": "서울 강남구 테헤란로 1",
              "addressDetail": "101동 1001호",
              "mobilePhone": "010-1234-5678",
              "homePhone": "",
              "emergencyRelation": "SPOUSE",
              "emergencyPhone": "010-9876-5432"
            }
            """;

    @Test
    @DisplayName("회원가입은 사번을 채번해 PENDING 으로 응답하고 토큰을 주지 않는다")
    void signupReturnsPendingWithGeneratedEmployeeNo() throws Exception {
        given(authService.signup(any()))
                .willReturn(new SignupResponse("EMP-2026-0001", "김철수", "kim@company.co.kr", "PENDING"));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SIGNUP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeNo").value("EMP-2026-0001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("잘못된 휴대폰 번호는 C001 과 필드별 메시지로 응답한다")
    void signupRejectsInvalidMobilePhone() throws Exception {
        String body = VALID_SIGNUP.replace("010-1234-5678", "123");

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.data.mobilePhone").value("휴대폰 번호 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("비밀번호 규칙(8자·영문·숫자)을 서버에서 다시 검증한다")
    void signupRejectsWeakPassword() throws Exception {
        String body = VALID_SIGNUP.replace("password1", "abc");

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    @DisplayName("중복 이메일은 C001 + data.email 로 응답한다")
    void signupRejectsDuplicateEmail() throws Exception {
        willThrow(FieldValidationException.of("email", "이미 사용중인 이메일입니다."))
                .given(authService).signup(any());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SIGNUP))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.data.email").value("이미 사용중인 이메일입니다."));
    }

    @Test
    @DisplayName("승인 대기 계정은 로그인할 수 없다 (A005)")
    void loginBlocksPendingAccount() throws Exception {
        willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_APPROVED))
                .given(authService).login(any());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"pending@company.co.kr","password":"password1"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("A005"))
                .andExpect(jsonPath("$.message").value("관리자 승인 대기 중인 계정입니다."));
    }

    @Test
    @DisplayName("로그인 성공 시 토큰과 사용자 정보를 반환한다")
    void loginReturnsTokensAndUser() throws Exception {
        given(authService.login(any())).willReturn(new LoginResponse(
                "access", "refresh",
                new AuthUserResponse(1L, "EMP-2026-0001", "홍길동", "hong@company.co.kr",
                        "MEMBER", "ACTIVE", 2L, "물류운영팀", "STAFF", "사원")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"hong@company.co.kr","password":"password1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"))
                .andExpect(jsonPath("$.data.user.employeeNo").value("EMP-2026-0001"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.user.deptName").value("물류운영팀"));
    }
}
