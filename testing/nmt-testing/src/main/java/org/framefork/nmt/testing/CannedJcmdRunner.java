package org.framefork.nmt.testing;

import org.framefork.nmt.core.JcmdRunner;

/**
 * A {@link JcmdRunner} that returns pre-recorded output for testing.
 */
public final class CannedJcmdRunner implements JcmdRunner {

    private final String output;

    public CannedJcmdRunner(String output) {
        this.output = output;
    }

    @Override
    public String run(String... arguments) {
        return output;
    }

}
