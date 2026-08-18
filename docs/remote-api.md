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

| command     | args             | opt                            | results           | 部分 status                                       |
| ----------- | ---------------- | ------------------------------ | ----------------- | ------------------------------------------------- |
| AUTHORIZE   | `password`       |                                | `token`           | `UNAUTHORIZED` 密码错误<br />`FORBIDDEN` 权限不足 |
| SUBSCRIBE   | `null`           |                                | `none`            |                                                   |
| UNSUBSCRIBE | `null`           |                                | `none`            |                                                   |
| FETCH_1000  | `null`           |                                | `content`         |                                                   |
| EXECUTE     | `command`        |                                | `null`            |                                                   |
| LIST        | `path, opt`      | `l` 详细信息                   | `dirCount, paths` | `NOT_FOUND` 目录不存在                            |
| MOVE        | `src, dest, opt` | `f` 强制覆盖                   | `none`            | `CONFLICT` 覆盖已有文件                           |
| COPY        | `src, dest, opt` | `f` 强制覆盖<br />`r` 递归复制 | `none`            | `CONFLICT` 覆盖已有文件<br />`FORBIDDEN` 复制目录 |
| REMOVE      | `path`           | `f` 递归删除                   | `none`            | `FORBIDDEN` 删除目录                              |

- AUTHORIZE：单人模式不鉴权，直接发放令牌
- FETCH_1000：每行最多1024个字符
- LIST：先返回所有目录，再返回文件



## Console Feed

控制台有新输出时，服务器主动向订阅的客户端发出 `ConsoleFeedS2CPayload`。

```java
public record ConsoleFeedS2CPayload (
        UUID feedId,
        UUID parentId,
        String content
) {}
```
