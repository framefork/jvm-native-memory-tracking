package org.framefork.nmt.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NmtAvailabilityTest
{

    @Test
    void getNmtMode_returnsNonNullMode()
    {
        var mode = NmtAvailability.getNmtMode();

        assertThat(mode).isNotNull();
        assertThat(mode).isInstanceOf(NmtMode.class);
    }

    @Test
    void isJcmdAvailable_returnsBooleanWithoutThrowing()
    {
        var available = NmtAvailability.isJcmdAvailable();

        assertThat(available).isIn(true, false);
    }

}
