package com.citi.tts.apibrick.core.script;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.net.NetPermission;
import java.net.SocketPermission;
import java.security.Permission;
import java.util.PropertyPermission;

/**
 * Security Manager for Groovy Script Sandbox
 * 
 * Blocks dangerous operations:
 * - File system access
 * - Network access
 * - Reflection on sensitive classes
 * - System property modification
 */
public class ScriptSecurityManager extends SecurityManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ScriptSecurityManager.class);
    
    // Whitelist of allowed classes for reflection
    private static final java.util.Set<String> REFLECTION_WHITELIST = java.util.Set.of(
        "java.lang.String",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Double",
        "java.lang.Boolean",
        "java.util.Map",
        "java.util.List"
    );
    
    @Override
    public void checkPermission(Permission perm) {
        // Allow all permissions for system classes
        if (isSystemClass()) {
            return;
        }
        
        // Block file system access
        if (perm instanceof FilePermission) {
            logger.warn("Blocked file system access: {}", perm);
            throw new SecurityException("File system access is not allowed in script sandbox");
        }
        
        // Block network access
        if (perm instanceof SocketPermission || perm instanceof NetPermission) {
            logger.warn("Blocked network access: {}", perm);
            throw new SecurityException("Network access is not allowed in script sandbox");
        }
        
        // Block reflection on non-whitelisted classes
        if (perm instanceof ReflectPermission) {
            String targetClass = getTargetClass();
            if (targetClass != null && !REFLECTION_WHITELIST.contains(targetClass)) {
                logger.warn("Blocked reflection on class: {}", targetClass);
                throw new SecurityException("Reflection on class " + targetClass + " is not allowed");
            }
        }
        
        // Block system property modification
        if (perm instanceof PropertyPermission) {
            if (perm.getActions().contains("write")) {
                logger.warn("Blocked system property write: {}", perm);
                throw new SecurityException("System property modification is not allowed");
            }
        }
    }
    
    /**
     * Check if current call stack is from system classes
     */
    private boolean isSystemClass() {
        Class<?>[] context = getClassContext();
        for (Class<?> clazz : context) {
            String className = clazz.getName();
            if (className.startsWith("java.") || 
                className.startsWith("javax.") ||
                className.startsWith("sun.") ||
                className.startsWith("com.sun.")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get target class for reflection permission
     */
    private String getTargetClass() {
        // Try to extract class name from stack trace
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            if (!className.startsWith("java.") && 
                !className.startsWith("groovy.") &&
                !className.startsWith("com.citi.tts.apibrick.core.script")) {
                return className;
            }
        }
        return null;
    }
}

