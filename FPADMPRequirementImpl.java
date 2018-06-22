package com.asiainfo.aibsm4.boms.demand;

import java.util.HashMap;
import java.util.Map;

import com.ailk.aiwf.engine.service.WFJobProcessContext;
import com.ailk.aiwf.engine.spi.WFJobProcessEvent;
import com.asiainfo.aibsm4.aidmp.attach.service.AttachProcessService;
import com.asiainfo.aibsm4.boms.CollectFeedback.FPADMPCollectUtil_bak;
import com.asiainfo.aibsm4.boms.CollectFeedback.FPADMPCollectUtil;
import com.asiainfo.aicf.commons.service.AICFServiceManager;

/*
 * @Author  :pengkun
 * @Email   :pengkun3@asiainfo.com
 * @Create  :Jan 19, 2015
 * @Version  :0.0.1
 * @Function: 根据业务需求，在需求提出时如果提交到组，需求提出部门在需求提出的处理类中无法获取，如果在组中的某个用户接单后，就把这个接单用户的部门更新到sm_busi_dema的部门字段中
 *
 */
public class FPADMPRequirementImpl implements WFJobProcessEvent
{
	private static AttachProcessService attachProcessEvent = (AttachProcessService) AICFServiceManager.getCustomService(AttachProcessService.class); 
	public void executeEvent(WFJobProcessContext context)
	{
		
		Map<String,Object> argMap = new HashMap<String	, Object>();
		argMap.put("uid", context.getCurrentUser().getId());
		argMap.put("job_id", context.getCurrentJob().getJob_id());
		//得到当前单号是不是提交到组的
		Map groupMap = context.getCommonService().queryForMap("admp.code.query.groud.dept_id",argMap);
		String temp = groupMap.get("DEMA_DEPT").toString();
		if(groupMap.get("DEMA_DEPT").toString().equals("-1"))
		{
			Map deptMap = context.getCommonService().queryForMap("admp.code.query.dept.caller",argMap);
			if(deptMap == null||deptMap.isEmpty() )
			{
				return;
			}
			//得到当前工单的工单编号
			String 	job_id = context.getCurrentJob().getJob_id();
			argMap.put("jobId", job_id);
			argMap.put("deptId", deptMap.get("DEPT_ID"));
			context.getCommonService().executeSqlByCode("admp.code.requiremen.dept.update", argMap);
		}
		  //附件控制 
	    attachProcessEvent.saveFormInfo(context,1);
	}
}
