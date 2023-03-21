// SPDX-FileCopyrightText: 2023 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.lfenergy.compas.sct.commons.dto.RecordingSettingsDTO;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecordingCSVParser {

    public List<RecordingSettingsDTO> parseCsv(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource(fileName)).getFile());
        Reader in = new FileReader(file);
        CSVFormat csvFormat = CSVFormat
                .Builder.create()
                .setHeader() //! not ignore it
                .setDelimiter(',')
                .setCommentMarker('#')
                .build();
        CSVParser parser = csvFormat.parse(in);
        List<CSVRecord> records = parser.getRecords();
        return records.stream()
                .filter(CSVRecord::isConsistent)
                .map(RecordingSettingsDTO::new)
                .peek(record -> System.out.println("some business staff for : "+record))
                .collect(Collectors.toList());
    }
}
