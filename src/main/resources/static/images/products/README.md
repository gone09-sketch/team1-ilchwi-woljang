# Dummy Product Images

총 75개 PNG 이미지입니다.

구성:
- 15개 상품 카테고리
- 카테고리별 5개 이미지
- 외부 이미지 없이 직접 생성한 테스트용 더미 이미지

추천 위치:
- Spring Boot: `src/main/resources/static/images/products/`
- Frontend: `public/images/products/`

예시 URL:
- `/images/products/laptop-01.png`
- `/images/products/snack-03.png`
- `/images/products/drink-05.png`

`apply-dummy-product-images.sql` 파일에는 기존 `products` 테이블에 `image_url`을 추가하고 값을 채우는 예시 SQL이 들어 있습니다.
