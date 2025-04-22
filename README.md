# 위성 영상 처리 백엔드 - 이진아

## 1. 프로젝트 설명
- S3에 저장된 원본 TIFF 영상을 COG (Cloud Optimized GeoTIFF) 포맷으로 변환하고, 그 결과를 S3 버킷에 업로드
- 변환, 업로드, 메타데이터 저장까지 처리할 수 있는 간단한 관리 시스템

---

## 2. 주요 역할
- AWS S3에서 파일 리스트 조회
- GDAL을 활용한 COG 변환 처리
- 변환된 영상 업로드
- 메타데이터 DB 저장 (JPA 사용)

---

## 3. 기술 스택
- Java 17
- Spring Boot 3
- AWS S3 SDK
- GDAL (CLI 기반)
- JPA (Spring Data)
- H2 (인메모리 테스트 DB)

---

## 4. 프로젝트 구조
- `Controller` : REST API 엔드포인트 정의
- `Service` : 비즈니스 로직 처리 (S3 접근, GDAL 변환 등)
- `Repository` : DB 접근 처리

---

## 5. 실행 방법
```bash
./gradlew bootRun
```
- **H2 DB 확인**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)  

- **S3 파일 조회**:
```bash
aws s3 ls s3://dev1-apne2-pre-test-tester-bucket/tester-LeeJina-1744854818/
```

---

## 6. API 요약

| 기능             | URL                                  | 메서드  | 설명                          |
|------------------|--------------------------------------|--------|-------------------------------|
| 파일 목록 조회    | `/satellite-images/source-list`      | GET    | 원본 TIFF 리스트 조회          |
| 메타데이터 저장   | `/satellite-images/metadata`         | POST   | DB에 메타데이터 저장           |
| 변환 + 업로드     | `/satellite-images/upload`           | POST   | GDAL로 COG 변환 후 S3 업로드  |

---

## 7. 예상 면접 질문

1. COG 변환을 어떻게 처리했나요? (GDAL 사용 방식 및 처리 흐름)
→ GDAL의 CLI 도구를 활용해 처리했습니다.
   TIFF 파일을 S3 에서 다운로드 > GDAL로 COG 변환 > 변환된 파일을 다시 S3에 업로드 하는 흐름 입니다.


2. S3 내 동일 파일명이 존재할 경우 어떻게 중복을 방지했나요? (시퀀스 증가 방식)
→ 파일명에 시퀀스를 붙이는 방식으로 중복을 방지했습니다.
   기존 파일들을 조회해 가장 높은 번호를 찾아 +1 시퀀스로 새 이름을 만들었습니다.


3. 실제 운영 환경에서 이 API의 성능 병목은 어떤 부분에서 발생할 수 있나요?
→ 주요 병목은 다음 세 가지 입니다.
   1. GDAL 변환 : 외부 CLI 호출이라 속도나 병렬 처리에 한계가 있음
   2. S3 업로드 : 대용량 파일은 업로드 시간이 오래 걸림
   3. DB 저장 : 대량 데이터 저장 시 트랜잭션 처리 속도가 이슈될 수 있음