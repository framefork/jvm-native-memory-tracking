package org.framefork.nmt.core;

/**
 * Exception thrown when a diagnostic command fails or cannot be run.
 */
public class DiagnosticCommandException extends RuntimeException
{

    public DiagnosticCommandException(String message)
    {
        super(message);
    }

    public DiagnosticCommandException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
