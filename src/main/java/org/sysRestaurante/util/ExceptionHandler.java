package org.sysRestaurante.util;

@SuppressWarnings("EmptyMethod")
public class ExceptionHandler {

    private static int globalExceptionsCount = 0;

    public static void incrementGlobalExceptionsCount() {
        ExceptionHandler.globalExceptionsCount += 1;
    }

    public static int getGlobalExceptionsCount() {
        return ExceptionHandler.globalExceptionsCount;
    }

    public static void doNothing() {
    }
}
