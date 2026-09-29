# Toy PKI 코드 생성 지침

이 저장소에서 코드를 작성하거나 자동완성할 때 적용한다. 기존 코드의 공통 패턴을
기준으로 하며, 패턴이 혼재된 부분은 아래의 신규 코드 규칙을 따른다.
기존 파일을 수정할 때는 주변 코드와의 일관성을 우선하고, 요청과 무관한 전체
포맷 변경이나 이름 변경은 하지 않는다.

## 프로젝트 구조

- Java 17, Spring Boot, Spring MVC, Thymeleaf, Lombok, Bouncy Castle을 사용한다.
  Java 문법과 API는 `build.gradle`의 Java 17 설정에 맞춘다.
- `toy.pki.ca`는 인증서와 프로필, `toy.pki.kms`는 키 관리와 서명을 담당한다.
- 기존 `controller`, `service`, `repository`, `domain`, `dto` 패키지에 역할에 맞게 배치한다.
  JCA 구현은 `kms.infrastructure.jca`에 둔다.
- 기존 타입과 의존성을 먼저 활용한다. 작은 기능을 위해 별도 프레임워크,
  공통 베이스 클래스, 서비스 인터페이스 계층을 추가하지 않는다.

## Java 형식과 이름

- 들여쓰기는 공백 4칸, 여는 중괄호는 선언과 같은 줄에 둔다.
  `if`, `for` 등의 본문은 한 줄이어도 중괄호를 사용한다.
- 긴 매개변수나 인자 목록은 한 줄에 하나씩 쓰고, 선언/호출보다 8칸 들여쓴다.
  닫는 괄호는 별도 줄에 선언/호출과 맞춘다. 짧고 읽기 쉬운 선언은 한 줄로 둔다.
- 필드, 생성자, 메서드 순으로 배치하고 메서드 사이에는 빈 줄 하나를 둔다.
  메서드 안에서는 의미 있는 처리 단계 사이에만 빈 줄을 둔다.
- 새 파일의 import는 `jakarta`/`lombok`/`org` 등의 외부 타입, `toy.pki`, 빈 줄,
  `java`/`javax` 순으로 묶고 각 그룹 안에서는 사전순으로 정렬한다.
  기존 코드에는 wildcard import도 있으나 새 import는 명시적으로 작성한다.
- 타입은 `PascalCase`, 메서드와 변수는 `camelCase`, 새 상수와 enum 항목은
  `UPPER_SNAKE_CASE`를 쓴다. 기존 `KeyID`, `Ed25519`, `secp256r1` 등의 이름은 유지한다.
- `keyAlgorithmPreset`, `bindingResult`처럼 의미가 드러나는 이름과 명시적인
  지역 변수 타입을 선호한다. 단순 흐름을 불필요한 Stream 체인으로 바꾸지 않는다.
- 기존 파일의 LF/CRLF를 유지한다. Gradle 파일은 기존 탭 들여쓰기를 따른다.

## 타입과 Lombok

- 불변 데이터 전달과 값 객체는 기존 패턴처럼 `record`를 우선 검토한다.
  예: `ProfileId`, `RsaKeyGenerationParameter`, `ExtendedKeyUsageExtension`.
- 수정 가능한 폼 DTO는 `@Data` 클래스로 작성한다. 기본/전체 인자 생성자는
  바인딩 방식과 실제 호출에 필요한 경우에만 추가한다.
- 도메인 클래스는 필요한 `@Getter`, `@Setter`를 선택한다. 기존 클래스가
  `@Data`를 사용한다는 이유만으로 모든 새 도메인 클래스에 붙이지 않는다.
- Spring 의존성은 `private final` 필드와 `@RequiredArgsConstructor`로 주입한다.
  `KeyGeneratorRegistry`처럼 생성 시 변환이 필요하면 명시적인 생성자를 사용한다.
- 알고리즘과 선택지는 기존 enum과 파라미터 타입을 재사용한다.
  `KeyGenerationParameter`의 sealed interface/record 구조를 확장할 때는
  관련 registry, factory와 호출부를 함께 확인한다.
- `record` 접근자와 Lombok getter를 구분하고, 생성자와 메서드가 실제로
  존재하는지 선언을 확인한다. TODO나 미완성 호출을 정상 구현의 근거로 삼지 않는다.

## Spring MVC와 구현 방식

- 화면 컨트롤러는 `@Controller`, `@RequestMapping`, `Model`과 Thymeleaf
  뷰 이름 반환 패턴을 따른다. 기존 화면 흐름에 임의로 REST 응답을 섞지 않는다.
- 폼 검증은 기존 `@Validated @ModelAttribute` 패턴을 따르고,
  `BindingResult`는 검증 대상 인자 바로 뒤에 둔다.
  오류는 폼 화면으로 반환하고, 저장 성공 시에는 redirect 패턴을 따른다.
- 폼 필드명을 바꿀 때는 Thymeleaf의 `th:object`, `th:field`와 바인딩도 확인한다.
- 서비스는 기능의 처리 흐름, repository는 저장과 조회, JCA 구현은 암호 연산을
  담당하도록 작성한다. 현재 repository는 메모리 저장소이므로 기존 구현을
  확인하고 확장하며, JPA나 데이터베이스를 가정하지 않는다.
- `GeneralSecurityException` 등 기존 예외 계약을 유지한다.
  예외를 잡아서 성공 값이나 의미 없는 `null`을 반환하지 않는다.
- 로깅은 `@Slf4j`와 `{}` 인자 치환을 사용한다.
  `SignatureService`처럼 알고리즘과 결과 길이 등 필요한 메타데이터를 기록한다.
- 주석은 코드만으로 드러나지 않는 의도와 제약을 짧게 설명한다.
  수정하는 파일의 주석 언어를 따르고, 요청된 구현을 TODO로 대체하지 않는다.

## 참고 코드와 검증

- 서비스/주입: `src/main/java/toy/pki/kms/service/KeyManagementService.java`
- 여러 줄 인자/JCA 구현: `src/main/java/toy/pki/kms/infrastructure/jca/RsaKeyGenerator.java`
- 폼 DTO: `src/main/java/toy/pki/ca/dto/profile/KeyUsageForm.java`
- 값 객체: `src/main/java/toy/pki/ca/domain/profile/ProfileId.java`
- 참고 파일의 역할과 형식을 따르되, 미완성 로직이나 불필요한 import까지 복제하지 않는다.
- Java 변경 후에는 `./gradlew compileJava`로 타입과 생성자 호환성을 확인하고,
  동작을 변경했다면 관련 테스트를 실행한다. 전체 테스트 명령은 `./gradlew test`다.
  문서만 변경한 경우 빌드는 필요하지 않다. 실행하지 못한 검증은 명시한다.
