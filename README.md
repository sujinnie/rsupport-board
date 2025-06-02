# 공지사항 관리 프로젝트

### 1. 프로젝트 개요

- **주제**: 공지사항 관리 REST API 구현

- **제출 기한**: 2025-06-05 10:00
- **언어/프레임워크**: Java/Spring Boot, JPA/Hibernate, MySQL

---

### 2. 기능 요구사항 요약

| 기능 | 설명 |
| --- | --- |
| 등록 | 제목, 내용, 공지 시작일시, 공지 종료일시, 첨부파일(다중) |
| 수정 | ID로 공지 수정 |
| 삭제 | ID로 공지 삭제 |
| 목록 조회 | 제목, 첨부파일 유무, 등록일시, 조회수, 작성자 |
| 상세 조회 | 제목, 내용, 등록일시, 조회수, 작성자, 첨부파일 |
| 검색 | (1) 제목+내용, (2) 제목만 / 검색기간: 등록일자 |

---

### 3. API 명세

- RESTful API 설계: 리소스 단위로 URI 설계, 행위는 HTTP 메서드
    
    
    | No | Method | URI | 설명 |
    | --- | --- | --- | --- |
    | 1 | POST | `/v1/notices` | 공지사항 등록 |
    | 2 | GET | `/v1/notices` | 공지사항 목록 조회 + 검색 |
    | 3 | GET | `/v1/notices/{noticeId}` | 공지사항 상세 조회 |
    | 4 | PATCH | `/v1/notices/{noticeId}` | 공지사항 수정 |
    | 5 | DELETE | `/v1/notices/{noticeId}` | 공지사항 삭제 |

- API 명세서: 첨부예정

- swagger (오류 응답까지 함께 정의) : 추가예ㅈ정

---

### 4. ERD
   첨부예정


---

### 5. 아키텍쳐 및 패키지 구조

- 아키텍쳐 (업데이트예정)

- 패키지구조 (업데이트예정)
    
    ```
    com.rsupport.board
    ├─ common
    │   ├─ config         // 공통 설정(configuration), Swagger, Auditing 등
    │   ├─ dto            // 공통 응답 DTO 등
    │   ├─ entity         // BaseTimeEntity 등
    │   └─ exception      // 공통 예외 정의 및 핸들러
    │
    ├─ member.domain
    │   ├─ entity         // 관련 엔티티
    │   └─ repository     // 관련 레파지토리
    │
    └─ notice
        ├─ api            // 1. Presentation Layer(API 관련)
        │   ├─ controller // 컨트롤러
        │   └─ dto        // Request/Response DTO
        │
        ├─ service        // 2. Service Layer(비즈니스 로직)
        │                 // 서비스 인터페이스, 구현체
        │
        ├─ domain         // 3. Persistence Layer (DB접근관련, JPA구현체 등)
        │   ├─ entity     // 관련 엔티티
        │   └─ repository // 관련 레파지토리, jpa 구현체
        │
        └─ infra         // 4. Infra Layer (외부 관련 설정)
    
    ```
    

---

### 6. 실행방법


### 7. 테스트 시나리오

### 8. 핵심 문제 해결 전략
