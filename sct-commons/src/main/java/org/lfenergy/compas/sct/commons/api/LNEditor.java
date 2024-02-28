// SPDX-FileCopyrightText: 2024 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons.api;

import org.lfenergy.compas.scl2007b4.model.TAnyLN;
import org.lfenergy.compas.sct.commons.dto.DaTypeName;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.dto.DoTypeName;
import org.lfenergy.compas.sct.commons.dto.SclReportItem;

import java.util.List;

public interface LNEditor {

    boolean isDOAndDAInstanceExists(TAnyLN tAnyLN, DoTypeName doTypeName, DaTypeName daTypeName);

    List<SclReportItem> getDOAndDAInstances(TAnyLN tAnyLN, DataAttributeRef dataAttributeRef);

    void updateOrCreateDOAndDAInstances(TAnyLN tAnyLN, DataAttributeRef dataAttributeRef);

}
