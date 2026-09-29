# Java 스타일 가이드

## 문서 수정 내역

아래의 버전 중 `배포 버전`은 공개 가능한 수준의 문서 작성이 완료 되었을 때 할당한다.

| **배포 버전** | **작성 완료일** | **내용**       |
| ------------- | --------------- | -------------- |
| v 1.0         | 2024-01-12      | 초안 작성 완료 |

##  개요

이 가이드의 목표는 **해야 할 것**과 **하지 말아야 할 것**을 명시하는 것이다. 이를 통해 협업 개발의 관리를 용이하게 하고, 소모적인 기술적 논쟁을 막는다.

이 가이드는 네이밍 컨벤션까지 포함한다. 네이밍 컨벤션은 옳고 그름이 없으며 개인의 취향에 의해 결정될 수 있으나, 개인의 취향이기 때문에 자주 논쟁의 소지가 된다. 그러나 제품의 완성도는 **일관성**의 유지와 큰 상관관계를 가지며, 이를 위해 본 가이드는 일부 네이밍 컨벤션을 고정한다. 이 가이드에서 고정되어 있는 네이밍 컨벤션은 강제사항이며, 개인적 취향에 맞지 않는 규정이 있어도 **규정은 규정**이라는 생각 하에 준수하도록 한다.

_이 가이드는 제품에 대한 **코드 리뷰**의 **기준**으로 사용된다. 가능한 경우 코드 리뷰시 이 가이드를 인용하도록 하며, 리뷰어는 리뷰시 가이드 내용을 전파하도록 한다._

이 가이드의 내용을 갱신해야 할 필요가 있을 경우 팀장에게 문의하도록 한다.

##  대전제

이 문서는 구글의 Java 코딩 스타일을 참고한 문서이다.

JAVA 17

##  소스 파일 기본

### \<파일명\>

소스 파일은 클래스를 포함하는 이름으로 되어있고 .java 확장자를 가진다.

### \<공백문자\>

개행 문자를 제외하고, ASCII 코드 공백문자(0x20)는 소스 파일에서 유일한 공백문자이다. 두가지를 의미하는데

1. String이나 문자 리터럴에서 공백문자는 이스케이프 된다.
2. 탭 문자는 들여쓰기에 사용하지 않는다.

### \<이스케이프 되는 특수문자\>

`\\b`, `\\t`, `\\n`, `\\f`, `\\r`, `\\"`, `\\'`, `\\\\` 에 대해선 octal방식( `\\012` )이나 유니코드(`\\u000a` ) 보단 앞의 방식을 사용한다.

### \<ASCII 코드 외의 문자\>

아스키코드가 아닌 문자는 유니코드 케릭터 ( `∞` )나, 유니코드 이스케이프 ( `\\u221e`)가 활용된다. 가장 읽기 좋은 방식으로 선택하는 것이 좋다.

### \<예시 코드\>

```java
/* 가장 좋은 경우 */
String unitAbbrev = "μs";
return '\ufeff' + content; // byte order mark
/* 허용되지만 권장되지는 않음 */
String unitAbbrev = "\u03bcs"; // "μs"
String unitAbbrev = "\u03bcs"; // Greek letter mu, "s"
/* 안됨 */
String unitAbbrev = "\u03bcs";
```

##  소스 파일 구조

### \<Import 문\>

다음 순서로 구성된다.

1. 라이센스 또는 저작권 정보(있을 경우)
2. package 명세
3. import 명세
4. 최상위 클래스 시작 그리고 각 섹션들은 하나의 빈 줄로 구분한다.

**라이센스**

있는 경우에 적어주면 된다.

**패키지 문**

보통 화면 최상단에 위치한 package 문이다. 패키지 문은 개행하지 않고, 다른 내용에 적용되는 열 제한(최대 100자)는 패키지문에는 적용되지 않는다.(길면 긴대로 적는다.)

**import 문**

1. 와일드 카드 (ex. java.util.\* 처럼 아스테리스크로 하위 클래스를 다 적용하는 방식)으로는 가져오지 않는다.

- 이유 1. 중요한 문제는 아니지만, 성능 이슈가 생길 수도 있다. Wildcard Import 는 컴파일 할 때 실제 클래스를 찾기위해 해당 패키지의 클래스를 전부 탐색하는데, 그 시간이 더 걸리니까. 하지만 파일 몇개를 탐색한다고 차이가 더 날지 사실 의문이다. 또한 많은 어플리케이션이 사전에 컴파일된 jar나 war를 사용하는 경우가 많으므로 이 이유는 크게 중요하지 않다.
- 이유 2. 별도의 두 패키지를 wildcard import 했는데, 두 패키지 모두에 있는 동일한 이름의 클래스를 활용하는 경우이다. 이 때 참조할 클래스를 결정할 수 없으므로 컴파일 할 수 없는 상황이 발생한다. 단순히 클래스를 import 했는데 소스코드를 수정해야 하므로 바람직하지 못하다.

1. import 문도 패키지 문과 마찬가지로 길다고 개행하지 않는다. 열제한(최대 100자) 또한 적용하지 않는다.
2. static import 와 non-static import은 따로 모아서 블록을 만든다. 블록의 순서는 static, non static이다. 블록 사이에는 1줄의 개행을 넣는다.각 블록 내에서의 정렬순서는 ASCII 코드 정렬순서이다.

_상위 클래스를 import 할 때 포함되므로 스태틱 inner class를 위해 static import하지 않는다._

### \<예시 코드\>

```
/*
 * 저작권이 있는 경우 작성
 */

package com.ozragwort.moaon.springboot.service.videos;

import static com.ozragwort.moaon.springboot.domain.specs.VideosSpecs.searchWith;
import static com.ozragwort.moaon.springboot.util.Calculation.calcScore;
import static com.ozragwort.moaon.springboot.util.ConvertTo.DurationStringToSecond;
import static com.ozragwort.moaon.springboot.util.ConvertTo.StringToUTCDateTime;
import static java.util.Objects.isNull;
import static org.h2.mvstore.DataUtils.checkArgument;

import com.ozragwort.moaon.springboot.domain.categories.Categories;
import com.ozragwort.moaon.springboot.domain.categories.CategoriesRepository;
import com.ozragwort.moaon.springboot.domain.channels.Channels;
import com.ozragwort.moaon.springboot.domain.channels.ChannelsRepository;
import com.ozragwort.moaon.springboot.domain.specs.VideosSpecs.VideosSearchKey;
import com.ozragwort.moaon.springboot.domain.videos.*;
import com.ozragwort.moaon.springboot.dto.videos.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

### \<클래스 선언\>

### **하나의 최상위 클래스 선언 (Exactly one top-level class declaration)**

소스 내에서 최상위 클래스는 단 하나여야 한다. 클래스 멤버들간의 순서는 정답이 없지만, 논리적 순서를 갖추는게 중요하다. 비슷한 역할을 하는 메소드 끼리 뭉쳐놓고, 추상화 단계에 따라 배치하자

_overload된 메소드들은 흩어놓지 않는다_

### **class 내용의 순서**

클래스의 멤버의 순서는 딱 정해진것은 없지만 논리적인 순서가 있어야 한다.

단순하게 새로운 메서드를 만들때 가장 뒤에 만드는 식으로 만들면 안된다는 의미이다.

### **Overloads: 분할하지 않음**

여러 생성자, 같은 이름을 가진 메서드 들은 꼭 모아서 순차적으로 나타낸다.

private도 포함해야한다.

##  포맷팅 (Formatting)

### \<중괄호\>

### **생략 가능하더라도 중괄호를 사용해야함**

예를들어 `if`, `else`, `for`, `do` and `while`에서 내용이 비어있거나 한줄이어도 중괄호를 꼭 사용함

|                                                    |
| -------------------------------------------------- |
| `if (sum > 0)     return sum; else     return -1;` |
| **하지 말 것**                                     |

|                                                            |
| ---------------------------------------------------------- |
| `if (sum > 0) {     return sum; } else {     return -1; }` |
| **해야함**                                                 |

### **블록이 비어있지 않는 경우 : K&R 스타일**

_Kernighan & Ritchie 스타일_

- _여는 중괄호 앞 : 줄 바꿈을 하지않음_
- _여는 중괄호 뒤 : 줄 바꿈을 함_
- _닫는 중괄호 앞 : 줄 바꿈을 함_
- _닫는 중괄호 뒤 : 줄 바꿈을 함_
  - _메서드 또는 클래스의 끝, else닫는 경우 중괄호 뒤 줄 바꿈이 없을 수 있음_

|                                                                                                                                                                                                                                                                                                                                                         |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `return () -> {   while (condition()) {     method();   } };  return new MyClass() {   @Override public void method() {     if (condition()) {       try {         something();       } catch (ProblemException e) {         recover();       }     } else if (otherCondition()) {       somethingElse();     } else {       lastThing();     }   } };` |
| **해야함**                                                                                                                                                                                                                                                                                                                                              |

### **빈 블록인 경우**

중괄호를 열고 닫아도 됨.

|                                                                                     |
| ----------------------------------------------------------------------------------- |
| `// (try에는 내용이 있기 때문에) try {     doSomething(); } catch (Exception e) {}` |
| **하지 말 것**                                                                      |

|                             |
| --------------------------- |
| `void doNothingElse() { } ` |
| **해야함**                  |

### \<블록 들여쓰기 : +4 spaces\>

블록 들여쓰기는 4공백 (Tab) 이다.

즉 새로운 블록이 시작하면 4공백을 추가해서 작성하다가, 블록이 끝나면 들여쓰기를 끝내면 된다.

### \<한 줄에 하나의 명령문\>

명령문 뒤에 줄 바꿈

### \<열 길이 제한: 120\>

한 줄에 **길이가 120이 넘으면 안됨**.

길이의 기준은 모든 유니코드 코드 포인트를 의미함

**Exceptions:**

- 열 제한을 할 수 없는 행(Javadoc의 긴 URL, JSNI 메소드 등)
- package, import
- 쉘에서 자를수 없는 주석 명령줄
- 특정 스타일 조건이 적용된 경우
- Method call arguments

### \<줄 바꿈 (Line-wrapping)\>

한 줄을 차지할 수 있는 코드를 여러줄 로 나누는 것을 줄바꿈 이라고 한다. 줄바꿈도 여러 방식이 있다. 보통 줄 바꿈은 하는 이유는 열 제한(120자)을 초과하지 않기 위해서이지만, 열 제한을 넘지 않아도 가독성을 위하여 줄바꿈을 할 수 있다.

_메소드나 지역변수를 생성함으로써 줄바꿈 없이 열제한을 해결할 수 있다._

### **줄 바꿈 규칙**

- 기호 앞에서
  - the dot separator (`.`)
  - the two colons of a method reference (`::`)
  - an ampersand in a type bound (`<T extends Foo & Bar>`)
  - a pipe in a catch block (`catch (FooException | BarException e)`).
- 대입 연산자 앞에서

|                                                          |
| -------------------------------------------------------- |
| `new StringBuilder()     .append("a")     .append("b");` |
| **해야함**                                               |

- 쉼표 (,) 뒤에서 (열 길이 제한을 넘었을 경우)

|                                                                                                                                                                                                                                                                                                                                                                                            |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `public Videos(         Channels channels,         String videoId,         String videoName,         String videoThumbnail,         String videoDescription,         LocalDateTime videoPublishedDate,         long videoDuration,         int viewCount,         int likeCount,         int dislikeCount,         int commentCount,         double score,         List<String> tags) { }` |
| **해야함**                                                                                                                                                                                                                                                                                                                                                                                 |

- 람다식에서 중괄호가 없는 단일 표현식에서 화살표 바로 뒤에 중단이 오는 경우를 제외한 경우 화살표 뒤에서 가능

|                                                                                                                                                                                      |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `MyLambda<String, Long, Object> lambda =     (String label, Long value, Object obj) -> {         ...     };  Predicate<String> predicate = str ->     longExpressionInvolving(str);` |
| **해야함**                                                                                                                                                                           |

### **연속 줄 바꿈은 들여쓰기 +4 spaces**

줄바꿈을 하고 나면 최초 행보다 최소 4자를 들여쓰기 한다.

### \<공백\>

### **세로 공백**

다음의 경우 한 줄의 세로 공백을 나타낸다.

1. 클래스 멤버를 구분할 때: **메소드, 생성자, 멤버 변수 등 사이**에  
   멤버 변수의 경우 사이에 코드가 없으면 공백이 없어도 됨
2. 메소드 내부에서 **논리적으로 그룹핑** 되는 경우

### **가로 공백**

1. `if`, `for` or `catch`, 뒤에 오는 (`(`) 사이에 공백
2. `else` or `catch`, 와 그 앞에 있는 (`}`) 사이에 공백
3. 여는 중괄호 (`{`), 앞에 공백 예외 두 가지:
   - `@SomeAnnotation({a, b})` (공간을 사용하지 않음)
   - `String[][] x = {{"foo"}};` (공백이 필요하지 않음`{{`, )
4. 이항, 삼항 연산자, operator-like 양쪽에 사용
   - 타입 바운딩의 & : `<T extends Foo & Bar>`
   - 예외 처리 시 | : `catch (FooException | BarException e)`
   - 향상된 `for` 문 에서 (`for (int n : list)`)
   - 람다에서 화살표 -\>: `(String str) -> str.length()`
     - 다음은 공백을 넣지 않음
   - 두개의 세미콜론 (`::`) : `Object::toString`
   - 하나의 점 (`.`) :`object.toString()`
5. `,:;` 또는 타입 변경 cast 시 (`)`) 뒤에 공백
6. 주석에서 (`//`) 뒤 공백. 여러개가 가능하지만 필수는 아님
7. 변수 선언 시 타입과 변수명 사이 공백: `List<String> list`
8. 배열 선언문 사이의 공백
   - `new int[] {5, 6}` and `new int[] { 5, 6 }` 둘 다 사용 가능
9. type annotation and 대괄호 (`[]` )or (`...`).

### **가로 정렬 : 필요없음**

가독성을 위해 공백을 이용하여 정렬을 하는 경우가 있지만 **google 스타일에선 요구되지 않음**

|                                                           |
| --------------------------------------------------------- |
| `private int x; // 괜찮음 private Color color; // 괜찮음` |
| **해야함**                                                |

### \<그룹화 괄호: 권장\>

코드 **작성자와 검토자가 합의한 경우에만 생략**된다.

java의 연산자 우선 순위 테이블을 가지고 있다고 해도 가능하면 써야한다.

예를들면 `int n = 5 * 3 + 1` 로 써도 되지만 가능하면 `int n = (5 * 3) + 1`로 쓰라는 말이다.

### \<특별한 구조\>

### **Enum 클래스**

enum 상수의 각 컴마 다음에 개행은 선택적이다. 추가의 개행도 허용된다. (보통 한번):

```
private enum Answer {
  YES {
    @Override public String toString() {
      return "yes";
    }
  },

  NO,
  MAYBE
}
```

별다른 documentation이 없는경우 배열 초기화와 같은 포맷으로 작성해도 된다. (반드시 개행)

```
private enum Suit {
        CLUBS,
        HEARTS,
        SPADES,
        DIAMONDS
}
```

### **변수 선언**

- 한줄에 하나씩 선언

|              |
| ------------ |
| `int a, b;`  |
| `하지 말 것` |

하지만 `for` 문의 헤더에서는 여러 변수 선언이 쓰일 수 있다.

- 필요할 때 선언

꼭 블럭이 시작될 때 변수를 선언하지 않아도 된다는 의미이다.

하지만 지역 변수는 그 변수가 사용될 곳에서 최대한 가까이 선언하고 선언과 동시에 초기화를 시킨다.

### **배열**

- 배열 초기화는 “block-like”

배열 초기화는 "block-like construct" 처럼 할 수 있다:

|                                                                                                                                                                                                                                                       |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `new int[] {           new int[] {   0, 1, 2, 3            0, }                       1,                         2, new int[] {             3,   0, 1,               }   2, 3 }                     new int[]                           {0, 1, 2, 3}` |
| **해야함**                                                                                                                                                                                                                                            |

- C언어 처럼 선언하지 않음

`String[] args`처럼 선언할 수 있고, `String args[]`처럼 선언하지 않는다.

### **Switch 문**

- 들여쓰기

다른 코드처럼 space +4로 한다.

- 실패 또는 지나감

switch문은 `break`, `continue`, `return` 과 같이 switch문을 종료시킬 수 있다. 이런 경우가 아닌 경우 다름 구문을 실행하게 되는데 이 때 주석을 사용할 수 있다. 또한 이 주석은 해당 case의 마지막에 온다. case 1에는 쓰지 않고 case 2에서 한번에 주석을 달면 된다.

|                                                                                                                                                                              |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `switch (input) {   case 1:   case 2:     prepareOneOrTwo();     // fall through   case 3:     handleOneTwoOrThree();     break;   default:     handleLargeNumber(input); }` |
| **해야함**                                                                                                                                                                   |

- `default` 를 포함

`default` 에서 아무 코드가 없더라도 포함하여 코드를 작성한다.

### **어노테이션 (Annotations)**

- 클래스, 메서드, 생성자, 로컬 변수에 적용되는 어노테이션은 documentation 블럭 바로 이후에 나열된다.

```
@Override
@Nullable
public String getNameIfPresent() { ...
```

- 어노테이션 파라미터는 열 길이 제한을 초과하였을 경우 줄바꿈한다.  
  (앞서 정의한 줄바꿈 규칙에 일관성을 유지하기 위해)

```
@Annotation3(
    param1 = "value1",
    param2 = "value2")
@Annotation4
class Foo { ...
```

**Exception**

```
// 필드, 로컬에서 사용되는 단일 어노테이션의 경우는 한줄에 작성 가능하다. (@Override 제외)
@Partial DataLoader loader;

// 파라미터 어노테이션은 한줄에 작성 한다.
public void method(
        @Annotation1 @Annotation3(
            param1 = "value1",
            param2 = "value2") final int param) {
}
```

### **접근 제한자 (Modifiers)**

사용 가능한 클래스와 멤버의 접근 제한자는 다음과 같다.

- public
- protected
- private
- abstract
- default
- static
- final
- transient
- volatile
- synchronized
- native
- strictfp

### **숫자 리터럴**

long형 숫자의 경우 `3000000000l`대신 `3000000000L` 으로 쓴다.

소문자 l은 숫자 1과 헷갈릴 수 있기 때문이다.

##  네이밍 (Naming)

### \<모든 식별자에 대한 공통 규칙\>

식별자는 ASCII 숫자와 문자만을 사용한다. 일부의 경우 `_`를 쓰기도 한다.

하지만 google style에서는 특별한 접미사나 접두사는 쓰지 않는다 : `name_`, `mName`, `s_name` and `kName`.는 쓰지않는다.

### \<식별자 타입에 대한 규칙\>

### **패키지 이름**

패키지 이름은 모두 소문자이며 연속된 단어는 단순히 함께 연결된다.

`com.example.deepspace`로 쓰고 `com.example.deepSpace` or `com.example.deep_space`로 쓰지 않는다.

### **클래스 이름**

- 클래스 이름은 **"UpperCamelCase"**로 작성한다.
- 주석의 경우 정해진 규칙은 없다.
- 테스트 클래스의 이름은 해당 클래스의 뒤에 Test를 추가한다.
- **naming convention의 네 가지 종류**
  1. UpperCamelCase : 띄어쓰기 부분을 모두 대문자로 치환. 가장 앞 문자는 대문자
  2. lowerCamelCase : 띄어쓰기 부분을 모두 대문자로 치환. 가장 앞 문자는 소문자
  3. snake*case : 띄어쓰기 대신 언더바(*)를 사용하고 모든 문자를 소문자로 치환
  4. CONSTANT*CASE : 띄어쓰기 대신 언더바(*)를 사용하고 모든 문자를 대문자로 치환

### **메서드 이름**

메서드 이름은 **"lowerCamelCase"**를 사용한다.

### **상수 이름**

상수 이름은 **"CONSTANT_CASE"**를 사용한다.

상수는 내용이 변경되지 않아야 한다. 따라서 상태가 바뀐다면 상수가 아니다.

```
// 상수
static final int NUMBER = 5;
static final ImmutableList<String> NAMES = ImmutableList.of("Ed", "Ann");
static final ImmutableMap<String, Integer> AGES = ImmutableMap.of("Ed", 35, "Ann", 32);
static final Joiner COMMA_JOINER = Joiner.on(','); // because Joiner is immutable
static final SomeMutableType[] EMPTY_ARRAY = {};
enum SomeEnum { ENUM_CONSTANT }

// 상수가 아님
static String nonFinal = "non-final";
final String nonStatic = "non-static";
static final Set<String> mutableCollection = new HashSet<String>();
static final ImmutableSet<SomeMutableType> mutableElements = ImmutableSet.of(mutable);
static final ImmutableMap<String, SomeMutableType> mutableValues =
    ImmutableMap.of("Ed", mutableInstance, "Ann", mutableInstance2);
static final Logger logger = Logger.getLogger(MyClass.getName());
static final String[] nonEmptyArray = {"these", "can", "change"};
```

### **상수가 아닌 필드 이름**

**"lowerCamelCase"**를 사용한다.

### **파라미터 이름**

**"lowerCamelCase"**를 사용한다.

### **지역 변수 이름**

**"lowerCamelCase"**를 사용한다.

### **유형 변수 이름**

- 단일 대문자 (such as `E`, `T`, `X`, `T2`)
- 클래스에 사용되는 형식 뒤에 `T` 를 합쳐서 사용(examples: `RequestT`, `FooBarT`).

### **카멜 케이스 : 정의**

가끔씩 "IPv6" 또는 "iOS"와 같이 비정상적인 형식이 있을 수 있다.

따라서 Google Style은 다음과 같은 체계를 사용한다.

1. 문장을 일반 적인 ASCII로 변환하고 어퍼스트로피를 지운다.  
   "Müller's algorithm" → "Muellers algorithm"
2. 결과를 남은 공백과 구두점을 기준으로 단어로 나눈다.  
   이미 캐멀 케이스면 단어로 나눈다. (AdWords → ad words)
3. 이제 모두 소문자로 바꾸고 첫 번째 글자만 대문자로 바꾼다.
4. 모든 단어를 합친다.

**_Examples:_**

- _XML HTTP request_
  - _XmlHttpRequest_
- _new customer ID_
  - _newCustomerId_
- _inner stopwatch_
  - _innerStopwatch_
- _supports IPv6 on iOS?_
  - _supportsIpv6OnIos_
- _YouTube importer_
  - _YouTubeImporter_
  - _YoutubeImporter - 허용 되지만 권장되지는 않음_
- _check non-empty_
  - _checkNonempty_
  - _checkNonEmpty_

##  프로그래밍 실습

### \<@Override: 항상 사용\>

`@Override` annotation 은 사용 가능하면 늘 붙인다.

예외 : 부모 함수가 @Deprecated가 되면 @Override를 생략할 수 있다.

### \<예외에서 catch : 생략하지 말 것\>

다음과 같은 경우가 아닌경우 catch의 내용을 생략하지 말자.

정말 빈칸이어도 되는 경우 주석으로 설명을 써두자.

```
try {
  int i = Integer.parseInt(response);
  return handleNumericResponse(i);
} catch (NumberFormatException ok) {
  // 숫자가 아니어도 그냥 리턴해도 된다.
}
return handleTextResponse(response);
```

**Exception:** 테스트를 할 때, 예외 발생 시 이름을 `expected`로 쓰는 경우 주석없이 무시될 수 있다.

```
try {
  emptyStack.pop();
  fail();
} catch (NoSuchElementException expected) {
}
```

### \<정적 멤버 : 클래스를 직접 사용할 수 있음\>

정적 클래스 멤버에 대한 참조는 해당 클래스의 이름을 바로 사용하여 작성해도 된다.

```
Foo aFoo = ...;
Foo.aStaticMethod(); // 좋음
aFoo.aStaticMethod(); // 나쁨
somethingThatYieldsAFoo().aStaticMethod(); // 아주 나쁨
```

### \<종료자(Finalizers): 사용하지 않음\>

`Object.finalize`와 같이 재정의하지 않음

##  Javadoc

**Formatter 적용은 하지 않고, 반드시 필요한 경우에만 작성하는 것으로 논의 되었다.**

### \<포맷팅 (Formatting)\>

### **일반적인 양식**

Javadoc의 기본 형식은 다음과 같다:

```
/**
 * Multiple lines of Javadoc text are written here,
 * wrapped normally...
 */
public int method(String p1) { ... }
```

한줄 예제:

```
/** An especially short bit of Javadoc. */
```

기본 형식은 **항상 허용**된다.

단 한줄로 작성하는 경우는 `@return`이 없을 때만 가능하다.

### **문단(Paragraphs)**

문단과 문단 사이에는 하나의 빈 줄이 추가된다. (\*)만 포함되는 줄

### **블럭 태그 (Block tags)**

사용되는 표준 블럭 태그는 `@param`, `@return`, `@throws`, `@deprecated`가 있다. 이 네 가지 유형의 설명은 빈칸으로 두면 안된다. 한 줄이 넘어가면 줄 바꿈을 한 뒤 띄어쓰기 4번 이상을 한다.

### **요약**

Javadoc은 해당 내용의 간단한 요약으로 시작한다. 클래스 및 메소드에 대해 나타나는 유일한 부분이기 때문이다.

`/** @return the customer ID */`가 아닌  `/** Returns the customer ID. */`와 같이 작성해야 한다.

### **어디서 Javadoc이 사용되는가**

모든 `public class` , `public`,`protected` 에서 예외를 제외하고 **가능하면 다 쓰자**.

### **예외 : 너무 당연한 경우**

Javadoc은 간단하고 명료한 메소드에 경우 선택적으로 사용해도 된다.

예를들면 `getFoo`와 같은 경우이다. 이것은 foo를 반환하는것이 너무나 당연하기 때문이다.

### **예외 : overrides**

overrides 어노테이션을 사용하는 경우 작성하지 않아도 된다.

### **Javadoc이 필요없는경우**

구현 구석과 착각하여 사용하는 경우가 있을 수 있는데 (`/**`를 사용한 경우) 필수가 아닌 Javadoc은 7.1.2, 7.1.3, 7.2와 같은 규칙을 꼭 따라야 하는건 아니지만 권장은 한다.

##  적용

### \<Google-Java-Style-Format 적용 방법\>

- 1\) IntelliJ IDEA -\> Preferences 접속
- 2\) Preferences -\> Editor -\> Code Style 접속
- 3\) Scheme -\> Import Scheme -\> IntelliJ IDEA code style XML

- 4) intellij-java-autocrypt-style.xml 파일을 다운 후, 아래와 같이 파일 open 후 저장

- 5\) AutocryptStyle로 변경완료

### \<Google-Java-Style-Format 사용법\>

위에 절차를 따르고, `Mac기준 Option+Command+L` 키를 누르시면 Google-Java-Style-Format 에 맞게 코드가 변경됩니다.

### **Save 시 자동으로 코드 변환**

1. Toos - Actions on save (저장 시 동작)
2. Reformat code 체크
3. Optimize imports 체크
4. Rearrange, Code cleanup 등은 사용하지 않습니다. (메서드 위치 변경, 삭제 등 사이드 이펙트 발생 가능성)

[Google Code Style](https://github.com/google/styleguide)

---
