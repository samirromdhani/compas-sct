// SPDX-FileCopyrightText: 2023 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons.util;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Getter;
import lombok.ToString;
import java.io.Reader;
import java.util.List;

@Getter
public class RecordingSettingsCsvHelper {

    private final List<RecordingDTO> recordings;

    public RecordingSettingsCsvHelper(Reader csvSource) {
        recordings = readCsvFile(csvSource);
    }

    private List<RecordingDTO> readCsvFile(Reader csvSource) {
        return CsvUtils.parseRows(csvSource, RecordingDTO.class).stream()
                .distinct()
                .toList();
    }

    @ToString
    public static class RecordingDTO {
        @CsvBindByPosition(position = 0)
        private String RTE_IEDType;
        @CsvBindByPosition(position = 1)
        private String IED_redundancy;
        @CsvBindByPosition(position = 2)
        private String IED_instance;
    }

}
