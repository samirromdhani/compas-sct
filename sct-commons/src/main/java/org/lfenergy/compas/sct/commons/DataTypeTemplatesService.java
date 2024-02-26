// SPDX-FileCopyrightText: 2024 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.api.DataTypeTemplateReader;
import org.lfenergy.compas.sct.commons.dto.DaTypeName;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.dto.DoTypeName;
import org.lfenergy.compas.sct.commons.dto.SclReportItem;
import org.lfenergy.compas.sct.commons.exception.ScdException;

import java.util.*;

import static org.lfenergy.compas.sct.commons.util.CommonConstants.MOD_DO_NAME;
import static org.lfenergy.compas.sct.commons.util.CommonConstants.STVAL_DA_NAME;

public class DataTypeTemplatesService implements DataTypeTemplateReader {

    final LnodeTypeService lnodeTypeService = new LnodeTypeService();
    final DoTypeService doTypeService = new DoTypeService();
    final DaTypeService daTypeService = new DaTypeService();
    final DoService doService = new DoService();
    final SDOOrDAService sdoOrDAService = new SDOOrDAService();
    final BDAService bdaService = new BDAService();

    /**
     * verify if DO(name=Mod)/DA(name=stVal) exists in DataTypeTemplate
     * @param dtt TDataTypeTemplates where Data object and Data attribute exists
     * @param lNodeTypeId LNode Type ID where Data object exists
     *  DataTypeTemplates model :
     * <DataTypeTemplates>
     *     <LNodeType lnClass="LNodeTypeClass" id="LNodeTypeID">
     *         <DO name="Mod" type="DOModTypeID" ../>
     *     </LNodeType>
     *     ...
     *     <DOType cdc="DOTypeCDC" id="DOModTypeID">
     *         <DA name="stVal" ../>
     *     </DOType>
     * </DataTypeTemplates>
     * @return true if the Data Object (Mod) and Data attribute (stVal) present, false otherwise
     */
    public boolean isDoModAndDaStValExist(TDataTypeTemplates dtt, String lNodeTypeId) {
        return lnodeTypeService.findLnodeType(dtt, lNodeType -> lNodeTypeId.equals(lNodeType.getId()))
                .map(lNodeType -> doService.findDo(lNodeType, tdo -> MOD_DO_NAME.equals(tdo.getName()))
                        .map(tdo -> doTypeService.findDoType(dtt, doType -> tdo.getType().equals(doType.getId()))
                                .map(doType -> doType.getSDOOrDA().stream()
                                        .filter(sdoOrDa -> sdoOrDa.getClass().equals(TDA.class))
                                        .map(TDA.class::cast)
                                        .anyMatch(tda -> STVAL_DA_NAME.equals(tda.getName())))
                                .orElse(false))
                        .orElse(false))
                .orElse(false);
    }
    public List<SclReportItem> isDataObjectsAndDataAttributesExists(TDataTypeTemplates dtt, String lNodeTypeId, String dataRef) {
        List<SclReportItem> sclReportItems = new ArrayList<>();
        LinkedList<String> dataRefList = new LinkedList<>(Arrays.asList(dataRef.split("\\.")));
        if (dataRefList.size() < 2) {
            sclReportItems.add(SclReportItem.error(null, "Invalid data reference %s. At least DO name and DA name are required".formatted(dataRef)));
        }
        String doName = dataRefList.remove();
        return lnodeTypeService.findLnodeType(dtt, lNodeType -> lNodeTypeId.equals(lNodeType.getId()))
                .map(lNodeType -> doService.findDo(lNodeType, tdo -> tdo.getName().equals(doName))
                        .map(tdo -> doTypeService.findDoType(dtt, doType -> doType.getId().equals(tdo.getType()))
                                .map(tdoType -> {
                                    TDOType lastDoType = findDOTypeBySdoName(dtt, tdoType, dataRefList);
                                    var tdaType = getDATypeByDaNameIfExist(dtt, lastDoType, dataRefList.get(0));
                                    tdaType.ifPresentOrElse(tdaType1 -> {
                                        if(tdaType1 instanceof TDOType) return;
                                        dataRefList.remove();
                                        checkDATypeByBdaName(dtt, (TDAType) tdaType1, dataRefList, sclReportItems);
                                    }, ()-> sclReportItems.add(SclReportItem.error(null,
                                            String.format("Unknown Sub Data Object SDO or Data Attribute DA (%s) in DOType.id (%s)",
                                                    dataRefList.get(0), lastDoType.getId()))));
                                    return sclReportItems;
                                })
                                .orElseGet(() -> {
                                    sclReportItems.add(SclReportItem.error(null, String.format("DOType.id (%s) for DO.name (%s) not found in DataTypeTemplates", tdo.getType(), tdo.getName())));
                                    return sclReportItems;
                                }))
                        .orElseGet(() -> {
                            sclReportItems.add(SclReportItem.error(null, String.format("Unknown DO.name (%s) in DOType.id (%s)", doName, lNodeTypeId)));
                            return sclReportItems;
                        }))
                .orElseGet(() -> {
                    sclReportItems.add(SclReportItem.error(null, "No Data Attribute found with this reference %s for LNodeType.id (%s)".formatted(dataRef, lNodeTypeId)));
                    return sclReportItems;
                });
    }

    public List<SclReportItem> verifyDataObjectsAndDataAttributes(TDataTypeTemplates dtt, String lNodeTypeId, DoTypeName doTypeName, DaTypeName daTypeName) {

        List<SclReportItem> sclReportItems = new ArrayList<>();
        LinkedList<String> structNamesRefList = new LinkedList<>();
        structNamesRefList.addFirst(doTypeName.getName());
        doTypeName.getStructNames().forEach(structNamesRefList::addLast);
        structNamesRefList.addLast(daTypeName.getName());
        daTypeName.getStructNames().forEach(structNamesRefList::addLast);
        if (structNamesRefList.size() < 2) {
            sclReportItems.add(SclReportItem.error(null, "Invalid data reference %s. At least DO name and DA name are required".formatted(sclReportItems)));
        }
        String doName = structNamesRefList.remove();
        return lnodeTypeService.findLnodeType(dtt, lNodeType -> lNodeTypeId.equals(lNodeType.getId()))
                .map(lNodeType -> doService.findDo(lNodeType, tdo -> tdo.getName().equals(doName))
                        .map(tdo -> doTypeService.findDoType(dtt, doType -> doType.getId().equals(tdo.getType()))
                                .map(tdoType -> {
                                    TDOType lastDoType = findDOTypeBySdoName(dtt, tdoType, structNamesRefList);
                                    var tdaType = getDATypeByDaNameIfExist(dtt, lastDoType, structNamesRefList.get(0));
                                    tdaType.ifPresentOrElse(tdaType1 -> {
                                        if(tdaType1 instanceof TDOType) return;
                                        System.out.println(":: DA Type found :: "+tdaType1.getId());
                                        structNamesRefList.remove();
                                        checkDATypeByBdaName(dtt, (TDAType) tdaType1, structNamesRefList, sclReportItems);
                                    }, ()-> sclReportItems.add(SclReportItem.error(null,
                                            String.format("Unknown Sub Data Object SDO or Data Attribute DA (%s) in DOType.id (%s)",
                                                    structNamesRefList.get(0), lastDoType.getId()))));
                                    return sclReportItems;
                                })
                                .orElseGet(() -> {
                                    sclReportItems.add(SclReportItem.error(null, String.format("DOType.id (%s) for DO.name (%s) not found in DataTypeTemplates", tdo.getType(), tdo.getName())));
                                    return sclReportItems;
                                }))
                        .orElseGet(() -> {
                            sclReportItems.add(SclReportItem.error(null, String.format("Unknown DO.name (%s) in DOType.id (%s)", doName, lNodeTypeId)));
                            return sclReportItems;
                        }))
                .orElseGet(() -> {
                    sclReportItems.add(SclReportItem.error(null, "No Data Attribute found with this reference %s for LNodeType.id (%s)".formatted(structNamesRefList, lNodeTypeId)));
                    return sclReportItems;
                });
    }

    public DataAttributeRef getDataObjectsAndDataAttributes(TDataTypeTemplates dtt, String lNodeTypeId, String dataRef) {
        DataAttributeRef dataAttributeRef = new DataAttributeRef();
        LinkedList<String> dataRefList = new LinkedList<>(Arrays.asList(dataRef.split("\\.")));
        if (dataRefList.size() < 2) {
            throw new ScdException("Invalid data reference %s. At least DO name and DA name are required".formatted(dataRef));
        }
        String doName = dataRefList.remove();
        return lnodeTypeService.findLnodeType(dtt, lNodeType -> lNodeTypeId.equals(lNodeType.getId()))
                .map(lNodeType -> doService.findDo(lNodeType, tdo -> tdo.getName().equals(doName))
                        .map(tdo -> doTypeService.findDoType(dtt, doType -> doType.getId().equals(tdo.getType()))
                                .map(tdoType -> {
                                    //DO: Setter
                                    DoTypeName doTypeName = new DoTypeName(doName);
                                    //last DoType finder
                                    TDOType lastDoType = findDOTypeBySdoName(dtt, tdoType, dataRefList, doTypeName.getStructNames());
                                    doTypeName.setCdc(tdoType.getCdc());
                                    dataAttributeRef.setDoName(doTypeName);
                                    //DA::Setter
                                    DaTypeName daTypeName = new DaTypeName(dataRefList.get(0));
                                    TDA tda = sdoOrDAService.findSDOOrDA(lastDoType, TDA.class, tda1 -> tda1.getName().equals(daTypeName.getName()))
                                            .orElseThrow();
                                    // FC only exist within DA
                                    daTypeName.setFc(tda.getFc());
                                    if(tda.getBType() != TPredefinedBasicTypeEnum.STRUCT) {
                                        daTypeName.setBType(tda.getBType());
                                        daTypeName.setType(tda.getType());
                                        daTypeName.setValImport(tda.isValImport());
                                        dataAttributeRef.setDaiValues(tda.getVal());
                                    }
                                    //DAType finder
                                    var tdaType = getDATypeByDaNameIfExist(dtt, lastDoType, daTypeName.getName());
                                    tdaType.ifPresentOrElse(tdaType1 -> {
                                        if(tdaType1 instanceof TDOType) return;
                                        //last DAType finder
                                        dataRefList.remove();
                                        TDAType lastDAType = findDATypeByBdaName(dtt, (TDAType) tdaType1, dataRefList, daTypeName.getStructNames());
                                        //DA/BDA::Setter
                                        TBDA tbda = bdaService.findBDA(lastDAType, bda -> bda.getName().equals(daTypeName.getStructNames().get(
                                                daTypeName.getStructNames().size() - 1
                                        ))).orElseThrow();
                                        if(tbda.getBType() != TPredefinedBasicTypeEnum.STRUCT) {
                                            daTypeName.setBType(tbda.getBType());
                                            daTypeName.setType(tbda.getType());
                                            daTypeName.setValImport(tbda.isValImport());
                                            dataAttributeRef.setDaiValues(tbda.getVal());
                                        }
                                        dataAttributeRef.setDaName(daTypeName);
                                    }, ()-> {
                                        throw new ScdException(String.format("Unknown Sub Data Object SDO or Data Attribute DA (%s) in DOType.id (%s)", dataRefList.get(0), lastDoType.getId()));
                                    });
                                    return dataAttributeRef;
                                })
                                .orElseThrow(() -> new ScdException( String.format("DOType.id (%s) for DO.name (%s) not found in DataTypeTemplates", tdo.getType(), tdo.getName()))))
                        .orElseThrow(() -> new ScdException(String.format("Unknown DO.name (%s) in DOType.id (%s)", doName, lNodeTypeId))))
                .orElseThrow(() -> new ScdException("No Data Attribute found with this reference %s for LNodeType.id (%s)".formatted(dataRef, lNodeTypeId)));
    }

    private <T extends TIDNaming> Optional<T> getDATypeByDaNameIfExist(TDataTypeTemplates dtt, TDOType tdoType, String daName) {
        Optional<TDA> dai = sdoOrDAService.findSDOOrDA(tdoType, TDA.class, tda -> tda.getName().equals(daName));
        if(dai.isPresent()){
            if(dai.get().getType() == null){
                return (Optional<T>) Optional.of(tdoType);
            } else {
                return (Optional<T>) daTypeService.findDaType(dtt, tdaType -> tdaType.getId().equals(dai.get().getType()));
            }
        }
        return Optional.empty();
    }

    private Optional<TDAType> getDATypeByBdaName(TDataTypeTemplates dtt, TDAType tdaType, String bdaName) {
        if(tdaType == null) return Optional.empty();
        return bdaService.findBDA(tdaType, tbda -> tbda.getName().equals(bdaName))
                .map(tbda ->  {
                    if(tbda.getBType() == TPredefinedBasicTypeEnum.STRUCT) {
                        return daTypeService.findDaType(dtt, tdaType2 -> tdaType2.getId().equals(tbda.getType()))
                                .orElseThrow(() -> new ScdException("bdaName = "+tbda + " not exist in datype id = "+tdaType.getId()));
                    }
                    return tdaType;
                })
                .stream().findFirst();
    }

    private TDOType findDOTypeBySdoName(TDataTypeTemplates dtt, TDOType tdoType, List<String> sdoNames) {
        if(sdoNames.isEmpty()) return tdoType;
        return sdoOrDAService.findSDOOrDA(tdoType, TSDO.class, tsdo -> tsdo.getName().equals(sdoNames.get(0)))
                .flatMap(sdo1 -> doTypeService.findDoType(dtt, tdoType1 -> tdoType1.getId().equals(sdo1.getType()))
                        .stream().findFirst())
                .map(tdoType1 -> {
                    sdoNames.remove(0);
                    return findDOTypeBySdoName(dtt, tdoType1, sdoNames);
                })
                .orElse(tdoType);
    }

    private TDOType findDOTypeBySdoName(TDataTypeTemplates dtt, TDOType tdoType, List<String> sdoNames, List<String> concreteList) {
        if(sdoNames.isEmpty()) return tdoType;
        return sdoOrDAService.findSDOOrDA(tdoType, TSDO.class, tsdo -> tsdo.getName().equals(sdoNames.get(0)))
                .flatMap(sdo1 -> doTypeService.findDoType(dtt, tdoType1 -> tdoType1.getId().equals(sdo1.getType()))
                        .stream().findFirst())
                .map(tdoType1 -> {
                    concreteList.add(sdoNames.get(0));
                    sdoNames.remove(0);
                    return findDOTypeBySdoName(dtt, tdoType1, sdoNames, concreteList);
                })
                .orElse(tdoType);
    }

    private void checkDATypeByBdaName(TDataTypeTemplates dtt, TDAType tdaType, List<String> bdaNames, List<SclReportItem> sclReportItems) {
        if(bdaNames.isEmpty()) return ;
        getDATypeByBdaName(dtt, tdaType, bdaNames.get(0))
                .ifPresentOrElse(tdaType1 -> {
                    bdaNames.remove(0);
                    checkDATypeByBdaName(dtt, tdaType1, bdaNames, sclReportItems);
                }, () -> sclReportItems.add(SclReportItem.error(null,String.format("Unknown BDA.name (%s) in DAType.id (%s)",
                        bdaNames.get(0), tdaType.getId()))));
    }

    private TDAType findDATypeByBdaName(TDataTypeTemplates dtt, TDAType tdaType, List<String> bdaNames, List<String> concreteList) {
        if(bdaNames.isEmpty()) return tdaType;
        return getDATypeByBdaName(dtt, tdaType, bdaNames.get(0))
                .map(tdaType1 -> {
                    concreteList.add(bdaNames.get(0));
                    bdaNames.remove(0);
                    return findDATypeByBdaName(dtt, tdaType1, bdaNames, concreteList);
                })
                .orElse(tdaType);
    }

}
