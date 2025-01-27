package org.igov.model.escalation;

import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.identity.User;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.igov.service.controller.ActivitiController;
import org.igov.service.interceptor.exception.ActivitiRestException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.igov.util.convert.BPMNUtil;
import org.igov.util.convert.AlgorithmLuna;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EscalationService {
    private static final Logger oLog = Logger.getLogger(EscalationService.class);

    private static final String SEARCH_DELAYED_TASKS_URL = "/task-activiti/";
    private static final String REGIONAL_SERVER_PATH = "https://region.org.gov.ua";

    @Autowired
    private TaskService taskService;
    @Autowired
    private FormService formService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private IdentityService identityService;
    @Autowired
    private EscalationRuleDao escalationRuleDao;
    @Autowired
    private EscalationHelper escalationHelper;

    public void runEscalationAll() throws ActivitiRestException {
        try {
            List<EscalationRule> aEscalationRule = escalationRuleDao.findAll();
            for (EscalationRule oEscalationRule : aEscalationRule) {
                runEscalationRule(oEscalationRule, REGIONAL_SERVER_PATH);
            }
        } catch (Exception oException) {
            oLog.error("[getTaskData]:" + oException);
            throw new ActivitiRestException("ex in controller!", oException);
        }

    }

    public void runEscalationRule(Long nID_escalationRule, String regionalServerPath) {
        runEscalationRule(escalationRuleDao.findById(nID_escalationRule).orNull(), regionalServerPath);
    }

    private void runEscalationRule(EscalationRule oEscalationRule, String regionalServerPath) {
        EscalationRuleFunction oEscalationRuleFunction = oEscalationRule.getoEscalationRuleFunction();

        String sID_BP = oEscalationRule.getsID_BP();
        oLog.info("[getTaskData]:sID_BP=" + sID_BP);
        TaskQuery oTaskQuery = taskService.createTaskQuery()
                .processDefinitionKey(sID_BP);//.taskCreatedAfter(dateAt).taskCreatedBefore(dateTo)

        String sID_State_BP = oEscalationRule.getsID_UserTask();
        oLog.info("[getTaskData]:sID_State_BP=" + sID_State_BP);
        if (sID_State_BP != null && !"*".equals(sID_State_BP)) {
            oTaskQuery = oTaskQuery.taskDefinitionKey(sID_State_BP);
        }

        Integer nRowStart = 0;
        Integer nRowsMax = 1000;
        List<Task> aTask = oTaskQuery.listPage(nRowStart, nRowsMax);

        oLog.info("[getTaskData]:Found " + aTask.size() + " tasks for specified business process and state");
        for (Task oTask : aTask) {
            try {
                Map<String, Object> mTaskParam = getTaskData(oTask);
                mTaskParam.put("processLink",
                        regionalServerPath + SEARCH_DELAYED_TASKS_URL + mTaskParam.get("nID_task_activiti"));
                mTaskParam.put("nID_EscalationRule", oEscalationRule.getId());
                oLog.info("[getTaskData]:checkTaskOnEscalation mTaskParam=" + mTaskParam);
                //send emails (or processing by other bean-handlers)
                escalationHelper.checkTaskOnEscalation(mTaskParam
                        , oEscalationRule.getsCondition()
                        , oEscalationRule.getSoData()
                        , oEscalationRule.getsPatternFile()
                        , oEscalationRuleFunction.getsBeanHandler()
                );
            } catch (ClassCastException e) {
                oLog.error("Error occured while processing task " + oTask.getId(), e);
            }
        }
    }

    private Map<String, Object> getTaskData(final Task oTask) {//Long nID_task_activiti
        final String taskId = oTask.getId();
        long nID_task_activiti = Long.valueOf(taskId);
        oLog.info("[getTaskData]:nID_task_activiti=" + nID_task_activiti);
        oLog.info("[getTaskData]:oTask.getCreateTime().toString()=" + oTask.getCreateTime());
        oLog.info("[getTaskData]:oTask.getDueDate().toString()=" + oTask.getDueDate());

        Map<String, Object> m = new HashMap<>();
        m.put("sTaskId", taskId);

        long nDiffMS = 0;
        if (oTask.getDueDate() != null) {
            nDiffMS = oTask.getDueDate().getTime() - oTask.getCreateTime().getTime();
        } else {
            nDiffMS = DateTime.now().toDate().getTime() - oTask.getCreateTime().getTime();
        }
        oLog.info("[getTaskData]:nDiffMS=" + nDiffMS);

        long nElapsedHours = nDiffMS / 1000 / 60 / 60;
        oLog.info("[getTaskData]:nElapsedHours=" + nElapsedHours);
        m.put("nElapsedHours", nElapsedHours);

        long nElapsedDays = nElapsedHours / 24;
        oLog.info("[getTaskData]:nElapsedDays=" + nElapsedDays);
        m.put("nElapsedDays", nElapsedDays);
        m.put("nDays", nElapsedDays);

        TaskFormData oTaskFormData = formService.getTaskFormData(taskId);
        for (FormProperty oFormProperty : oTaskFormData.getFormProperties()) {
        	String sType = oFormProperty.getType().getName();
        	String sValue = null;
            oLog.info(String.format("[getTaskData]Matching property %s:%s:%s with fieldNames", oFormProperty.getId(),
                    oFormProperty.getName(), sType));
            if ("long".equalsIgnoreCase(oFormProperty.getType().getName()) &&
                    StringUtils.isNumeric(oFormProperty.getValue())) {
                m.put(oFormProperty.getId(), Long.valueOf(oFormProperty.getValue()));
            } else {
            	if ("enum".equalsIgnoreCase(sType)) {
					sValue = ActivitiController.parseEnumProperty(oFormProperty);
				} else {
					sValue = oFormProperty.getValue();
				}
            	if (sValue != null){
            		m.put(oFormProperty.getId(), sValue);
            	}
            }
        }

        m.put("sID_BP_full", oTask.getProcessDefinitionId());
        m.put("sID_BP", StringUtils.substringBefore(oTask.getProcessDefinitionId(), ":"));
        m.put("nID_task_activiti", AlgorithmLuna.getProtectedNumber(Long.valueOf(oTask.getProcessInstanceId())));
        m.put("sTaskName", oTask.getName());
        m.put("sTaskDescription", oTask.getDescription());
        m.put("sProcessInstanceId", oTask.getProcessInstanceId());
        m.put("sLoginAssigned", oTask.getAssignee());
        
        List<User> aUser = BPMNUtil
                .getUsersInfoBelongToProcess(repositoryService, identityService, oTask.getProcessDefinitionId(),
                        oTask.getTaskDefinitionKey());
        StringBuffer osaUser = new StringBuffer();
        int nCount = aUser.size();
        int n = 0;
        for (User oUser : aUser) {
            n++;
            osaUser.append(oUser.getLastName());
            osaUser.append(" ");
            osaUser.append(oUser.getFirstName());
            osaUser.append(" (");
            osaUser.append(oUser.getId());
            osaUser.append(")");
            if (n < nCount) {
                osaUser.append(", ");
            }
        }

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(oTask.getProcessDefinitionId()).singleResult();

        m.put("sServiceType", processDefinition != null ? processDefinition.getName() : "");
        m.put("sTaskName", String.format("%s", oTask.getName()));
        m.put("sTaskNumber", AlgorithmLuna.getProtectedNumber(Long.valueOf(oTask.getProcessInstanceId())));
        m.put("sElapsedInfo", String.format("%d", nElapsedDays));
        m.put("sResponsiblePersons", String.format("%s", osaUser.toString()));

        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(oTask.getProcessInstanceId()).singleResult();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        m.put("sDate_BP", formatter.format(processInstance.getStartTime().getTime()));
        m.putAll(processInstance.getProcessVariables());


        return m;
    }
}
