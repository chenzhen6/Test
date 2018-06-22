
/*
 * Copyright (c) Asiainfo Technologies(China),Inc.
 * 
 */
package com.asiainfo.aibsm4.boms.demand;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.record.formula.functions.True;

import com.ailk.aiwf.engine.service.WFJobProcessContext;
import com.ailk.aiwf.engine.spi.WFJobProcessEvent;
import com.asiainfo.aibsm4.aidmp.attach.service.AttachProcessService;
import com.asiainfo.aibsm4.aidmp.common.CommonService;
import com.asiainfo.aibsm4.aidmp.model.SM_TEME_FUNC;
import com.asiainfo.aibsm4.boms.CollectFeedback.FPADMPCollectUtil_bak;
import com.asiainfo.aibsm4.boms.CollectFeedback.FPADMPCollectUtil;
import com.asiainfo.aicf.commons.service.AICFCommonService;
import com.asiainfo.aicf.commons.service.AICFServiceManager;
import com.informix.msg.isam_en_US;

/***
 * 工单任务关联关系，业务需求-科室经理-班组长-组员-开发商-系统需求
 * @author chenzhen 2017.07.21
 * @version 1.0
 * 
 */
public class FPADMPCDemandTaskImpl implements WFJobProcessEvent {
	private static CommonService commonService = (CommonService) AICFServiceManager.getCustomService(CommonService.class);
	private static AICFCommonService aicfCommonService = (AICFCommonService) AICFServiceManager.getCustomService(AICFCommonService.class);
	private static AttachProcessService attachProcessEvent = (AttachProcessService) AICFServiceManager.getCustomService(AttachProcessService.class);
	public void executeEvent(WFJobProcessContext context) {
		//更新需求状态
		String job_id = context.getCurrentJob().getJob_id();
		String par_job_id = context.getCurrentJob().getPar_job_id();
		Map paraMap = context.getSubmitParameters();
		paraMap.put("job_id", job_id);
		
		Map<String,Integer> statusResult=commonService.getDemaStatus(context,1,null);
		
		if(statusResult!=null){ 
			int dema_status=statusResult.get("dema_status");
			int dema_stage=statusResult.get("dema_stage");
			paraMap.put("dema_status", dema_status);
			paraMap.put("busi_dema_stage", dema_stage);
			aicfCommonService.executeSqlByCode("admp.code.demand.basic.demaStatus.update", paraMap);
		}

		Long nextStep=context.getCurrentFlow().getNext_step();
		if(null!= context.getCurrentJob().getPar_job_id()){//为子单的时候
			/***
			 * 1、存在当前工单，则更新接收时间、完成时间
			 * 2、不存在当前工单，则插入完整信息
			 */
//			paraMap.put("last_upd_by", context.getCurrentUser().getUserId());
			paraMap.put("sub_job_id", job_id);//当前工单ID
			paraMap.put("job_id", context.getCurrentJob().getPar_job_id());//父工单ID
			if(1314==context.getCurrentFlow().getStep_id()){//2)科室经理拆分
		    	    paraMap.put("param_level", 1);//父工单ID
		    	}else if(1315==context.getCurrentFlow().getStep_id()){//3)班组长拆分
		    	    paraMap.put("param_level", 2);//父工单ID
		    	}else if(1148==context.getCurrentFlow().getStep_id()){//4)组员拆分
		    	    paraMap.put("param_level", 3);//父工单ID
		    	}else if(1316==context.getCurrentFlow().getStep_id()){//5)开发商分析
		    	    paraMap.put("param_level", 4);//父工单ID
		    	}
			Map resultMap = aicfCommonService.queryForMap("aidmp.code.demand.task.query", paraMap);//查询当前工单是否存在
			resultMap.put("job_id", context.getCurrentJob().getJob_id());//当前工单ID
			resultMap.put("CUR_FINISHDATE_MIN", paraMap.get("cur_finishdate_min"));//当前工单ID
			resultMap.put("CURRENTTASKCALLER", paraMap.get("currentTaskCaller"));//当前工单ID
//			resultMap = aicfCommonService.queryForMap("aidmp.code.demand.task.query", paraMap);//重新获取
			if("2".equals(resultMap.get("FLOOR").toString())){//科室经理审批（子任务）
//			    updateDemandTask(context,resultMap,2);
			    insertDemandTask(context,resultMap,2);
			}else if("3".equals(resultMap.get("FLOOR").toString())){//班组长审批
//			    updateDemandTask(context,resultMap,3);
			    insertDemandTask(context,resultMap,3);
			}else if("4".equals(resultMap.get("FLOOR").toString())){//组员分析
//			    updateDemandTask(context,resultMap,4);
			    insertDemandTask(context,resultMap,4);
			}else if("5".equals(resultMap.get("FLOOR").toString())){//开发商分析
//			    updateDemandTask(context,resultMap,5);
			    insertDemandTask(context,resultMap,5);
			}else if("6".equals(resultMap.get("FLOOR").toString())){//系统需求
//			    updateDemandTask(context,resultMap,6);
			    insertDemandTask(context,resultMap,6);
			}
			
		}else{//业务需求单
			/***
			 * 1、存在当前工单，则更新接收时间、完成时间
			 * 2、不存在当前工单，则插入完整信息
			 */
			paraMap.put("sub_job_id", job_id);//当前工单ID
			Map resultMap = aicfCommonService.queryForMap("aidmp.code.demand.task.query", paraMap);//查询当前工单是否存在
			if (resultMap == null) {//科室经理审批（业务需求）
			    resultMap = new HashMap();
			    resultMap.put("job_id", context.getCurrentJob().getJob_id());//当前工单ID
			    resultMap.put("CUR_FINISHDATE_MIN", paraMap.get("cur_finishdate_min"));//当前工单ID
			    resultMap.put("CURRENTTASKCALLER", paraMap.get("currentTaskCaller"));//当前工单ID
			    aicfCommonService.executeSqlByCode("aidmp.code.demand.task.manager.update",resultMap);//更新业务需求主办科室
			    insertDemandTask(context,resultMap,1);
//			    
			}else{//已存在则更新时间
//			    updateDemandTask(context,resultMap,1);
			}		    
		    
		}
		  //附件控制 
		attachProcessEvent.saveFormInfo(context,1);
	}
	
	/***
	 * 更新任务关系
	 * @param context
	 * @param updmap
	 * @param level:工单任务层级
	 */
	private void updateDemandTask(WFJobProcessContext context, Map<String, Object> updmap,int level)
	{
	    Map hisMap =aicfCommonService.queryForMap("aidmp.code.demand.task.his.query", updmap);//获取工单处理历史的时间
	    if (level==1) {
		updmap.put("busi_job_id", context.getCurrentJob().getJob_id());
		updmap.put("yz_receive", hisMap.get("CUR_SOLVEDATE").toString());
		updmap.put("yz_finish", true);		
	    } else if(level==2){
		
	    } else if(level==3){
		
	    } else if(level==4){
		
	    } else if(level==5){
		
	    } else if(level==6){
		
	    } else if(level==7){
		
	    }
	    if (level<7) {
		aicfCommonService.executeSqlByCode("aidmp.code.demand.task.update", updmap);//更新关系
	    }	    
	}
	/***
	 * 插入任务关系
	 * @param context
	 * @param updmap
	 * @param level:工单任务层级
	 */
	private void insertDemandTask(WFJobProcessContext context, Map<String, Object> updmap,int level)
	{
	    Map hisMap =aicfCommonService.queryForMap("aidmp.code.demand.task.his.query", updmap);//获取工单处理历史的时间
	    if (level==1) {
		updmap.put("BUSI_JOB_ID", context.getCurrentJob().getJob_id());
		updmap.put("YZ_RECEIVE", hisMap.get("CUR_SOLVEDATE").toString());
		updmap.put("YZ_FINISH", updmap.get("CUR_FINISHDATE_MIN").toString());
	    } else if(level==2){
		updmap.put("MANAGER_JOB_ID", context.getCurrentJob().getJob_id());
		updmap.put("MANAGER_RECEIVE", hisMap.get("CUR_SOLVEDATE").toString());
		updmap.put("MANAGER_FINISH", updmap.get("CUR_FINISHDATE_MIN").toString());
	    } else if(level==3){
		updmap.put("MONITOR_JOB_ID", context.getCurrentJob().getJob_id());
		updmap.put("MONITOR_RECEIVE", hisMap.get("CUR_SOLVEDATE").toString());
		updmap.put("MONITOR_FINISH", updmap.get("CUR_FINISHDATE_MIN").toString());

	    } else if(level==4){
		updmap.put("MEMBER_JOB_ID", context.getCurrentJob().getJob_id());
		updmap.put("MEMBER_RECEIVE", hisMap.get("CUR_SOLVEDATE").toString());
		updmap.put("MEMBER_FINISH", updmap.get("CUR_FINISHDATE_MIN").toString());

	    } else if(level==5){
		updmap.put("FAC_JOB_ID", context.getCurrentJob().getJob_id());
		updmap.put("FAC_RECEIVE", hisMap.get("CUR_SOLVEDATE").toString());
		updmap.put("FAC_FINISH", updmap.get("CUR_FINISHDATE_MIN").toString());

	    } else if(level==6){
		updmap.put("DEMA_JOB_ID", context.getCurrentJob().getJob_id());
		updmap.put("DEMA_RECEIVE", hisMap.get("CUR_SOLVEDATE").toString());
		updmap.put("DEMA_FINISH", updmap.get("CUR_FINISHDATE_MIN").toString());

	    } else if(level==7){

	    }
	    updmap.put("TASK_LEVEL", level);//设置层级参数
	    if (level<7) {
		aicfCommonService.executeSqlByCode("aidmp.code.demand.task.insert", updmap);//插入关系
	    }
	}	
}
