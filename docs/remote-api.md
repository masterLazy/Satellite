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

| command     | args        | opt          | results                 | 部分 status                                       |
| ----------- | ----------- | ------------ | ----------------------- | ------------------------------------------------- |
| AUTHORIZE   | `password`  |              | `token/null`            | `UNAUTHORIZED` 密码错误<br />`FORBIDDEN` 权限不足 |
| SUBSCRIBE   | `null`      |              | `null`                  |                                                   |
| UNSUBSCRIBE | `null`      |              | `null`                  |                                                   |
| FETCH_1000  | `null`      |              | `content`               |                                                   |
| EXECUTE     | `command`   |              | `null`                  |                                                   |
| LIST        | `path, opt` | `l` 详细信息 | `(dirCount, paths)/msg` | `NOT_FOUND` 目录不存在                            |
| MOVE        | `src, dest` |              | `null/msg`              | `NOT_FOUND` 源不存在                              |
| COPY        | `src, dest` | `r` 递归复制 | `null/msg`              | `NOT_FOUND` 源不存在                              |
| REMOVE      | `target`    | `r` 递归删除 | `null/msg`              | `NOT_FOUND` 目标不存在                            |
| MKDIR       | `target`    |              | `null`                  | `FORBIDDEN` 已存在同名目录或文件                  |
| TOUCH       | `target`    |              | `null`                  |                                                   |

- AUTHORIZE：单人模式不鉴权，直接发放令牌
- FETCH_1000：每行最多1024个字符
- LIST：先返回所有目录，再返回文件
- MOVE/COPY：**始终覆盖目标目录中的文件**
- LIST/MOVE/COPY/REMOVE/MKDIR/TOUCH：可能会通过 results 返回错误信息



## Console Feed

控制台有新输出时，服务器主动向订阅的客户端发出 `ConsoleFeedS2CPayload`。

```java
public record ConsoleFeedS2CPayload (
        UUID feedId,
        UUID parentId,
        String content
) {}
```
