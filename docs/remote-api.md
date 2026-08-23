# 远程 API

## 速率限制

- 鉴权请求（含命令）：5次 / 60s
- 除鉴权外所有请求：1200次 / 60s

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

| command     | args               | opt          | results (when OK)     | 部分 status                                                |
| ----------- | ------------------ | ------------ | --------------------- | ---------------------------------------------------------- |
| AUTHORIZE   | `password`         |              | `token`               | `UNAUTHORIZED` 密码错误<br />`FORBIDDEN` 权限不足          |
| SUBSCRIBE   | `null`             |              |                       |                                                            |
| UNSUBSCRIBE | `null`             |              |                       |                                                            |
| FETCH_1000  | `null`             |              | `content`             |                                                            |
| EXECUTE     | `command`          |              |                       |                                                            |
| LIST        | `path, opt`        | `l` 详细信息 | `dirCount, paths`     | `NOT_FOUND` 目录不存在                                     |
| MOVE        | `src, dest`        |              |                       | `NOT_FOUND` 源不存在                                       |
| COPY        | `src, dest`        | `r` 递归复制 |                       | `NOT_FOUND` 源不存在                                       |
| REMOVE      | `target`           | `r` 递归删除 |                       | `NOT_FOUND` 目标不存在<br />`FORBIDDEN` 目标是目录         |
| MKDIR       | `target`           |              |                       | `FORBIDDEN` 已存在同名目录或文件                           |
| TOUCH       | `target`           |              |                       | `NOT_FOUND` 路径无效                                       |
| GET         | `target`           |              | `sessionId, fileSize` | `NOT_FOUND` 目标不存在<br />`FORBIDDEN` 目标是目录         |
| PUT         | `target, fileSize` | `f` 覆盖远程 | `sessionId`           | `NOT_FOUND` 路径无效<br />`FORBIDDEN` 已存在同名目录或文件 |

- AUTHORIZE：单人模式不鉴权，直接发放令牌
- FETCH_1000：每行最多1024个字符
- LIST：先返回所有目录，再返回文件
- MOVE/COPY：**始终覆盖目标目录中的文件**

命令失败时可能会通过 results 返回错误信息。

## Console Feed

控制台有新输出时，服务器主动向订阅的客户端发出 `ConsoleFeedS2CPayload`。

```java
public record ConsoleFeedS2CPayload (
        UUID feedId,
        UUID parentId,
        String content
) {}
```

## File

传输文件专用。客户端先通过 GET 命令开启文件会话，获取文件会话 ID 后才能使用此 API。

```java
public record FileC2SPayload (
        String token,
        UUID sessionId,
        FilePayloadType payloadType,
        int arg,
        byte[] data
) {}


public record FileS2CPayload (
        UUID sessionId,
        FilePayloadType payloadType,
        int arg,
        byte[] data
) {}
```

| payloadType | arg                                      | data[]     |
| ----------- | ---------------------------------------- | ---------- |
| TRANSFER    | 当前 part 编号，如果读到文件尾则取相反数 | 传输的数据 |
| FETCH       | 从哪个 part 开始传输                     |            |
| INTERRUPT   | 0                                        |            |
| WAIT        | 等待的秒数                               |            |

> [!note]
>
> Part 编号是**从 1 开始**的。
