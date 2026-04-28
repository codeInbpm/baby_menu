# 宝贝的专属菜单

> 一个为情侣打造的浪漫互动小程序：一方点菜单、另一方收到微信通知后宠 TA。

## 项目结构

```
baby-menu/
├── backend/          Spring Boot 3.x 后端（Java 17）
├── frontend/         UniApp（Vue 3 + Vite + wotUI）微信小程序
├── database/         MySQL 8 建表脚本
└── README.md         本文档
```

## 一、技术栈

| 端     | 技术                                                         |
| ------ | ------------------------------------------------------------ |
| 后端   | Spring Boot 3.2.5、MyBatis-Plus 3.5、Hutool、JWT、Redis（可选） |
| 数据库 | MySQL 8.0+                                                   |
| 前端   | UniApp（Vue 3 Script Setup + Pinia + wotUI）、Vite           |
| 微信   | 小程序登录、订阅消息、分享邀请码                             |

## 二、模块清单

| 模块            | 说明                                 | 主要文件                                               |
| --------------- | ------------------------------------ | ------------------------------------------------------ |
| 1. 基础框架     | Result/异常/JWT 拦截器/CORS/上传     | `common/`, `interceptor/`, `config/`                   |
| 2. 数据库与实体 | 7 张表 + Entity + Mapper             | `entity/`, `mapper/`, `database/baby_menu.sql`         |
| 3. 邀请码与绑定 | 8 位随机码 / 7 天有效 / 互推订阅消息 | `service/CoupleServiceImpl.java`                       |
| 4. 订阅消息工具 | access_token 缓存 + 模板严格匹配     | `wechat/WechatSubscribeService.java`                   |
| 5. 菜单 CRUD    | 分类 + 菜品 + 像素级菜单首页         | `service/MenuServiceImpl.java`, `pages/menu/index.vue` |
| 6. 服务请求     | 创建/推送/列表/接受/拒绝/完成        | `service/RequestServiceImpl.java`, `pages/request/`    |
| 7. 前端完整工程 | pages.json + Pinia + 全部页面        | `frontend/src/`                                        |
| 8. 部署测试     | 见下方运行说明                       | 本文档                                                 |

## 三、快速开始

### 1. 启动 MySQL 与导入脚本

```bash
mysql -uroot -p < database/baby_menu.sql
```

默认数据库名 `baby_menu`、用户 `root`、密码 `123456`。如需修改，请同步修改
`backend/src/main/resources/application.yml` 的 `spring.datasource` 段。

### 2. （可选）启动 Redis

用于缓存微信 access_token；不启动也可正常跑（每次会重新调微信接口）。

### 3. 配置微信小程序信息

在 `backend/src/main/resources/application.yml` 内填写：

```yaml
wechat:
  appid: 你的小程序 AppID
  secret: 你的小程序 Secret
  template-id: 「宝贝的专属请求通知」订阅消息模板 ID
  miniprogram-state: formal   # 正式版用 formal，开发版用 developer
```

并在 `frontend/src/api/index.ts` 内同步：

```ts
export const SUBSCRIBE_TEMPLATE_ID = '你的订阅消息模板 ID';
```

订阅消息模板「宝贝的专属请求通知」必须包含以下 5 个字段（顺序固定）：

| 字段    | 类型        | 含义                   |
| ------- | ----------- | ---------------------- |
| thing1  | thing.DATA  | 通知标题               |
| thing2  | thing.DATA  | 服务内容（最多 20 字） |
| time3   | time.DATA   | 发起时间               |
| phrase4 | phrase.DATA | 行动指引               |
| thing5  | thing.DATA  | 备注                   |

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
# 或者打包
mvn clean package -DskipTests
java -jar target/baby-menu-backend.jar
```

启动成功后访问 `http://localhost:8080/api`。

### 5. 启动前端（微信小程序）

需要先安装 [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html) 与 Node.js 18+。

```bash
cd frontend
npm install        # 或 pnpm install
npm run dev:mp-weixin
```

然后用微信开发者工具打开 `frontend/dist/dev/mp-weixin` 目录即可调试。

> 调试时需在微信开发者工具的「项目设置 → 不校验合法域名」勾选，
> 或将后端域名加入小程序后台的「服务器域名」白名单。

修改 `frontend/src/utils/request.ts` 中的 `BASE_URL` 为你后端的实际地址：

```ts
export const BASE_URL = 'https://your-domain.com/api';
```

### 6. 上传发布

1. 在微信开发者工具点击「上传」 → 在小程序后台提交审核 → 发布上线。
2. 后端需部署到服务器并配置 HTTPS 域名（小程序 request 必须 HTTPS）。

## 四、核心接口一览

| 方法            | 路径                                 | 说明                             |
| --------------- | ------------------------------------ | -------------------------------- |
| POST            | `/auth/login`                        | 微信 code 换 token               |
| GET             | `/user/me`                           | 当前用户与伴侣                   |
| POST            | `/couple/invite`                     | 生成邀请码                       |
| POST            | `/couple/bind`                       | 通过邀请码绑定                   |
| POST            | `/couple/unbind`                     | 解除绑定                         |
| GET/POST/DELETE | `/menu/category[/{id}]`              | 分类 CRUD                        |
| GET/POST/DELETE | `/menu/item[/{id}]`                  | 菜单项 CRUD                      |
| POST            | `/request`                           | 发起服务请求（自动推送订阅消息） |
| GET             | `/request?status=`                   | 请求列表                         |
| POST            | `/request/{id}/accept|reject|finish` | 接受 / 拒绝 / 完成               |
| POST            | `/upload/image`                      | 上传图片                         |

所有接口除 `/auth/**` 外都需要 `Authorization: Bearer <token>` 头。

## 五、订阅消息发送 JSON（严格符合 PRD）

```json
{
  "template_id": "你的模板ID",
  "touser": "对方openid",
  "data": {
    "thing1": { "value": "你的宝贝向你发起请求啦～ ❤️" },
    "thing2": { "value": "洗洗脚 + 按后背" },
    "time3":  { "value": "2026-04-27 14:02" },
    "phrase4":{ "value": "快来宠TA吧！点击进入小程序处理" },
    "thing5": { "value": "点击卡片即可查看详情" }
  },
  "miniprogram_state": "formal",
  "lang": "zh_CN"
}
```

## 六、像素级 UI 还原说明

`frontend/src/pages/menu/index.vue` 完整复刻截图设计：

- 顶部：情侣头像 + 「宝贝的专属菜单」 + 「全都是你的最爱 💜」
- 第二行：当前分类标签 + 「管理菜谱」/「+ 添加菜谱」绿色描边按钮 + 灰底搜索框
- 左侧：分类列表，私房菜默认选中，下拉项含菜品 / 水果 / 零食 / 饮品 / 按摩 / 鲜花 / 分组管理 emoji
- 右侧：白色圆角卡片，左图右文，绿色 + 圆形按钮，含「5 分钟」绿色描边小标签
- 左下：白色圆形悬浮购物车 + 红色数字 badge

绑定后会自动初始化默认 8 个分类（私房菜 / 菜品 / 水果 / 零食 / 饮品 / 按摩 / 鲜花 / 分组管理），
管理员可在 `管理菜谱` 页增删改。

## 七、注意事项

1. 小程序订阅消息每次只能授权 1 次，前端 `requestSubscribeMessage` 已自动处理。
2. 邀请码 8 位大小写字母 + 数字，唯一并 7 天过期；冲突最多重试 3 次。
3. 头像与图片由用户上传到 `uploads/` 目录，生产环境建议接入对象存储（OSS/COS）。
4. JWT 默认 7 天有效，密钥请改 `application.yml` 中的 `jwt.secret`。
5. 微信侧 access_token 用 Redis 缓存（无 Redis 时也可正常运行）。

## 八、目录树（核心）

```
backend/src/main/java/com/babymenu/
├── BabyMenuApplication.java
├── common/         Result, BizException
├── exception/      GlobalExceptionHandler
├── config/         WebConfig, MybatisPlusConfig, MetaObjectConfig
├── interceptor/    JwtInterceptor
├── util/           JwtUtil, UserContext
├── wechat/         WechatService, WechatSubscribeService
├── entity/         User, Couple, CoupleInvite, MenuCategory, MenuItem, ServiceRequest, PointsTransaction
├── mapper/         全部 Mapper
├── service/        接口
├── service/impl/   实现
├── dto/            LoginDTO, BindDTO, RequestCreateDTO ...
└── controller/     AuthController, UserController, CoupleController, MenuController, RequestController, UploadController

祝你们的小程序甜甜蜜蜜上线 ❤️
