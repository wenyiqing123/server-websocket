# 云聊后端（server-websocket）

云聊是一个基于 Spring Boot 的即时通讯后端，提供用户与好友管理、群聊、WebSocket 实时消息、AI 对话、文档分析/RAG、红包与转账、钱包及充值、后台统计等能力。`web` 模块同时承载 Spring Boot 启动入口和打包后的静态前端资源。

## 功能概览

- 用户注册、登录、刷新 Token、资料/密码/邮箱管理，以及管理员审批。
- 好友申请、好友列表和好友关系维护。
- 群聊创建、成员管理、群资料修改、踢人、退群和解散。
- 基于 WebSocket 的私聊、群聊、消息撤回和离线消息。
- AI 多会话聊天：女友、智能客服、文档分析等模式。
- 文档上传、解析、向量化和基于知识库的问答（RAG）。
- 聊天页“高情商回复”建议接口。
- 红包、转账、钱包余额、充值订单、资金流水和支付宝沙箱支付。
- 管理端消息、活跃用户及财务数据统计。
- Redis 缓存、RabbitMQ 异步消息、Sentinel 流量保护和定时任务。

## 技术栈

- Java 17
- Spring Boot 3.3.4、Spring Web、Validation、WebSocket、JDBC
- Spring AI 1.0.0-M8（OpenAI 兼容聊天接口、向量化、JDBC Chat Memory）
- MyBatis-Plus 3.5.9、MySQL Connector/J、PageHelper
- Redis / Spring Data Redis、Redisson 3.23.0
- RabbitMQ、Sentinel 1.8.6
- MapStruct、Hutool、FastJSON、EasyExcel
- Knife4j / OpenAPI
- Apache Tika、PDF 文档解析相关组件
- 支付宝 Java SDK

## 项目结构

```text
server-websocket/
├── common/                         公共结果类、异常、JWT、Redis、邮件及工具
├── pojo/                           Entity、DTO、VO 和 WebSocket 数据对象
├── service/                        业务接口、实现类、Mapper 及 AI/RAG 业务
├── web/                            Spring Boot 启动模块、Controller、配置、WebSocket
│   └── src/main/resources/static/  打包后的前端静态资源和知识库文件
├── sql/                            数据库初始化及拆分脚本
├── upload/                         本地上传文件目录（运行时生成/使用）
└── pom.xml                         Maven 父工程
```

启动类：`web/src/main/java/cn/wyq/serverwebsocket/ServerWebsocketApplication.java`

启动类启用了缓存、事务、定时任务和异步任务：`@EnableCaching`、`@EnableTransactionManagement`、`@EnableScheduling`、`@EnableAsync`。

## 环境要求

| 组件 | 默认地址/端口 | 说明 |
| --- | --- | --- |
| JDK | 17 | 必须使用 Java 17 或兼容版本 |
| Maven | 3.8+ | 用于多模块构建 |
| MySQL | `127.0.0.1:3306` | 默认数据库名 `chat` |
| Redis | `127.0.0.1:6379` | 默认使用 database `1` |
| RabbitMQ | `localhost:5672` | 默认账号为 `guest/guest` |
| Sentinel Dashboard | `localhost:8090` | 可选，用于查看流控状态 |

HTTP 服务默认监听 `8000` 端口。

## 数据库初始化

1. 创建数据库（字符集必须支持中文和表情）：

   ```sql
   CREATE DATABASE chat
     CHARACTER SET utf8mb4
     COLLATE utf8mb4_0900_ai_ci;
   ```

2. 新环境导入核心脚本：

   ```bash
   mysql -uroot -p chat < sql/chat.sql
   ```

   `sql/chat.sql` 已包含用户、好友、群组、消息、AI 会话、文档、订单、钱包、红包和转账等核心表。当前仓库的核心结构也已包含 `ai_agent`、`ai_service_question`、会话归属和文档归属字段；已有数据库升级应使用增量迁移脚本，不要重复导入带 `DROP TABLE` 的核心脚本。

3. 升级已有数据库时，先备份后执行当前版本的幂等迁移脚本：

    ```bash
    mysql -uroot -p chat < sql/20260906_agent_and_system_settings_migration.sql
    ```

    该脚本初始化内置 AI 女友、平台级系统设置，并兼容旧版 `ai_*` 配置键。脚本不会删除或重建业务表；除模型配置会按当前版本统一迁移为 `qwen3.8-max-0902` 外，其余已经存在的配置值会保留。红包/转账默认 24 小时未接收自动原路退回，充值订单默认 5 分钟未支付自动关闭。

    `sql/chat.sql` 是完整初始化脚本，包含 `DROP TABLE` 和测试数据，仅适用于全新开发库；生产环境不要重复导入。

## 配置

主配置文件为 `web/src/main/resources/application.yaml`。真实凭据和部署地址均由环境变量或外部配置中心提供，仓库中不保存可用密钥。常用配置如下：

| 配置类别 | 环境变量 | 说明 |
| --- | --- | --- |
| JWT | `JWT_SECRET`、`APP_ENV` | 未注入 `JWT_SECRET` 时使用 `jwt_secret_for_server_websocket`；生产部署建议通过环境变量覆盖为随机高强度密钥。 |
| MySQL | `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` | 本地 URL 和用户名有无密码默认值；生产应使用专用最小权限账户。 |
| Redis | `REDIS_HOST`、`REDIS_PORT`、`REDIS_DATABASE`、`REDIS_PASSWORD` | 默认连接本机的 database `1`，密码默认为空。 |
| RabbitMQ | `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD` | 本机 `guest` 默认值只能用于未暴露到公网的开发环境。 |
| 邮件 | `MAIL_HOST`、`MAIL_PORT`、`MAIL_USERNAME`、`MAIL_PASSWORD` | 邮件发送功能需显式配置。 |
| 支付宝 | `ALIPAY_APP_ID`、`ALIPAY_PRIVATE_KEY`、`ALIPAY_PUBLIC_KEY`、`ALIPAY_GATEWAY_URL`、`ALIPAY_NOTIFY_URL` | 支付功能需显式配置，回调地址应为 HTTPS 公网地址。 |
| 服务与文件 | `SERVER_PORT`、`APP_DOMAIN`、`IMAGE_LOCATION`、`AI_DOCUMENT_STORAGE_LOCATION` | HTTP 默认端口为 `8000`；AI 私有文档默认不落入公开图片目录。 |

上传文件的单文件和单次请求上限均为 `10MB`。文档服务还额外限制页数、可提取字符数和切片数，具体数值在 `ai.document.limits.*` 配置。

### AI 配置

项目通过 OpenAI 兼容协议连接阿里云百炼或其他兼容服务：

```yaml
spring:
  ai:
    openai:
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      chat:
        options:
          model: qwen3.8-max-0902
      embedding:
        enabled: true
        options:
          model: text-embedding-v3
```

当前所有云端聊天功能统一使用 `qwen3.8-max-0902`，聊天模型由 `spring.ai.openai.chat.options.model` 控制；文档 RAG 的向量模型是 `text-embedding-v3`。自定义智能体的模型字段也只接受 `qwen3.8-max-0902`。

`QwenTagExtractor` 使用本地 Ollama 的 `qwen2.5:3b` 做标签提取，这是独立的本地功能，不属于上述远程聊天模型配置。

工程知识库文件位于：

- `web/src/main/resources/static/dataset/kg.txt`
- `web/src/main/resources/static/dataset/mecha_vector_cache.json`

应用启动时由 `KnowledgeDataLoader` 加载或构建向量缓存。文档上传后会解析、切片并写入文档及向量存储。

建议通过部署平台的密钥管理或环境变量注入凭据。PowerShell 示例：

```powershell
$env:JWT_SECRET = "<strong-random-secret>"
$env:APP_ENV = "production"
$env:DASHSCOPE_API_KEY = "<api-key>"
$env:DB_PASSWORD = "<database-password>"
mvn -pl web -am spring-boot:run
```

## 构建与运行

在 `server-websocket` 目录执行：

```bash
# 编译并打包全部模块
mvn -DskipTests clean package

# 运行打包后的应用
java -jar web/target/web-0.0.1.jar
```

开发阶段也可以直接运行：

```bash
mvn -pl web -am spring-boot:run
```

启动后访问：<http://localhost:8000>。如果前端资源已复制到 `web/src/main/resources/static/`，Spring Boot 会直接提供这些静态文件。

## 鉴权

普通 REST 接口使用 JWT。请求头格式：

```http
Authorization: Bearer <access-token>
```

当前 Token 有效期：

- Access Token：30 分钟
- Refresh Token：7 天

主要无需登录接口包括：

- `/user/login`、`/user/register`、`/user/refresh`
- `/user/path`、`/user/path-email`、`/user/email`
- `/file/upload`
- `/alipay/notify`
- Swagger/Knife4j 资源

所有 `/ai/**` 接口（包括文档上传、列表、删除和下载）都需要有效的 Access Token。文档、文档会话和 AI 会话还会校验当前用户的所有权，不能通过猜测 ID 访问他人的内容。`/file/upload` 是保留的通用上传入口，生产部署应按业务需要收紧或移除其匿名访问。

其余接口默认需要有效的 Bearer Token。具体放行规则以 `JWTInterceptor` 和 `@LoginNotRequired` 标注为准。

## WebSocket

实时消息端点：

```text
ws://localhost:8000/websocket/{username}
```

浏览器连接必须传入有效的 **Access Token**。推荐将原始 Token 作为 `Sec-WebSocket-Protocol` 传入；该位置不能添加 `Bearer ` 前缀：

```javascript
const wsUrl = `ws://localhost:8000/websocket/${encodeURIComponent(username)}`;
const socket = new WebSocket(wsUrl, accessToken);
```

服务端在握手阶段验证签名、过期时间和令牌用途，只接受 Access Token；Token 中的用户名必须与 `{username}` 路径完全一致。无效令牌、Refresh Token 或用户名不匹配的连接会被拒绝，客户端消息体中的发送者身份也会由服务端以认证身份覆盖。为兼容旧客户端，服务端仍支持 `?token=`，但 URL 更容易被访问日志记录，不应作为新客户端的常规方案。

服务端会将消息保存到数据库，并按私聊或群聊路由给在线用户；离线系统消息会暂存到 Redis。

## 主要接口分组

所有接口默认以 `http://localhost:8000` 为服务根地址。

| 前缀 | 用途 |
| --- | --- |
| `/user` | 登录注册、用户资料、分页、邮箱、密码和管理员审批 |
| `/ai` | AI 会话、SSE 聊天、文档分析、文档上传、高情商回复 |
| `/message` | 聊天记录查询、筛选、撤回、分页和导出 |
| `/friend` | 好友申请、同意、删除和好友列表 |
| `/group` | 群聊创建、成员、群信息、踢人、退群和解散 |
| `/file` | 通用文件上传 |
| `/finance` | 财务汇总、趋势、红包流水和财富排行 |
| `/finance/order` | 充值订单和人工补单 |
| `/finance/log` | 资金流水 |
| `/finance/wallet` | 用户钱包管理 |
| `/account` | 模拟充值和余额查询 |
| `/redpacket` | 发红包、抢红包和红包详情 |
| `/transfer` | 发起转账、接收转账和转账详情 |
| `/alipay` | 支付宝预创建、状态查询、取消和异步通知 |
| `/sys-config` | 系统配置查询和批量更新 |
| `/message-stats` | 消息量和活跃用户统计 |

### AI 流式聊天示例

`/ai/chat` 返回 `text/event-stream`，并且只接受 `POST` JSON 请求体，避免聊天正文与会话标识出现在 URL、浏览器历史或代理日志中。`message` 和 `sessionId` 必填，`agentType` 默认为 `girlfriend`；文档模式可传 `docId` 或 `docIds`：

```bash
curl -N -X POST "http://localhost:8000/ai/chat" \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  --data '{"message":"请总结这份简历","sessionId":"document_session_xxx","agentType":"document","docIds":["<document-id>"]}'
```

实际使用时请将 `sessionId` 替换为已创建且属于当前用户的会话 ID。文档模式中传入的文档必须已经关联到该会话；未关联或不属于当前用户的文档会被拒绝。浏览器端应使用 `fetch` 读取 POST SSE 响应流，原生 `EventSource` 不支持自定义 Authorization 头和 POST 请求体。

高情商回复接口：

```text
POST /ai/suggest-reply
Authorization: Bearer <access-token>
Content-Type: application/json
```

请求体使用项目中的 `AiReplyDto`，服务端会结合当前聊天上下文生成建议文本。

## 缓存与定时任务

- 公共 `CacheClient` 提供空值缓存防穿透、随机 TTL 防雪崩、带 Token 的互斥锁防击穿、有限重试和缓存损坏回源。
- 缓存清理使用 Redis `SCAN`，避免生产环境使用 `KEYS` 阻塞 Redis。
- `MoneyExpiryScheduleTask` 等定时任务负责处理资金相关的过期状态；部署多实例时请确认任务是否需要分布式锁或只允许运行一个实例。

## 接口文档

启动应用后可尝试访问：

- Knife4j：<http://localhost:8000/doc.html>
- OpenAPI：<http://localhost:8000/v3/api-docs>

实际可用性取决于当前依赖、资源配置以及是否被安全规则放行。

## 安全与生产部署建议

`application.yaml` 不再保存真实数据库、邮箱、支付或模型凭据。部署前务必：

- 通过环境变量、外部配置中心或独立的生产 profile 提供凭据；生产部署应覆盖默认的 `jwt_secret_for_server_websocket`，改用足够随机且未复用的 `JWT_SECRET`。
- 立即轮换任何曾写入本地配置、日志、截图或版本控制历史的数据库、邮箱、JWT、支付宝和模型密钥。
- 文档上传、列表、删除、下载和文档分析接口已经要求登录并校验资源归属；仍应在网关层设置请求体限制、审计和速率限制。
- WebSocket 握手已经强制校验 Access Token、路径用户与令牌用户的一致性；生产环境仍应根据容量增加连接数、消息大小和频率限制。
- `/file/upload` 仍是兼容性的匿名通用上传接口，应在公开部署前按实际需求收紧或关闭。
- 支付宝异步通知必须校验签名、订单金额、商户号和订单状态，回调地址应使用 HTTPS 公网地址。
- 生产环境关闭详细 SQL、调试日志和测试数据，定期备份数据库及上传目录。

## 开发提示

- 修改数据库表结构时，同时更新 `sql/chat.sql` 和对应的拆分脚本（如适用）。
- 新增需要登录的接口无需额外手写 Token 解析，使用项目现有的 `@CurrentUser`、`BaseContext` 和 `JWTInterceptor` 机制。
- AI 会话必须校验会话归属；文档会话只能访问已关联的文档。
- 提交前建议执行：

  ```bash
  mvn -DskipTests compile
  mvn test
  ```
