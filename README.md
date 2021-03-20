<div align="center"> 
<h1 align="center">贴吧签到助手</h1>
<img src="https://img.shields.io/github/issues/srcrs/TiebaSignIn?color=green">
<img src="https://img.shields.io/github/stars/srcrs/TiebaSignIn?color=yellow">
<img src="https://img.shields.io/github/forks/srcrs/TiebaSignIn?color=orange">
<img src="https://img.shields.io/github/license/srcrs/TiebaSignIn?color=ff69b4">
<img src="https://img.shields.io/github/languages/code-size/srcrs/TiebaSignIn?color=blueviolet">
</div>

# 简介

用的是手机端的接口，签到经验更多，用户只需要填写`BDUSS`即可，每日自动帮你签到，最多支持`200`个贴吧签到。

# 功能

+ 贴吧签到(最多支持 200 个)
+ 支持推送运行结果至微信(通过 server 酱)、企业微信（企业微信API）、Telegram

# 使用方法

## 1.fork本项目

## 2.获取BDUSS

在网页中登录上贴吧，然后按下`F12`打开调试模式，在`cookie`中找到`BDUSS`，并复制其`Value`值。

![](./assets/获取BDUSS.gif)

## 3.将BDUSS添加到仓库的Secrets中

| Name  | Value       |
| ----- | ----------- |
| BDUSS | xxxxxxxxxxx |

将上一步骤获取到的`BDUSS`粘贴到`Secrets`中

![](./assets/添加BDUSS.gif)

## 4.开启actions

默认`actions`是处于禁止的状态，需要手动开启。

![](./assets/开启actions.gif)

## 5.第一次运行actions

+ 自己提交一次`push`。

将`run.txt`中的`flag`由`0`改为`1`

```patch
- flag: 0
+ flag: 1
```

![](./assets/运行结果.gif)

## 成功了

每天早上`6:30`将会自动进行签到

## 添加server酱推送

需在Secrets中添加[server酱](http://sc.ftqq.com/)的`SCKEY`，格式如下

Name | Value
-|-
PUSHINFO | ft=`SCKEY` 

## 添加Telegram推送

需在Secrets中添加Telegram的`chat_id`和`bot_token`，格式如下

| Name     | Value                    |
| -------- | ------------------------ |
| PUSHINFO | tg=`chat_id`,`bot_token` |

## 添加企业微信推送

参考[Server酱Turbo的文档](https://sct.ftqq.com/forward)注册企业微信并创建内部应用。

需在Secrets中添加企业微信的`企业ID`、应用`Secret`、应用`AgentId`、接收消息的成员（默认为`@all`向该企业应用的全部成员发送）

| Name     | Value                                 |
| -------- | ------------------------------------- |
| PUSHINFO | qywx=`企业ID`,`Secret`,@all,`AgentId` |

## 2021-03-21

- 增加Telegram推送
- 增加企业微信推送，可以推送至微信

## 2020-11-01

+ 代码重构

+ 修改签到策略

大大提高一次运行，贴吧签到的成功率，基本很少的贴吧会签到失败。

+ 去除多用户的支持

+ 增加支持server酱推送，可以推送至微信

## 2020-10-19

~~增加支持多账户签到，每个账号的`BDUSS`使用`&&`分割，具体格式如下。~~
