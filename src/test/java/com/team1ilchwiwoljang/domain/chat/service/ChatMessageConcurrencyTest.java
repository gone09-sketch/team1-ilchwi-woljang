package com.team1ilchwiwoljang.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.chat.entity.ChatMessage;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoomStatus;
import com.team1ilchwiwoljang.domain.chat.repository.ChatMessageRepository;
import com.team1ilchwiwoljang.domain.chat.repository.ChatRoomRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class ChatMessageConcurrencyTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        chatMessageRepository.deleteAllInBatch();
        chatRoomRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("채팅방 완료 처리와 메시지 저장이 경합하면 상태 검증을 통과한 메시지는 마지막 메시지로 저장될 수 있다")
    void givenChatRoomCompletionRacesWithMessageSave_whenMessagePassedStatusCheck_thenSaveAsLastMessage()
            throws Exception {
        Member member = memberRepository.save(
                Member.create("chat-concurrency@example.com", "password", "member", "010-1234-5678")
        );
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create(member));

        Long memberId = member.getId();
        Long chatRoomId = chatRoom.getId();

        CountDownLatch messageTransactionReadWaitingStatus = new CountDownLatch(1);
        CountDownLatch adminCompletionCommitted = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            Future<?> messageSaveFuture = executorService.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        ChatRoom messageTransactionChatRoom = chatRoomRepository.findById(chatRoomId)
                                .orElseThrow();
                        Member sender = memberRepository.findById(memberId)
                                .orElseThrow();

                        assertThat(messageTransactionChatRoom.getStatus()).isEqualTo(ChatRoomStatus.WAITING);

                        /*
                         * 메시지 저장 트랜잭션이 WAITING 상태를 읽은 직후 멈춰둡니다.
                         * 이 사이에 관리자 트랜잭션이 같은 채팅방을 COMPLETED로 변경하고 커밋합니다.
                         */
                        messageTransactionReadWaitingStatus.countDown();
                        await(adminCompletionCommitted);

                        /*
                         * 현재 ChatMessageService.saveMessage()의 핵심 검증 흐름을 재현합니다.
                         * 이미 영속성 컨텍스트에 올라온 ChatRoom이 오래된 WAITING 상태라면
                         * 아래 검증을 통과해 완료 이후 메시지가 저장될 수 있습니다.
                         */
                        if (messageTransactionChatRoom.isCompleted()) {
                            throw new BusinessException(ErrorCode.COMPLETED_CHAT_ROOM_MESSAGE_NOT_ALLOWED);
                        }

                        chatMessageRepository.save(
                                ChatMessage.create(messageTransactionChatRoom, sender, "완료 처리와 경합한 메시지")
                        );
                    })
            );

            Future<?> completionFuture = executorService.submit(() -> {
                await(messageTransactionReadWaitingStatus);

                transactionTemplate.executeWithoutResult(status -> {
                    ChatRoom adminTransactionChatRoom = chatRoomRepository.findById(chatRoomId)
                            .orElseThrow();

                    /*
                     * ChatRoomStatus 전이 정책상 WAITING에서 COMPLETED로 바로 갈 수 없으므로
                     * 실제 허용 경로인 WAITING -> IN_PROGRESS -> COMPLETED 순서로 완료 처리합니다.
                     */
                    adminTransactionChatRoom.changeStatus(ChatRoomStatus.IN_PROGRESS);
                    adminTransactionChatRoom.changeStatus(ChatRoomStatus.COMPLETED);
                });

                adminCompletionCommitted.countDown();
            });

            completionFuture.get(5, TimeUnit.SECONDS);
            messageSaveFuture.get(5, TimeUnit.SECONDS);
        } finally {
            executorService.shutdownNow();
        }

        ChatRoom completedChatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow();
        List<ChatMessage> savedMessages = chatMessageRepository.findAllWithSenderByChatRoomId(chatRoomId);

        assertThat(completedChatRoom.getStatus()).isEqualTo(ChatRoomStatus.COMPLETED);
        assertThat(savedMessages).hasSize(1);
        assertThat(savedMessages.get(0).getContent()).isEqualTo("완료 처리와 경합한 메시지");
    }

    private void await(CountDownLatch latch) {
        try {
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("동시성 테스트 대기 중 interrupt가 발생했습니다.", e);
        }
    }
}
