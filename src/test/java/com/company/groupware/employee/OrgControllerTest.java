package com.company.groupware.employee;

import com.company.groupware.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 조직 마스터 조회 계약 — 관리자 승인 화면의 부서·직급 선택지를 채운다.
 * 대응: groupware-front · src/api/org.ts · docs/04_인증_API_명세.md §5.1
 */
class OrgControllerTest {

    private MockMvc mockMvc;
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = mock(EmployeeService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new OrgController(employeeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("부서 목록은 상위 부서 참조(parentId)를 포함한다")
    void listDepts() throws Exception {
        given(employeeService.findDepts()).willReturn(List.of(
                new DeptResponse(1L, "경영지원실", null),
                new DeptResponse(2L, "물류운영팀", 1L)));

        mockMvc.perform(get("/api/v1/depts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("경영지원실"))
                .andExpect(jsonPath("$.data[0].parentId").doesNotExist())
                .andExpect(jsonPath("$.data[1].parentId").value(1));
    }

    @Test
    @DisplayName("직급 목록은 서열(sortOrder) 순서를 그대로 유지한다")
    void listPositionsInRankOrder() throws Exception {
        given(employeeService.findPositions()).willReturn(List.of(
                new PositionResponse("STAFF", "사원", 1),
                new PositionResponse("DEPUTY_GENERAL_MANAGER", "차장", 5),
                new PositionResponse("DIRECTOR", "실장", 7)));

        mockMvc.perform(get("/api/v1/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("STAFF"))
                .andExpect(jsonPath("$.data[1].name").value("차장"))
                .andExpect(jsonPath("$.data[2].sortOrder").value(7));
    }
}
