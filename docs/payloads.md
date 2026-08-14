## 负载一览

是 `masterlazy.satellite.remote.playload` 下的负载。

### 速率限制

- 常规请求：1200 / 60s
- 鉴权请求：5 / 60s

### 超时时间

- 全局 token：未活跃 30min
- 控制台订阅流：5min



## Hello

服务器主动发出 `HelloS2CPayload`，客户端接收后验证版本信息，回复 `HelloC2SPayload`。

```java
public record HelloS2CPayload (
    	String version
) {}

public record HelloC2SPayload (
    	boolean isCompatible
) {}
```



## Authorize

客户端主动发出 `AuthorizeC2SPayload`，鉴权通过后服务器通过 `AuthorizeS2CPayload` 发放 token。

```java
public record AuthorizeC2SPayload (
        int requestId,
        String password
) {}

public record AuthorizeS2CPayload (
        int requestId,
        String result, // RequestResult
        String token
) {}
```

`result` 的取值：

- `OK` 鉴权通过
- `NOT_FOUND` 未找到会话
- `TOO_MANY_REQUEST` 请求速率达到限制
- `UNAUTHORIZED` 密码不匹配
- `INTERNAL_SERVER_ERROR` 生成 token 时出现错误



## Console Command

客户端主动发出 `ConsoleCmdC2SPayload`，服务器通过 `ConsoleCmdS2CPayload` 答复。

订阅超时：60s

```java
public record ConsoleCmdC2SPayload (
        String token,
        String command // ConsoleCmdEnum
) {}


public record ConsoleCmdS2CPayload (
        String result, // RequestResult
        CompressedLoad data
) {}
```

`command` 的取值：

- `SUBSCRIBE` 订阅控制台输出流 & 保持订阅会话
- `UNSUBCRIBE` 取消订阅控制台输出流
- `FETCH_1000` 拉取最近 1000 条控制台输出



## Console Feed

控制台有新输出时，服务器主动向订阅的客户端发出 `ConsoleFeedS2CPayload`。

```java
public record ConsoleFeedS2CPayload (
        String feedId,
        String parentId,
        String content
) {}
```

