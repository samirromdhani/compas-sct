// SPDX-FileCopyrightText: 2023 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons.dto;

import lombok.*;
import org.apache.commons.csv.CSVRecord;

@Data
public class RecordingSettingsDTO {
    private String iedType;
    private String iedRedundancy;
    private String iedInstance;
    private String channel_ShortLabel;
    private String channel_MREP;
    private String channel_LevMod_q;
    private String channel_LevMod_LevMod;
    private String BAP_Variant;
    private String BAP_Ignored_Value;
    private String LD_inst;
    private String LN_prefix;
    private String LN_name;
    private String LN_inst;
    // TODO DO.name,DO.inst,SDO.name,DA.name,DA.type,DA.bType,BDA.name,SBDA.name,Channel.Analog.num,Channel.Digital.num,Opt

    public RecordingSettingsDTO(CSVRecord record) {
        this.iedType = record.get("RTE-IEDType");
        this.iedRedundancy = record.get("IED.redundancy");
        this.iedInstance = record.get("IED.instance");
        this.channel_ShortLabel = record.get("Channel.ShortLabel");
        this.channel_MREP = record.get("Channel.MREP");
        this.channel_LevMod_q = record.get("Channel.LevMod.q");
        this.channel_LevMod_LevMod = record.get("Channel.LevMod");
        this.BAP_Variant = record.get("BAP.Variant");
        this.BAP_Ignored_Value = record.get("BAP.Ignored Value");
        this.LD_inst = record.get("LD.inst");
        this.LN_prefix = record.get("LN.prefix");
        this.LN_name = record.get("LN.name");
        this.LN_inst = record.get("LN.inst");
    }
}
