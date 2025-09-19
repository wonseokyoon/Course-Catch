## Course-Catch
<img width="520" height="222" alt="image" src="https://github.com/user-attachments/assets/f3077ae2-7678-45e7-ba6a-0ef7575dd4dc" />

## 프로젝트 소개
### 개요
- 주제: 수강신청 서비스
- 설명: 강의를 신청할 수 있는 실시간 기반 예매 시스템
- 목표: 트래픽 폭주에도 안정적으로 작동하고, 공정한 예매 및 실시간 대기열 기능 제공
### 기획 의도
- 강의 신청 오픈 시 동시 접속자 폭주 문제 해결
- 대기열, 실시간 여석, 알림 등 실제 환경과 유사한 서비스 구현
### 특징
- 안정적인 예매 시스템이 필요
- 서버 과부하, 예약 시스템 불만족 등 민원 최소화 목표
- 신청 데이터 분석 및 실시간 현황 모니터링 요구
### 🔑 핵심 기능
<img width="879" height="369" alt="image" src="https://github.com/user-attachments/assets/9e4c9514-151c-4fd4-9180-dfabf73911a9" />
<br/><br/>

## 개발 기간
2025.07.16 ~ 2025.8.17 (약 5주)
<br/><br/>

## 팀 구성 및 역할
1인 개발
<br/><br/>

## 기능
### 1차 스프린트 - 기본 기능
- 인증/인가: 이메일 로그인, 회원가입
- 강의: 등록, 조회, 삭제, 수정
- 수강 신청: 신청, 취소, 조회, 여석제한
- CICD: 자동화 파이프라인 환경 구축

### 2차 스프린트 - 주요 기능
- 인증/인가: SSO 로그인, 이메일 인증, 회원 (소프트)탈퇴, 1년뒤 하드 삭제, 계정 복구, 회원 정보 수정, 비밀번호 DB에 암호화하여 저장, 토큰 정보 Redis에 저장
- 강의: 검색, 신청 시간 제한, 대기열 시스템
- 신청: 신청 목록 페이징, 동시성 제어, 중복 신청 방지

### 3차 스프린트 - 고도화
- 모니터링
- 부하테스트
- 성능개선
- 결제: 결제와 취소
- 알림: 수강 신청 상태 알림
<br/><br/>

## ERD
<img width="1392" height="782" alt="image" src="https://github.com/user-attachments/assets/3390d95f-ff33-4d7e-a488-5bb0287dd1ab" />

## API 명세서
<img width="512" height="730" alt="image" src="https://github.com/user-attachments/assets/811c1f8d-cc55-4ba8-8ff1-2caf1174f9d7" />

## 기술 스택
<img width="720" height="226" alt="image" src="https://github.com/user-attachments/assets/4380efa7-2cc7-4a9c-bdc8-6385b3a9f751" />
<br/><br/>

## CI/CD
<img width="432" height="694" alt="image" src="https://github.com/user-attachments/assets/a6e1b129-01b2-42d5-bc6f-88453ac65963" />

### 검증 체인 활성화

<img width="769" height="799" alt="image" src="https://github.com/user-attachments/assets/0c5e25bf-9e4d-4dec-9149-5563a2d5e9cd" />

### 배포 자동화

<img width="1294" height="525" alt="image" src="https://github.com/user-attachments/assets/72866de7-3add-496a-a4d2-6b5140f9d3d7" />

### 개발 환경과 운영 환경 분리

## 트러블 슈팅
[트러블 슈팅](https://www.notion.so/232f56a42b87812ca48bd24e9ccf37bb)

## 기술 선택
### 동시성 제어
<img width="1250" height="722" alt="image" src="https://github.com/user-attachments/assets/5eaa3ac4-e0c2-4865-ac20-aa832975ef13" />

### 대기열 시스템
<img width="1250" height="725" alt="image" src="https://github.com/user-attachments/assets/4b94726a-c236-4d44-b97a-28becb144fb2" />

### 신청 알림
<img width="1250" height="251" alt="image" src="https://github.com/user-attachments/assets/1ca21c0f-6a6e-4a95-90af-92972b7cc27b" />

### 결제
#### HTTP Client: RestTemplate vs RestClient
- 동기 vs 비동기
- 스레드 병목현상 감안하여 비동기 방식 사용
- Kafka를 사용하여 외부 메시지 발행을 안정적으로 처리

## Link
[Notion](https://www.notion.so/232f56a42b87812ca48bd24e9ccf37bb)
