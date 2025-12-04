package com.citi.tts.apibrick.core.script;


import com.citi.tts.apibrick.common.exception.ScriptExecutionException;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Groovy Script Engine with Sandbox Isolation
 * <p>
 * Features:
 * - Security sandbox (restricts file/network/reflection operations)
 * - Execution timeout (default 500ms)
 * - Memory limits
 * - Custom ClassLoader isolation
 */
@Component
public class GroovyScriptEngine {

    private static final Logger logger = LoggerFactory.getLogger(GroovyScriptEngine.class);

    private static final long DEFAULT_TIMEOUT_MS = 500;
    private static final long DEFAULT_MEMORY_LIMIT_MB = 100;


    private final ScriptSandbox sandbox;

    public GroovyScriptEngine() {
        this.sandbox = new ScriptSandbox();
    }

    /**
     * Execute Groovy script in sandbox
     *
     * @param scriptCode Groovy script code
     * @param context    Execution context (variables available to script)
     * @return Mono<Object> Script execution result
     */
    public Mono<Object> execute(String scriptCode, Map<String, Object> context) {
        return execute(scriptCode, context, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Execute Groovy script with custom timeout
     *
     * @param scriptCode Groovy script code
     * @param context    Execution context
     * @param timeoutMs  Timeout in milliseconds
     * @return Mono<Object> Script execution result
     */
    public Mono<Object> execute(String scriptCode, Map<String, Object> context, long timeoutMs) {
        if (scriptCode == null || scriptCode.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Script code cannot be null or empty"));
        }

        return Mono.fromCallable(() -> {
                    // Create sandboxed Groovy shell
                    GroovyShell shell = sandbox.createSandboxedShell(context);
                    // Parse and execute script
                    Script script = shell.parse(scriptCode);

                    // Set script variables from context
                    if (context != null) {
                        for (Map.Entry<String, Object> entry : context.entrySet()) {
                            script.setProperty(entry.getKey(), entry.getValue());
                        }
                    }
                    logger.info("Executing script: {}", scriptCode);
                    logger.info("Executing script content: {}", script);

                    // Execute script with timeout
                    CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            return script.run();
                        } catch (Exception e) {
                            throw new RuntimeException("Script execution error: " + e.getMessage(), e);
                        }
                    });

                    try {
                        return future.get(timeoutMs, TimeUnit.MILLISECONDS);
                    } catch (java.util.concurrent.TimeoutException e) {
                        future.cancel(true);
                        throw new ScriptExecutionException("Script execution timeout after " + timeoutMs + "ms");
                    } catch (Exception e) {
                        throw new ScriptExecutionException("Script execution error: " + e.getMessage(), e);
                    }
                })
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .doOnError(error -> {
                    logger.error("Groovy script execution error", error);
                });
    }

    /**
     * Validate script syntax without execution
     *
     * @param scriptCode Groovy script code
     * @return Mono<Boolean> true if syntax is valid
     */
    public Mono<Boolean> validateSyntax(String scriptCode) {
        return Mono.fromCallable(() -> {
                    try {
                        GroovyShell shell = new GroovyShell();
                        shell.parse(scriptCode);
                        return true;
                    } catch (Exception e) {
                        logger.debug("Script syntax validation failed", e);
                        return false;
                    }
                })
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
}

