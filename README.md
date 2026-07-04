# IMPrint - 智能照片整理应用

> 大三小学期 6 天实习项目 | Spring Boot + Vue 3 + 百度 AI API

## 项目结构

```
智能照片整理应用开发/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue 3 前端
├── sql/init.sql      # 数据库初始化脚本
└── README.md
```

## 环境要求

| 工具 | 版本 | 本机安装路径 |
|------|------|-------------|
| JDK | 17+ | `C:\Program Files\Amazon Corretto\jdk17.0.19_10` |
| Maven | 3.9+ | `D:\dev\apache-maven-3.9.9` |
| Node.js | 20+ | `C:\Program Files\nodejs` |
| MySQL | 8.0+ | `D:\MySQL\MySQL Sever8.0` |

> 环境变量已写入用户级 PATH。新开终端若命令不可用，先执行项目根目录的 `setup-env.ps1`。

## 环境配置（已完成）

本项目已通过 winget 安装 JDK 17、Node.js 20，并下载 Maven 到 `D:\dev\`。

**每次新开终端时（可选）：**

```powershell
cd "D:\Users\lzl\Desktop\智能照片整理应用开发"
.\setup-env.ps1
```

**启动 MySQL（需管理员权限，首次执行一次）：**

```powershell
# 右键 PowerShell → 以管理员身份运行
.\start-mysql.ps1
```

或手动：Win+R → `services.msc` → 找到 **MySQL80** → 启动。

> PowerShell 若提示「禁止运行脚本」，请改用 `.cmd` 启动文件（双击或在 CMD 中运行）：
> - `start-backend.cmd` 启动后端
> - `start-frontend.cmd` 启动前端

## 快速启动

### 1. 初始化数据库

```bash
mysql -u root -p < sql/init.sql
```

### 2. 配置后端

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    username: root      # 改成你的 MySQL 用户名
    password: root      # 改成你的 MySQL 密码

imprint:
  baidu:
    api-key: YOUR_BAIDU_API_KEY       # 百度 AI API Key
    secret-key: YOUR_BAIDU_SECRET_KEY # 百度 AI Secret Key
    enabled: false                     # 配置 Key 后改为 true
```

> **未配置百度 API 时**：系统会根据文件名关键词做简单分类，不影响演示。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端地址：http://localhost:8080

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：http://localhost:5173

## 百度 AI 接入指南

1. 打开 [百度 AI 开放平台](https://ai.baidu.com/)
2. 注册并创建应用，选择「图像识别 - 通用物体和场景识别」
3. 获取 API Key 和 Secret Key
4. 填入 `application.yml`，将 `enabled` 设为 `true`
5. 重启后端

## 功能清单

| 功能 | 状态 | 说明 |
|------|------|------|
| 用户注册/登录 | ✅ | JWT 鉴权 |
| 批量上传 | ✅ | 支持多文件，自动生成缩略图 |
| AI 自动分类 | ✅ | 百度 API + 关键词兜底 |
| Gallery 分类浏览 | ✅ | 7 类 Tab 筛选 |
| 搜索 | ✅ | 按文件名/分类搜索 |
| 手动改分类 | ✅ | 纠正 AI 错分 |
| 照片编辑 | ✅ | 旋转/翻转/灰度/下载 |
| 个人中心 | ✅ | 照片统计 |

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 |
| POST | `/api/photos/upload` | 批量上传（含 AI 分类） |
| GET | `/api/photos?category=` | 照片列表 |
| GET | `/api/photos/search?keyword=` | 搜索 |
| PUT | `/api/photos/{id}/category` | 修改分类 |
| GET | `/api/user/profile` | 个人中心 |

## 6 天开发计划

| 天 | 任务 |
|----|------|
| Day 1 | ✅ 项目骨架 + 用户系统（当前已完成） |
| Day 2 | 上传功能联调 + 测试图准备 |
| Day 3 | 百度 AI API 接入 + 分类映射调优 |
| Day 4 | Gallery 完善 + 手动改分类 |
| Day 5 | 搜索 + Edit 编辑页 |
| Day 6 | 联调 + 答辩 PPT + 彩排 |

## 演示流程

1. 注册账号 → 登录
2. Home 页批量上传 6 张照片
3. 查看 AI 分类结果表格
4. Gallery 切换 FOOD / FESTIVAL / SCENE Tab
5. 点击照片 → 手动修改分类
6. 搜索演示
7. Edit 页旋转 + 灰度 + 下载

## 常见问题

**Q: Maven 编译失败？**
确保 JDK 17 已安装，`JAVA_HOME` 配置正确。

**Q: 上传后图片不显示？**
检查 `uploads/` 目录是否有写入权限，后端是否正常启动。

**Q: AI 分类全是 OTHER？**
检查百度 API Key 是否正确，`enabled` 是否为 `true`；或使用含关键词的文件名测试（如 `food_xxx.jpg`）。
