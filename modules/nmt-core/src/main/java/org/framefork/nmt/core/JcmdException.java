package org.framefork.nmt.core;

/**
 * Exception thrown when a jcmd command fails.
 */
public class JcmdException extends RuntimeException
{

    public JcmdException(String message)
    {
        super(message);
    }

    public JcmdException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
