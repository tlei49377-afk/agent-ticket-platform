# Agent Ticket Platform Web

智能工单客服 Agent 平台的前端工程，位于后端仓库的 `web/` 目录。

## 技术栈

- Vue 3
- Vite
- Element Plus
- Pinia
- Vue Router
- Axios

## 前置条件

先启动后端：

```text
http://127.0.0.1:18096
```

并通过健康检查：

```powershell
Invoke-RestMethod 'http://127.0.0.1:18096/actuator/health'
```

## 安装依赖

在仓库根目录执行：

```powershell
cd .\web
npm ci
```

## 开发启动

```powershell
npm run dev
```

浏览器访问：

```text
http://127.0.0.1:5174
```

Vite 会把：

```text
/api/*
```

代理到：

```text
http://127.0.0.1:18096/*
```

所以前端请求 `/api/login`，实际访问的是后端 `/login`。

## 生产构建

```powershell
npm run build
```

构建产物：

```text
web/dist/
```

本地预览：

```powershell
npm run preview
```

## 页面与权限

| 路由 | 页面 | 权限 |
|---|---|---|
| `/login` | 登录 | 公共 |
| `/chat` | 对话工作台 | 登录 |
| `/tickets` | 工单管理 | `ticket:list` |
| `/tickets/:id` | 工单详情 | `ticket:list` |
| `/approvals` | 审批中心 | `ticket:approve` |
| `/kb` | 知识库检索 | `kb:read` |

## 目录结构

```text
web/
├── src/
│   ├── api/          Axios 接口封装
│   ├── components/  审批卡片、知识命中、工具面板
│   ├── layout/      主布局和侧边栏
│   ├── router/      路由和权限守卫
│   ├── store/       登录态和权限
│   ├── styles/      全局样式
│   ├── utils/       字典、格式化
│   └── views/       登录、对话、工单、审批、知识库
├── index.html
├── package.json
├── package-lock.json
└── vite.config.js
```

## 说明

当前 GitHub 主线提供知识库混合检索测试页。  
在线知识文件上传、文档全文管理属于后续增强功能，尚未包含在当前后端主线接口中。

## 演示账号

密码均为 `123456`：

```text
admin      管理员
zhangsan   主管
lisi       客服
wangwu     客服
zhaoliu    提交人
```
