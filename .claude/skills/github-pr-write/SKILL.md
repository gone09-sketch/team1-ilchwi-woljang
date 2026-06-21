---
name: github-pr-write
description: 사용자가 "PR 작성해줘", "PR 본문 써줘", "PR 제목 써줘"라고 요청하면 현재 브랜치 변경사항을 기준으로 PR 제목과 본문 초안을 작성한다.
---

# GitHub PR 작성

## 역할

현재 브랜치의 실제 변경사항을 기준으로 GitHub Pull Request 제목과 본문 초안을 작성한다.

PR 생성, push, commit, merge, rebase는 하지 않는다.

## 기본 규칙

- 실제 변경 파일과 diff에 근거해서만 작성한다.
- 구현되지 않은 내용, 향후 계획, 추측은 포함하지 않는다.
- 확인하지 않은 테스트나 수동 검증을 완료로 표시하지 않는다.
- 사용자가 PR 생성을 요청해도 제목과 본문 초안만 제공한다.

## 확인 절차

먼저 변경 범위를 확인한다.

git branch --show-current
git status --short
git diff --stat
git diff --name-only

base 브랜치가 필요하면 별도 지시가 없는 한 origin/develop, origin/main, origin/master 순서로 확인한다.

전체 diff는 한 번에 읽지 않고 필요한 파일만 좁혀서 확인한다.

git diff <base>...HEAD -- <file>

문서는 변경사항과 직접 관련된 경우에만 필요한 파일만 읽는다.

## PR 제목

변경의 핵심 성격에 맞는 태그 하나를 사용한다.

- [feat]: 기능 추가
- [fix]: 버그 수정
- [refactor]: 구조 개선
- [docs]: 문서 수정
- [test]: 테스트 추가 또는 수정
- [chore]: 설정, 빌드, 유지보수

예시:

- [feat] 고객 조회 기능 구현
- [fix] 주문 상태 필터링 오류 수정

## PR 본문 형식

## 작업 내용

-

## 변경 이유

-

## 테스트 및 확인

- [ ] 

## 리뷰 포인트

-

## AI 활용 기록

-

## 참고 사항

-

## AI 활용 기록 작성

- 구현 과정에서 사용자가 Codex에 요청한 내용을 확인할 수 있으면 1~3개로 요약한다.
- 확인할 수 없으면 확인 불가로 적는다.
- 원문 프롬프트, 민감정보, 세션 ID, 로그 경로는 포함하지 않는다.
- PR 작성 요청이나 단순 상태 확인 요청은 제외한다.