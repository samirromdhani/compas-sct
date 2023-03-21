// SPDX-FileCopyrightText: 2023 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons.util;

import lombok.Getter;
import org.lfenergy.compas.sct.commons.dto.RecordingSettingsDTO;

import java.io.IOException;
import java.util.List;

@Getter
public class RecordingSettingsCsvHelperApacheWay {

    private final List<RecordingSettingsDTO> recordings;

    public RecordingSettingsCsvHelperApacheWay(String fileName) throws IOException {
        RecordingCSVParser recordingCSVParser = new RecordingCSVParser();
        recordings = recordingCSVParser.parseCsv(fileName);
    }

}
