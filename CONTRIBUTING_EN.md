# Bella Workflow Contribution Guidelines

Thank you for your interest in the Bella Workflow project! We warmly welcome contributions from the community, whether they are feature improvements, documentation enhancements, or bug fixes. This document will help you understand how to contribute to Bella Workflow.

## Contribution Agreement

Before contributing code, please ensure you agree to the following terms:

1. Project maintainers have the right to adjust the open source license as needed for project development
2. Your contributed code may be used for commercial purposes
3. Your contributions must follow the code standards and processes outlined in this document

## Contribution Process

### 1. Preparation

1. Fork this repository to your GitHub account
2. Clone your fork to your local machine:
   ```bash
   git clone https://github.com/YOUR_USERNAME/bella-workflow.git
   cd bella-workflow
   ```
3. Add the original repository as a remote:
   ```bash
   git remote add upstream https://github.com/LianjiaTech/bella-workflow.git
   ```
4. Ensure your local repository is synchronized with the original repository:
   ```bash
   git fetch upstream
   git checkout main
   git merge upstream/main
   ```
5. Install locally required JAR packages:
   
   **Important**: Before starting the source code locally, you need to add `api/resources/bella-job-queue-sdk-1.0.1-SNAPSHOT.jar` to your local Maven repository:
   
   ```bash
   # Execute in the project root directory
   mvn install:install-file \
     -Dfile=api/resources/bella-job-queue-sdk-1.0.1-SNAPSHOT.jar \
     -DgroupId=com.ke.bella \
     -DartifactId=bella-job-queue-sdk \
     -Dversion=1.0.0-SNAPSHOT \
     -Dpackaging=jar
   ```
   
   After successful execution, you will see output similar to "`BUILD SUCCESS`", indicating that the JAR package has been successfully installed in your local Maven repository.

### 2. Create a Branch

Create a new branch for your contribution:

```bash
# For feature improvements
git checkout -b feature/your-feature-name

# For bug fixes
git checkout -b fix/issue-description

# For documentation updates
git checkout -b docs/update-description
```

### 3. Development and Testing

1. Perform your development work on your branch
2. Follow the code standards (see below)
3. Add or update tests, ensuring all tests pass
4. Update relevant documentation (if necessary)

### 4. Commit Changes

1. Commit your changes:
   ```bash
   git add .
   git commit -m "feat: add some amazing feature"
   ```
   Note: We use the [Conventional Commits](https://www.conventionalcommits.org/) specification for formatting commit messages

2. Push to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

### 5. Create a Pull Request

1. Go to your fork on GitHub
2. Click the "Compare & pull request" button
3. Provide a clear PR title and description, explaining your changes and the reasons for them
4. If your PR resolves an issue, reference that issue in the description (e.g., "Fixes #123")

## Code Standards

### Java Code Standards

**Important**: Backend Java code must follow the Eclipse formatting standards in the `api/configuration` directory of the project:

- Use `eclipse-formatter.xml` for code formatting
- Follow the import order defined in `eclipse.importorder` (java > javax > org > com)
- Configure these files in your IDE to ensure code format consistency

## Issues and Discussions

- If you find a bug or have a feature suggestion, please create an Issue
- Before starting major work, it's best to first create an Issue for discussion to ensure your direction aligns with project goals

## Other Ways to Contribute

Besides code contributions, you can support the project in the following ways:

- Improve documentation
- Answer community questions
- Share your experience using Bella Workflow
- Share the project on social media

## Contact Us

If you have any questions or need help, please contact us through GitHub Issues or visit our [official website](https://doc.bella.top/).

---

Thank you again for your contribution to Bella Workflow!
