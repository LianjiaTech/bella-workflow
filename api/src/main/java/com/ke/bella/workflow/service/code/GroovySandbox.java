package com.ke.bella.workflow.service.code;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Map;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.BreakStatement;
import org.codehaus.groovy.ast.stmt.CaseStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.DoWhileStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.SwitchStatement;
import org.codehaus.groovy.ast.stmt.SynchronizedStatement;
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

import com.ke.bella.workflow.WorkflowSys;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.utils.HttpUtils;
import com.ke.bella.workflow.utils.Utils;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

public class GroovySandbox {

    private static final SecureASTCustomizer secure = new SecureASTCustomizer();
    private static final CompilerConfiguration config = new CompilerConfiguration();
    private static final GroovyClassLoader loader = new GroovyClassLoader(GroovySandbox.class.getClassLoader(), config);
    static {
        secure.setMethodDefinitionAllowed(false);
        secure.setPackageAllowed(false);
        secure.setIndirectImportCheckEnabled(true);
        secure.setImportsBlacklist(Arrays.asList("java.lang.SecurityManager",
                "java.lang.Runtime",
                "java.lang.ClassLoader",
                "java.lang.ProcessBuilder"));
        secure.setStarImportsBlacklist(Arrays.asList("java.io.*",
                "java.nio.*",
                "java.net.*",
                "java.lang.reflect",
                "java.security.*",
                "java.util.concurrent.*",
                "javax.naming.*",
                "javax.management.*",
                "java.sql.*",
                "groovy.io.*"));
        secure.setReceiversClassesBlackList(Arrays.asList(Thread.class,
                File.class,
                System.class,
                Class.class,
                ClassLoader.class,
                Runtime.class,
                SecurityManager.class,
                ProcessBuilder.class));
        secure.addExpressionCheckers(expr -> {
            if(expr instanceof MethodCallExpression) {
                // TODO
            }
            return true;
        });
        config.setDebug(false);
        config.addCompilationCustomizers(secure);
        config.addCompilationCustomizers(new ImportCustomizer()
                .addImports("com.ke.bella.workflow.WorkflowRunState.NodeRunResult")
                .addStarImports("com.theokanning.openai.completion.chat."));
        config.addCompilationCustomizers(new ASTTransformationCustomizer(new InterruptCheckTransformation()));
    }

    public static Object execute(String code, Map<String, Object> inputs) {
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
            return s.run();
        } catch (MultipleCompilationErrorsException e1) {
            throw new IllegalArgumentException(compileMessage(e1));
        } catch (Exception e) {
            throw new IllegalArgumentException(Utils.getRootCause(e).getMessage(), e);
        }
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
            } else if (statement instanceof TryCatchStatement) {
                TryCatchStatement tryCatchStatement = (TryCatchStatement)statement;
                addInterruptChecks(tryCatchStatement.getTryStatement());
                tryCatchStatement.getCatchStatements().forEach(this::addInterruptChecks);
                addInterruptChecks(tryCatchStatement.getFinallyStatement());
            } else if(statement instanceof CatchStatement) {
                CatchStatement catchStatement = (CatchStatement) statement;
                addInterruptChecks(catchStatement.getCode());
            } else if(statement instanceof SynchronizedStatement) {
                SynchronizedStatement synchronizedStatement = (SynchronizedStatement) statement;
                addInterruptChecks(synchronizedStatement.getCode());
            }
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
    }
}
