# 云聊后端（server-websocket）

云聊是一个基于 Spring Boot 的模块化单体后端，提供用户与好友管理、群聊、WebSocket 实时消息、AI 对话、文档分析/RAG、红包与转账、钱包及充值、后台运营管理等能力。`web` 模块承载 Spring Boot 启动入口和 Controller/配置/WebSocket，`service` 负责业务编排，`pojo` 负责数据模型，`common` 负责跨模块基础能力。部署时可以独立提供 REST/WebSocket，也可以由 `web` 模块同时托管构建后的前端静态资源。

## 功能概览

- 用户注册、登录、刷新 Token、资料/密码/邮箱管理，以及管理员审批。
- 好友申请、好友列表和好友关系维护。
- 群聊创建、成员管理、群资料修改、踢人、退群和解散。
- 基于 WebSocket 的私聊、群聊、消息撤回和离线消息。
- AI 多会话聊天：女友、智能客服、文档分析等模式。
- 用户自定义智能体：创建、编辑提示词/头像/欢迎语/模型参数；管理员可按用户查询、编辑、启停和软删除。
- 文档上传、解析、向量化和基于知识库的问答（RAG）。
- 智能客服工单：会话状态、客服设置、知识库挂载、文档发布/重建索引、检索测试、问题提示题库、回答反馈和运营审计。
- 聊天页“高情商回复”建议接口，服务端按发送者身份整理上下文后再请求模型。
- 红包、转账、钱包余额、充值订单、资金流水和支付宝沙箱支付。
- 管理端消息、活跃用户、财务、智能客服、智能体和用户登录审计统计。
- Redis 缓存、RabbitMQ 异步消息、Sentinel 流量保护、布隆过滤器和定时任务。

## 技术栈

- Java 17
- Spring Boot 3.3.4、Spring Web、Validation、WebSocket、JDBC
- Spring AI 1.0.0-M8（OpenAI 兼容聊天接口、向量化、JDBC Chat Memory）
- MyBatis-Plus 3.5.9、MySQL Connector/J、PageHelper
- Redis / Spring Data Redis、Redisson 3.23.0
- RabbitMQ、Sentinel 1.8.6
- MapStruct、Hutool、FastJSON、EasyExcel
- Knife4j / OpenAPI
- Spring AI PDF Document Reader、Apache PDFBox、文本读取与 TokenTextSplitter
- 支付宝 Java SDK

## 架构与关键链路

```text
浏览器 / Vue 2
     │ Axios REST、Fetch POST SSE、WebSocket
     ▼
web（Controller、JWT 拦截器、WebSocket、配置）
     │
     ├── service（业务服务、MyBatis Mapper、AI/RAG、事务）
     ├── pojo（Entity、DTO、VO、WebSocket 消息模型）
     └── common（统一响应、异常、JWT、缓存、邮件和消息模型）
          │
          ├── MySQL：业务数据、AI 会话记忆、文档元数据、登录审计
          ├── Redis/Redisson：缓存、Refresh Token、验证码、离线消息、布隆过滤器
          ├── RabbitMQ：登录审计、邮件和登录通知异步事件
          └── SimpleVectorStore：工程知识库与用户/客服文档向量快照
```

启动完成后，`CacheInitRunner` 会预热管理端消息/用户缓存、AI 推荐矩阵、客服问题提示题库和用户布隆过滤器；`KnowledgeDataLoader` 会加载 `kg.txt` 或已有向量快照。

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
| RabbitMQ | `localhost:5672` | 默认虚拟主机为 `/websocket`，账号为 `websocket`，密码为 `websocket`；仅适用于本地开发 |
| Sentinel Dashboard | `localhost:8090` | 可选，用于查看流控状态 |

HTTP 服务默认监听 `8000` 端口。

## 数据库初始化

1. 创建数据库（字符集必须支持中文和表情）：

   ```sql
   CREATE DATABASE chat
     CHARACTER SET utf8mb4
     COLLATE utf8mb4_0900_ai_ci;
   ```

2. 新环境导入完整初始化脚本：

   ```bash
   mysql -uroot -p chat < sql/chat.sql
   ```

   `sql/chat.sql` 当前包含用户、好友、群组、消息、AI 会话、用户智能体、客服知识库、文档、订单、钱包、红包、转账、`login_audit_log` 和 `message_log` 等表，并包含初始化配置/测试数据。

关键数据表按领域划分如下：

| 领域 | 主要表 |
| --- | --- |
| AI 与 RAG | `ai_agent`、`ai_chat_session`、`ai_chat_memory`、`ai_knowledge_base`、`ai_knowledge_document`、`ai_knowledge_chunk_hash`、`ai_session_document` |
| 客服运营 | `ai_service_question`、`ai_service_question_log`、`ai_service_feedback`、`ai_service_knowledge_binding`、`ai_service_operation_log` |
| 即时通讯 | `user`、`friend`、`chat_group`、`group_member`、`message`、`conversation`、`conversation_message` |
| 资金 | `u_account`、`u_account_log`、`u_red_packet`、`u_red_packet_record`、`u_transfer`、`recharge_order` |
| 平台管理 | `sys_config`、`sys_document`、`login_audit_log` |
| 消息可靠投递 | `message_log` |

## 配置

主配置文件为 `web/src/main/resources/application.yaml`。当前文件包含便于本地开发的默认值，生产环境必须通过环境变量、外部配置中心或 Secret 覆盖，不能直接沿用开发凭据。常用配置如下：

| 配置类别 | 环境变量 | 说明 |
| --- | --- | --- |
| JWT | `JWT_SECRET`、`APP_ENV` | 未注入 `JWT_SECRET` 时使用 `jwt_secret_for_server_websocket`；生产部署建议通过环境变量覆盖为随机高强度密钥。 |
| MySQL | `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` | 本地 URL 和用户名有无密码默认值；生产应使用专用最小权限账户。 |
| Redis | `REDIS_HOST`、`REDIS_PORT`、`REDIS_DATABASE`、`REDIS_PASSWORD` | 默认连接本机的 database `1`，密码默认为空。 |
| RabbitMQ | `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD` | 默认连接 `/websocket` 虚拟主机和 `websocket/websocket` 开发账号；生产环境必须覆盖并限制权限。 |
| 邮件 | `MAIL_HOST`、`MAIL_PORT`、`MAIL_USERNAME`、`MAIL_PASSWORD` | 邮件发送功能需显式配置。 |
| 支付宝 | `ALIPAY_APP_ID`、`ALIPAY_PRIVATE_KEY`、`ALIPAY_PUBLIC_KEY`、`ALIPAY_GATEWAY_URL`、`ALIPAY_NOTIFY_URL` | 支付功能需显式配置，回调地址应为 HTTPS 公网地址。 |
| 模型服务 | `DASHSCOPE_API_KEY`、`DASHSCOPE_BASE_URL` | 云端聊天使用 OpenAI 兼容接口；密钥不要写入前端。 |
| 可靠消息 | `LOCAL_MESSAGE_DISPATCH_ENABLED`、`LOCAL_MESSAGE_FIXED_DELAY_MS`、`LOCAL_MESSAGE_BATCH_SIZE` | 本地消息表投递开关、扫描间隔和批量大小；默认开启、每秒扫描、每批 100 条。 |
| 服务与文件 | `SERVER_PORT`、`APP_DOMAIN`、`IMAGE_LOCATION`、`AI_DOCUMENT_STORAGE_LOCATION` | HTTP 默认端口为 `8000`；AI 私有文档默认不落入公开图片目录。 |

Spring Multipart 的传输安全上限默认是 `1GB`，普通文件和 RAG 文档的业务上限默认由 `sys_config` 控制为 `10MB`；文档服务还额外限制 PDF 页数、可提取字符数和向量切片数，具体数值在 `ai.document.limits.*` 与 `rag_*` 配置中。

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

当前所有云端聊天功能统一使用 `qwen3.8-max-0902`，聊天模型由 `spring.ai.openai.chat.options.model` 和数据库 `ai_default_model`/`ai_girlfriend_model` 配置共同控制；后端会校验远程模型白名单，自定义智能体也只接受 `qwen3.8-max-0902`。文档 RAG 的向量模型是 `text-embedding-v3`。

`QwenTagExtractor` 使用本地 Ollama 的 `qwen2.5:3b` 做标签提取，这是独立的本地功能，不属于上述远程聊天模型配置；没有 Ollama 时该功能会返回兜底标签，不会替换云端聊天模型。

工程知识库文件位于：

- `web/src/main/resources/static/dataset/kg.txt`
- `web/src/main/resources/static/dataset/mecha_vector_cache.json`

应用启动时由 `KnowledgeDataLoader` 加载或构建向量缓存。用户文档保存在 `private-ai-documents/`，客服运营知识库默认保存在 `private-ai-knowledge/`，均与公开 `/file/**` 上传目录隔离；文档上传后会进行扩展名、大小、内容 SHA-256、PDF 页数、文本长度、切片数量校验，再写入向量存储。

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

- Access Token：默认 120 分钟，可由 `sys_config.jwt_access_token_minutes` 调整
- Refresh Token：默认 30 天，可由 `sys_config.jwt_refresh_token_days` 调整

Access Token 是无状态 JWT；Refresh Token 会按用户 ID 保存在 Redis 中，同一用户重新登录或修改密码后旧 Refresh Token 会失效。生产环境必须覆盖默认的 `jwt_secret_for_server_websocket`，并避免在日志、截图或前端代码中输出 Token。

主要无需登录接口包括：

- `/user/login`、`/user/register`、`/user/refresh`
- `/user/path`、`/user/path-email`、`/user/email`
- `/file/upload`
- `/alipay/notify`
- Swagger/Knife4j 资源

所有 `/ai/**` 接口（包括文档上传、列表、删除和下载）都需要有效的 Access Token。文档、文档会话和 AI 会话还会校验当前用户的所有权，不能通过猜测 ID 访问他人的内容。`/file/upload` 是保留的通用上传入口，生产部署应按业务需要收紧或移除其匿名访问。

其余接口默认需要有效的 Bearer Token。具体放行规则以 `JWTInterceptor` 和 `@LoginNotRequired` 标注为准。

### RabbitMQ 登录事件

登录模块使用持久化 Direct Exchange `login.events`，当前绑定关系如下：

| 路由键 | 队列 | 消费内容 |
| --- | --- | --- |
| `login.audit` | `login.audit.queue` | 登录/注册/验证码请求审计，按 `event_id` 幂等写入 `login_audit_log` |
| `login.mail` | `login.mail.queue` | 验证码、注册欢迎邮件、异地登录提醒 |
| `login.notification` | `login.notification.queue` | 登录成功和异地登录的 WebSocket 系统通知 |

登录成功后，服务端会把最近 IP 保存在 Redis `login:last-ip:{userId}`（保留 30 天），发现新 IP 时标记异地登录并发送提醒。审计、邮件和通知不会直接依赖请求线程发送，而是先写入 `message_log`：投递器抢占记录后使用持久化消息发送，publisher ACK 才标记 `SENT`，NACK、网络异常或 mandatory return 会记录原因，并按 `1s -> 2s -> 4s` 的指数退避策略继续重试（上限可通过 `RABBIT_PRODUCER_RETRY_MAX_DELAY_MS` 调整）；应用重启后仍会扫描 `PENDING`、`RETRY` 以及超时的 `SENDING` 记录。审计消费者通过 `event_id` 幂等写入，避免重试造成重复审计。审计 IP 是后端观察到的连接地址：本机访问通常是 `127.0.0.1` 或 IPv6 回环地址，反向代理部署时应正确转发 `X-Real-IP`/`X-Forwarded-For`。

业务消费者默认最多处理 3 次，采用指数退避；达到上限后由 `RepublishMessageRecoverer` 转发到持久化错误交换机 `message.error.exchange`，再路由到 `message.error.queue`。错误队列由独立消费者监听，发送包含原消息体、原交换机、路由键和异常堆栈的告警邮件到 `2402317148@qq.com`。消费者重试次数和间隔可通过 `RABBIT_CONSUMER_RETRY_*` 环境变量调整；告警邮件失败也会按 `RABBIT_ERROR_CONSUMER_RETRY_*` 做有限重试。

RabbitMQ 队列 `ai_task_queue`、`login.audit.queue`、`login.mail.queue`、`login.notification.queue` 和 `message.error.queue` 均声明为持久化惰性队列（`x-queue-mode=lazy`），积压消息会优先落盘以降低内存压力。RabbitMQ 不允许修改已存在队列的参数；如果旧队列已经以非 lazy 模式创建，需要在确认消息已处理或已备份后删除旧队列，再重启应用重新声明，或使用 RabbitMQ 管理策略完成迁移。生产环境应先在备用虚拟主机验证队列迁移。

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
| `/ai` | AI 会话、SSE 聊天、内置/自定义智能体、文档分析、文档上传、高情商回复 |
| `/ai-service` | 用户客服设置/反馈；管理员知识库、客服工单、问题题库、检索测试和运营日志 |
| `/login-audit` | 管理端登录审计分页和统计 |
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
| `/message-stats` | 消息量和活跃用户统计 |
| `/sys-config` | 系统公告、站点信息、模型/RAG、登录、文件、维护模式等平台配置 |

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

### 智能体与客服权限边界

- 用户智能体接口为 `/ai/agents`，每个用户最多创建 30 个；智能体会记录提示词版本、使用次数和软删除状态。
- 用户可以修改名称、头像、系统提示词、欢迎语、温度、最大输出 Token 和可见范围，但不能自行修改启用状态；管理员通过 `/ai/admin/agents/**` 管理状态和内容。
- `/ai/session/create`、`/ai/chat`、历史记录和删除接口都会校验会话所属用户、智能体所属用户以及智能体当前是否可用，避免通过猜测会话或智能体编号越权。
- 官方客服的代发消息能力通过服务端工具执行，工具会以当前认证用户为发送者，模型提示词不能绕过收件人和消息权限校验。
- 客服问题提示接口 `/ai/service/question-suggestions` 只查询本地题库，用户问题在请求模型前先持久化；应用启动时题库会预热到内存，模型请求失败不会丢失问题记录。

### RAG 文档链路

1. 用户或管理员上传 `pdf`、`txt`、`md`，后端校验扩展名、大小、内容哈希和归属。
2. PDF 先进行页数和加密检查，文本经过 Spring AI Reader 提取后使用 `TokenTextSplitter` 分块，并根据 `rag_chunk_size`、`rag_chunk_overlap`、`rag_max_slices` 限制规模。
3. 向量写入 `SimpleVectorStore` 并持久化 JSON 快照；客服知识库同时记录版本、片段哈希和发布状态，用于重复片段过滤和重建索引。
4. 检索阶段按当前会话关联的文档/已挂载客服知识库过滤，不把其他用户文档混入上下文；无证据时由提示词约束模型明确说明未找到内容。

## 缓存与定时任务

- 公共 `CacheClient` 提供空值缓存防穿透、随机 TTL 防雪崩、带 Token 的互斥锁防击穿、有限重试和缓存损坏回源。
- 缓存清理使用 Redis `SCAN`，避免生产环境使用 `KEYS` 阻塞 Redis。
- Redisson 布隆过滤器在启动时预热用户 ID，用于拦截明显不存在的用户查询；验证码、Refresh Token、最近登录 IP、离线通知和推荐缓存使用 Redis。
- 充值订单、红包和转账的过期处理使用 RabbitMQ 分段延迟消息，不再依赖全表定时扫描。业务事务提交时先写入 `message_log`，投递器发送第一个时间块；到期消费者查询数据库，未完成则幂等地安排下一个时间块，最后一块才关闭订单或原路退款。
- 延迟消息使用持久化惰性队列和消息 TTL + 死信路由：充值默认按 `recharge_close_minutes` 切成 1:2:3 三段，红包和转账分别按 `red_packet_expire_hours`、`transfer_expire_hours` 切分。充值也可用 `RABBIT_RECHARGE_EXPIRY_DELAY_SECONDS=10,20,30` 覆盖时间块（单位：秒）。
- 生产者确认、消息返回、本地消息表唯一 ID 和消费者重试共同保证消息可补偿；资金到期消息的 `CorrelationData.id`、`MessageProperties.messageId` 和业务消息头始终使用同一个 ID。部署多实例时无需额外的过期扫描锁，但必须保证所有实例连接同一 RabbitMQ 和数据库。

## 平台运行配置

`/sys-config` 管理端接口将可变策略保存在 `sys_config`，常用配置包括：

- AI：模型白名单、请求超时/重试、并发、最大 Token、服务客服温度和问题相似度阈值。
- RAG：允许类型、分块大小/重叠、Top-K、上下文字符数、最大切片数、上传大小和启动/上传刷新策略。
- 会话与安全：消息长度、单用户会话数、历史保留、登录失败次数、密码最小长度、JWT 有效期和 WebSocket 心跳。
- 运营与资金：站点名称、默认首页、系统公告、维护模式、红包/转账过期时长、充值订单关闭时长、文件重复策略。

配置接口会对模型、数值范围、布尔值、RAG 分块关系和文件类型做服务端校验；前端表单校验不能替代这些检查。

## 接口文档

启动应用后可尝试访问：

- Knife4j：<http://localhost:8000/doc.html>
- OpenAPI：<http://localhost:8000/v3/api-docs>

实际可用性取决于当前依赖、资源配置以及是否被安全规则放行。

## 安全与生产部署建议

当前代码库的 `application.yaml` 仍包含本地开发默认值，不能视为生产安全配置。部署前务必：

- 通过环境变量、外部配置中心或独立的生产 profile 提供凭据；生产部署应覆盖默认的 `jwt_secret_for_server_websocket`，改用足够随机且未复用的 `JWT_SECRET`。
- 立即轮换任何曾写入本地配置、日志、截图或版本控制历史的数据库、邮箱、JWT、支付宝和模型密钥。
- 文档上传、列表、删除、下载和文档分析接口已经要求登录并校验资源归属；仍应在网关层设置请求体限制、审计和速率限制。
- WebSocket 握手已经强制校验 Access Token、路径用户与令牌用户的一致性；生产环境仍应根据容量增加连接数、消息大小和频率限制。
- `/file/upload` 仍是兼容性的匿名通用上传接口，应在公开部署前按实际需求收紧或关闭。
- 支付宝异步通知必须校验签名、订单金额、商户号和订单状态，回调地址应使用 HTTPS 公网地址。
- 生产环境关闭详细 SQL、调试日志和测试数据，定期备份数据库及上传目录。

## 验证命令

在后端目录执行：

```bash
# 仅编译
mvn -pl web -am -DskipTests compile

# 运行后端单元测试
mvn -pl web -am test

# 打包可运行 JAR
mvn -pl web -am -DskipTests package
java -jar web/target/web-0.0.1.jar
```

启动验收至少应确认：MySQL、Redis、RabbitMQ 可连接；启动日志出现缓存/题库/布隆过滤器预热；`POST /user/login` 的成功或失败事件最终能在 `login_audit_log` 查询到。前端项目的构建和测试命令见前端 README。

## 开发提示

- 修改数据库表结构时，同时更新 `sql/chat.sql` 和对应的拆分脚本（如适用）。
- 新增需要登录的接口无需额外手写 Token 解析，使用项目现有的 `@CurrentUser`、`BaseContext` 和 `JWTInterceptor` 机制。
- AI 会话必须校验会话归属；文档会话只能访问已关联的文档。
- 提交前建议执行：

  ```bash
  mvn -DskipTests compile
  mvn test
  ```
