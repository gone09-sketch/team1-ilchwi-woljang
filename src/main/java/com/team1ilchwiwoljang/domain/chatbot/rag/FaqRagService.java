package com.team1ilchwiwoljang.domain.chatbot.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FaqRagService implements FaqContextService {
     //질문과 가장 비슷한 FAQ를 최대 몇 개까지 가져올지 정합니다.
    private static final int TOP_FAQ = 3;
    // FAQ와 질문의 의미가 충분히 가까운 경우에만 문맥에 포함합니다. 초기값은 테스트로 조정합니다.
    private static final double MIN_SIMILARITY_SCORE = 0.4;

    private final EmbeddingModel embeddingModel;
    private final FaqKnowledgeBase faqKnowledgeBase;

    /**
     * 서버 메모리에 저장되는 FAQ 색인 결과입니다.
     * 현재 방식은 서버 재시작 시 다시 색인합니다.
     */
    private List<IndexedFaq> indexedFaqList = List.of();
    private boolean indexed = false;

    /**
     * FAQ 문서를 embedding 해서 메모리에 저장합니다. (PUSH)
     * 문서를 미리 검색 가능한 형태로 넣어두는 과정입니다.
     */
    public synchronized void indexFaqs() {
        if (indexed) {
            log.info("FAQ RAG 색인을 건너뜁니다. 이미 색인된 FAQ 수={}", indexedFaqList.size());
            return;
        }

        log.info("FAQ RAG 색인을 시작합니다.");
        indexedFaqList = faqKnowledgeBase.findAll()
                .stream()
                .map(document -> new IndexedFaq(
                        document,
                        embeddingModel.embed(document.textForEmbedding())
                ))
                .toList();
        indexed = true;
        log.info("FAQ RAG 색인을 완료했습니다. 색인된 FAQ 수={}", indexedFaqList.size());
    }

    /**
     * 사용자 질문과 관련 있는 FAQ 내용을 찾아서 LLM에게 넘길 context 문자열로 만듭니다.
     */
    @Override
    public String retrieveContext(String question) {
        log.info("FAQ RAG 문맥 조회를 시작합니다.");
        indexFaqs();

        log.info("FAQ RAG 질문 임베딩을 시작합니다.");
        float[] questionEmbedding = embeddingModel.embed(question);
        log.info("FAQ RAG 질문 임베딩을 완료했습니다.");

        String context = indexedFaqList.stream()
                .map(indexedFaq -> scoreFaq(questionEmbedding, indexedFaq))
                .filter(scoredFaq -> scoredFaq.score() >= MIN_SIMILARITY_SCORE)
                .sorted(Comparator.comparingDouble(ScoredFaq::score).reversed())
                .limit(TOP_FAQ)
                .map(scoredFaq -> """
                        제목: %s
                        내용: %s
                        """.formatted(
                        scoredFaq.document().title(),
                        scoredFaq.document().content()
                ))
                .collect(Collectors.joining("\n"));

        log.info("FAQ RAG 문맥 조회를 완료했습니다. 문맥 비어 있음={}", context.isBlank());

        return context;
    }

    private ScoredFaq scoreFaq(float[] questionEmbedding, IndexedFaq indexedFaq) {
        // 유사도 점수는 기준값 조정을 위해 로그로 남깁니다.
        double score = cosineSimilarity(questionEmbedding, indexedFaq.embedding());
        log.info("FAQ RAG 유사도 점수. faqId={}, score={}", indexedFaq.document().id(), score);
        return new ScoredFaq(indexedFaq.document(), score);
    }

    /**
     * 두 embedding이 얼마나 비슷한지 계산합니다.
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private record IndexedFaq(
            FaqDocument document,
            float[] embedding
    ) {
    }

    private record ScoredFaq(
            FaqDocument document,
            double score
    ) {
    }
}
