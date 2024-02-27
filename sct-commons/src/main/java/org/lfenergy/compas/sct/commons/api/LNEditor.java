// SPDX-FileCopyrightText: 2024 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons.api;

import org.lfenergy.compas.scl2007b4.model.TAnyLN;
import org.lfenergy.compas.sct.commons.dto.DaTypeName;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.dto.DoTypeName;

public interface LNEditor {

    boolean isDoObjectsInstanceAndDataAttributesInstanceExists(TAnyLN tAnyLN, DoTypeName doTypeName, DaTypeName daTypeName);

    void updateOrCreateDoObjectsAndDataAttributesInstances(TAnyLN tAnyLN, DataAttributeRef dataAttributeRef);
}
