# 远程 API



## 速率限制

- 鉴权请求（含命令）：5次 / 60s
- 常规请求：1200次 / 60s



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



## Command

客户端主动发出 `CommandC2SPayload`，服务器通过 `CommandPayload` 答复。

```java
public record CommandC2SPayload(
        UUID requestId,
        String token,
        Command command,
        String[] args
) {}


public record CommandS2CPayload(
        UUID requestId,
        Status status,
        String[] results
) {}
```

以下是 `command` 取不同值时的逻辑。

| command     | 作用                     | args       | results      | 备注                                                         |
| ----------- | ------------------------ | ---------- | ------------ | ------------------------------------------------------------ |
| AUTHORIZE   | 获取会话令牌             | `password` | `token|null` | 密码错误：`UNAUTHORIZED`；权限不足：`FORBIDDEN`；单人模式不鉴权 |
| SUBSCRIBE   | 订阅控制台输出           | `null`     | `none`       |                                                              |
| UNSUBSCRIBE | 取消订阅控制台输出       | `null`     | `none`       |                                                              |
| FETCH_1000  | 拉取最近1000行控制台输出 | `null`     | `content`    | 每行最多1024个字符                                           |
| EXECUTE     | 以服务器身份执行命令     | `command`  | `null`       |                                                              |
|             |                          |            |              |                                                              |



## Console Feed

控制台有新输出时，服务器主动向订阅的客户端发出 `ConsoleFeedS2CPayload`。

```java
public record ConsoleFeedS2CPayload (
        UUID feedId,
        UUID parentId,
        String content
) {}
```

> [!note]
>
> 已知的问题：服务器运行跨天时
