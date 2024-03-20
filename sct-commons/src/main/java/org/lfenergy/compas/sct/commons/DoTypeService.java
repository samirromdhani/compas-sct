// SPDX-FileCopyrightText: 2023 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.dto.DaTypeName;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DoTypeService {

    final DaTypeService daTypeService =  new DaTypeService();
    final SDOOrDAService sdoOrDAService =  new SDOOrDAService();
    final BDAService bdaService =  new BDAService();

    public Stream<TDOType> getDoTypes(TDataTypeTemplates tDataTypeTemplates) {
        return tDataTypeTemplates.getDOType().stream();
    }

    public Stream<TDOType> getFilteredDoTypes(TDataTypeTemplates tDataTypeTemplates, Predicate<TDOType> tdoTypePredicate) {
        return getDoTypes(tDataTypeTemplates).filter(tdoTypePredicate);
    }

    public Optional<TDOType> findDoType(TDataTypeTemplates tDataTypeTemplates, Predicate<TDOType> tdoTypePredicate) {
        return getFilteredDoTypes(tDataTypeTemplates, tdoTypePredicate).findFirst();
    }

    public List<DataAttributeRef> getAllSDOAndDA(TDataTypeTemplates dtt, TDOType tdoType, DataAttributeRef filter) {
        List<DataAttributeRef> result = new ArrayList<>();
        // DA -> BDA -> BDA..
        sdoOrDAService.getDAs(tdoType).forEach(tda -> {
            DataAttributeRef newDataObjectRef = DataAttributeRef.copyFrom(filter);
            newDataObjectRef.getDaName().setName(tda.getName());
            if(tda.isSetFc()) {
                newDataObjectRef.getDaName().setFc(tda.getFc());
            }

            // STRUCT type (BType=STRUCT) refer to BDA, otherwise it is DA
            if(tda.isSetType() && tda.isSetBType() && tda.getBType().equals(TPredefinedBasicTypeEnum.STRUCT)) {
                daTypeService.findDaType(dtt, tdaType -> tdaType.getId().equals(tda.getType()))
                        .ifPresent(nextDaType -> result.addAll(getDataAttributesFromBDA(dtt, nextDaType, newDataObjectRef)));
            } else {
                updateDaNameFromDaOrBda(tda, newDataObjectRef.getDaName());
                result.add(newDataObjectRef);
            }
        });
        // SDO -> SDO -> SDO..
        sdoOrDAService.getSDOs(tdoType)
                .forEach(tsdo -> findDoType(dtt, tdoType1 -> tsdo.isSetType() && tdoType1.getId().equals(tsdo.getType()))
                        .ifPresent(nextDoType -> {
                            DataAttributeRef newDataAttributeRef = DataAttributeRef.copyFrom(filter);
                            newDataAttributeRef.getDoName().getStructNames().add(tsdo.getName());
                            if(nextDoType.isSetCdc()) {
                                newDataAttributeRef.getDoName().setCdc(nextDoType.getCdc());
                            }
                            result.addAll(getAllSDOAndDA(dtt, nextDoType, newDataAttributeRef));
                        }));
        return result;
    }

    private List<DataAttributeRef> getDataAttributesFromBDA(TDataTypeTemplates dtt, TDAType tdaType1, DataAttributeRef dataAttributeRef) {
        List<DataAttributeRef> result = new ArrayList<>();
        // BDA -> BDA -> BDA..
        bdaService.getBDAs(tdaType1).forEach(tbda -> {
            DataAttributeRef newDataAttributeRef = DataAttributeRef.copyFrom(dataAttributeRef);
            newDataAttributeRef.getDaName().getStructNames().add(tbda.getName());

            // STRUCT type (BType=STRUCT) refer to complex BDA object, otherwise it is kind of DA object
            if(tbda.isSetType() && tbda.getBType().equals(TPredefinedBasicTypeEnum.STRUCT)){
                daTypeService.findDaType(dtt, tdaType -> tdaType.getId().equals(tbda.getType()))
                        .ifPresent(nextDaType -> result.addAll(getDataAttributesFromBDA(dtt, nextDaType, newDataAttributeRef)));
            } else {
                updateDaNameFromDaOrBda(tbda, newDataAttributeRef.getDaName());
                result.add(newDataAttributeRef);
            }
        });
        return result;
    }

    private void updateDaNameFromDaOrBda(TAbstractDataAttribute daOrBda, DaTypeName daTypeName) {
        if (daOrBda.isSetType()) daTypeName.setType(daOrBda.getType());
        if (daOrBda.isSetBType()) daTypeName.setBType(daOrBda.getBType());
        if (daOrBda.isSetValImport()) daTypeName.setValImport(daOrBda.isValImport());
        if (daOrBda.isSetVal()) daTypeName.addDaiValues(daOrBda.getVal());
    }


    public List<DataAttributeRef> getFilteredSDOAndDA(TDataTypeTemplates dtt, TDOType tdoType, DataAttributeRef filter) {
        List<DataAttributeRef> result = new ArrayList<>();
        System.out.println(" getSdoNames :: "+filter.getSdoNames());
        // DA -> BDA -> BDA..
        sdoOrDAService.getFilteredDAs(tdoType, tda -> (!filter.isDaNameDefined() || filter.getDaRef().contains(tda.getName())))
                .forEach(tda -> {
                    System.out.println(".getFilteredDAs() :: "+tda.getName());
//                    if (excludedByFilter(filter, tda)) {
//                        return;
//                    }
                    DataAttributeRef newDataObjectRef = DataAttributeRef.copyFrom(filter);
                    newDataObjectRef.getDaName().setName(tda.getName());
                    if(tda.isSetFc()) {
                        newDataObjectRef.getDaName().setFc(tda.getFc());
                    }

                    // STRUCT type (BType=STRUCT) refer to BDA, otherwise it is DA
                    if(tda.isSetType() && tda.isSetBType() && tda.getBType().equals(TPredefinedBasicTypeEnum.STRUCT)) {
                        daTypeService.findDaType(dtt, tdaType -> tdaType.getId().equals(tda.getType()))
                                .ifPresent(nextDaType -> result.addAll(getDataAttributesFromBDA(dtt, nextDaType, newDataObjectRef)));
                    } else {
                        updateDaNameFromDaOrBda(tda, newDataObjectRef.getDaName());
                        result.add(newDataObjectRef);
                    }
                });
//        // SDO -> SDO -> SDO..
////        System.out.println("filter.getSdoNames() :: "+filter.getSdoNames());
        sdoOrDAService.getFilteredSDOs(tdoType ,tsdo -> filter.getDoRef().contains(tsdo.getName()))
                .forEach(tsdo -> findDoType(dtt, tdoType1 -> tsdo.isSetType() && tdoType1.getId().equals(tsdo.getType()))
                        .ifPresent(nextDoType -> {
                            System.out.println(" getDoRef:: "+filter.getDoRef());
                            System.out.println("tsdo ADDED:: "+tsdo.getName());
//                            if(!filter.getDoRef().endsWith(tsdo.getName())) {
//                                return;
//                            }
//                            System.out.println("nextDoType filter.tsdo.getName()s() :: "+tsdo.getName());
                            DataAttributeRef newDataAttributeRef = DataAttributeRef.copyFrom(filter);
                            if(filter.getSdoNames().isEmpty() || !filter.getSdoNames().contains(tsdo.getName())) {
                                System.out.println("tsdo ADDED:: "+tsdo.getName());
                                newDataAttributeRef.addDoStructName(tsdo.getName());
                                if(nextDoType.isSetCdc()) {
                                    newDataAttributeRef.getDoName().setCdc(nextDoType.getCdc());
                                }
                            }
                            result.addAll(getFilteredSDOAndDA(dtt, nextDoType, newDataAttributeRef));
//                            sdoOrDAService.getFilteredDAs(nextDoType, tda -> !newDataAttributeRef.isDaNameDefined()
//                                            || newDataAttributeRef.getDaRef().contains(tda.getName()))
//                                    .forEach(tda -> {
//                                        System.out.println(".getFilteredDAs() :: "+tda.getName());
////                    }
////                                        DataAttributeRef newDataObjectRef = DataAttributeRef.copyFrom(filter);
//                                        newDataAttributeRef.getDaName().setName(tda.getName());
//                                        if(tda.isSetFc()) {
//                                            newDataAttributeRef.getDaName().setFc(tda.getFc());
//                                        }
//
//                                        // STRUCT type (BType=STRUCT) refer to BDA, otherwise it is DA
//                                        if(tda.isSetType() && tda.isSetBType() && tda.getBType().equals(TPredefinedBasicTypeEnum.STRUCT)) {
//                                            daTypeService.findDaType(dtt, tdaType -> tdaType.getId().equals(tda.getType()))
//                                                    .ifPresent(nextDaType -> result.addAll(getDataAttributesFromBDA(dtt, nextDaType, newDataAttributeRef)));
//                                        } else {
//                                            updateDaNameFromDaOrBda(tda, newDataAttributeRef.getDaName());
//                                            result.add(newDataAttributeRef);
//                                        }
//                                    });
                        }));
        return result;
    }

    private boolean excludedByFilter(DataAttributeRef filter, TSDO tsdo) {
        return filter != null &&
                !filter.getSdoNames().isEmpty() &&
                !filter.getSdoNames().contains(tsdo.getName());
    }

    private boolean excludedByFilter(DataAttributeRef filter, TDA da) {
        return filter != null && filter.isDaNameDefined() &&
                !filter.getDaName().getName().equals(da.getName());
    }



}