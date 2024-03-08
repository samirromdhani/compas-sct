package org.lfenergy.compas.sct.test;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.lfenergy.compas.sct.header.IHeaderService;
import org.lfenergy.compas.sct.header.impl.HeaderService;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
class HeaderServiceTest {
    IHeaderService headerService = new HeaderService();

    @Test
    void log() {
        headerService.log("");
        assertThat(true).isTrue();
    }
}