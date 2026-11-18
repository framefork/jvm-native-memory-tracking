package org.framefork.nmt.testing;

import org.framefork.nmt.core.DiagnosticCommandRunner;

/**
 * A {@link DiagnosticCommandRunner} that returns pre-recorded output for testing.
 */
public final class CannedDiagnosticCommandRunner implements DiagnosticCommandRunner
{

    private final String output;

    public CannedDiagnosticCommandRunner(String output)
    {
        this.output = output;
    }

    @Override
    public String run(String... arguments)
    {
        return output;
    }

}
