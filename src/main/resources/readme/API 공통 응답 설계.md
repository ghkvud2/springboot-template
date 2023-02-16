# API 공통 응답 설계

- API 공통 응답을 설계해보자. 블로그에 올릴 글이라는 강박관념 때문에 정작 이해하기 쉬운 문장을 구사하는데 시간을 많이 쏟게 되는 것 같다. 이 글은 그저 내가 고민하고 개선했던 사항들을 간단하게 기록하는 글이다. 초기에는 아래 블로그를 참고했고 점차 발전시켜나갔다.

  - [도메인 계층 패키지 설계](https://cheese10yun.github.io/spring-guide-directory/)

  - [공통 응답 개발하기](https://velog.io/@qotndus43/%EC%8A%A4%ED%94%84%EB%A7%81-API-%EA%B3%B5%ED%86%B5-%EC%9D%91%EB%8B%B5-%ED%8F%AC%EB%A7%B7-%EA%B0%9C%EB%B0%9C%ED%95%98%EA%B8%B0)

    

## 1. 공통 응답 구조

- 내가 만든 공통 응답 구조는 아래와 같다. 네이버나 카카오의 오픈 API 명세를 참고했다.

```json
```성공```
{
    "success": true,
    "response": {
    	```응답 body```
    },
    "error": null
}

```실패```
{
    "success": false,
    "response": null,
    "error": {
		"code" : "999",
        "message" : "기타 오류"
    }
}
```

- 요청이 성공했을 땐, response 필드에 응답 데이터를 세팅하고 요청이 실패했을 땐, error 필드에 에러 코드와 에러 메시지를 세팅한다.



## 2. 공통 응답 ApiResponse 객체

```java
package com.example.template.global.response;

public class ApiResponse<T> {

	private boolean success;
	private T response;
	private ApiError error;

	private ApiResponse(boolean success, T response, ApiError error) {
		this.success = success;
		this.response = response;
		this.error = error;
	}
    //getter

	static class ApiError {

		private String code;

		private String message;

		private ApiError(String code, String message) {
			this.code = code;
			this.message = message;
		}
    	//getter
	}
}

```

- response 필드에는 다양한 타입의 객체가 세팅될 수 있도록 제네릭으로 선언했다. 그리고 에러 필드의 경우 `code, message` 쌍으로 구성되어 있기 때문에 ApiError 타입의 객체를 별도로 선언했다. 여기서 ApiError 객체는 ApiResponse에서만 참조되도록 설계할 것이기 때문에 inner 클래스로 선언했다.



### 2.1 ApiResponse 스태틱 팩토리 메소드

- ApiResponse 객체를 생성하기 위해 아래처럼 스태틱 팩토리 메소드를 선언했다.

```java
public class ApiResponse<T> {
    
    //생략
    
	public static <T> ApiResponse<T> createSuccess(T response) {
		return new ApiResponse<T>(true, response, null);
	}

	public static <T> ApiResponse<T> createSuccess() {
		return new ApiResponse<T>(true, null, null);
	}

	public static <T> ApiResponse<T> createFail(String code, String message) {
		return new ApiResponse<T>(true, null, new ApiError(code, message));
	}  
    
    static class ApiError {
    	//생략          
    }

}
```

- `createFail` 메소드를 보면 String 타입의 파라미터를 두 개 받아서 ApiError 객체를 생성하고 있다. **여기서 꺼림칙헀던 것은 createFail 메소드를 호출하는 쪽에서 하드 코딩이 불가피할 것이라고 생각했다.** 예를 들면 아래처럼 말이다.

```java
@RequiredArgsConstructor
@RestController
public class UserApi {

	@GetMapping("/user")
	public ApiResponse<String> findTeamById(String teamId) {
        //비즈니스로직;
		return ApiResponse.createFail("U001", "존재하지 않는 사용자입니다.");
	}
}

```



### 2.2 응집도(?)를 높이는 방법

- `createFail` 메소드에 전달되는 파라미터의 타입을 강제하고 싶었다. 어플리케이션 전체에서 사용할 공통 객체를 호출하는 방법을 강제해서 응집도(?)를 높일 수 있는 방법을 고민했다. **첫 번째 시도는, Enum 타입의 객체를 파라미터로 받도록 하는 것이다. 그리고 그 Enum의 code와 message 필드를 가지고 ApiError 객체를 생성하도록 했다.** 제네릭으로 createFail 메소드에는 Enum 타입의 객체만 전달되도록 변경했다. 그리고 그에 맞춰서 ApiError 객체의 생성자도 변경해봤다.

```java
public class ApiResponse<T> {
    
    //생략
    
    //파라미터가 Enum타입이라는 것을 강제했다.
	public static <T, E extends Enum<E>> ApiResponse<T> createFail(E e) {
		return new ApiResponse<>(false, null, new ApiError(e));
	}
    //getter
    
	static class ApiError {

		private String code;
		private String message;

		//파라미터가 Enum타입이라는 것을 강제했다.
		public <E extends Enum<E>> ApiError(E e) {
			this.code = e.getCode();		//컴파일 오류 발생
			this.message = e.getMessage();	//컴파일 오류 발생
		}
    	//getter
    }
}
```

- ApiError의 생성자에서 에러가 발생한다. ApiError 생성자에 전달된 파라미터는 Enum 타입이긴 하지만 getCode()와 getMessage() 메소드는 정의되어 있지 않다. **나는 createFail 메소드의 파라미터 타입을 강제하면서, 전달되는 파라미터가 getCode(), getMessage() 메소드도 가지고 있다는 것을 보장할 수 있는 방법이 무엇일지 고민했다.** 정답은 인터페이스를 사용하면 될 것이라고 생각했다.

```java
public interface ErrorCode {
	public String getCode();
	public String getMessage();
}

public enum UserErrorCode implements ErrorCode {
	
	NOT_FOUND("U001", "존재하지 않는 사용자입니다.");

	private String code;
	private String message;

	private UserErrorCode(String code, String message) {
		this.code = code;
		this.message = message;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
```

- getCode(), getMessage() 메소드를 갖는 `ErrorCode` 인터페이스를 선언하고 내가 정의할 Enum 클래스가 이 인터페이스를 구현하도록 만들었다.  createFail메소드와 ApiError 클래스의 생성자를 아래와 같이 변경하면 더 이상 컴파일 에러는 발생하지 않는다.

```java
public class ApiResponse<T> {
    
    //생략
	public static <T, E extends Enum<E> & ErrorCode> ApiResponse<T> createFail(E e) {
		return new ApiResponse<>(false, null, new ApiError(e));
	}
    //getter
    
	static class ApiError {

		private String code;
		private String message;
		
		public <E extends Enum<E> & ErrorCode> ApiError(E e) {
			this.code = e.getCode();		//정상 컴파일
			this.message = e.getMessage();	//정상 컴파일
		}
    	//getter
    }
}
```

- 그리고 createFail 메소드를 호출할 때 더 이상 String 타입의 파라미터를 직접 넘겨주지 않고, 내가 정의한 Enum 클래스를 넘길 수 있게 되었다.

```java
	@GetMapping
	public ApiResponse<String> findTeamById(String teamId) {
        //비즈니스로직
		return ApiResponse.createFail(UserErrorCode.NOT_FOUND);
	}
```

