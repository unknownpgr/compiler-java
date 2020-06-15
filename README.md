# Context-free grammar based java parser with pure java with GUI

[TOC]

### Goal

 본 프로젝트는 객체지향프로그래밍 수업의 과제를 위한 것으로, 목표는 주어진 [MyStack.java](./MyStack.java) 파일을 String으로 읽어 그 구조를 파악하고, 결과를 GUI로 나타내는 것이다.

### Approach

 이를 수행하려면 java 파일의 구조를 파악해야 한다. 물론, 과제에서는 각 함수가 참조하는 변수가 무엇인지, 클래스가 포함하는 field가 무엇인지 정도만을 출력하면 된다고 한다. 그러므로 과제의 구현사항을 만족시키는 가장 간단한 방법은 스택이나 정규표현식을 사용한 매칭이다.(교수님께서 ANTLR 등 외부 라이브러리는 사용할 수 없다고 하셨다.) 그러나 최근 컴파일러를 공부하고 있으므로, 이 기회에 순수 Java로 밑바닥부터 프로그래밍 언어의 파서를 구현해보기로 한다.

 물론 실제 자바 언어의 규칙들을 전부 구현하는 것은 무리가 있고, 또한 과제의 범위도 주어진 소스파일만을 정상적으로 파싱하면 된다고 하였으므로, 소스파일에 포함된 최소한의 기능만을 인식하도록 파서를 구현하였다. 따라서 제네릭이나 익명 클래스 등 복잡한 문법은 구현하지 않았다.

### Structure

 본 프로젝트는 크게 Main, Parser, Visualizer의 세 가지 패키지로 구성되어있다.

- Main은 `main()`함수가 포함된 패키지로, Visualizer를 실행하는 역할밖에 하지 않는다.
- Parser는 Java 소스코드를 파싱하는 기능들이 포함되어 있으며, 크게 `Lexer`와 `Parser`로 구성되어있다.
- Visualizer는 파일 선택 및 파싱된 코드를 GUI로 보여주는 역할을 한다. 크게 `TokenWrapper`와 `Visualizer`로 구성되어있다.

### Result

- 다음 과제의 내용들을 모두 구현하였다.
  - Java 소스 파일을 파싱
  - 화면 / 메뉴를 구성
  - 상단 메뉴에서 `File -> Open`을 사용하여 파일을 열고, `File -> Exit`을 선택하여 프로그램을 종료
  - 클래스 구조를 `TreeModel`로 표시
  - 

### How to Use

1. 프로그램을 실행하면 `Class Viewer` Window가 표시된다.

2. 상단의 `File` 메뉴에서 `Open`을 클릭하여 파싱할 자바 파일을 선택한다.

3. 좌측에 클래스 구조가 트리로 표시되며, 트리에서 요소를 선택하면 다음과 같이 작동한다.

   - 클래스 선택 시 모든 멤버를 `TableModel`을 사용하여 표시

   - 메서드 선택 시 소스 코드를 우측 `JTextArea`에 표시하고 좌측 하단 `JTextArea`에 메서드에서 사용하는 변수를 표시

   - 필드 선택 시 이를 사용하는 모든 메서드를 `TableModel`을 사용하여 표시

4. 우상단의 `X`아이콘을 클릭하거나 `File->Exit`를 클릭하여 종료한다.

# Parser

### Lexer
 Lexer는 정규표현식을 사용하여 구현하였다. 소스 코드에 [Lexing rule](./lex-rule.txt)을 위에서부터 적용하여, rule에 매칭되는 부분 문자열이 발견되면 그 문자열을 #으로 치환하고 해당 문자열은 `Token` 클래스로 wrapping하여 priority queue에 저장하도록 하였다. `Token`에는 어떤 lexing rule이 적용되었는지, 매칭된 부분의 시작 위치와 끝 위치 등의 정보가 포함되어있다. Priority queue는 문자열의 시작 위치를 기준으로 정렬된다. 따라서 priority queue에는 파싱된 토큰이 원래 문자열에서 나타나는 순서대로 들어가있다.

 모든 Lexing rule을 적용하고 나면 입력 문자열의 모든 문자가 #으로 치환된다. 만약 #이 아닌 문자가 포함되어있다면 이는 어떤 토큰에도 맞지 않는 문자가 있다는 뜻으로, 그럴 경우 예외를 일으킨다.

 다만 실제 컴파일러상에서는 위와 같은 규칙에 앞서 길이 순으로 lexing rule을 적용한다. 즉, lexing rule우선순위가 낮다 하더라도, 가장 긴 부분 문자열에 매칭되는 Lexing rule을 적용한다. 따라서 위와 같이 단순히 순서대로 매칭하면 몇 가지 예외가 생긴다. 예컨대 `println`이라는 문자열은 `IDENTIFIER`에 매칭되어야한다. 그러나 `IDENTIFIER`의 우선순위가 `TYPE`의 우선순위보다 낮기 때문에, 다음과 같이 매칭되어버린다.

```
pr 	: IDENTIFIER
int	: TYPE
ln 	: IDENTIFIER
```

따라서 다음과 같이 Lexing rule에 약간의 트릭을 써서 이런 문제를 회피한다.

```
TYPE : int#
```

 위와 같이 하면 `int`라는 글자 다음에 공백이나 특수문자가 하나 이상 올 때에만 `int`를 `IDENTIFIER`로 인식한다. 공백 및 특수문자의 우선순위는 `TYPE`이나 `IDENTIFIER`보다 높기 때문에, 이러한 트릭을 쓸 수 있다. 물론 이것 역시 문제가 있는데, 소스코드에 `print()`와 같이 `int`로 끝나는 `IDENTIFIER`가 있으면 똑같은 문제가 발생한다는 것이다. 따라서 이런 트릭은 그런 `IDENTIFIER`가 없는 `MyStack.Java`에만 적용될 수 있는 것으로, 추후 수정할 필요가 있다.

#### Source Code

```java
class MyStack {
	private int size;
	private int top;
	private int[] data;

	public MyStack(int size) {
		data = new int[size];
		this.size = size;
		top = 0;
	}
	... 중략
}
```

#### Lexed Tokens

 콜론 왼쪽은 토큰의 내용이며, 오른쪽은 적용된 Lexing rule이다. 괄호로 표시된 내용은 해당 토큰이 소스코드에서 나타나는 시작과 끝 위치다. 시작 위치는 포함하며, 끝 위치는 포함하지 않는다.

```
class	: CLASS[0,6]
MyStack	: IDENTIFIER[6,13]
{		: BRACKET_B_O[14,15]
private	: QUALIFIER[17,25]
int		: TYPE[25,29]
size	: IDENTIFIER[29,33]
;		: SPLIT[33,34]
private	: QUALIFIER[36,44]
int		: TYPE[44,48]
top		: IDENTIFIER[48,51]
;		: SPLIT[51,52]
private	: QUALIFIER[54,62]
int		: TYPE[62,66]
[		: BRACKET_A_O[65,66]
]		: BRACKET_A_C[66,67]
data	: IDENTIFIER[68,72]
;		: SPLIT[72,73]
public	: QUALIFIER[76,83]
MyStack	: IDENTIFIER[83,90]
(		: BRACKET_C_O[90,91]
int		: TYPE[91,95]
size	: IDENTIFIER[95,99]
)		: BRACKET_C_C[99,100]
{		: BRACKET_B_O[101,102]
data	: IDENTIFIER[105,109]
=		: OPERATOR_ASSIGN[110,111]
new		: NEW[112,116]
int		: TYPE[116,120]
[		: BRACKET_A_O[119,120]
size	: IDENTIFIER[120,124]
]		: BRACKET_A_C[124,125]
;		: SPLIT[125,126]
this	: THIS[129,134]
.		: OPERATOR_REFER[133,134]
size	: IDENTIFIER[134,138]
=		: OPERATOR_ASSIGN[139,140]
size	: IDENTIFIER[141,145]
;		: SPLIT[145,146]
top		: IDENTIFIER[149,152]
=		: OPERATOR_ASSIGN[153,154]
0		: NUMBER[155,156]
;		: SPLIT[156,157]
}		: BRACKET_B_C[159,160]
... 중략
}		: BRACKET_B_C[537,538]
```

Split등을 쓴 경우와 다르게, 각 토큰들이 잘 분리되었을 뿐만 아니라 각 토큰이 가지는 의미 역시 잘 표현되어있다.

### Parser
 Parser는 다음과 같이 구현되었다. 먼저 [Parsing rule](./parse-rule.txt)은 여러 개의 토큰을 하나의 토큰으로 바꾸는 규칙을 기술한다. 이 파일을 따라 순서대로 규칙을 적용한다. 규칙을 적용한다는 것은, Parsing rule에서 오른쪽에 있는 토큰들을 왼쪽에 있는 토큰으로 치환한다는 의미다.

 다만 상용 컴파일러-컴파일러에서는 `|`기호를 사용하여 여러 룰을 한번에 표현하거나, parsing rule에 리터럴을 포함하면 자동으로 lexing시에 처리해주는 기능이 있다. 그러나 본 프로젝트에서는 그런 기능들을 구현하지 않았으므로 하나의 이름을 가진 parsing rule이 여러 개 있어도 되며, 리터럴이 포함되면 안 된다.

### Parsing Algorithm

 Parsing rule을 적용하는 것은 다음의 과정을 따른다.
1. 가장 처음의 파싱 룰을 token의 배열에 한 번만 적용한다. 한 번 적용한다 함은, parsing rule에 매칭되는 부분이 여러 개 있더라도 가장 왼쪽의 매칭되는 부분만을 치환한다는 것이다.
2. 만약 파싱 룰을 적용하여 바뀐 부분이 있다면, 다시 1번으로 돌아간다. 없다면, 다음 단계로 간다. 바뀐 부분이 있을 경우 1번으로 돌아가는 이유는, 토큰이 바뀌면서 이미 적용한 규칙들을 한 번 더 적용 가능할 수도 있기 때문이다.
3. 마찬가지로 두번째의 파싱 룰을 딱 한 번 적용한다.
4. 만약 바뀐 부분이 있다면 1번 과정으로 돌아가 첫 번째 룰부터 다시 적용한다. 없다면, 다음 단계로 넘어간다. 
5. 위의 과정을 모든 파싱 룰에 대해 반복한다.
6. 위와 같은 과정을 따라, 모든 룰을 다 적용한 후 마지막 파싱 룰을 적용했는데, 바뀐 부분이 없다면 파싱을 종료한다.
7. 만약 source 및 parsing rule에 모두 오류가 없다면, 6번까지의 과정을 마치면 토큰 하나만이 남게 된다. 그럴 경우 그 토큰을 반환하며, 만약 오류가 있어 두 개 이상의 토큰이 남게 되면 예외를 일으킨다.

 이는 시간복잡도를 따져 보면 `R^2`이나 `R^3`가 나올 수도 있는 매우 비효율적인 구현이다. 하지만 다른 라이브러리의 도움 없이 정상적으로 작동하는 Parser를 구현하였다는 것에 의의를 둔다. 

#### Parse Tree

 파싱을 수행하여 위의 토큰 배열들에 구조를 부여하였다. 아래는 `class`의 선언부터 `data` field의 선언까지를 파싱한 것을 나타낸 것이다. 실제 소스코드에서는 4줄 분량에 해당하는 내용이지만, 코드가 가지는 구조를 모두 표시하니 크기가 매우 커졌다. 

```
class_full[4,611]
    class_def[4,17]
        class	: CLASS[4,10]
        MyStack	: IDENTIFIER[10,17]
    {	: BRACKET_B_O[18,19]
    fields[28,609]
        fields[28,520]
            fields[28,430]
                fields[28,344]
                    fields[28,229]
                        fields[28,98]
                            fields[28,70]
                                fields[28,45]
                                    field[28,45]
                                        var_def_full[28,45]
                                            var_def[28,44]
                                                private	: QUALIFIER[28,36]
                                                type[36,40]
                                                    int	: TYPE[36,40]
                                                size	: IDENTIFIER[40,44]
                                            ;	: SPLIT[44,45]
                                field[54,70]
                                    var_def_full[54,70]
                                        var_def[54,69]
                                            private	: QUALIFIER[54,62]
                                            type[62,66]
                                                int	: TYPE[62,66]
                                            top	: IDENTIFIER[66,69]
                                        ;	: SPLIT[69,70]
                            field[79,98]
                                var_def_full[79,98]
                                    var_def[79,97]
                                        private	: QUALIFIER[79,87]
                                        type[87,92]
                                            type_arr[87,92]
                                                int	: TYPE[87,91]
                                                [	: BRACKET_A_O[90,91]
                                                ]	: BRACKET_A_C[91,92]
                                        data	: IDENTIFIER[93,97]
                                    ;	: SPLIT[97,98]
                                    ... 하략
```

### Abstract Semantic Tree (AST)

 `Lexer` / `Parser`로부터 분석된 parse Tree는 바로 분석에 사용할 수 없고, AST로 변환이 필요하다. 

 현재 parsing rule은 위와 같이 오직 치환만을 사용하여 구현되어있기 때문에, 상용 컴파일러-컴파일러와 다르게 `|`기호나 `+`기호 등을 사용하여 parsing rule을 축약할 수 없다. 따라서 `{a,b,c,d}`와 같이 특정 요소의 가변 개수만큼의 나열을 위해서는 다음과 같은 방법을 사용해야만 한다.
```
elements : element
elements : elements COMMA element
```
 위는 `element`들의 나열을 나타내기 위하여 `elements`라는 새로운 규칙을 도입한 예이다.  그러나 이런 방식으로 나열을 표시하면 직관적으로 알아보기 힘들 뿐만 아니라, 나열된 원소들이 계층 구조를 가지므로 나열이라 보기도 어렵다. 예를 들어 아래는 `class` 내부의 `field`들이 파싱된 부분이다.

```
	fields[2]
        fields[2]
            fields[2]
                fields[2]
                    fields[2]
                        fields[2]
                            fields[2]
                                fields[2]
                                    field[2]
```

실제 소스코드에는 `field`들이 나열되어있으나, 파싱된 결과는 계층구조로 표현되어있다.

 이런 의미와 표현이 다른 상황은 나열 뿐만 아니라 연산을 나타내는 부분 등 여러 상황에서 발생한다. 그러므로 소스코드를 분석할 때 parse tree를 그대로 사용하는 것은 비효율적이다. 그래서 parse tree를 구축한 이후에, 내부의 특정 `Token`을 제거하는 방법을 통하여 parse tree를 abstract semantic tree로 변환하였다.

이때 특정 `Token`을 제거한다 함은, 어떤 `Token`의 자식들을 모두 그 `Token`의 부모의 자식으로 옮긴 후, 해당 토큰을 제거한다는 의미다. Pseudo code로 표현하면 다음과 같다.
```java
// token은 지우고자 하는 토큰
void removeToken(Token token){
    token.parent.addChildren(token.children);
    token.parent.children.remove(token);
}
```
실제로는 위 과정을 재귀적으로 반복하여, Leaf부터 Root까지 역순으로 적용하도록 구현되어있다.

 이러한 과정을 거치면 구조를 나타내기 위해서만 필요한 토큰(e.g `fields`, `;`, `,`, `codelines`, `{`, `}`이나 식별을 위한 keyword(e.g. `class`, `if`, `else`, `return`, 연산자)등이 제거되어 유의미한 Token만이 남게 된다. 아래는 위의 parse tree를 AST로 변환한 것이다.

``` 
class_full[4,611]
    class_def[4,17]
        MyStack	: IDENTIFIER[10,17]
    field[28,45]
        var_def_full[28,45]
            var_def[28,44]
                private	: QUALIFIER[28,36]
                type[36,40]
                    int	: TYPE[36,40]
                size	: IDENTIFIER[40,44]
    field[54,70]
        var_def_full[54,70]
            var_def[54,69]
                private	: QUALIFIER[54,62]
                type[62,66]
                    int	: TYPE[62,66]
                top	: IDENTIFIER[66,69]
    field[79,98]
        var_def_full[79,98]
            var_def[79,97]
                private	: QUALIFIER[79,87]
                type[87,92]
                    type_arr[87,92]
                        int	: TYPE[87,91]
                data	: IDENTIFIER[93,97]
                ... 하략
```

 Parse Tree에 비해 양이 매우 줄어들었을 뿐만 아니라, 서로 다른 계층에 있던 변수들이 하나의 계층으로 합쳐졌고 계층 구조 자체도 없어졌다. SPLITTER나 BRACLET과 같은 구분만을 위한 토큰 역시 삭제되어 유의미한 토큰만이 남은 것을 확인할 수 있다.

# Visualizer
### TokenWrapper

 `TokenWrapper`는 `Parser`를 통해 파싱된 토큰의 정보를 쉽게 알 수 있도록 하고, context와 관련된 의미를 부여하는 클래스다. 본 프로젝트에서 사용한 `Parser`는 context-free language를 기반으로 한다. 즉, context에 관계된 의미를 파악할 수 없다. 아래 예시를 보면, context-free language에 기반한 파서로 `var`이 변수인 것은 알 수 있지만, 전역변수인지 파라매터인지, 충돌이 발생하는지 아닌지 판단할 방법은 전혀 없다.

```java
String var;
void method(String var){
    String var;
    String newVar = "new" + var;
}
```

 따라서 본 과제의 구현사항 중 하나인, "메서드에서 사용하는 전역 변수를 표시하는 기능" 등을 구현하려면 Parse tree를 한 번 더 분석해야만 하므로, 이를 전담하는 클래스가 있으면 편리하다. 이를 위하여 `TokenWrapper`클래스를 구현하였으며,  `TokenWrapper` 자체는 Abstract class로 실질적으로 기능을 수행하는 세 개의 자식 클래스 `TokenField`, `TokenMethod`, `TokenClass` 를 갖는다.

 `TokenWrapper`는 다음과 같은 구조로 이루어져 있다.

```java
abstract class TokenWrapper {

	private TokenWrapper root;
	private TokenWrapper parent;
	private Token token;
	private String ruleName;

	protected TokenWrapper(Token astToken, TokenWrapper parent) {
		...중략
	}

	public static TokenWrapper getTokenWrapper(Token token, TokenWrapper parent) {
		...중략
	}

	public TokenWrapper getChild(int index) {
		return getChildren().get(index);
	}

	public int getChildCount() {
		return getChildren().size();
	}

	public boolean isLeaf() {
		return getChildren().size() == 0;
	}

	public String getRuleName() {
		return ruleName;
	}

	public Token getToken() {
		return token;
	}

	public TokenWrapper getParent() {
		return parent;
	}

	public TokenWrapper getRoot() {
		return root;
	}

	@Override
	public abstract String toString();

	public abstract String getName();

	public abstract String getType();

	public abstract String getModifier();

	protected abstract ArrayList<TokenWrapper> getChildren();
}
```

* 본래는 지역변수에는 접근제한자가 없기 때문에 `getModifier()`함수는 `TokenWrapper`에 포함될 필요가 없다. 그러나 본 프로젝트에서는 전역 변수나 메서드만을 분석할 것이기에 이를 포함시켰다.

 위 소스코드를 보면 생성자의 접근제한자가 `protected`인 것을 알 수 있다. 이는 factory design pattern을 구현한 것인데, 이렇게 한 이유는 클래스의 생성 방식 때문이다. `TokenWrapper`는 추상 클래스로, wrapping할 `Token`의 종류에 따라 `TokenField`, `TokenMethod`, `TokenClass`세 개의 서로 다른 자식 클래스로 wrapping되어야 한다. 그런데 만약 생성자가 public이라면 `TokenField`를 생성할 때 인자로 메서드를 나타내는 토큰을 넘기는 등 여러 예외 상황이 발생할 수 있고, 그럴 경우 입력값을 제한할 수 있는 방법이 exception을 일으키는 것 외에는 없다. 그렇기에 생성자는 `protected`로 하여 패키지 외부에서의 접근을 제한하고, `getTokenWrapper(Token token, TokenWrapper parent)`함수를 통해서만 생성할 수 있도록 하여 factory design pattern을 구현한 것이다.

 아래는 위에서 생략한 `getTokenWrapper(Token token, TokenWrapper parent)`함수 내부다.

```java
public static TokenWrapper getTokenWrapper(Token token, TokenWrapper parent) {
    String ruleName = token.getRuleName();
    if (ruleName.equals("class_full")) {
        return new TokenClass(token, parent);
    } else if (ruleName.equals("var_def_full")) {
        return new TokenField(token, parent);
    } else if (ruleName.equals("function_full") || ruleName.equals("constructor_full")) {
        return new TokenMethod(token, parent);
    } else {
        System.out.println("Unknown token : " + token.getRuleName() + ":" + token.getText());
        return null;
    }
}
```

토큰의 종류에 따라 서로 다른 `TokenWrapper`를 생성한다.

### Visualizer

 Visualizer는 과제에서 제시된 GUI를 사용한 시각화를 구현한 클래스. JFrame을 상속하였으며, 아래와 같은 `menu bar`, `tree view`, `text area`, `right panel`의 4개의 기본 component로 구성되어있다. `right panel`은 `CardLayout`으로 되어 있으며 두 개의 component를 가진다. 첫 번째는 클래스의 정보를 나타내는 `JTable`이며 두 번째는 메서드 소스코드를 보여주고 편집할 수 있는 `JTextArea`이다.

<img src=".\doc\img\image-20200615072753984.png" alt="image-20200615074953972" style="zoom:50%;" />

#### Menu Bar

 File 한 개의 메뉴로만 되어있으며, 클릭하면 Open 메뉴를 선택할 수 있다. Open을 누르면 프로그램 현재 프로그램 디렉토리를 보여주는 파일 선택 창이 뜬다. 파일을 선택하면 파일을 읽어 파싱을 시작한다. 파일을 선택하지 않거나, 파싱 중 에러가 발생하면 팝업창을 띄운다.

아래는 선택된 파일을 파싱하는 코드다. 소스 코드를 AST로 변환하는 전반적인 과정을 보여준다. 실제 소스코드에는 디버깅을 위하여 lexing / parsing rule, lexed tokens, parse tree, AST를 보여주는 부분이 있으나, 생략하였다.

```java
public Token parse(String sourcFilePath) throws Exception {
    // 소스 파일(이 경우 MyStack.java)를 읽어온다. IO는 소스 입력을 간편하게 하기 위해 만든 클래스다.
    String src = IO.readFile(sourcFilePath);
    
    // Lexer와 parser를 로드한다.
    Lexer lexer = new Lexer(new File("./lex-rule.txt"));
    Parser parser = new Parser(new File("./parse-rule.txt"));

    // Lexer에 Skip-rule을 추기한다. 즉, Lexing된 token 중 SPACE는 무시한다.
    lexer.addSkip("SPACE");

    // Lexing을 수행하여 token들의 배열을 얻는다.
    Token[] tokens = lexer.lex(src);

    // Parsing을 수행하여 root token을 얻는다.
    Token parsedToken = parser.parse(tokens);

    // 나열, 연산 우선순위 등을 위해 정의된, 의미적으로는 필요없는 토큰들을 제거한다.
    Token.skipToken(parsedToken, "fields");
    Token.skipToken(parsedToken, "exp");
    Token.skipToken(parsedToken, "codelines");
    Token.skipToken(parsedToken, "codeline");

    // 키워드, 특수문자 토큰들을 제거한다.
    Token.skipToken(parsedToken, "CLASS");
    Token.skipToken(parsedToken, "IF");
    Token.skipToken(parsedToken, "ELSE");
    Token.skipToken(parsedToken, "SPLIT");
    Token.skipToken(parsedToken, "NEW");
    Token.skipToken(parsedToken, "RETURN");
    Token.skipToken(parsedToken, "BRACKET.+");
    Token.skipToken(parsedToken, "OPERATOR_NOT");
    Token.skipToken(parsedToken, "OPERATOR_ASSIGN");
    Token.skipToken(parsedToken, "OPERATOR_REFER");

    return parsedToken;
}

```

#### Tree

 파싱된 소스 코드의 구조를 보여준다. 파일이 선택된 후, `parse()`메서드로 구한 `Token`은 `TokenTreeModel`로 변환된다. `TokenTreeModel`은 `TreeModel`을 구현한 것으로,`Token`을 `TokenWrapper`로 변환한 후, `JTree`로 표시할 수 있도록 해 준다. 

<img src=".\doc\img\image-20200615074842094.png" alt="image-20200615075028587" style="zoom:50%;" />

 Tree의 어떤 요소가 선택되면 `event(TreePath path)`함수가 호출된다. 여기서는 호출된 요소가 무엇인지에 따라 서로 다른 동작을 수행한다. 아래는 실제 코드다.

```java
private void event(TreePath selPath) {
    // 선택된 path가 올바른지 점검한다.
	if (selPath == null) {
		System.out.println("SELECTED PAHT IS NULL");
		return;
	}

	// 선택한 노드
	TokenWrapper tokenWrapper = (TokenWrapper) selPath.getLastPathComponent();

	// 만약 선택한 노드가 클래스였을 경우
	if (tokenWrapper instanceof TokenClass) {
        // Name / Type / Access modifier 형태의 TableModel을 만든다.
		String[] columns = { "Name", "Type", "Access" };
		Object[][] rows = new Object[tokenWrapper.getChildCount()][];
		for (int i = 0; i < tokenWrapper.getChildCount(); i++) {
			TokenWrapper child = tokenWrapper.getChild(i);
			Object[] row = { child.getName(), child.getType(), child.getModifier() };
			rows[i] = row;
		}
        
        // TableModel을 table에 적용한다.
		DefaultTableModel tableModel = new DefaultTableModel(rows, columns);
		table.setModel(tableModel);
        
        // textArea의 내용을 지운다.
		textArea.setText("");
        
        // cardLayout에서 table이 있는 card를 표시하도록 바꾼다.
		cardLayout.first(panelRight);
	}

	// 만약 선택된 노드가 메서드였을 경우
	else if (tokenWrapper instanceof TokenMethod) {
        // sourceCode JTextArea에 현재 토큰의 텍스트를 표시한다.
        // replace는 JTextArea 상에서 탭 간격이 너무 커서 가독성을 떨어트리기 때문에, 스페이스 네 개로 바꾼 것이다.
		sourceCode.setText(tokenWrapper.getToken().getText().replace("\t", "    "));
        
        // 메서드에서 사용하는 field를 가져온다.
		String usedFieldString = "Use :\n";
		TokenMethod tokenMethod = (TokenMethod) tokenWrapper;
        // 현재 메서드에서 사용하는 reference를 전부 가져온다. 여기에는 지역 변수, 전역 변수, 메서드 등이 전부 포함된다.
        TokenClass root = (TokenClass) tokenMethod.getRoot();
		for (String field : tokenMethod.getReferences()) {
			// 만약 해당 reference가 field이면 usedFieldString에 추가한다.
            if (root.hasField(field))
				usedFieldString += field + "\n";
		}
        // JTree 아래의 JTextArea에 usedFieldString을 표시한다.
		textArea.setText(usedFieldString);
        
        // cardLayout에서 sourceCode JTextArea가 있는 card를 표시하도록 바꾼다.
		cardLayout.last(panelRight);
	}

	// 만약 선택된 노드가 변수(Field)였을 경우
	else if (tokenWrapper instanceof TokenField) {
        // 모든 메서드를 순회하면서 해당 field를 사용하는 변수만을 가져온다.
		ArrayList<TokenMethod> usingMethods = new ArrayList<TokenMethod>();
		for (TokenMethod method : ((TokenClass) tokenWrapper.getRoot()).getMethods()) {
			if (method.getReferences().contains(tokenWrapper.getName()))
				usingMethods.add(method);
		}
        
        // Name / Method 형태의 TableModel을 만든다.
        String[] columns = { "Name", "Method" };
		Object[][] rows = new Object[usingMethods.size()][];
		for (int i = 0; i < usingMethods.size(); i++) {
			Object[] row = { tokenWrapper.getName(), usingMethods.get(i) };
			rows[i] = row;
		}
        
        // TableModel을 table에 적용한다.
		DefaultTableModel tableModel = new DefaultTableModel(rows, columns);
		table.setModel(tableModel);
        
        // textArea의 내용을 지운다.
		textArea.setText("");
        
        // cardLayout에서 table이 있는 card를 표시하도록 바꾼다.
		cardLayout.first(panelRight);
	} else {
        // 만약 이상한 입력이 들어오면 예외 메시지를 콘솔에 띄우고, 아무것도 하지 않는다.
		System.err.println("Unregistered token");
	}
}
```
#### Text Area / Right Panel

 Tree에서 선택된 요소에 따라 각기 다른 내용을 표시한다. 표시하는 방법은 Tree 단락의 소스코드에서 설명하였으므로, 작동 화면만을 첨부한다.

 아래는 클래스를 선택했을 때의 화면으로, 오른쪽에 모든 메서드와 필드의 정보를 담은 `JTable`을 표시한다. 생성자의 경우. Type을 무엇으로 표시할지 애매하였으나, `Constructor`로 표시하기로 하였다.

<img src=".\doc\img\image-20200615082858461.png" alt="image-20200615083315440" style="zoom:50%;" />

 아래는 필드를 선택했을 때의 화면으로, 오른쪽에 해당 필드를 사용하는 모든 메서드를 표시하도록 하였다.

<img src=".\doc\img\image-20200615083452554.png" alt="image-20200615083452554" style="zoom:50%;" />

 아래는 메서드를 선택했을 때의 화면으로, 오른쪽에 해당 메서드의 소스코드가 표시되며 이는 편집가능하다. 또한 아래의 `JTextArea`에 해당 메서드에서 사용하는 모든 필드를 나열한다.

<img src=".\doc\img\image-20200615083618884.png" alt="image-20200615083618884" style="zoom:50%;" />



# Class Details

 위에서는 프로그램의 전체적인 동작 원리를 설명하고자, 구체적인 클래스 내용 등을 상당히 생략하였다. 아래는 자세한 클래스의 내용이다.

### main.Main

메인 함수만을 포함한다. 메인 함수에서는 Visualizer를 호출하기만 한다.

### parser.IO

##### Methods

| Modifier and Type         | Method                                | Description                      |
| :------------------------ | :------------------------------------ | :------------------------------- |
| `static java.lang.String` | `readFile(java.io.File file)`         | 파일을 읽어 String으로 반환한다. |
| `static java.lang.String` | `readFile(java.lang.String filePath)` | 파일을 읽어 String으로 반환한다. |

### parser.Lex

렉싱 규칙을 저장하는 클래스. 정규표현식과 토큰 이름의 튜플 (Patter,Name)이다.

##### Fields

| Modifier and Type                 | Field     | Description       |
| :-------------------------------- | :-------- | :---------------- |
| `private java.lang.String`        | `name`    | 규칙 이름         |
| `private java.util.regex.Pattern` | `pattern` | 매칭할 정규표현식 |

##### Constructor

| Constructor                                                  | Description |
| :----------------------------------------------------------- | :---------- |
| `Lex(java.lang.String name, java.util.regex.Pattern pattern)` |             |

##### Methods

| Modifier and Type         | Method          | Description                               |
| :------------------------ | :-------------- | :---------------------------------------- |
| `java.util.regex.Pattern` | `getPattern()`  | 이 토큰을 나타내는 정규표현식을 반환한다. |
| `java.lang.String`        | `getRuleName()` | 이 토큰의 이름을 반환한다.                |
| `java.lang.String`        | `toString()`    |                                           |

### parser.Lexer

##### Fields

| Modifier and Type                               | Field        | Description                         |
| :---------------------------------------------- | :----------- | :---------------------------------- |
| `private Lex`                                   | `currentLex` | 파싱 중에 사용되는 변수.            |
| `private java.lang.String`                      | `fullText`   | 파싱할 전체 문자열.                 |
| `private java.util.ArrayList<Lex>`              | `lexes`      | 토큰 파싱의 규칙을 담고 있는 리스트 |
| `private boolean`                               | `logging`    | 파싱 중 로그를 띄울지 나타내는 변수 |
| `private java.util.ArrayList<java.lang.String>` | `skips`      | 무시할 토큰들을 담고 있는 리스트    |

##### Constructors

| Constructor                           | Description |
| :------------------------------------ | :---------- |
| `Lexer(java.io.File file)`            |             |
| `Lexer(java.lang.String lexerString)` |             |

##### Methods

- #### init

  ```
  private void init(java.lang.String lexerRule) throws java.lang.Exception
  ```

  Lexer의 초기화에 사용

  - **Parameters:**

    `lexerRule` - 파싱 룰 String

  - **Throws:**

    `java.lang.Exception` - 파싱 룰이 잘못되거나 파일이 손상되었을 경우 발생



- #### addSkip

  ```
  public void addSkip(java.lang.String skip)
  ```

  무시할 토큰을 등록한다.

  - **Parameters:**

    `skip` - 무시할 토큰의 이름



- #### getLexes

  ```
  public Lex[] getLexes()
  ```

  로드된 모든 파싱 룰을 반환한다.

  - **Returns:**

    파싱 룰(Lex)들의 반환



- #### setLoggin

  ```
  public void setLoggin(boolean logging)
  ```

  파싱 중 로그를 띄울지 설정한다.

  - **Parameters:**

    `logging` - true=로그 띄움, false=로그 숨김



- #### log

  ```
  private void log(java.lang.String log)
  ```

  만약 로그가 설정되어있다면 입력받은 문자열을 그대로 출력

  - **Parameters:**

    `log` - 출력할 문자열



- #### lex

  ```
  public Token[] lex(java.lang.String input) throws java.lang.Exception
  ```

  주어진 문자열을 렉싱하여 토큰들의 리스트로 변환한다. 어떤 토큰에도 해당되지 않는 문자가 있으면 예외가 발생한다.

  - **Parameters:**

    `input` - 렉싱 문자열

  - **Returns:**

    토큰들의 리스트

  - **Throws:**

    `java.lang.Exception` - 어떤 토큰에도 해당되지 않는 문자가 있을 시 발생

### parser.Parse

Parser rule을 저장하는 클래스

##### Fields

| Modifier and Type                      | Field   | Description |
| :------------------------------------- | :------ | :---------- |
| `(package private) java.lang.String`   | `name`  | 규칙 이름   |
| `(package private) java.lang.String[]` | `parse` | 치환할 패턴 |

##### Constructor

| Constructor                                              | Description |
| :------------------------------------------------------- | :---------- |
| `Parse(java.lang.String name, java.lang.String[] parse)` |             |

##### Methods

| Modifier and Type  | Method          | Description |
| :----------------- | :-------------- | :---------- |
| `java.lang.String` | `getRuleName()` |             |
| `java.lang.String` | `toString()`    |             |

### parse.Parser

##### Feild

| Modifier and Type                    | Field    | Description                  |
| :----------------------------------- | :------- | :--------------------------- |
| `private java.util.ArrayList<Parse>` | `parses` | 파싱 규칙을 담고 있는 리스트 |

##### Constructors

| Constructor                             | Description                      |
| :-------------------------------------- | :------------------------------- |
| `Parser(java.io.File parserFile)`       | 파일에서 파싱 규칙을 로드        |
| `Parser(java.lang.String parserString)` | 파싱 문자열에서 파싱 규칙을 로드 |

##### Methods

- #### init

  ```
  private void init(java.lang.String parserString) throws java.lang.Exception
  ```

  파서를 초기화

  - **Parameters:**

    `parserString` - 파서 정보를 담고 있는 문자열

  - **Throws:**

    `java.lang.Exception` - 파싱 규칙이 잘못될 경우 발생



- #### getLexes

  ```
  public Parse[] getLexes()
  ```

  파싱 규칙들의 배열을 반환한다.

  - **Returns:**

    파싱 규칙들의 배열



- #### replaceFirst

  ```
  private static boolean replaceFirst(java.util.ArrayList<Token> tokens, Parse parse)
  ```

  주어진 토큰 리스트에서 주어진 파싱 룰을 적용할 수 있는 가장 왼쪽의 경우에 파싱 룰을 적용한다.

  - **Parameters:**

    `tokens` - 파싱 룰을 적용할 토큰 리스트

    `parse` - 적용할 파싱 룰

  - **Returns:**

    파싱 룰을 적용했는지의 여부



- #### parse

  ```
  public Token parse(Token[] tokens) throws java.lang.Exception
  ```

  더이상 불가능할 때까지, 왼쪽에서 오른쪽으로, 우선순위가 높은 룰부터 낮은 룰 순으로 파싱 규칙을 적용한다. 사실 원래대로라면 왼쪽에서 오른쪽이 아니라 최대한 많은 문자열이 매칭되는 순으로 파싱을 진행하여야 한다. 그러나 제대로 된 컴파일러를 구현하는 것이 목적이 아니라, 과제로 제출할 간단한 컴파일러가 목표이므로 생략한다.

  - **Parameters:**

    `tokens` - 파싱할 토큰들의 배열

  - **Returns:**

    루트 토큰

  - **Throws:**

    `java.lang.Exception` - 만약 파싱 룰을 모두 적용했을 때 단 하나의 토큰만 남는 경우가 아니라면 발생

### parser.Token

분석된 토큰을 저장하는 클래스. 원래는 그렇게 하면 안 되지만, 개발의 편의를 위하여 토큰과 Parse tree의 Nonterminal을 함께 구현한다.

##### Fields

| Modifier and Type                    | Field      | Description                                     |
| :----------------------------------- | :--------- | :---------------------------------------------- |
| `private java.util.ArrayList<Token>` | `children` | 파싱을 통해 얻은 토큰일 경우, 자식 토큰들       |
| `private int`                        | `end`      | 토큰의 끝 지점                                  |
| `private java.lang.String`           | `fullText` | 원래 소스코드 전문                              |
| `private Lex`                        | `lex`      | 렉싱을 통해 얻은 토큰일 경우, 해당 lexing rule  |
| `private Parse`                      | `parse`    | 파싱을 통해 얻은 토큰일 경우, 해당 parsing rule |
| `private int`                        | `start`    | 토큰의 시작 지점                                |
| `private java.lang.String`           | `text`     | 현재 토큰의 row text                            |

##### Constructors

| Constructor                                                  | Description                                                  |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| `Token(Lex lex, java.util.regex.MatchResult mr, java.lang.String fullText)` | 이 토큰에 해당하는 규칙과 정규표현식 매칭 결과를 받아 토큰을 생성한다. |
| `Token(Parse parse)`                                         | 이 토큰에 해당하는 규칙을 받아 토큰을 생성한다.              |

##### Methods

- #### getText

  ```
  public java.lang.String getText()
  ```

  현재 토큰의 raw text를 반환한다.

  - **Returns:**

    현재 토큰의 raw text



- #### getLex

  ```
  public Lex getLex()
  ```

  현재 토큰에 해당하는 생성 규칙을 반환한다.

  - **Returns:**

    현재 토큰에 해당하는 생성 규칙



- #### getRuleName

  ```
  public java.lang.String getRuleName()
  ```

  현재 토큰의 이름, 즉 현재 토큰의 생성 규칙의 이름을 반환한다.

  - **Returns:**

    현재 토큰의 이름



- #### addChild

  ```
  public void addChild(Token token)
  ```

  새로운 자식 노드를 추가한 후, 노드 정보를 업데이트한다.

  - **Parameters:**

    `token` - 새로 추가할 자식 노드



- #### getChild

  ```
  public Token getChild(int index)
  ```

  토큰의 index번째 child를 반환

  - **Parameters:**

    `index` - 몇 번째 자식인지

  - **Returns:**

    토큰의 index번째 child



- #### getChildren

  ```
  public Token[] getChildren()
  ```

  토큰의 자식 토큰들을 모두 담은 배열을 반환

  - **Returns:**

    자식 토큰들의 배열



- #### getChildCount

  ```
  public int getChildCount()
  ```

  자식 토큰의 개수를 반환

  - **Returns:**

    자식 토큰의 개수



- #### compareTo

  ```
  public int compareTo(Token o)
  ```

  - **Specified by:**

    `compareTo` in interface `java.lang.Comparable<Token>`



- #### toString

  ```
  public java.lang.String toString()
  ```

  - **Overrides:**

    `toString` in class `java.lang.Object`



- #### isTerminal

  ```
  public boolean isTerminal()
  ```

  이 토큰이 Terminal인지 반환

  - **Returns:**

    이 토큰이 Terminal인지 여부



- #### isNonterminal

  ```
  public boolean isNonterminal()
  ```

  이 토큰이 Nonterminal인지 반환

  - **Returns:**

    이 토큰이 Nonterminal인지 반환



- #### skipToken

  ```
  public static Token skipToken(Token tree, java.lang.String skip)
  ```

  특정 토큰을 무시한다. 예컨대 a(b(b(c)))에서 b를 무시하면 a(c)로 된다.

  - **Parameters:**

    `tree` - skipToken을 적용할 트리 구조의 토큰

    `skip` - 무시할 토큰 이름, 혹은 정규표현식

  - **Returns:**

    skipToken이 적용된 tree

### parser.visualizer.TokenWrapper

 토큰을 사용하기 쉽도록 래핑한 클래스

##### Fields

| Modifier and Type          | Field      | Description |
| :------------------------- | :--------- | :---------- |
| `private TokenWrapper`     | `parent`   |             |
| `private TokenWrapper`     | `root`     |             |
| `private java.lang.String` | `ruleName` |             |
| `private Token`            | `token`    |             |

##### Constructor

| Modifier    | Constructor                                         | Description |
| :---------- | :-------------------------------------------------- | :---------- |
| `protected` | `TokenWrapper(Token astToken, TokenWrapper parent)` |             |

##### Methods

| Modifier and Type                                      | Method                                              | Description |
| :----------------------------------------------------- | :-------------------------------------------------- | :---------- |
| `TokenWrapper`                                         | `getChild(int index)`                               |             |
| `int`                                                  | `getChildCount()`                                   |             |
| `protected abstract java.util.ArrayList<TokenWrapper>` | `getChildren()`                                     |             |
| `abstract java.lang.String`                            | `getModifier()`                                     |             |
| `abstract java.lang.String`                            | `getName()`                                         |             |
| `TokenWrapper`                                         | `getParent()`                                       |             |
| `TokenWrapper`                                         | `getRoot()`                                         |             |
| `java.lang.String`                                     | `getRuleName()`                                     |             |
| `Token`                                                | `getToken()`                                        |             |
| `static TokenWrapper`                                  | `getTokenWrapper(Token token, TokenWrapper parent)` |             |
| `abstract java.lang.String`                            | `getType()`                                         |             |
| `boolean`                                              | `isLeaf()`                                          |             |
| `abstract java.lang.String`                            | `toString()`                                        |             |

### parser.visualizer.TokenClass

##### Feilds

| Modifier and Type                                         | Field      | Description |
| :-------------------------------------------------------- | :--------- | :---------- |
| `private java.util.ArrayList<TokenWrapper>`               | `children` |             |
| `private java.util.HashMap<java.lang.String,TokenField>`  | `fields`   |             |
| `private java.util.HashMap<java.lang.String,TokenMethod>` | `methods`  |             |
| `private java.lang.String`                                | `name`     |             |
| `private java.lang.String`                                | `text`     |             |

##### Constructor

| Constructor                                       | Description |
| :------------------------------------------------ | :---------- |
| `TokenClass(Token astToken, TokenWrapper parent)` |             |

##### Methods

* `TokenWrapper`에 포함된 메서드는 생략함

| `protected java.util.ArrayList<TokenWrapper>` | `getChildren()`                   |      |
| :-------------------------------------------- | --------------------------------- | ---- |
| `TokenField[]`                                | `getFileds()`                     |      |
| `TokenMethod[]`                               | `getMethods()`                    |      |
| `boolean`                                     | `hasField(java.lang.String name)` |      |

### parser.visualizer.TokenField

##### Fields

| Modifier and Type                    | Field      | Description |
| :----------------------------------- | :--------- | :---------- |
| `(package private) java.lang.String` | `modifier` |             |
| `(package private) java.lang.String` | `name`     |             |
| `(package private) java.lang.String` | `type`     |             |

##### Constructor

| Modifier    | Constructor                                       | Description |
| :---------- | :------------------------------------------------ | :---------- |
| `protected` | `TokenField(Token astToken, TokenWrapper parent)` |             |

* 해당 클래스는 `TokenWrapper`에 포함되지 않은 메서드를 가지고있지 않음.

### parser.visualizer.TokenMethod

#### Fields

| Modifier and Type                         | Field        | Description                                                  |
| :---------------------------------------- | :----------- | :----------------------------------------------------------- |
| `private java.lang.String`                | `modifier`   |                                                              |
| `private java.lang.String`                | `name`       |                                                              |
| `private java.lang.String`                | `paramType`  |                                                              |
| `private java.util.Set<java.lang.String>` | `references` | 현재 메서드가 참조하는 변수와 함수들. 지역 변수, 전역 변수 를모두 포함함. |
| `private java.lang.String`                | `type`       |                                                              |

##### Consturctor

| Modifier    | Constructor                                        | Description |
| :---------- | :------------------------------------------------- | :---------- |
| `protected` | `TokenMethod(Token astToken, TokenWrapper parent)` |             |

##### Methods

| Modifier and Type                             | Method            | Description |
| :-------------------------------------------- | :---------------- | :---------- |
| `java.util.List<java.lang.String>`            | `getReferences()` |             |

### parser.visualizer.Visualizer

##### Fields

| Modifier and Type               | Field        | Description                                            |
| :------------------------------ | :----------- | :----------------------------------------------------- |
| `private java.awt.CardLayout`   | `cardLayout` |                                                        |
| `private javax.swing.JPanel`    | `panelRight` | Table, text area를 보여줄 panel                        |
| `private javax.swing.JTextArea` | `sourceCode` |                                                        |
| `private javax.swing.JTable`    | `table`      | 클래스의 멤버 / field를 사용하는 메서드를 보여줄 table |
| `private javax.swing.JTextArea` | `textArea`   | Tree 아래의 text 영역                                  |
| `private javax.swing.JTree`     | `tree`       | 자바 파일 구조를 보여 줄 tree model                    |

##### Constructor

| Constructor    | Description |
| :------------- | :---------- |
| `Visualizer()` |             |

##### Methods

- #### parse

  ```
  private Token parse(java.lang.String sourcFilePath) throws java.lang.Exception
  ```

  주어진 경로에서 파일을 읽어 파싱을 수행한다.

  - **Parameters:**

    `sourcFilePath` - 파일을 읽어올 경로

  - **Returns:**

    파싱된 루트 토큰

  - **Throws:**

    `java.lang.Exception` - 파일이 잘못되거나, 파싱이 실패할 경우 발생



- #### printTokens

  ```
  private static void printTokens(Token[] tokens)
  ```

  토큰들의 배열, 혹은 토큰들의 트리를 나타내는 문자열을 콘솔로 출력한다.

  - **Parameters:**

    `tokens` - 출력할 토큰들



- #### _printTokens

  ```
  private static java.lang.String _printTokens(Token[] tokens, int depth)
  ```

  재귀적으로 토큰들의 배열, 혹은 토큰들의 트리를 나타내는 문자열을 출력한다.

  - **Parameters:**

    `tokens` - 출력할 토큰들

    `depth` - 재귀적 출력의 깊이. 호출할 때에는 depth=0으로 하고 호출하면 된다.

  - **Returns:**

    토큰들을 나타내는 문자열



- #### event

  ```
  private void event(javax.swing.tree.TreePath selPath)
  ```

  Tree에서 특정 노드가 선택되었을 때 호출

  - **Parameters:**

    `selPath` - 선택된 노드를 담고 있는 Path