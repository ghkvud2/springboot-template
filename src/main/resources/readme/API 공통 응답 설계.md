# 1. API 공통 응답 설계

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
public class TeamApi {

	@GetMapping("/team")
	public ApiResponse<String> findTeamById(String teamId) {
        //비즈니스로직;
		return ApiResponse.createFail("U001", "존재하지 않는 사용자입니다.");
	}
}

```



### 2.2 응집도(?)를 높이는 방법

- `createFail` 메소드에 전달되는 파라미터의 타입을 강제하고 싶었다. 어플리케이션 전체에서 사용할 공통 응답 객체를 생성하는 방법을 강제해서 응집도(?)를 높일 수 있는 방법을 고민했다. **첫 번째 시도는, Enum 타입의 객체를 파라미터로 받도록 하는 것이다. 그리고 그 Enum의 code와 message 필드를 가지고 ApiError 객체를 생성하도록 했다.** 제네릭으로 createFail 메소드에는 Enum 타입의 객체만 전달되도록 변경했다. 그리고 그에 맞춰서 ApiError 객체의 생성자도 변경했다.

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

- ApiError의 생성자에서 에러가 발생한다. ApiError 생성자에 전달된 파라미터는 Enum 타입이긴 하지만 getCode()와 getMessage() 메소드는 정의되어 있지 않다. **createFail 메소드의 파라미터 타입을 강제하면서, 전달되는 파라미터가 getCode(), getMessage() 메소드도 가지고 있다는 것을 보장할 수 있도록 인터페이스를 사용했다.**

```java
public interface ErrorCode {
	public String getCode();
	public String getMessage();
}

public enum TeamErrorCode implements ErrorCode {
	
	NOT_FOUND("U001", "존재하지 않는 팀입니다.");

	private String code;
	private String message;

	private TeamErrorCode(String code, String message) {
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
		return ApiResponse.createFail(TeamErrorCode.NOT_FOUND);
	}
```

### 2.3 사용자 정의 예외 처리

- 비즈니스 로직을 수행하면서 개발자가 예외 클래스를 정의할 수 있다. findTeamById 메소드에 주어진 teamId 파라미터 값에 해당하는 팀이 존재하지 않을 때, TeamNotFoundException 예외를 반환하도록 했다고 가정하자.

```java
package com.example.template.domain.team.exception;

public class TeamNotFoundException extends RuntimeException {
	private String code;
	private String message;
    //getter, setter
}
```

- controller 클래스에서 `@ExceptionHandler` 어노테이션을 이용해서 아래와 같이 예외를 처리한다.

```java
package com.example.template.domain.team.api;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.template.domain.team.exception.TeamNotFoundException;
import com.example.template.global.code.TeamErrorCode;
import com.example.template.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class TeamApi {
	
	@SuppressWarnings("rawtypes")
	@ExceptionHandler
	public ApiResponse teamNotFoundExceptionHandler(TeamNotFoundException e) {
		return ApiResponse.createFail(???); //Enum & ErrorCode 타입을 전달할 방법이 없다.
	}

	@GetMapping("/team")
	public ApiResponse<String> findTeamById(String teamId) {
		return ApiResponse.createFail(TeamErrorCode.NOT_FOUND);
	}
}
```

- 위에서 공통 응답 객체를 생성하는 방식을 강제하기 위해 ApiResponse 객체의 createFail 메소드는 Enum타입이면서 ErrorCode 인터페이스를 구현한 객체를 전달 받도록 했다. 그러나 ExceptionHandler에 전달된 파라미터는 TeamNotFoundException타입의 객체이므로 createFail 메소드에 그대로 전달할 수 없다. createFail 메소드는 두 개의 String 타입의 파라미터를 필요로 하기 때문에 어플리케이션에서 정의할 사용자 예외 클래스들도 두 개의 String 필드를 가지고 있다면 createFail 메소드를 오버로딩해서 사용할 수 있을 것이다.

```java
public class ApiResponse<T> {
    
    //생략
	
	public static <T> ApiResponse<T> createFail(TeamNotFoundException e) {
		return new ApiResponse<>(false, null, new ApiError(e.getCode(), e.getMessage()));
	}
    //getter
    
	static class ApiError {

		private String code;
		private String message;
        
        //생략
		
        //생성자 추가
		public ApiError(String code, String message) {
			this.code = code;
			this.message = message;
		}
    	//getter
    }
}
```

- createFail에 TeamNotFoundException 타입의 예외 객체를 전달하고 getCode()와 getMessage()를 통해서 ApiError 객체를 생성한다. 사용자 정의 예외는 TeamNotFoundException 이외에도 계속해서 생겨날 것이므로 BaseException이라는 추상 클래스를 선언해서 앞으로 생성할 사용자 정의 예외 클래스들이 상속하도록 한다. 그리고 BaseException의 생성자로 위에서 정의한 ErrorCode 타입의 파라미터를 전달받도록하자. 이렇게되면 사용자 정의 예외 클래스도 ErrorCode라는 파라미터와 함께 생성되도록 강제할 수 있기 때문에 응집도를 높일 수 있다.

```java
package com.example.template.global.exception;

import com.example.template.global.code.ErrorCode;

@SuppressWarnings("serial")
public abstract class BaseException extends RuntimeException {

	private String code;
	private String message;

	public BaseException(ErrorCode errorCode, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(errorCode.getMessage(), cause, enableSuppression, writableStackTrace);
		this.code = errorCode.getCode();
	}

	public BaseException(ErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
	}

	public BaseException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.code = errorCode.getCode();
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
```

- BaseException을 상속 받은 TeamNotFoundException을 다시 작성하면 아래와 같다. 다른 점은 Team이라는 도메인에서 사용될 사용자 정의 예외 클래스들을 모아둔 TeamException이라는 클래스를 생성하고 그 안에 Team 도메인에서 사용될 예외 클래스들을 inner 클래스로 선언한다. BaseException을 상속 받은 하위 클래스들을 생성할 때 ErrorCode를 전달하기로 했으니 Team도메인에서 공통으로 쓰일 TeamErrorCode도 TeamException클래스 내부에 함께 정의한다.  이렇게 하면 Team 도메인 내부에서 쓰이는 ErrorCode와 Exception이 한 곳에 모여있어 응집도를 높일 수 있다.

```java
package com.example.template.domain.team.exception;

import com.example.template.global.code.ErrorCode;
import com.example.template.global.exception.BaseException;

public class TeamException {

	static enum TeamErrorCode implements ErrorCode {
		
		NOT_FOUND("T404", "존재하지 않는 팀입니다.");

		private String code;
		private String message;

		private TeamErrorCode(String code, String message) {
			this.code = code;
			this.message = message;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	public static class TeamNotFoundException extends BaseException {

		private static final long serialVersionUID = 3071276038672931799L;

		public TeamNotFoundException() {
			super(TeamErrorCode.NOT_FOUND);
		}
	}
}
```

```java
package com.example.template.domain.team.api;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.template.domain.team.exception.TeamException;
import com.example.template.global.exception.BaseException;
import com.example.template.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class TeamApi {

	@SuppressWarnings("rawtypes")
	@ExceptionHandler
	public ApiResponse baseExceptionHandler(BaseException e) {
		return ApiResponse.createFail(e);
	}

	@GetMapping("/team")
	public ApiResponse<String> findTeamById(String teamId) {

		if (teamId == null) {
			throw new TeamException.TeamNotFoundException();
		}
		return ApiResponse.createSuccess();
	}
}
```

- `TeamNotFoundException` 을 생성해서 throw할 때의 장점은 TeamNotFoundException 자체를 생성할 때 TeamErrorCode.NOT_FOUND 요소를 전달하므로 code와 message를 하드코딩하지 않아도 된다는 것이다. 추가적으로 어플리케이션에서 새롭게 정의한 모든 예외 클래스들은  BaseException을 상속하기로 결정했으므로 BaseException을 처리하는 예외 처리는 `@RestControllerAdvice` 어노테이션을 사용하여 공통 예외 핸들링 클래스에서 처리할 수 있도록 변경할 수 있다.

```java
package com.example.template.global.exception.handler;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.template.global.exception.BaseException;
import com.example.template.global.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@SuppressWarnings("rawtypes")
	@ExceptionHandler
	public ApiResponse baseExceptionHandler(BaseException e) {
		return ApiResponse.createFail(e);
	}
}
```



### 2.4 이미 정의된 예외 처리

- 이제까지 타입을 강제하여 일관된 방법으로 ApiResponse라는 공통 응답을 생성하도록 코드를 작성했다. 실패했을 때의 공통 응답은 code와 message를 포함하는 error 필드를 채워넣는 일이었다. 여기서 문제는 BaseException을 상속한 예외뿐만 아니라 `NullPointerException, IllegalArgumentException, ConstraintViolationException` 예외처럼 자바나 스프링에서 이미 제공하고 있는 예외를 처리할 경우도 생기는데 이러한 예외 클래스들은 이제껏 작성한 실패했을 때의 공통 응답 포맷인 {에러코드, 에러메시지}를 포함하지 않는다는 것이다. 스프링 validation을 사용하여 파라미터를 검증하는 API를 작성하고, 유효성 검사가 실패했을 경우 어떻게 공통 응답으로 변환할 수 있는지 살펴본다.

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

```JAVA
package com.example.template.domain.team.api;

import org.hibernate.validator.constraints.Length;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.template.domain.team.exception.TeamException;
import com.example.template.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@Validated
@RequiredArgsConstructor
@RestController
public class TeamApi {

	@GetMapping("/team")
	public ApiResponse<String> findTeamById(@Length(min = 3, message = "팀ID는 최소 세 글자입니다.") String teamId) {

		if (teamId == null) {
			throw new TeamException.TeamNotFoundException();
		}
		return ApiResponse.createSuccess();
	}
}
```

- `localhost:8080/team?teamId=1`로 요청하면 `@Length` 제약 조건 때문에 ConstraintViolationException이 발생한다. 그리고 해당 예외는 적절히 처리하지 않고 있어서 서버에서 발생한 stackTrace가 클라이언트에게 그대로 노출된다. GlobalExceptionHandler에서 ConstraintViolationException을 처리할 수 있도록 추가하면 아래와 같을 것이다.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

	//생략

	@SuppressWarnings("rawtypes")
	@ExceptionHandler
	public ApiResponse constraintViolationExceptionHandler(ConstraintViolationException e) {
		String message = e.getConstraintViolations().stream().findFirst().get().getMessage();
		return ApiResponse.createFail(???);
	}   
}
```

- ConstraintViolationException 내부의 API를 사용해서 `팀ID는 최소 세 글자입니다.` 라는 메시지는 얻었지만, 공통 응답에서의 에러 코드는 어떻게 세팅해야할까? 위에서 했던 방식과 비슷하게 {에러 코드, 에러 메시지}를 포함하는 ErrorCode 타입의 새로운 객체를 만들자. 클래스명은 `CommonErrorWrapper` 이라고 하고 내부적으로 또 다른 에러 코드를 나타내는 ErrorType의 필드를 갖도록 한다.

```java
package com.example.api.global.exception.handler;

import javax.validation.ConstraintViolationException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.api.global.code.ErrorCode;
import com.example.api.global.exception.BaseException;
import com.example.api.global.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@SuppressWarnings("rawtypes")
	@ExceptionHandler
	public ApiResponse baseExceptionHandler(BaseException e) {
		return ApiResponse.createFail(e);
	}

	@SuppressWarnings("rawtypes")
	@ExceptionHandler
	public ApiResponse constraintViolationExceptionHandler(ConstraintViolationException e) {
		String message = e.getConstraintViolations().stream().findFirst().get().getMessage();
		return ApiResponse.createFail(new ErrorWrapper(ErrorType.VALIDATION_FAIL, message));
	}

	static private class ErrorWrapper implements ErrorCode {
		private ErrorType errorCode;
		private String message;

		public ErrorWrapper(ErrorType errorCode, String message) {
			this.errorCode = errorCode;
			this.message = message;
		}

		@Override
		public String getCode() {
			return errorCode.getCode();
		}

		@Override
		public String getMessage() {
			return message;
		}
	}

	static private enum ErrorType {

		ETC("9999"), VALIDATION_FAIL("8888");

		private String code;

		private ErrorType(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}
}
```

- 즉, BaseException을 상속하지 않는 예외 클래스들은 ErrorType에 정의한 에러 코드와 예외 클래스 자체가 가지고 있는 메시지를 조합하여 ApiResponse를 일관되게 생성할 수 있다.

```java
	@SuppressWarnings("rawtypes")
	@ExceptionHandler
	public ApiResponse constraintViolationExceptionHandler(ConstraintViolationException e) {
		String message = e.getConstraintViolations().stream().findFirst().get().getMessage();
		return ApiResponse.createFail(new ErrorWrapper(ErrorType.VALIDATION_FAIL, message));
	}
```

```json
{
    "success": false,
    "response": null,
    "error": {
        "code": "8888",
        "message": "팀ID는 최소 세 글자입니다."
    }
}
```

- 추가적으로 해야할 일은 ApiResponse의 createFail 메소드와 ApiError 클래스의 생성자를 수정해야하는 일이다. 기존에는 Enum 타입이면서 ErrorCode 타입인 파라미터을 받을 수 있도록 했지만 ErrorCode 인터페이스만 구현한 ErrorWrapper 타입을 파라미터로 받도록 수정해야한다.

```java
package com.example.template.global.response;

import com.example.template.global.code.ErrorCode;
import com.example.template.global.exception.BaseException;

public class ApiResponse<T> {

	private boolean success;
	private T response;
	private ApiError error;

	private ApiResponse(boolean success, T response, ApiError error) {
		this.success = success;
		this.response = response;
		this.error = error;
	}

	static class ApiError {

		private String code;

		private String message;

		public <E extends Enum<E> & ErrorCode> ApiError(E e) {
			this.code = e.getCode();
			this.message = e.getMessage();
		}
		
		public <E extends ErrorCode> ApiError(E e) {
			this.code = e.getCode();
			this.message = e.getMessage();
		}

		public ApiError(BaseException e) {
			this.code = e.getCode();
			this.message = e.getMessage();
		}
		//getter, setter

	}

	public static <T> ApiResponse<T> createSuccess(T data) {
		return new ApiResponse<>(true, data, null);
	}

	public static <T> ApiResponse<T> createSuccess() {
		return new ApiResponse<>(true, null, null);
	}
	
	public static <T, E extends ErrorCode> ApiResponse<T> createFail(E e) {
		return new ApiResponse<>(false, null, new ApiError(e));
	}

	public static <T, E extends Enum<E> & ErrorCode> ApiResponse<T> createFail(E e) {
		return new ApiResponse<>(false, null, new ApiError(e));
	}

	public static <T> ApiResponse<T> createFail(BaseException e) {
		return new ApiResponse<>(false, null, new ApiError(e));
	}
    //getter, setter
}

```



