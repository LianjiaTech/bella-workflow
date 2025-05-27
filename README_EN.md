<div align="center">

# Bella-Workflow

<h3>Professional LLM Workflow Engine, Making AI Application Development Simpler</h3>

[![Static Badge](https://img.shields.io/badge/Docs-Bella%20Home-green?style=flat)](https://doc.bella.top/)
[![License](https://img.shields.io/badge/License-Bella--Workflow%20License-blue?style=flat)](./LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/LianjiaTech/bella-workflow?style=flat)](https://github.com/LianjiaTech/bella-workflow/stargazers)
[![GitHub issues](https://img.shields.io/github/issues/LianjiaTech/bella-workflow?style=flat)](https://github.com/LianjiaTech/bella-workflow/issues)

</div>

<p align="center">
  <a href="./README.md">中文</a> | 
  <b>English</b> | 
  <a href="https://doc.bella.top/">Documentation</a>
</p>

## 🔥 Project Introduction

**Bella-Workflow** is the core LLM application development platform within **Beike**, dedicated to providing developers with **more flexible, efficient, and powerful** AI application building capabilities.

Based on the "Backend as a Service" concept, we have independently developed a powerful workflow execution engine. At the same time, we continuously expand our capabilities, striving to deeply integrate traditional backend services across the entire lifecycle of development, testing, deployment, and operations, creating a truly one-stop service platform for AI applications.

To accelerate the implementation of the entire project, we reused Dify's excellent frontend modules, which not only shortened the project development cycle but also greatly enhanced the user experience. We would like to express our sincere gratitude to the Dify project team.

## ✨ Core Advantages

<table>
  <tr>
    <th width="200">Capability</th>
    <th>Description</th>
  </tr>
  <tr>
    <td><b>☕ Java Friendly</b></td>
    <td>Backend built entirely on Java technology stack, facilitating quick integration with Java's active ecosystem and fully leveraging existing technical accumulation</td>
  </tr>
  <tr>
    <td><b>💪 Enterprise Reliability</b></td>
    <td>Validated in large-scale production environments within Beike, supporting high-concurrency, high-availability enterprise application scenarios</td>
  </tr>
  <tr>
    <td><b>🔎 Data Integration</b></td>
    <td>Zero-code direct connection to MySQL, Redis, PostgreSQL, Kafka, and other enterprise data sources, easily building data-driven AI applications</td>
  </tr>
  <tr>
    <td><b>🔔 Smart Triggers</b></td>
    <td>Support for multiple triggering methods (Kafka messages, timers, API calls, etc.), enabling automated workflow orchestration</td>
  </tr>
  <tr>
    <td><b>🔄 Asynchronous Callbacks</b></td>
    <td>Support for asynchronous callback mode, providing efficient execution mechanisms for long-running workflows</td>
  </tr>
  <tr>
    <td><b>💻 Code Integration</b></td>
    <td>Built-in Groovy script engine, supporting writing and executing custom business logic within workflows</td>
  </tr>
  <tr>
    <td><b>🤖 RAG Encapsulation</b></td>
    <td>Provides professional Retrieval-Augmented Generation (RAG) nodes, improving the accuracy and relevance of large model outputs</td>
  </tr>
  <tr>
    <td><b>🌐 HTTP Extensions</b></td>
    <td>Powerful HTTP nodes supporting one-click JSON use case parsing, asynchronous callbacks, and other advanced features, seamlessly connecting to third-party services</td>
  </tr>
  <tr>
    <td><b>📁 Version Control</b></td>
    <td>One-click workflow version switching, supporting quick deployment and rollback, ensuring production environment stability</td>
  </tr>
  <tr>
    <td><b>🔍 Reasoning Process</b></td>
    <td>Support for outputting the complete reasoning process of inference models, improving model output explainability and debuggability</td>
  </tr>
  <tr>
    <td><b>📝 Flexible Configuration</b></td>
    <td>Support for defining JSON type fields in start nodes, enabling complex data structure transmission and processing</td>
  </tr>
  <tr>
    <td><b>......</b></td>
    <td>......</td>
  </tr>
</table>

> Note: The tools and knowledge base modules in Bella-Workflow are not yet open-sourced. Please stay tuned for future versions.

## 📍 Quick Start

### Usage Methods

<table>
  <tr>
    <td width="200"><b>🌐 Cloud Service Version</b></td>
    <td>
      Directly visit our <a href="https://workflow.bella.top/">official website</a>, no deployment or maintenance required, quickly start building your AI applications.
    </td>
  </tr>
  <tr>
    <td><b>💻 Self-Deployed Version</b></td>
    <td>
      Deploy Bella-Workflow on your own infrastructure, with complete control over data and environment.<br/>
      For detailed steps, please refer to our <a href="https://doc.bella.top/deployment">deployment documentation</a>.
    </td>
  </tr>
</table>

### Quick Deployment

```bash
# Clone the repository
git clone https://github.com/LianjiaTech/bella-workflow.git
cd bella-workflow/docker

# Start with docker-compose
docker-compose --env-file .example.env -f docker-compose.yaml up
```

For more detailed deployment instructions, please refer to the [Deployment Guide](./docker/README.md).

## 👨‍💻 Contribution Guidelines

We warmly welcome community contributions! Contributors need to agree that project maintainers may adjust the open source license as needed, and that contributed code may be used for commercial purposes.

For detailed contribution guidelines, please refer to the [Contribution Guide](./CONTRIBUTING_EN.md).

## 🔐 Commercial Usage Notice

Bella-Workflow adopts a dual licensing model with the following specific usage restrictions:

<table>
  <tr>
    <td width="200"><b>📷 Frontend Restrictions</b></td>
    <td>
      The frontend part follows the Dify license agreement. When using it, you must not remove or modify the LOGO or copyright information in the Dify console or applications. If you need to use the frontend for multi-tenant services, please ensure compliance with the relevant terms of the Dify license agreement.
    </td>
  </tr>
  <tr>
    <td><b>🌟 Backend Free Usage</b></td>
    <td>
      The backend and other parts use the MIT license, allowing free use, modification, and distribution, including for commercial purposes, as long as the original copyright notice and license text are retained.
    </td>
  </tr>
</table>

## 📃 License Agreement

Bella-Workflow adopts a dual licensing model, applying different licenses to the frontend and other parts. For detailed terms, please refer to the [LICENSE](./LICENSE) file.

---

<div align="center">
  <p>© 2025 Bella. All rights reserved.</p>
  <p>
    <a href="https://doc.bella.top/">Official Website</a> · 
    <a href="https://github.com/LianjiaTech/bella-workflow">Repository</a>
  </p>
</div>
