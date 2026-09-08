package com.company.groupware.approval;

import com.company.groupware.approval.internal.ApprovalDocRepository;
import com.company.groupware.approval.internal.ApprovalLineFactory;
import com.company.groupware.approval.internal.ApprovalLineRepository;
import com.company.groupware.approval.internal.DocFormRepository;
import com.company.groupware.approval.internal.DocNoGenerator;
import com.company.groupware.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * 순차 결재 처리 (T1-3, TRD §4.2).
 *
 * 규칙은 하나다 — 현재 순번의 대기 중인 결재자만 처리할 수 있다.
 * 상태·순번·권한 셋 중 하나라도 어긋나면 거부되는지 전부 확인한다.
 */
class ApprovalServiceTest {

    private static final Long DOC_ID = 1L;
    private static final Long DRAFTER = 10L;
    private static final Long DEPUTY = 11L;    // 1차 — 차장
    private static final Long DIRECTOR = 12L;  // 최종 — 실장
    private static final Long STRANGER = 99L;

    private ApprovalDocRepository docRepository;
    private ApprovalLineRepository lineRepository;
    private ApprovalService service;

    private ApprovalDoc doc;
    private List<ApprovalLine> lines;

    @BeforeEach
    void setUp() {
        docRepository = mock(ApprovalDocRepository.class);
        lineRepository = mock(ApprovalLineRepository.class);
        service = new ApprovalService(docRepository, lineRepository,
                mock(DocFormRepository.class), mock(ApprovalLineFactory.class),
                mock(DocNoGenerator.class));

        doc = ApprovalDoc.draft(1L, DRAFTER, 2L, "휴가 신청", "{}");
        doc.submit("SH-0000001");

        lines = new ArrayList<>(List.of(
                ApprovalLine.pending(DOC_ID, 1, DEPUTY, LineType.APPROVAL),
                ApprovalLine.pending(DOC_ID, 2, DIRECTOR, LineType.APPROVAL)));

        given(docRepository.findById(DOC_ID)).willReturn(Optional.of(doc));
        given(lineRepository.findByDocIdOrderByStepAsc(DOC_ID)).willReturn(lines);
    }

    @Test
    @DisplayName("1차 승인만으로는 완료되지 않고 다음 순번으로 넘어간다")
    void firstApprovalAdvances() {
        service.approve(DOC_ID, DEPUTY, "확인함");

        assertThat(lines.get(0).getResult()).isEqualTo(LineResult.APPROVED);
        assertThat(lines.get(0).getComment()).isEqualTo("확인함");
        assertThat(lines.get(1).isPending()).isTrue();
        assertThat(doc.getStatus()).isEqualTo(DocStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("최종 순번까지 승인하면 문서가 완료된다")
    void lastApprovalCompletes() {
        service.approve(DOC_ID, DEPUTY, null);
        service.approve(DOC_ID, DIRECTOR, "최종 승인");

        assertThat(doc.getStatus()).isEqualTo(DocStatus.COMPLETED);
        assertThat(doc.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("순번을 건너뛴 결재는 막는다 — 차장 전에 실장이 승인할 수 없다")
    void cannotSkipStep() {
        assertThatThrownBy(() -> service.approve(DOC_ID, DIRECTOR, "먼저 승인"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("현재 결재 차례가 아닙니다.");

        assertThat(lines).allMatch(ApprovalLine::isPending);
        assertThat(doc.getStatus()).isEqualTo(DocStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("결재선에 없는 사람은 처리할 수 없다")
    void strangerCannotApprove() {
        assertThatThrownBy(() -> service.approve(DOC_ID, STRANGER, "몰래"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("현재 결재 차례가 아닙니다.");
    }

    @Test
    @DisplayName("같은 사람이 두 번 승인할 수 없다")
    void cannotApproveTwice() {
        service.approve(DOC_ID, DEPUTY, null);

        assertThatThrownBy(() -> service.approve(DOC_ID, DEPUTY, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("현재 결재 차례가 아닙니다.");
    }

    @Test
    @DisplayName("완료된 문서는 더 결재할 수 없다")
    void completedCannotBeApproved() {
        service.approve(DOC_ID, DEPUTY, null);
        service.approve(DOC_ID, DIRECTOR, null);

        assertThatThrownBy(() -> service.approve(DOC_ID, DIRECTOR, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("결재할 수 없는 상태입니다");
    }

    @Test
    @DisplayName("한 단계만 반려해도 문서 전체가 반려된다")
    void rejectStopsDocument() {
        service.reject(DOC_ID, DEPUTY, "증빙 누락");

        assertThat(doc.getStatus()).isEqualTo(DocStatus.REJECTED);
        assertThat(doc.getRejectReason()).isEqualTo("증빙 누락");
        assertThat(lines.get(0).getResult()).isEqualTo(LineResult.REJECTED);
        assertThat(lines.get(1).isPending()).isTrue();
    }

    @Test
    @DisplayName("사유 없는 반려는 라인도 바꾸지 않는다")
    void rejectWithoutReasonChangesNothing() {
        assertThatThrownBy(() -> service.reject(DOC_ID, DEPUTY, "  "))
                .isInstanceOf(BusinessException.class)
                .hasMessage("반려 사유를 입력해 주세요.");

        assertThat(doc.getStatus()).isEqualTo(DocStatus.IN_PROGRESS);
        assertThat(lines.get(0).isPending()).isTrue();
    }

    @Test
    @DisplayName("차례가 아닌 사람은 반려도 할 수 없다")
    void strangerCannotReject() {
        assertThatThrownBy(() -> service.reject(DOC_ID, DIRECTOR, "반려"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("현재 결재 차례가 아닙니다.");
    }

    @Test
    @DisplayName("합의 라인은 순차 진행을 막지 않는다")
    void agreementDoesNotBlockSequence() {
        lines.add(ApprovalLine.pending(DOC_ID, 1, STRANGER, LineType.AGREEMENT));

        service.approve(DOC_ID, DEPUTY, null);
        service.approve(DOC_ID, DIRECTOR, null);

        // 합의가 대기 중이어도 승인 라인이 끝나면 완료된다 (TRD §4.2 합의는 참고)
        assertThat(doc.getStatus()).isEqualTo(DocStatus.COMPLETED);
    }

    @Test
    @DisplayName("결재 대기함에는 내 차례인 문서만 나온다")
    void pendingBoxShowsOnlyMyTurn() {
        given(lineRepository.findByApproverIdAndResultOrderByStepAsc(DIRECTOR, LineResult.PENDING))
                .willReturn(List.of(lines.get(1)));
        given(lineRepository.findByApproverIdAndResultOrderByStepAsc(DEPUTY, LineResult.PENDING))
                .willReturn(List.of(lines.get(0)));

        // 실장은 아직 차장 차례라 안 보인다
        assertThat(service.findPendingDocs(DIRECTOR)).isEmpty();
        assertThat(service.findPendingDocs(DEPUTY)).containsExactly(doc);
    }

    @Test
    @DisplayName("상신자만 회수할 수 있다")
    void onlyDrafterWithdraws() {
        assertThatThrownBy(() -> service.withdraw(DOC_ID, DEPUTY))
                .isInstanceOf(BusinessException.class)
                .hasMessage("상신자만 회수할 수 있습니다.");

        service.withdraw(DOC_ID, DRAFTER);
        assertThat(doc.getStatus()).isEqualTo(DocStatus.WITHDRAWN);
    }
}
