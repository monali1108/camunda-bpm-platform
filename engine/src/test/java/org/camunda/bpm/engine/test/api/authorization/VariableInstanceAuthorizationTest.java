/*
 * Copyright © 2013-2019 camunda services GmbH and various authors (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.test.api.authorization;

import static org.camunda.bpm.engine.authorization.Authorization.ANY;
import static org.camunda.bpm.engine.authorization.Permissions.READ;
import static org.camunda.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.camunda.bpm.engine.authorization.TaskPermissions.READ_VARIABLE;
import static org.camunda.bpm.engine.authorization.ProcessDefinitionPermissions.READ_INSTANCE_VARIABLE;
import static org.camunda.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.camunda.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.camunda.bpm.engine.authorization.Resources.TASK;

import org.camunda.bpm.engine.impl.AbstractQuery;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.camunda.bpm.engine.runtime.VariableInstanceQuery;

/**
 * @author Roman Smirnov
 *
 */
public class VariableInstanceAuthorizationTest extends AuthorizationTest {

  protected static final String PROCESS_KEY = "oneTaskProcess";
  protected static final String CASE_KEY = "oneTaskCase";

  protected String deploymentId;
  protected boolean ensureSpecificVariablePermission;

  @Override
  public void setUp() throws Exception {
    deploymentId = createDeployment(null,
        "org/camunda/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
        "org/camunda/bpm/engine/test/api/authorization/oneTaskCase.cmmn").getId();
    ensureSpecificVariablePermission = processEngineConfiguration.isEnforceSpecificVariablePermission();
    super.setUp();
  }

  @Override
  public void tearDown() {
    super.tearDown();
    deleteDeployment(deploymentId);
    processEngineConfiguration.setEnforceSpecificVariablePermission(ensureSpecificVariablePermission);
  }

  public void testProcessVariableQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  public void testCaseVariableQueryWithoutAuthorization () {
    // given
    createCaseInstanceByKey(CASE_KEY, getVariables());

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  public void testProcessLocalTaskVariableQueryWithoutAuthorization () {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  public void testCaseLocalTaskVariableQueryWithoutAuthorization () {
    // given
    createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  public void testStandaloneTaskVariableQueryWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    setTaskVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);

    deleteTask(taskId, true);
  }

  public void testProcessVariableQueryWithReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY, getVariables()).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertNotNull(variable);
    assertEquals(processInstanceId, variable.getProcessInstanceId());
  }

  public void testProcessVariableQueryWithReadInstancesPermissionOnOneTaskProcess() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY, getVariables()).getId();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_INSTANCE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertNotNull(variable);
    assertEquals(processInstanceId, variable.getProcessInstanceId());
  }

  public void testProcessVariableQueryWithReadInstancesVariablePermission() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY, getVariables()).getId();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_INSTANCE_VARIABLE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertNotNull(variable);
    assertEquals(processInstanceId, variable.getProcessInstanceId());
  }

  public void testProcessVariableQueryWithReadVariablePermission() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY, getVariables()).getId();
    createGrantAuthorization(TASK, PROCESS_KEY, userId, READ_VARIABLE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertNotNull(variable);
    assertEquals(processInstanceId, variable.getProcessInstanceId());
  }

  public void testProcessVariableQueryWithReadProcessInstanceWhenReadVariableIsEnabled() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(PROCESS_KEY, getVariables()).getId();
    createGrantAuthorization(PROCESS_INSTANCE, PROCESS_KEY, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  public void testProcessVariableQueryWithReadTaskWhenReadVariableIsEnabled() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(PROCESS_KEY, getVariables()).getId();
    createGrantAuthorization(TASK, PROCESS_KEY, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  public void testProcessLocalTaskVariableQueryWithReadPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  public void testProcessLocalTaskVariableQueryWithMultiple() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(TASK, taskId, userId, READ);
    createGrantAuthorization(TASK, ANY, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  public void testProcessLocalTaskVariableQueryWithReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertNotNull(variable);
    assertEquals(processInstanceId, variable.getProcessInstanceId());
  }

  public void testProcessLocalTaskVariableQueryWithReadPermissionOnOneProcessTask() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_INSTANCE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertNotNull(variable);
    assertEquals(processInstanceId, variable.getProcessInstanceId());
  }

  public void testProcessLocalTaskVariableQueryWithReadInstanceVariablePermission() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_INSTANCE_VARIABLE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertNotNull(variable);
    assertEquals(processInstanceId, variable.getProcessInstanceId());
  }

  public void testProcessLocalTaskVariableQueryWithReadVariablePermission() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(TASK, PROCESS_KEY, userId, READ_VARIABLE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertNotNull(variable);
    assertEquals(processInstanceId, variable.getProcessInstanceId());
  }

  public void testProcessLocalTaskVariableQueryWithReadProcessInstanceWhenReadVariableIsEnabled() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(PROCESS_INSTANCE, PROCESS_KEY, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }


  public void testProcessLocalTaskVariableQueryWithReadTaskWhenReadVariableIsEnabled() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(TASK, PROCESS_KEY, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  public void testStandaloneTaskVariableQueryWithReadPermissionOnTask() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    setTaskVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    deleteTask(taskId, true);
  }

  public void testStandaloneTaskVariableQueryWithReadVariablePermissionOnTask() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String taskId = "myTask";
    createTask(taskId);
    setTaskVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(TASK, taskId, userId, READ_VARIABLE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    deleteTask(taskId, true);
  }

  public void testMixedVariables() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    setTaskVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY, getVariables()).getProcessInstanceId();

    createCaseInstanceByKey(CASE_KEY, getVariables());

    // when (1)
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then (1)
    verifyQueryResults(query, 1);

    // when (2)
    createGrantAuthorization(TASK, taskId, userId, READ);

    // then (2)
    verifyQueryResults(query, 2);

    // when (3)
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // then (3)
    verifyQueryResults(query, 3);

    deleteTask(taskId, true);
  }

  public void testMixedVariablesWhenReadVariableIsEnabled() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String taskId = "myTask";
    createTask(taskId);
    setTaskVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY, getVariables()).getProcessInstanceId();

    createCaseInstanceByKey(CASE_KEY, getVariables());

    // when (1)
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then (1)
    verifyQueryResults(query, 1);

    // when (2)
    createGrantAuthorization(TASK, taskId, userId, READ_VARIABLE);

    // then (2)
    verifyQueryResults(query, 2);

    // when (3)
    createGrantAuthorization(PROCESS_DEFINITION, processInstanceId, userId, READ_INSTANCE_VARIABLE);

    // then (3)
    verifyQueryResults(query, 3);

    deleteTask(taskId, true);
  }

  // helper ////////////////////////////////////////////////////////////////

  protected void verifyQueryResults(VariableInstanceQuery query, int countExpected) {
    verifyQueryResults((AbstractQuery<?, ?>) query, countExpected);
  }

  protected void setReadVariableAsDefaultReadVariablePermission() {
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);
  }

}
