# JavaFieldGenerator

![Logo](logo.png)

[![Download](https://img.shields.io/badge/download-plugin-blue.svg)](https://raw.githubusercontent.com/jenly1314/JavaFieldGenerator/master/release/JavaFieldGenerator-1.1.0.zip)
[![License](https://img.shields.io/badge/license-Apche%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

<!-- Plugin description -->
JavaFieldGenerator This is a plugin you can generate Java field from String.
<!-- Plugin description end -->

JavaFieldGenerator 是一个可以根据字符串内容快速生成Java字段的插件。

> 在日常开发的过程中，常常会根据接口文档去定义一些JavaBean，而接口文档的请求和响应相关信息，在大部分情况下都是使用表格的形式列出相关的字段信息，每次无脑式的对着文档的字段信息去定义对应的JavaBean就略显无聊。
> 如果这时我们使用了 **JavaFieldGenerator** 插件，就可以快速根据定义的字段信息来生成Java对象中的字段信息。


## Install
- Using IDE built-in plugin system on Windows:
  - <kbd>File</kbd> > <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>Browse repositories...</kbd> > <kbd>Search for "JavaFieldGenerator"</kbd> > <kbd>Install Plugin</kbd>
- Using IDE built-in plugin system on MacOs:
  - <kbd>Preferences</kbd> > <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>Browse repositories...</kbd> > <kbd>Search for "JavaFieldGenerator"</kbd> > <kbd>Install Plugin</kbd>
- Manually:
  - Download the [latest release](https://raw.githubusercontent.com/jenly1314/JavaFieldGenerator/master/release/JavaFieldGenerator-1.1.0.zip) and install it manually using <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Install plugin from disk...</kbd>
  - [Get from JetBrains Marketplace](https://plugins.jetbrains.com/plugin/19258)
  
Restart IDE.

## 主要功能
* 快速生成 **Java** 字段
* 快速生成 **Getter** 和 **Setter** 方法
* 快速生成 **toString** 方法

## 使用

### 字段类型配置说明

当文档上定义的字段类型并非Java的基本类型或对象时，这时就需要用到字段类型转换配置，通过配置来动态将文档上定义的类型转换成我们期望的Java字段类型。

在 **Java Field** 的 **Setting** 中提供了字段类型配置，你可以根据自己的需要来修改配置。

* 配置格式：**{未转换前的字段类型}** = **{转换成Java后的字段类型}**

* 配置示例如下：
```
varchar=String
tinytext=String
text=String
string=String
bool=boolean
integer=Integer

```

### 示例
#### 根据输入的内容生成Java字段
![Image](art/generate-java-field.gif)

#### 根据复制的内容快速生成Java字段
![Image](art/quick-generate-java-field.gif)

## 相关推荐

#### [WordPOI](https://github.com/jenly1314/WordPOI) 一个将Word接口文档转换成JavaBean的工具库。

## 更新说明

#### v1.1.0 ：2022-7-8
*  支持字段类型转换配置

#### v1.0.0 ：2022-5-30
*  JavaFieldGenerator初始版本


## 赞赏
如果您喜欢JavaFieldGenerator，或感觉JavaFieldGenerator帮助到了您，可以点右上角“Star”支持一下，您的支持就是我的动力，谢谢 :smiley:
<p>您也可以扫描下面的二维码，请作者喝杯咖啡 :coffee:

<div>
   <img src="https://jenly1314.github.io/image/page/rewardcode.png">
</div>

## 关于我

| 我的博客                                                                                | GitHub                                                                                  | Gitee                                                                                  | CSDN                                                                                 | 博客园                                                                            |
|:------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------------|:---------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------|
| <a title="我的博客" href="https://jenly1314.github.io" target="_blank">Jenly's Blog</a> | <a title="GitHub开源项目" href="https://github.com/jenly1314" target="_blank">jenly1314</a> | <a title="Gitee开源项目" href="https://gitee.com/jenly1314" target="_blank">jenly1314</a>  | <a title="CSDN博客" href="http://blog.csdn.net/jenly121" target="_blank">jenly121</a>  | <a title="博客园" href="https://www.cnblogs.com/jenly" target="_blank">jenly</a>  |

## 联系我

| 微信公众号        | Gmail邮箱                                                                          | QQ邮箱                                                                              | QQ群                                                                                                                       | QQ群                                                                                                                       |
|:-------------|:---------------------------------------------------------------------------------|:----------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------|
| [Jenly666](http://weixin.qq.com/r/wzpWTuPEQL4-ract92-R) | <a title="给我发邮件" href="mailto:jenly1314@gmail.com" target="_blank">jenly1314</a> | <a title="给我发邮件" href="mailto:jenly1314@vip.qq.com" target="_blank">jenly1314</a> | <a title="点击加入QQ群" href="https://qm.qq.com/cgi-bin/qm/qr?k=6_RukjAhwjAdDHEk2G7nph-o8fBFFzZz" target="_blank">20867961</a> | <a title="点击加入QQ群" href="https://qm.qq.com/cgi-bin/qm/qr?k=Z9pobM8bzAW7tM_8xC31W8IcbIl0A-zT" target="_blank">64020761</a> |

<div>
   <img src="https://jenly1314.github.io/image/page/footer.png">
</div>


