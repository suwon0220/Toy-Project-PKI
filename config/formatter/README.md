# Java Formatter

`Java 스타일 가이드.md`와 `intellij-java-autocrypt-style.xml`의 Java 설정을 기준으로 한다.

- 공백 4칸, 연속 줄 들여쓰기 4칸, 한 줄 120자, K&R 중괄호를 사용한다.
- static import를 먼저 배치하고 빈 줄 다음에 일반 import를 ASCII 순으로 정렬한다.
- wildcard import 전환 임계값은 원본 XML과 동일하게 999로 설정한다.
- 기존 `AGENTS.md`의 연속 줄 들여쓰기 8칸 및 import 그룹 규칙과는 차이가 있다.
  이 Formatter 설정에는 요청된 가이드와 XML의 규칙을 반영했다.

## VS Code

`.vscode/settings.json`에서 Red Hat Java 확장(`redhat.java`)을 기본 Java Formatter로
선택하고 `eclipse-java-autocrypt-style.xml`의 `AutocryptStyle` 프로필을 연결한다.
Java 파일에서 **Format Document**를 실행한다. import 정렬은 **Organize Imports**를 실행한다.

Eclipse Formatter로 표현 가능한 규칙을 변환했으므로 IntelliJ와 줄바꿈 결과가 완전히
같지는 않을 수 있다. 누락된 중괄호 추가, 이름 변경, 멤버 재배치는 수행하지 않는다.
저장 시 자동 포맷 여부는 사용자의 기존 설정을 따른다.

## IntelliJ IDEA

`.idea/codeStyles/Project.xml`에 원본 XML의 Java 설정을 반영하고 프로젝트 코드 스타일을
활성화했다. Java 이외의 언어 설정은 복사하지 않았다.
**Reformat Code**로 포맷하고 **Optimize Imports**로 import를 정리한다.

원본 XML을 수정하면 IntelliJ 프로젝트 설정과 Eclipse 프로필도 함께 갱신해야 한다.
