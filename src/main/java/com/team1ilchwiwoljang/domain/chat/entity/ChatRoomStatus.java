package com.team1ilchwiwoljang.domain.chat.entity;

/**
 * 채팅방 상담 상태를 나타내는 enum입니다.
 * 상태 흐름:
 * WAITING -> IN_PROGRESS -> COMPLETED
 * 현재 정책:
 * - WAITING 상태에서는 IN_PROGRESS로만 변경할 수 있습니다.
 * - IN_PROGRESS 상태에서는 COMPLETED로만 변경할 수 있습니다.
 * - COMPLETED 상태에서는 다른 상태로 변경할 수 없습니다.
 * - 동일 상태로의 변경도 허용하지 않습니다.
 */
public enum ChatRoomStatus {

    WAITING,
    IN_PROGRESS,
    COMPLETED;

    /**
     * 현재 상태에서 다음 상태로 변경 가능한지 확인합니다.
     * WAITING.canChangeTo(IN_PROGRESS) -> true
     * WAITING.canChangeTo(COMPLETED) -> false
     * IN_PROGRESS.canChangeTo(COMPLETED) -> true
     * COMPLETED.canChangeTo(WAITING) -> false
     */
    public boolean canChangeTo(ChatRoomStatus nextStatus) {
        if (nextStatus == null) {
            return false;
        }

        return switch (this) {
            case WAITING -> nextStatus == IN_PROGRESS;
            case IN_PROGRESS -> nextStatus == COMPLETED;
            case COMPLETED -> false;
        };
    }
}