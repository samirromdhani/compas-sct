// SPDX-FileCopyrightText: 2024 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons.api;

import org.lfenergy.compas.scl2007b4.model.TDataTypeTemplates;
import org.lfenergy.compas.sct.commons.dto.DaTypeName;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.dto.DoTypeName;
import org.lfenergy.compas.sct.commons.dto.SclReportItem;

import java.util.List;

public interface DataTypeTemplateReader {

    boolean isDoModAndDaStValExist(TDataTypeTemplates dtt, String lNodeTypeId);

    List<SclReportItem> isDataObjectsAndDataAttributesExists(TDataTypeTemplates dtt, String lNodeTypeId, String dataRef);

    List<SclReportItem> verifyDataObjectsAndDataAttributes(TDataTypeTemplates dtt, String lNodeTypeId, DoTypeName doTypeName, DaTypeName daTypeName);

    DataAttributeRef getDataObjectsAndDataAttributes(TDataTypeTemplates dtt, String lNodeTypeId, String dataRef);

    List<DataAttributeRef> getAllDataObjectsAndDataAttributes(TDataTypeTemplates dtt);

}
