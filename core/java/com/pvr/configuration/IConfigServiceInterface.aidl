// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package com.pvr.configuration;

/** @hide */
interface IConfigServiceInterface {
    String getConfigPropertyDirectAccess(int type, String configName, String token);
    boolean setConfigPropertyDirectAccess(String pkg, int type, String configJson, String category, String token, int priorityLevel);
    String getConfigProperty(String pkg, int type, String configName, String token);
    boolean setConfigProperty(String pkg, int type, String configJson, String category, String token, int priorityLevel);
    boolean updateConfiguration(String pkg, String configJson);
    String addClientProperty(String pkg, int status);
    String addClientPropertyJson(String jsonString);
    boolean deleteClientPropertyByClientId(String clientId);
    boolean deleteClientPropertyByPkg(String pkg);
    boolean deleteClientProperty(String jsonString);
    boolean updateClientProperty(String pkg, int status, String comment, String property1);
    boolean updateClientPropertyJson(String jsonString);
    String queryClientPropertyByClientId(String clientId);
    String queryClientPropertyByPkg(String pkg);
    String queryClientProperty(String jsonString);
    String addClientMappingData(String clientId, String permissionId, int status);
    String addClientMappingDataJson(String jsonString);
    boolean deleteClientMappingDataByClientId(String clientId);
    boolean deleteClientMappingDataByPermissionId(String permissionId);
    boolean deleteClientMappingData(String jsonString);
    boolean updateClientMappingData(String pkg, int status, String comment, String property1);
    boolean updateClientMappingDataJson(String jsonString);
    String queryClientMappingDataByClientId(String clientId);
    String queryClientMappingDataByPermissionId(String permissionId);
    String queryClientMappingData(String jsonString);
    String addConfigData(String configName, String configValue, String defaultConfigValue, String groupId, int level, String token, int status);
    String addConfigDataJson(String jsonString);
    boolean deleteConfigDataByConfigId(String configId);
    boolean deleteConfigDataByPkg(String configName);
    boolean deleteConfigData(String jsonString);
    boolean updateDefaultConfigData(String configName, String defaultConfigValue);
    boolean updateConfigData(String configName, String configValue, String defaultConfigValue, String groupId, int level, String token, int status);
    boolean updateConfigDataJson(String jsonString);
    String queryConfigDataByConfigId(String configId);
    String queryConfigDataByConfigName(String configName);
    String queryConfigData(String jsonString);
    String addConfigGroupData(int groupNum, String groupName, String childGroupId, String parentGroupId, String configIds, int status);
    String addConfigGroupDataJson(String jsonString);
    boolean deleteConfigGroupDataByConfigGroupId(String configGroupId);
    boolean deleteConfigGroupData(String jsonString);
    boolean updateConfigGroupData(String configGroupId, int status);
    boolean updateConfigGroupData1(String configGroupId, int groupNum, String groupName, String childGroupId, String parentGroupId, String configIds, int status);
    boolean updateConfigGroupDataJson(String jsonString);
    String queryConfigGroupDataByConfigGroupId(String configGroupId);
    String queryConfigGroupData(String jsonString);
    String addPermissionData(String tableId, int rootPermission, String data, int action, int grantPermission, String groupId, int status);
    String addPermissionDataJson(String jsonString);
    boolean deletePermissionDataByPermissionId(String permissionId);
    boolean deletePermissionData(String jsonString);
    boolean updatePermissionData(String permissionId, byte action);
    boolean updatePermissionData1(String permissionId, String tableId, int rootPermission, String data, int action, int grantPermission, String groupId, int status);
    boolean updatePermissionDataJson(String jsonString);
    String queryPermissionDataByPermissionId(String permissionId);
    String queryPermissionData(String jsonString);
    String addRuleData(String key, String value, String linkageKey, String linkageValue, int rule);
    String addRuleDataJson(String jsonString);
    boolean deleteRuleDataByRuleId(String ruleId);
    boolean deleteRuleData(String jsonString);
    boolean updateRuleData(String key, String value, String linkageKey, String linkageValue, int rule);
    boolean updateRuleDataJson(String jsonString);
    String queryRuleDataByRuleId(String ruleId);
    String queryRuleData(String jsonString);
    String addLogTableData(String path, int date, int status);
    String addLogTableDataJson(String jsonString);
    boolean deleteLogTableDataByLogId(String logId);
    boolean deleteLogTableData(String jsonString);
    boolean updateLogTableData(String path, int date, int status);
    boolean updateLogTableDataJson(String jsonString);
    String queryLogTableDataByLogId(String logId);
    String queryLogTableData(String jsonString);
    boolean saveConfigData(String filepath);
}
