package com.company.groupware.approval;

import com.company.groupware.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 문서 상태 머신 — TRD §4.1 (T1-1).
 *
 * 허용 전이만 통과하고 나머지는 전부 막히는지 고정한다.
 * 사원 승인에서 상태 검사가 통째로 빠져 재직자를 잠글 수 있었던 전례가 있어,
 * 결재 문서는 처음부터 전이표를 테스트로 못박고 시작한다.
 */
class ApprovalDocStateTest {

    private static final Long DRAFTER = 10L;
    private static final Long OTHER = 99L;
    private static final String DOC_NO = "SH-0000001";

    private static ApprovalDoc draft() {
        return ApprovalDoc.draft(1L, DRAFTER, 2L, "휴가 신청", "{\"days\":3}");
    }

    private static ApprovalDoc inProgress() {
        ApprovalDoc doc = draft();
        doc.submit(DOC_NO);
        return doc;
    }

    private static ApprovalDoc completed() {
        ApprovalDoc doc = inProgress();
        doc.complete();
        return doc;
    }

    private static ApprovalDoc rejected() {
        ApprovalDoc doc = inProgress();
        doc.reject("증빙 누락");
        return doc;
    }

    private static ApprovalDoc withdrawn() {
        ApprovalDoc doc = inProgress();
        doc.withdraw(DRAFTER);
        return doc;
    }

    @Test
    @DisplayName("새 문서는 임시저장이고 문서번호가 없다")
    void newDocIsDraftWithoutNumber() {
        ApprovalDoc doc = draft();

        assertThat(doc.getStatus()).isEqualTo(DocStatus.DRAFT);
        assertThat(doc.getDocNo()).isNull();
        assertThat(doc.getSubmittedAt()).isNull();
    }

    @Nested
    @DisplayName("상신")
    class Submit {

        @Test
        @DisplayName("임시저장을 상신하면 진행중이 되고 문서번호가 붙는다")
        void draftToInProgress() {
            ApprovalDoc doc = draft();

            doc.submit(DOC_NO);

            assertThat(doc.getStatus()).isEqualTo(DocStatus.IN_PROGRESS);
            assertThat(doc.getDocNo()).isEqualTo(DOC_NO);
            assertThat(doc.getSubmittedAt()).isNotNull();
        }

        @Test
        @DisplayName("반려된 문서를 재상신하면 반려 사유가 지워진다")
        void rejectedResubmitClearsReason() {
            ApprovalDoc doc = rejected();
            assertThat(doc.getRejectReason()).isEqualTo("증빙 누락");

            doc.submit("SH-9999999");

            assertThat(doc.getStatus()).isEqualTo(DocStatus.IN_PROGRESS);
            assertThat(doc.getRejectReason()).isNull();
        }

        @Test
        @DisplayName("재상신해도 문서번호는 최초 것을 유지한다")
        void resubmitKeepsOriginalNumber() {
            ApprovalDoc doc = rejected();

            doc.submit("SH-9999999");

            assertThat(doc.getDocNo()).isEqualTo(DOC_NO);
        }

        @Test
        @DisplayName("회수한 문서도 다시 상신할 수 있다")
        void withdrawnCanResubmit() {
            ApprovalDoc doc = withdrawn();

            doc.submit("SH-9999999");

            assertThat(doc.getStatus()).isEqualTo(DocStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("이미 진행중인 문서는 다시 상신할 수 없다")
        void inProgressCannotResubmit() {
            assertThatThrownBy(() -> inProgress().submit(DOC_NO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("상신할 수 없는 상태입니다");
        }

        @Test
        @DisplayName("완료된 문서는 다시 상신할 수 없다")
        void completedCannotResubmit() {
            assertThatThrownBy(() -> completed().submit(DOC_NO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("상신할 수 없는 상태입니다");
        }
    }

    @Nested
    @DisplayName("완료")
    class Complete {

        @Test
        @DisplayName("진행중 문서를 완료하면 완료일시가 남는다")
        void inProgressToCompleted() {
            ApprovalDoc doc = inProgress();

            doc.complete();

            assertThat(doc.getStatus()).isEqualTo(DocStatus.COMPLETED);
            assertThat(doc.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("임시저장 문서는 곧바로 완료할 수 없다")
        void draftCannotComplete() {
            assertThatThrownBy(() -> draft().complete())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("완료할 수 없는 상태입니다");
        }

        @Test
        @DisplayName("완료는 종착 상태다 — 두 번 완료되지 않는다")
        void completedIsTerminal() {
            assertThatThrownBy(() -> completed().complete())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("완료할 수 없는 상태입니다");
        }
    }

    @Nested
    @DisplayName("반려")
    class Reject {

        @Test
        @DisplayName("사유와 함께 반려한다")
        void inProgressToRejected() {
            ApprovalDoc doc = inProgress();

            doc.reject("증빙 누락");

            assertThat(doc.getStatus()).isEqualTo(DocStatus.REJECTED);
            assertThat(doc.getRejectReason()).isEqualTo("증빙 누락");
        }

        @Test
        @DisplayName("사유 없는 반려는 막는다")
        void rejectRequiresReason() {
            assertThatThrownBy(() -> inProgress().reject("  "))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("반려 사유를 입력해 주세요.");

            assertThatThrownBy(() -> inProgress().reject(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("반려 사유를 입력해 주세요.");
        }

        @Test
        @DisplayName("완료된 문서는 반려할 수 없다")
        void completedCannotReject() {
            assertThatThrownBy(() -> completed().reject("늦은 반려"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("반려할 수 없는 상태입니다");
        }
    }

    @Nested
    @DisplayName("회수")
    class Withdraw {

        @Test
        @DisplayName("상신자는 진행중 문서를 회수할 수 있다")
        void drafterCanWithdraw() {
            ApprovalDoc doc = inProgress();

            doc.withdraw(DRAFTER);

            assertThat(doc.getStatus()).isEqualTo(DocStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("상신자가 아니면 회수할 수 없다")
        void othersCannotWithdraw() {
            ApprovalDoc doc = inProgress();

            assertThatThrownBy(() -> doc.withdraw(OTHER))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("상신자만 회수할 수 있습니다.");

            assertThat(doc.getStatus()).isEqualTo(DocStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("최종 승인이 끝난 문서는 회수할 수 없다")
        void completedCannotWithdraw() {
            assertThatThrownBy(() -> completed().withdraw(DRAFTER))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("회수할 수 없는 상태입니다");
        }
    }

    @Nested
    @DisplayName("임시저장 수정")
    class EditDraft {

        @Test
        @DisplayName("임시저장 문서는 고칠 수 있다")
        void draftIsEditable() {
            ApprovalDoc doc = draft();

            doc.editDraft("연차 신청", "{\"days\":1}");

            assertThat(doc.getTitle()).isEqualTo("연차 신청");
            assertThat(doc.getContent()).isEqualTo("{\"days\":1}");
        }

        @Test
        @DisplayName("진행중 문서는 고칠 수 없다 — 결재자가 본 내용이 바뀌면 안 된다")
        void inProgressIsNotEditable() {
            ApprovalDoc doc = inProgress();

            assertThatThrownBy(() -> doc.editDraft("몰래 수정", "{\"days\":30}"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("임시저장 문서만 수정할 수 있습니다");

            assertThat(doc.getTitle()).isEqualTo("휴가 신청");
        }
    }
}
