# 命令一览

## Auth

```
/login <password>
```

```
/password
```

```
/register <password> <confirmPassword>
```

重写了以下命令：

```
/whitelist add <target>
```

## Guard

```
/guard add rule <ruleId> <action> <priority> [<description>]
/guard add condition <ruleId> <type> <value>
/guard remove rule <ruleId>
/guard remove condition <ruleId> <conditionNo>
/guard set action <ruleId> <action>
/guard set priority <ruleId> <priority>
/guard set description <ruleId> <description>
/guard list
/guard details <ruleId>
/guard test <command>
```

