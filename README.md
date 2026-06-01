# TradeAgent

투자 판단 분석 플랫폼입니다. Spring Boot + JPA + H2 + 정적 프론트엔드로 구성되어 있습니다.

## 요구 사항

- Java 21
- Maven 3.9+ (선택, `mvnw` 사용 시 불필요)
- Windows 기준 `cmd.exe` 또는 PowerShell

## 빌드 방법

프로젝트 루트(`c:\Users\kmh26\IdeaProjects\TradeAgent`)에서 실행합니다.

### CMD (Maven 설치 없이 권장)
```cmd
mvnw.cmd clean test
mvnw.cmd clean package
```

### CMD (Maven 설치된 경우)
```cmd
mvn clean test
mvn clean package
```

### PowerShell
PowerShell 프로필 오류가 보이면, 프로필을 로드하지 않고 실행하세요.

```powershell
powershell -NoProfile -Command ".\mvnw.cmd clean test"
powershell -NoProfile -Command ".\mvnw.cmd clean package"
```

## 실행 방법

패키징 후 JAR로 실행합니다.

```cmd
java -jar target\TradeAgent-0.0.1-SNAPSHOT.jar
```

또는 개발 중에는:

```cmd
mvnw.cmd spring-boot:run
```

## 접속 주소

앱이 기본 포트(8080)로 뜨면 다음 경로를 사용합니다.

- `http://localhost:8080/` → `http://localhost:8080/chart` 로 이동
- `http://localhost:8080/chart/index.html` → 차트 페이지
- `http://localhost:8080/dashboard/index.html` → 대시보드
- `http://localhost:8080/analysis/index.html` → 투자 판단 품질 평가
- `http://localhost:8080/api-test.html` → API 테스트 화면

## 데이터 저장

기본적으로 H2 파일 DB를 사용합니다.

- DB 파일: `data/tradeagent.mv.db`
- 잠금 파일: `data/tradeagent.lock.db`
- 추적 파일: `data/tradeagent.trace.db`

앱 실행 시 `data/` 아래 파일이 자동으로 사용됩니다.

## 주요 설정 파일

- `src/main/resources/application.properties`

핵심 설정 요약:

- `alpaca.*` : Alpaca 시세 API 설정 (키/시크릿은 환경변수로만 주입)
- `gdelt.raw.*` : GDELT Raw GKG 설정
- `vllm.*` : 로컬 LLM(vLLM) 설정
- `spring.datasource.*` : H2 파일 DB 설정
- `trade.seed.*` : 초기 데이터 주입 설정

## GDELT GKG Raw 정책

섹터 동향 갱신은 GDELT DOC API 대신 GDELT 2.0 GKG Raw CSV(`.gkg.csv.zip`)를 사용합니다.

- masterfilelist: `http://data.gdeltproject.org/gdeltv2/masterfilelist.txt`
- 저장 경로: `./data/gdelt-raw`
- 기본 샘플: 하루 1개(18:00 기준) x 최근 30일(오늘 제외)
- 캐시 파일 최대 개수: 30개
- vLLM 호출: 섹터당 1회(최대 6회)

관련 설정(`gdelt.raw.*`):

- `gdelt.raw.enabled`
- `gdelt.raw.master-file-list-url`
- `gdelt.raw.cache-dir`
- `gdelt.raw.default-days`
- `gdelt.raw.default-sample-time`
- `gdelt.raw.max-cached-files`
- `gdelt.raw.max-rows-per-file`
- `gdelt.raw.selected-files-per-refresh`
- `gdelt.raw.request-timeout-seconds`

## 섹터 동향 API

- `POST /api/sectors/refresh-news?startDate=2026-04-30&days=30&sampleTime=1930`
  - GKG Raw 파일 기반 뉴스 갱신 및 `sector_score` 저장
- `GET /api/sectors/trends?from=2026-04-30&to=2026-05-29`
  - 저장된 섹터 점수 조회 (외부 API 호출 없음)
- `GET /api/sectors/user/{userId}/trend-match?date=2026-05-29`
  - 저장 점수와 포트폴리오 비중 비교

## 환경 변수 (권장)

Alpaca 키/시크릿은 저장소 파일에 넣지 않고 환경변수로만 주입하세요.

프로젝트 루트의 `.env.example`을 복사해 `.env`를 만들고 로컬에서만 사용하세요.

### Alpaca
- `ALPACA_API_KEY`
- `ALPACA_API_SECRET`

### vLLM
- `VLLM_ENABLED=true`
- `VLLM_BASE_URL=http://localhost:8000`
- `VLLM_MODEL=Qwen/Qwen2.5-7B-Instruct`
- `VLLM_API_KEY` (필요한 경우)

예시(CMD):

```cmd
set ALPACA_API_KEY=your_key
set ALPACA_API_SECRET=your_secret
set VLLM_ENABLED=true
set VLLM_BASE_URL=http://localhost:8000
```

## PowerShell 프로필 오류 대응

현재 보이는 오류는 PowerShell 프로필(`Microsoft.PowerShell_profile.ps1`)에서 존재하지 않는 Copilot 관련 경로를 실행하려고 해서 발생한 것으로 보입니다.

가장 쉬운 우회 방법:

```powershell
powershell -NoProfile
```

또는 프로필 파일의 3번째 줄에서 잘못된 경로를 제거/수정하세요.

## 실행 순서 추천

1. Java 21 설치 확인
2. `mvnw.cmd -version` 실행 (최초 1회 Wrapper 자동 다운로드)
3. `mvnw.cmd clean test`
4. `mvnw.cmd spring-boot:run`
5. 브라우저에서 `http://localhost:8080/` 접속

## 테스트

기본 테스트는 Spring context와 루트 경로 리다이렉트를 확인합니다.

```cmd
mvnw.cmd test
```

## Maven Wrapper 파일

다음 파일이 포함되어 있으며, Maven 미설치 환경에서도 빌드가 가능합니다.

- `mvnw.cmd`
- `mvnw`
- `.mvn/wrapper/maven-wrapper.properties`

최초 실행 시 `.mvn/wrapper/maven-wrapper.jar`를 자동 다운로드합니다.
