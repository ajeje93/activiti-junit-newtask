package org.activiti;

import org.activiti.engine.HistoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.List;

public class NewTaskJUnit {

	@Rule
	public ActivitiRule activitiRule = new ActivitiRule();

	@Test
	@Deployment(resources = { "org/activiti/test/my-process.bpmn20.xml" })
	public void test() {
		ProcessInstance processInstance = activitiRule.getRuntimeService().startProcessInstanceByKey("financialReport");
		assertNotNull(processInstance);

		// get task service
		TaskService taskService = activitiRule.getTaskService();

		Task firstTask = taskService.createTaskQuery().singleResult();
		assertEquals("Write monthly financial report", firstTask.getName());

		ProcessDefinition processDefinition = activitiRule.getRepositoryService().createProcessDefinitionQuery().singleResult();
		String processDefinitionId= processDefinition.getId();
		assertEquals("financialReport:1:4", processDefinitionId);

		String procId = activitiRule.getRuntimeService().createProcessInstanceQuery().singleResult().getId();
		assertEquals("5", procId);

		List<Task> tasks = taskService.createTaskQuery().list();

		for (Task task : tasks) {
			TaskEntity newTask = (TaskEntity) taskService.newTask();
			newTask.setProcessDefinitionId(processDefinitionId);
			newTask.setProcessInstanceId(procId);
			newTask.setExecutionId(procId);
			newTask.setTaskDefinitionKey("newTask");
			newTask.setName("New Task");
			newTask.setDescription("This is a new task");
			taskService.saveTask(newTask);
			taskService.complete(task.getId());
			taskService.complete(newTask.getId());
		}

		tasks = taskService.createTaskQuery().list();
		for (Task task : tasks) {
			taskService.complete(task.getId());
		}

		HistoryService historyService = activitiRule.getHistoryService();
		HistoricActivityInstance endActivity = null;
		List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
				.processInstanceId(processInstance.getId()).finished().orderByHistoricActivityInstanceEndTime().asc()
				.list();
		for (HistoricActivityInstance activity : activities) {
			if (activity.getActivityType() == "startEvent") {
				System.out.println("BEGIN " + processDefinition.getName() + " ["
						+ processInstance.getProcessDefinitionKey() + "] " + activity.getStartTime());
			}
			if (activity.getActivityType() == "endEvent") {
				endActivity = activity;
			} else {
				System.out.println("-- " + activity.getActivityName() + " [" + activity.getActivityId() + "] "
						+ activity.getDurationInMillis() + " ms");
			}
		}
		if (endActivity != null) {
			System.out.println("-- " + endActivity.getActivityName() + " [" + endActivity.getActivityId() + "] "
					+ endActivity.getDurationInMillis() + " ms");
			System.out.println("COMPLETE " + processDefinition.getName() + " ["
					+ processInstance.getProcessDefinitionKey() + "] " + endActivity.getEndTime());
		}

	}

}