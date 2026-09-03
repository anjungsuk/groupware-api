package com.company.groupware.employee;

/** 직급 선택지. sortOrder 는 서열이다 (PRD §5 결재선: 차장 → 실장). */
public record PositionResponse(String code, String name, int sortOrder) {

    public static PositionResponse from(Position position) {
        return new PositionResponse(position.getCode(), position.getName(), position.getSortOrder());
    }
}
