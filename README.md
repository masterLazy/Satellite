# Satellite

<img src="src\main\resources\assets\satellite\icon.png" style="height:128px;" align="right"/>

一个强大的 Minecraft **Fabric** 服务器管理 mod，适用于正版 / 离线服务器。

> [!note]
>
> 本模组的前身是 [LazyLogin](https://github.com/masterLazy/LazyLogin)，使用 LazyLogin 的服务器可以丝滑迁移到本模组。（方法见下文）



## 功能简介

Satellite 由多个模块构成，可以按需开关：

- **认证模块 (Auth)**：玩家进入游戏后必须登录才能正常游戏。登录前处于旁观模式且无法移动、发送信息、执行登录以外的命令。
- **命令保护模块 (Guard)**：添加自定义规则，执行命令时如果命中规则，执行设定的行为：放行 / 禁止 / 二次确认 / 需要 OP 批准。
- **远程控制模块 (Remote)**：通过 Minecraft 网络连接远程管理服务器，无需开放其他端口。支持管理服务器文件、监视 Minecraft 服务器控制台、下载文件等功能。

    > [!tip]
    >
    > 要使用远程控制模块，需要客户端也安装 Satellite。如果不需要此功能，**只需服务器**安装 Satellite 即可。

此外，还支持开启下面的 Mixin，可以提升**离线模式**的服务器的游玩体验：

- **白名单检查用户名**：让服务器通过识别玩家的**用户名**来实现白名单管理，而不是 UUID。默认开启。
- **强制使用离线游戏档案**：强制使用基于**用户名**的离线游戏档案，这可以避免一些游戏数据不同步的问题。默认开启。

    > [!warning]
    >
    > 如果你的服务器曾在在线模式下运行，或者此前没有安装过 Satellite，启用 “强制使用离线游戏档案” 可能会导致兼容性问题。推荐在开启此项之前使用迁移助手（开发中）以保证数据正常加载。



## 初次使用

1. 安装 Fabric 端服务器。
2. 将 Satellite 模组文件放置到 `mods` 目录下，然后启动服务器。
3. 初次运行时，Satellite 会创建默认配置并保存到 `.satellite/config.json`。你可以编辑这个文件以修改配置，重启服务器后将读取新的配置文件。



### 关于数据存储

Satellite 使用中产生的各种数据（如注册数据、命令规则等）都会存储在 `.satellite` 文件夹下。



### 从 LazyLogin 迁移注册数据

将 `registered-players.json` 移动到 `.satellite/register.json` 即可。
