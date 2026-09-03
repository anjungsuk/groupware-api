package com.company.groupware.approval;

import com.company.groupware.common.exception.BusinessException;
import com.company.groupware.employee.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static tools.jackson.databind.json.JsonMapper.builder;

/**
 * 기본결재선 → 결재선 생성 (T1-2).
 *
 * PRD §5 확정 결재선: 신청자 → 차장(1차) → 실장(최종).
 * 결재자는 직급으로만 적혀 있고 상신 시점 조직도에서 찾는다.
 */
class ApprovalLineFactoryTest {

    private static final Long DOC_ID = 100L;
    private static final Long TEAM_DEPT = 2L;   // 물류운영팀
    private static final Long DEPUTY = 11L;      // 차장
    private static final Long DIRECTOR = 12L;    // 실장

    private static final String LEAVE_LINE = """
            {"steps":[
              {"step":1,"positionCode":"DEPUTY_GENERAL_MANAGER","type":"APPROVAL"},
              {"step":2,"positionCode":"DIRECTOR","type":"APPROVAL"}
            ]}
            """;

    private EmployeeService employeeService;
    private ApprovalLineFactory factory;

    @BeforeEach
    void setUp() {
        employeeService = mock(EmployeeService.class);
        factory = new ApprovalLineFactory(employeeService, builder().build());
    }

    /** DocForm 은 setter 가 없어(불변 의도) 테스트에서만 리플렉션으로 채운다. */
    private static DocForm form(String code, String defaultLine) {
        try {
            DocForm form = DocForm.class.getDeclaredConstructor().newInstance();
            set(form, "code", code);
            set(form, "defaultLine", defaultLine);
            return form;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    @DisplayName("차장 → 실장 순서로 결재선을 만든다")
    void createsSequentialLine() {
        given(employeeService.findApproverId(TEAM_DEPT, "DEPUTY_GENERAL_MANAGER"))
                .willReturn(Optional.of(DEPUTY));
        given(employeeService.findApproverId(TEAM_DEPT, "DIRECTOR"))
                .willReturn(Optional.of(DIRECTOR));

        List<ApprovalLine> lines = factory.create(DOC_ID, form("LEAVE", LEAVE_LINE), TEAM_DEPT);

        assertThat(lines).hasSize(2);
        assertThat(lines).allSatisfy(line -> {
            assertThat(line.getDocId()).isEqualTo(DOC_ID);
            assertThat(line.getResult()).isEqualTo(LineResult.PENDING);
            assertThat(line.isPending()).isTrue();
            assertThat(line.getActedAt()).isNull();
        });
        assertThat(lines.get(0).getStep()).isEqualTo(1);
        assertThat(lines.get(0).getApproverId()).isEqualTo(DEPUTY);
        assertThat(lines.get(1).getStep()).isEqualTo(2);
        assertThat(lines.get(1).getApproverId()).isEqualTo(DIRECTOR);
    }

    @Test
    @DisplayName("정의 순서가 뒤섞여 있어도 순번대로 정렬한다")
    void sortsByStep() {
        String reversed = """
                {"steps":[
                  {"step":2,"positionCode":"DIRECTOR","type":"APPROVAL"},
                  {"step":1,"positionCode":"DEPUTY_GENERAL_MANAGER","type":"APPROVAL"}
                ]}
                """;
        given(employeeService.findApproverId(TEAM_DEPT, "DEPUTY_GENERAL_MANAGER"))
                .willReturn(Optional.of(DEPUTY));
        given(employeeService.findApproverId(TEAM_DEPT, "DIRECTOR"))
                .willReturn(Optional.of(DIRECTOR));

        List<ApprovalLine> lines = factory.create(DOC_ID, form("LEAVE", reversed), TEAM_DEPT);

        assertThat(lines).extracting(ApprovalLine::getStep).containsExactly(1, 2);
        assertThat(lines).extracting(ApprovalLine::getApproverId).containsExactly(DEPUTY, DIRECTOR);
    }

    @Test
    @DisplayName("합의·후결 유형도 정의된 대로 옮긴다")
    void keepsLineType() {
        String mixed = """
                {"steps":[
                  {"step":1,"positionCode":"DEPUTY_GENERAL_MANAGER","type":"POST"},
                  {"step":2,"positionCode":"DIRECTOR","type":"AGREEMENT"}
                ]}
                """;
        given(employeeService.findApproverId(TEAM_DEPT, "DEPUTY_GENERAL_MANAGER"))
                .willReturn(Optional.of(DEPUTY));
        given(employeeService.findApproverId(TEAM_DEPT, "DIRECTOR"))
                .willReturn(Optional.of(DIRECTOR));

        List<ApprovalLine> lines = factory.create(DOC_ID, form("ACCIDENT", mixed), TEAM_DEPT);

        assertThat(lines).extracting(ApprovalLine::getLineType)
                .containsExactly(LineType.POST, LineType.AGREEMENT);
    }

    @Test
    @DisplayName("조직도에 해당 직급이 없으면 상신을 막는다 — 결재선 없는 문서를 만들지 않는다")
    void missingApproverIsRejected() {
        given(employeeService.findApproverId(TEAM_DEPT, "DEPUTY_GENERAL_MANAGER"))
                .willReturn(Optional.of(DEPUTY));
        given(employeeService.findApproverId(TEAM_DEPT, "DIRECTOR"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> factory.create(DOC_ID, form("LEAVE", LEAVE_LINE), TEAM_DEPT))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DIRECTOR");
    }

    @Test
    @DisplayName("부서가 없는 상신자는 결재선을 만들 수 없다")
    void drafterWithoutDeptIsRejected() {
        assertThatThrownBy(() -> factory.create(DOC_ID, form("LEAVE", LEAVE_LINE), null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("부서가 배정되지 않아 결재선을 만들 수 없습니다.");
    }

    @Test
    @DisplayName("기본 결재선이 비어 있으면 막는다")
    void emptyLineIsRejected() {
        assertThatThrownBy(() -> factory.create(DOC_ID, form("LEAVE", "{\"steps\":[]}"), TEAM_DEPT))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("기본 결재선이 비어 있습니다");
    }

    @Test
    @DisplayName("깨진 JSON 은 500 이 아니라 입력 오류로 끊는다")
    void brokenJsonIsRejected() {
        assertThatThrownBy(() -> factory.create(DOC_ID, form("LEAVE", "{not json"), TEAM_DEPT))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("기본 결재선 정의를 읽을 수 없습니다");
    }
}
