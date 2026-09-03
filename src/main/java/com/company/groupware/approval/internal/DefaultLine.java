package com.company.groupware.approval.internal;

import com.company.groupware.approval.LineType;

import java.util.List;

/**
 * 양식의 기본 결재선 정의 — `doc_forms.default_line` JSON 의 스키마.
 *
 * 결재자를 사람이 아니라 <b>직급</b>으로 적는다 (PRD §5: 신청자 → 차장 → 실장).
 * 인사이동이 있어도 양식을 고칠 필요가 없고, 상신 시점의 조직도로 실제 결재자를 찾는다.
 *
 * <pre>
 * {"steps":[{"step":1,"positionCode":"DEPUTY_GENERAL_MANAGER","type":"APPROVAL"},
 *           {"step":2,"positionCode":"DIRECTOR","type":"APPROVAL"}]}
 * </pre>
 */
public record DefaultLine(List<Step> steps) {

    public record Step(int step, String positionCode, LineType type) {
    }
}
