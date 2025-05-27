<div align="center">

# Bella-Workflow

<h3>专业的大模型工作流引擎，让 AI 应用开发更简单</h3>

[![Static Badge](https://img.shields.io/badge/Docs-Bella%20Home-green?style=flat)](https://doc.bella.top/)
[![License](https://img.shields.io/badge/License-Bella--Workflow%20License-blue?style=flat)](./LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/LianjiaTech/bella-workflow?style=flat)](https://github.com/LianjiaTech/bella-workflow/stargazers)
[![GitHub issues](https://img.shields.io/github/issues/LianjiaTech/bella-workflow?style=flat)](https://github.com/LianjiaTech/bella-workflow/issues)

</div>

<p align="center">
  <b>中文</b> | 
  <a href="./README_EN.md">English</a> | 
  <a href="https://doc.bella.top/">文档中心</a>
</p>

## 🔥 项目简介

**Bella-Workflow** 是 **贝壳找房** 内部最为核心的LLM应用开发平台，致力于为开发者提供**更灵活、高效、强大**的AI应用构建能力。

基于"后台即服务"（Backend as a Service）理念，我们自主研发了一套性能强大的工作流执行引擎。同时，我们持续扩展能力边界，力求将传统后端服务从开发、测试、部署、运维的全生命周期管理能力进行深度整合，打造真正意义上的AI应用一站式服务平台。

为了加速整个项目的落地，我们复用了 Dify 的优秀前端模块，这不仅缩短了项目开发周期，也极大地提升了用户体验，在此，我们对 Dify 项目组表示诚挚的感谢。

## ✨ 核心优势

<table>
  <tr>
    <th width="200">能力</th>
    <th>介绍</th>
  </tr>
  <tr>
    <td><b>☕ Java友好</b></td>
    <td>后端完全基于Java技术栈构建，方便快速基于Java活跃的生态融合，充分利用已有技术积累</td>
  </tr>
  <tr>
    <td><b>💪 企业级可靠性</b></td>
    <td>在贝壳找房内部经过大规模生产环境验证，支持高并发、高可用的企业级应用场景</td>
  </tr>
  <tr>
    <td><b>🔎 数据集成</b></td>
    <td>零编码直连 MySQL、Redis、PostgreSQL、Kafka 等企业数据源，轻松构建数据驱动型 AI 应用</td>
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
  <tr>
    <td><b>......</b></td>
    <td>......</td>
  </tr>
</table>

> 注：Bella-Workflow 中的工具、知识库模块目前尚未开源，敬请期待后续版本。

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
      在您自己的基础设施上部署 Bella-Workflow，完全控制数据和环境。<br/>
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

## 👨‍💻 贡献指南

我们热心欢迎社区贡献！贡献者需要同意项目维护者可根据需要调整开源协议，以及贡献代码可能被用于商业目的。

详细的贡献指南请参考 [贡献指南](./CONTRIBUTING.md) 。

## 🔐 商业使用须知

Bella-Workflow 采用双重许可协议，具体使用限制如下：

<table>
  <tr>
    <td width="200"><b>📷 前端限制</b></td>
    <td>
      前端部分遵循 Dify 许可协议，使用时不得移除或修改 Dify 控制台或应用程序中的 LOGO 或版权信息。如需将前端用于多租户服务，请确保遵循 Dify 许可协议的相关条款。
    </td>
  </tr>
  <tr>
    <td><b>🌟 后端自由使用</b></td>
    <td>
      后端及其他部分采用 MIT 许可协议，允许自由使用、修改和分发，包括商业用途，只要保留原始版权声明和许可证文本。
    </td>
  </tr>
</table>

## 📃 许可协议

Bella-Workflow 采用双重许可协议模式，分别针对前端和其他部分使用不同的许可协议。详细条款请参阅 [LICENSE](./LICENSE) 文件。

---

<div align="center">
  <p>© 2025 Bella. 保留所有权利。</p>
  <p>
    <a href="https://doc.bella.top/">官方网站</a> · 
    <a href="https://github.com/LianjiaTech/bella-workflow">项目仓库</a>
  </p>
</div>



