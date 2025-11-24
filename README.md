# Dialog Backend Service
> **AI 기반 회의록 및 일정 통합 관리 플랫폼, Dialog의 백엔드 서버입니다.**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6.2-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)](https://spring.io)
[![Google Calendar API](https://img.shields.io/badge/Google_Calendar_API-v3-4285F4?style=for-the-badge&logo=google-calendar&logoColor=white)](https://developers.google.com/calendar)

---

## 📖 프로젝트 개요 (Overview)
**Dialog**는 파편화된 업무 일정과 회의 기록을 한곳에서 관리할 수 있는 통합 플랫폼입니다.
본 리포지토리(Backend)는 **Google Calendar API와의 실시간 양방향 동기화**, **JWT 기반의 이중 보안 인증**, **대용량 일정 데이터의 정합성 처리**를 담당합니다.

### 🚀 핵심 목표
* **All-in-One View:** 사내 업무(Task/Meeting)와 개인 일정(Google)의 통합 대시보드 제공
* **Data Integrity:** 외부 API 장애 시에도 서비스 가용성을 보장하는 Fail-Safe 아키텍처 구축
* **Secure Auth:** OAuth2와 JWT를 결합한 하이브리드 인증 시스템 구현

---

## 🛠 기술 스택 (Tech Stack)

| Category | Technology |
| --- | --- |
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.x, Spring Data JPA |
| **Security** | Spring Security, OAuth 2.0, JWT |
| **Database** | MySQL 8.0, Redis (Optional: 캐싱 사용 시) |
| **API Client** | Spring WebClient (Non-blocking I/O) |
| **Build Tool** | Gradle |
| **Infra** | Docker, AWS EC2 (배포 환경에 따라 수정) |

---

## 💡 주요 기술적 특징 (Key Features)

### 1. Google Calendar 연동 및 데이터 동기화
* **Aggregator 패턴 적용:** 로컬 DB 데이터와 Google API 데이터를 실시간으로 병합(Merge)하여 단일 뷰로 제공.
* **HashMap 기반 중복 제거:** $O(N^2)$의 리스트 탐색 대신 `Map` 구조를 활용하여 일정 병합 속도를 $O(N)$으로 최적화.
* **UTC Timezone 표준화:** 서버-클라이언트-구글 간 시간차 문제를 해결하기 위해 모든 통신에 ISO 8601(UTC) 표준 적용.

### 2. 이중 토큰 보안 아키텍처 (Dual Token Strategy)
* **1:1 매핑 구조:** 서비스 로그인용 `JWT`와 구글 API용 `OAuth Token`을 분리 관리.
* **Security Context 활용:** `JwtAuthenticationFilter`를 통해 식별된 `Principal(Email)`을 키(Key)로 사용하여, DB 내의 암호화된 구글 토큰을 안전하게 인출.
* **Silent Refresh:** API 호출 시 Access Token 만료가 임박하면 사용자 개입 없이 백그라운드에서 자동 갱신.

### 3. 데이터 무결성 보장 (Write-Through)
* 일정 생성/수정 시 **[Google 선 저장 → 로컬 후 저장]** 프로세스를 강제하여 데이터 불일치(Inconsistency) 원천 차단.
* 외부 API 장애 발생 시 커스텀 예외(`GoogleOAuthException`)를 통해 클라이언트 측에 재연동 가이드 제공.

---

## 📂 프로젝트 구조 (Package Structure)
com.dialog 
├── 📂 calendarevent # 일정 통합 관리 도메인 (Service, Controller) 
├── 📂 googleauth # Google OAuth 인증 관련 로직
├── 📂 meeting # 회의록 관리 도메인
├── 📂 security # JWT 필터 및 보안 설정
├── 📂 token # Social Token 관리 및 갱신 로직 
└── 📂 user # 사용자 관리 도메인
