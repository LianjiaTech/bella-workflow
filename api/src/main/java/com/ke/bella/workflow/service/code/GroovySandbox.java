package com.ke.bella.workflow.service.code;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.BreakStatement;
import org.codehaus.groovy.ast.stmt.CaseStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.DoWhileStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.SwitchStatement;
import org.codehaus.groovy.ast.stmt.SynchronizedStatement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.ast.stmt.WhileStatement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import com.ke.bella.openapi.BellaContext;
import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.WorkflowSys;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.utils.HttpUtils;
import com.ke.bella.workflow.utils.Utils;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

@interface GroovyScript {
}

public class GroovySandbox {
    private static final Set<String> PROPERTY_BLACKLIST = new HashSet<>();
    private static final Set<String> METHOD_CALL_BLACKLIST = new HashSet<>();
    private static final SecureASTCustomizer secure = new SecureASTCustomizer();
    private static final CompilerConfiguration config = new CompilerConfiguration();
    private static final GroovyClassLoader loader = new GroovyClassLoader(GroovySandbox.class.getClassLoader(), config);
    static {
        PROPERTY_BLACKLIST.addAll(Arrays.asList(
                "class"));
        METHOD_CALL_BLACKLIST.addAll(Arrays.asList(
                "java.lang.String#execute",
                "java.lang.Object#execute",
                "java.lang.Object#getClass",
                "java.lang.Object#wait",
                "java.lang.Object#notify",
                "java.lang.Object#notifyAll",
                "java.lang.Object#invoke",
                "java.lang.Object#exec"));
        secure.setMethodDefinitionAllowed(true);
        secure.setPackageAllowed(false);
        secure.setIndirectImportCheckEnabled(true);
        secure.setImportsBlacklist(Arrays.asList("java.lang.SecurityManager",
                "java.lang.Runtime",
                "java.lang.ClassLoader",
                "java.lang.ProcessBuilder",
                "groovy.lang.Binding",
                "groovy.lang.GroovyClassLoader",
                "groovy.lang.GroovyCodeSource",
                "groovy.lang.GroovyShell",
                "groovy.lang.Script",
                "groovy.util.Eval",
                "groovy.util.GroovyScriptEngine"));
        secure.setStarImportsBlacklist(Arrays.asList("java.io.*",
                "java.nio.*",
                "java.net.*",
                "java.lang.reflect.*",
                "java.lang.invoke.*",
                "java.security.*",
                "java.util.concurrent.*",
                "javax.naming.*",
                "javax.management.*",
                "javax.script.*",
                "java.sql.*",
                "groovy.io.*"));
        secure.setReceiversClassesBlackList(Arrays.asList(Thread.class,
                File.class,
                System.class,
                Class.class,
                ClassLoader.class,
                Runtime.class,
                SecurityManager.class,
                ProcessBuilder.class,
                BellaContext.class));
        secure.addExpressionCheckers(expr -> {
            if(expr instanceof MethodCallExpression) {
                MethodCallExpression methodCall = (MethodCallExpression) expr;
                Expression objexpr = methodCall.getObjectExpression();
                if(objexpr instanceof PropertyExpression) {
                    String prop = ((PropertyExpression) objexpr).getPropertyAsString();
                    if(PROPERTY_BLACKLIST.contains(prop)) {
                        return false;
                    }
                }
                String className = objexpr.getType().getName();
                String methodName = methodCall.getMethodAsString();
                return !METHOD_CALL_BLACKLIST.contains(className + '#' + methodName);
            }
            return true;
        });
        config.setDebug(false);
        config.addCompilationCustomizers(secure);
        config.addCompilationCustomizers(new ImportCustomizer()
                .addImports("com.ke.bella.workflow.WorkflowRunState.NodeRunResult")
                .addStarImports("com.theokanning.openai.completion.chat."));
        config.addCompilationCustomizers(new ASTTransformationCustomizer(GroovyScript.class, InterruptCheckTransformation.class.getName()));
    }

    public static Object execute(String code, Map<String, Object> inputs, long timeout, long maxMemoryBytes) {
        try {
            if(!Configs.GROOVY_SANDBOX_ENABLE) {
                throw new IllegalArgumentException("沙箱环境暂时不支持groovy");
            }

            String key = HttpUtils.sha256(code);
            GroovyCodeSource gcs = AccessController.doPrivileged(new PrivilegedAction<GroovyCodeSource>() {
                @Override
                public GroovyCodeSource run() {
                    return new GroovyCodeSource(code, key, GroovyShell.DEFAULT_CODE_BASE);
                }
            });
            Binding binding = new Binding(inputs);
            Script s = InvokerHelper.createScript(loader.parseClass(gcs, true), binding);
            return TaskExecutor.invoke(s::run, timeout, maxMemoryBytes);
        } catch (MultipleCompilationErrorsException e1) {
            throw new IllegalArgumentException(compileMessage(e1));
        } catch (Exception e) {
            throw new IllegalArgumentException(Utils.getRootCause(e).getMessage(), e);
        }
    }

    public static Class<?> compile(String code) {
        String key = HttpUtils.sha256(code);
        GroovyCodeSource source = new GroovyCodeSource(code, key, GroovyShell.DEFAULT_CODE_BASE);
        return loader.parseClass(source, false);
    }

    @SuppressWarnings("unchecked")
    public static String compileMessage(MultipleCompilationErrorsException e) {
        StringWriter data = new StringWriter();
        PrintWriter writer = new PrintWriter(data);
        e.getErrorCollector().getErrors().forEach(ee -> {
            if(ee instanceof ExceptionMessage) {
                writer.println(Utils.getRootCause(((ExceptionMessage) ee).getCause()).getMessage());
            } else if(ee instanceof SyntaxErrorMessage) {
                SyntaxException cause = ((SyntaxErrorMessage) ee).getCause();
                writer.println(cause.getMessage());
            }
        });

        return data.toString();
    }

    @GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
    public static class InterruptCheckTransformation implements ASTTransformation {
        @Override
        public void visit(ASTNode[] nodes, SourceUnit source) {
            source.getAST().getClasses().forEach(classNode -> {
                classNode.getMethods().forEach(methodNode -> {
                    if(methodNode.getCode() instanceof BlockStatement) {
                        addInterruptChecks(methodNode.getCode());
                    }
                });
            });
        }

        private void addInterruptChecks(Statement statement) {
            if(statement instanceof BlockStatement) {
                BlockStatement blockStatement = (BlockStatement) statement;
                blockStatement.getStatements().forEach(this::addInterruptChecks);
            } else if(statement instanceof WhileStatement) {
                WhileStatement whileStatement = (WhileStatement) statement;
                whileStatement.setLoopBlock(wrapWithInterruptCheck(whileStatement.getLoopBlock()));
            } else if(statement instanceof ForStatement) {
                ForStatement forStatement = (ForStatement) statement;
                forStatement.setLoopBlock(wrapWithInterruptCheck(forStatement.getLoopBlock()));
            } else if(statement instanceof DoWhileStatement) {
                DoWhileStatement doWhileStatement = (DoWhileStatement) statement;
                doWhileStatement.setLoopBlock(wrapWithInterruptCheck(doWhileStatement.getLoopBlock()));
            } else if(statement instanceof IfStatement) {
                IfStatement ifStatement = (IfStatement) statement;
                addInterruptChecks(ifStatement.getIfBlock());
                addInterruptChecks(ifStatement.getElseBlock());
            } else if(statement instanceof SwitchStatement) {
                SwitchStatement switchStatement = (SwitchStatement) statement;
                switchStatement.getCaseStatements().forEach(this::addInterruptChecks);
                addInterruptChecks(switchStatement.getDefaultStatement());
            } else if(statement instanceof CaseStatement) {
                CaseStatement caseStatement = (CaseStatement) statement;
                addInterruptChecks(caseStatement.getCode());
            } else if(statement instanceof TryCatchStatement) {
                TryCatchStatement tryCatchStatement = (TryCatchStatement) statement;
                addInterruptChecks(tryCatchStatement.getTryStatement());
                tryCatchStatement.getCatchStatements().forEach(this::addInterruptChecks);
                addInterruptChecks(tryCatchStatement.getFinallyStatement());
            } else if(statement instanceof CatchStatement) {
                CatchStatement catchStatement = (CatchStatement) statement;
                addInterruptChecks(catchStatement.getCode());
            } else if(statement instanceof SynchronizedStatement) {
                SynchronizedStatement synchronizedStatement = (SynchronizedStatement) statement;
                addInterruptChecks(synchronizedStatement.getCode());
            } else if(statement instanceof ExpressionStatement) {
                ExpressionStatement expressionStatement = (ExpressionStatement) statement;
                Expression expression = expressionStatement.getExpression();
                addInterruptChecks(expression);
            }
        }

        private void addInterruptChecks(Expression expression) {
            if(expression instanceof ClosureExpression) {
                ClosureExpression closureExpression = (ClosureExpression) expression;
                // 确保闭包内部有中断检查
                if(closureExpression.getCode() instanceof BlockStatement) {
                    BlockStatement closureBlock = (BlockStatement) closureExpression.getCode();

                    if(closureBlock.getStatements().isEmpty()) {
                        return;
                    }

                    BlockStatement newBlock = new BlockStatement();

                    newBlock.addStatement(createInterruptCheckStatementByThrowStatement());
                    newBlock.addStatements(closureBlock.getStatements());

                    closureExpression.setCode(newBlock);
                } else {
                    addInterruptChecks(closureExpression.getCode());
                }
            } else if(expression instanceof MethodCallExpression) {
                MethodCallExpression methodCallExpression = (MethodCallExpression) expression;

                // 处理方法参数
                if(methodCallExpression.getArguments() instanceof ArgumentListExpression) {
                    ((ArgumentListExpression) methodCallExpression.getArguments())
                            .getExpressions().forEach(this::addInterruptChecks);
                }

                // 处理方法调用对象
                addInterruptChecks(methodCallExpression.getObjectExpression());

                // 对高开销方法调用进行特殊处理
                if(isExpensiveMethodCall(methodCallExpression) &&
                        methodCallExpression.getArguments() instanceof ArgumentListExpression) {
                    ((ArgumentListExpression) methodCallExpression.getArguments())
                            .getExpressions().stream()
                            .filter(arg -> arg instanceof ClosureExpression)
                            .forEach(arg -> enhanceClosureWithMoreChecks((ClosureExpression) arg));
                }
            } else if(expression instanceof BinaryExpression) {
                BinaryExpression binaryExpression = (BinaryExpression) expression;
                addInterruptChecks(binaryExpression.getLeftExpression());
                addInterruptChecks(binaryExpression.getRightExpression());
            } else if(expression instanceof PropertyExpression) {
                PropertyExpression propertyExpression = (PropertyExpression) expression;
                addInterruptChecks(propertyExpression.getObjectExpression());
                addInterruptChecks(propertyExpression.getProperty());
            } else if(expression instanceof VariableExpression) {
                Expression initialExpression = ((VariableExpression) expression).getInitialExpression();
                if(initialExpression != null) {
                    addInterruptChecks(initialExpression);
                }
            }
        }

        private void enhanceClosureWithMoreChecks(ClosureExpression closureExpression) {
            // 检查闭包代码是否为空或不是 BlockStatement
            if(closureExpression.getCode() == null || !(closureExpression.getCode() instanceof BlockStatement)) {
                return;
            }

            // 在闭包内部添加更多的中断检查点
            BlockStatement closureBlock = (BlockStatement) closureExpression.getCode();
            for (int i = 0; i < closureBlock.getStatements().size(); i++) {
                Statement statement = closureBlock.getStatements().get(i);
                if(statement instanceof ExpressionStatement) {
                    ExpressionStatement expressionStatement = (ExpressionStatement) statement;
                    Expression expression = expressionStatement.getExpression();
                    if(expression instanceof MethodCallExpression) {
                        MethodCallExpression methodCallExpression = (MethodCallExpression) expression;
                        if(isExpensiveMethodCall(methodCallExpression)) {
                            // 在方法调用前添加中断检查
                            BlockStatement newBlock = new BlockStatement();
                            newBlock.addStatement(createInterruptCheckStatementByThrowStatement());
                            newBlock.addStatement(expressionStatement);
                            closureBlock.getStatements().set(i, newBlock);
                        }
                    }
                }
            }
        }

        private boolean isExpensiveMethodCall(MethodCallExpression methodCall) {
            String methodName = methodCall.getMethodAsString();
            if(methodName == null) {
                return false;
            }

            // 识别可能导致大量内存分配或长时间运行的方法
            return methodName.equals("each") ||
                    methodName.equals("collect") ||
                    methodName.equals("findAll") ||
                    methodName.equals("sort") ||
                    methodName.equals("join") ||
                    methodName.equals("split") ||
                    methodName.equals("multiply") ||
                    methodName.equals("times") ||
                    methodName.equals("repeat") ||
                    methodName.equals("add") ||
                    methodName.equals("addAll") ||
                    methodName.equals("leftShift") || // << 操作符
                    methodName.equals("plus"); // + 操作符
        }

        private Statement wrapWithInterruptCheck(Statement originalStatement) {
            BlockStatement newBlock = new BlockStatement();
            newBlock.addStatement(createInterruptCheckStatement());
            if(originalStatement instanceof BlockStatement) {
                ((BlockStatement) originalStatement).getStatements().forEach(newBlock::addStatement);
            } else {
                newBlock.addStatement(originalStatement);
            }
            return newBlock;
        }

        private Statement createInterruptCheckStatement() {
            return new IfStatement(
                    new BooleanExpression(
                            new MethodCallExpression(
                                    new ClassExpression(new ClassNode(WorkflowSys.class)),
                                    new ConstantExpression("isInterrupted"),
                                    ArgumentListExpression.EMPTY_ARGUMENTS)),
                    new BreakStatement(),
                    EmptyStatement.INSTANCE);
        }

        private Statement createInterruptCheckStatementByThrowStatement() {
            Expression newException = new ConstructorCallExpression(
                    new ClassNode(InterruptedException.class),
                    new ArgumentListExpression(new ConstantExpression("Groovy Script is interrupted")));

            Statement throwStatement = new ThrowStatement(newException);

            return new IfStatement(
                    new BooleanExpression(
                            new MethodCallExpression(
                                    new ClassExpression(new ClassNode(WorkflowSys.class)),
                                    new ConstantExpression("isInterrupted"),
                                    ArgumentListExpression.EMPTY_ARGUMENTS)),
                    throwStatement,
                    EmptyStatement.INSTANCE);
        }

    }
}
