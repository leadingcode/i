package org.igov.model.escalation;

import com.google.gson.Gson;
import com.mongodb.util.JSON;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.igov.model.escalation.handler.EscalationHandler;
import org.igov.util.convert.JSExpressionUtil;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

@Component
public class EscalationHelper implements ApplicationContextAware {
    private static final Logger log = Logger.getLogger(EscalationHelper.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void checkTaskOnEscalation
            (Map<String, Object> mTaskParam,
                    String sCondition, String soData,
                    String sPatternFile, String sBeanHandler) {

        //1 -- result of condition
        Map<String, Object> jsonData = parseJsonData(soData);//from json
        mTaskParam = mTaskParam != null ? mTaskParam : new HashMap<String, Object>();

        Boolean conditionResult = false;
        try {
            conditionResult = new JSExpressionUtil().getResultOfCondition(jsonData, mTaskParam, sCondition);
        } catch (ClassNotFoundException e) {
            log.error("wrong parameters!", e);
        } catch (ScriptException e) {
            log.error("wrong sCondition or parameters! condition=" + sCondition + "params_json=" + soData, e);
        } catch (NoSuchMethodException e) {
            log.error("error in script", e);
        }

        mTaskParam.putAll(jsonData); //concat

        //2 - check beanHandler
        if (conditionResult) {
            EscalationHandler escalationHandler = getHandlerClass(sBeanHandler);
            if (escalationHandler != null) {
                escalationHandler.execute(mTaskParam, (String[]) mTaskParam.get("asRecipientMail"), sPatternFile);
            }
        }
    }

    private EscalationHandler getHandlerClass(String sBeanHandler) {
        EscalationHandler res = (EscalationHandler) applicationContext
                .getBean(sBeanHandler);//"EscalationHandler_SendMailAlert");
        log.info("Retrieved EscalationHandler component : " + res);
        return res;
    }

    private Map<String, Object> parseJsonData(String soData) {
        Map<String, Object> json = (Map<String, Object>) JSON.parse(soData);
        Map<String, Object> json_ = new Gson().fromJson(soData, HashMap.class);
        return json;
    }

}
