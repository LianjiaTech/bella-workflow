<div align="center">

# Bella Workflow

<h3>专业的大模型工作流引擎，让 AI 应用开发更简单</h3>

[![Static Badge](https://img.shields.io/badge/Docs-Bella%20Home-green?style=for-the-badge)](https://doc.bella.top/)
[![License](https://img.shields.io/badge/License-Dify%20License-blue?style=for-the-badge)](./LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/LianjiaTech/bella-workflow?style=for-the-badge)](https://github.com/LianjiaTech/bella-workflow/stargazers)
[![GitHub issues](https://img.shields.io/github/issues/LianjiaTech/bella-workflow?style=for-the-badge)](https://github.com/LianjiaTech/bella-workflow/issues)

</div>

<p align="center">
  <b>中文</b> | 
  <a href="./README_EN.md">English</a> | 
  <a href="https://doc.bella.top/">文档中心</a>
</p>

## 🔥 项目简介

**Bella Workflow** 是一个企业级工作流开发平台，基于 **Dify** 二次开发而成。我们秉承 "工作流即服务"（Workflow as a
Service）的理念，重新设计了核心架构，专注于提供企业级的工作流能力。

Bella Workflow 精简了 Dify 中的非核心功能（如 Agent、文本生成应用等高层封装），同时大幅增强了工作流引擎的核心能力，为企业级
AI 应用开发提供更稳定、更强大、更灵活的解决方案。

## ✨ 核心优势

<table>
  <tr>
    <th width="200">功能模块</th>
    <th>企业级特性</th>
  </tr>
  <tr>
    <td><b>🔎 数据集成</b></td>
    <td>无编码直连 MySQL、Redis、PostgreSQL、Kafka 等企业数据源，轻松构建数据驱动型 AI 应用</td>
  </tr>
  <tr>
    <td><b>🔔 智能触发器</b></td>
    <td>支持多种触发方式（Kafka 消息、定时器、API 调用等），实现自动化工作流编排</td>
  </tr>
  <tr>
    <td><b>🔄 异步回调</b></td>
    <td>支持异步回调模式，为长时间运行的工作流提供高效执行机制</td>
  </tr>
  <tr>
    <td><b>💻 代码集成</b></td>
    <td>内置 Groovy 脚本引擎，支持在工作流中编写和执行自定义业务逻辑</td>
  </tr>
  <tr>
    <td><b>🤖 RAG 封装</b></td>
    <td>提供专业的检索增强生成（RAG）节点，提升大模型输出的准确性和相关性</td>
  </tr>
  <tr>
    <td><b>🌐 HTTP 扩展</b></td>
    <td>强大的 HTTP 节点支持 JSON 用例一键解析、异步回调等高级功能，无缝对接第三方服务</td>
  </tr>
  <tr>
    <td><b>📁 版本控制</b></td>
    <td>工作流版本一键切换，支持快速上线、回滚，保障生产环境稳定性</td>
  </tr>
  <tr>
    <td><b>🔍 思考过程</b></td>
    <td>支持输出推理模型完整思考过程，提高模型输出可解释性和可调试性</td>
  </tr>
  <tr>
    <td><b>📝 灵活配置</b></td>
    <td>支持开始节点定义 JSON 类型字段，实现复杂数据结构的传递和处理</td>
  </tr>
</table>

> 注：Bella Workflow 中的工具、知识库模块目前尚未开源，敬请期待后续版本。

## 📍 快速开始

### 使用方式

<table>
  <tr>
    <td width="200"><b>🌐 云服务版</b></td>
    <td>
      直接访问我们的<a href="https://workflow.bella.top/">官方网站</a>，无需部署和维护，快速开始构建您的 AI 应用。
    </td>
  </tr>
  <tr>
    <td><b>💻 自部署版</b></td>
    <td>
      在您自己的基础设施上部署 Bella Workflow，完全控制数据和环境。<br/>
      详细步骤请参考我们的<a href="https://doc.bella.top/deployment">部署文档</a>。
    </td>
  </tr>
</table>

### 快速部署

```bash
# 克隆代码
git clone https://github.com/LianjiaTech/bella-workflow.git
cd bella-workflow/docker

# docker-compose启动
docker-compose --env-file .example.env -f docker-compose.yaml up
```

更详细部署指南，请参考 [部署指南](./docker/README.md) 。

## 🔐 商业使用须知

Bella Workflow 采用开源协议，可用于商业目的，但请注意以下限制：

<table>
  <tr>
    <td width="200"><b>📷 品牌保护</b></td>
    <td>
      使用前端时，不得移除或修改 Dify 控制台或应用程序中的 LOGO 或版权信息。
    </td>
  </tr>
  <tr>
    <td><b>🌟 多租户服务</b></td>
    <td>
      如需将 Bella-Workflow 用于多租户服务，确保遵循 Dify License。
    </td>
  </tr>
</table>

## 👨‍💻 贡献指南

我们热心欢迎社区贡献！贡献者需要同意项目维护者可根据需要调整开源协议，以及贡献代码可能被用于商业目的。

详细的贡献指南请参考 [贡献指南](./CONTRIBUTING.md) 。

## 📃 许可协议

Bella Workflow 遵循 Dify 项目的修改版 Apache License 2.0 协议。详细条款请参阅 [LICENSE](./LICENSE) 文件。

---

<div align="center">
  <p>© 2025 Bella. 保留所有权利。</p>
  <p>
    <a href="https://doc.bella.top/">官方网站</a> · 
    <a href="https://github.com/LianjiaTech/bella-workflow">项目仓库</a>
  </p>
</div>



