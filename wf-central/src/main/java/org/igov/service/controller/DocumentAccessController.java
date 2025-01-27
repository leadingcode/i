package org.igov.service.controller;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.igov.model.enums.HistoryEventMessage;
import org.igov.model.enums.HistoryEventType;
import org.igov.model.DocumentAccessDao;
import org.igov.model.DocumentDao;
import org.igov.model.HistoryEventDao;
import org.igov.model.AccessURL;
import org.igov.model.Document;
import org.igov.model.DocumentAccess;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ApiResponse;


import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.igov.service.interceptor.exception.ActivitiRestException;

@Api(tags = { "ActivitiDocumentAccessController" }, description = "Предоставление и проверка доступа к документам")
@Controller
public class DocumentAccessController {

    private static final Logger oLog = LoggerFactory.getLogger(DocumentAccessController.class);
    private static final String REASON_HEADER = "Reason";
    private static final String NO_ACCESS_MESSAGE = "You don't have access!";
    private static final String UNAUTHORIZED_ERROR_CODE = "UNAUTHORIZED_ERROR_CODE";

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Подробные описания сервисов для документирования в Swagger
    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String noteCODE= "\n```\n";    
    private static final String noteCODEJSON= "\n```json\n";    
    private static final String noteController = "##### Предоставление и проверка доступа к документам. ";

    private static final String noteSetDocumentAccessLink = noteController + "Запись на доступ, с генерацией и получением уникальной ссылки на него #####\n\n"
		+ "HTTP Context: https://seriver:port/wf/service/setDocumentLink\n\n"
		+ "- nID_Document - ИД-номер документа\n"
		+ "- sFIO - ФИО, кому доступ\n"
		+ "- sTarget - цель получения доступа\n"
		+ "- sTelephone - телефон того, кому доступ предоставляется\n"
		+ "- nDays - число милисекунд, на которое предоставляется доступ\n"
		+ "- sMail - эл. почта того, кому доступ предоставляется\n"
		+ "- nID_Subject - ID авторизированого субъекта (добавляется в запрос автоматически после аутентификации пользователя)\n\n"
		+ "Response\n\n"
		+ noteCODEJSON
		+ "[  //[0..N]\n"
		+ "  {\"name\":\"sURL\",   //[1..1]\n"
		+ "    \"value\":\"https://e-gov.org.ua/index#nID_Access=4345&sSecret=JHg3987JHg3987JHg3987\" //[1..1]\n"
		+ "  }\n"
		+ "]\n"
		+ noteCODE;

    private static final String noteGetDocumentAccessLink = noteController + "проверка доступа к документу и получения данных о нем, если доступ есть #####\n\n"
		+ "HTTP Context: https://seriver:port/wf/service/getDocumentLink - \n\n"
		+ "- nID_Document - ИД-номер документа\n"
		+ "- sSecret - секретный ключ\n"
		+ "- nID_Subject - ID авторизированого субъекта (добавляется в запрос автоматически после аутентификации пользователя)\n\n"
		+ "Response\n\n"
		+ "HTTP STATUS 200\n\n"
		+ noteCODEJSON
		+ "[  //[0..N]\n"
		+ "  {\n"
		+ "    \"nID\":4355\n"
		+ "    ,\"nID_Document\":53245\n"
		+ "    ,\"sDateCreate\":\"2015-05-05 22:32:24.425\"\n"
		+ "    ,\"nMS\":3523\n"
		+ "    ,\"sFIO\":\"Вася Пупкин\"\n"
		+ "    ,\"sTarget\":\"По прикколу\"\n"
		+ "    ,\"sTelephone\":\"001 354 3456\"\n"
		+ "    ,\"sMail\":\"vasya@i.ua\"\n"
		+ "  }\n"
		+ "]\n"
		+ noteCODE
		+ "\n"
		+ "Если доступа нет, возвращается HTTP STATUS 403 Если доступ есть, но секрет не совпадает, возвращается "
		+ "HTTP STATUS 403 Если доступ просрочен, возвращается HTTP STATUS 403 Если возникла исключительная "
		+ "ситуация, возвращается HTTP STATUS 400. В заголовок ответа добавляется параметр Reason, в "
		+ "котором описана причина возникновения ситуации.";

    private static final String noteGetDocumentAccess = noteController + "Получение подтверждения на доступ к документу(с отсылкой СМС ОТП-паролем на телефон)) #####\n\n"
		+ "HTTP Context: https://seriver:port/wf/service/getDocumentAccess - \n\n"
		+ "- nID_Document - ИД-номер документа\n"
		+ "- sSecret - секретный ключ\n"
		+ "- nID_Subject - ID авторизированого субъекта (добавляется в запрос автоматически после аутентификации пользователя)\n\n"
		+ "Response\n\n"
		+ noteCODEJSON
		+ "[ //[0..N]\n"
		+ "  {\"name\":\"sURL\",   //[1..1]\n"
		+ "    \"value\":\"https://seriver:port/index#nID_Access=4345&sSecret=JHg3987JHg3987JHg3987\" //[1..1]\n"
		+ "  }\n"
		+ "]\n"
		+ noteCODE;

    private static final String noteSetDocumentAccess = noteController + "Установка подтверждения на доступ к документу, по введенному коду, из СМС-ки(ОТП-паролем) #####\n\n"
		+ "HTTP Context: https://seriver:port/wf/service/setDocumentAccess -\n"
		+ "Возвращает уникальную разовую ссылку на докуемнт.\n\n"
		+ "- nID_Access - ид доступа\n"
		+ "- sSecret - секретный ключ\n"
		+ "- sAnswer - ответ (введенный пользователем ОТП-пароль из СМС)\n"
		+ "- nID_Subject - ID авторизированого субъекта (добавляется в запрос автоматически после аутентификации пользователя)\n\n"
		+ "Response\n\n"
		+ noteCODEJSON
		+ "[ //[0..N]\n"
		+ "  {\"name\":\"sURL\",   //[1..1]\n"
		+ "    \"value\":\"https://seriver:port/index#nID_Access=4345&sSecret=JHg3987JHg3987JHg3987\" //[1..1]\n"
		+ "  }\n"
		+ "]\n"
		+ noteCODE;
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    
    @Autowired
    private DocumentAccessDao documentAccessDao;
    @Autowired
    private HistoryEventDao historyEventDao;
    @Autowired
    private DocumentDao documentDao;

    /**
     * запись на доступ, с генерацией и получением уникальной ссылки на него
     * @param nID_Document ИД-номер документа
     * @param sFIO ФИО, кому доступ
     * @param sTarget цель получения доступа
     * @param sTelephone телефон того, кому доступ предоставляется
     * @param nMS число милисекунд, на которое предоставляется доступ
     * @param sMail эл. почта того, кому доступ предоставляется
     * @param nID_Subject ID авторизированого субъекта (добавляется в запрос автоматически после аутентификации пользователя)
     */
    @ApiOperation(value = "запись на доступ, с генерацией и получением уникальной ссылки на него", notes = noteSetDocumentAccessLink )
    @RequestMapping(value = "/setDocumentLink", method = RequestMethod.GET, headers = { "Accept=application/json" })
    public
    @ResponseBody
    AccessURL setDocumentAccessLink(
	    @ApiParam(value = "ИД-номер документа", required = true) @RequestParam(value = "nID_Document") Long nID_Document,
	    @ApiParam(value = "ФИО, кому доступ", required = false) @RequestParam(value = "sFIO", required = false) String sFIO,
	    @ApiParam(value = "цель получения доступа", required = false) @RequestParam(value = "sTarget", required = false) String sTarget,
	    @ApiParam(value = "телефон того, кому доступ предоставляется", required = false) @RequestParam(value = "sTelephone", required = false) String sTelephone,
	    @ApiParam(value = "число милисекунд, на которое предоставляется доступ", required = true) @RequestParam(value = "nMS") Long nMS,
	    @ApiParam(value = "эл. почта того, кому доступ предоставляется", required = false) @RequestParam(value = "sMail", required = false) String sMail,
            @ApiParam(value = "ID авторизированого субъекта (добавляется в запрос автоматически после аутентификации пользователя)", required = true) @RequestParam(value = "nID_Subject") Long nID_Subject,
            HttpServletResponse response) throws ActivitiRestException {
        
        Document document = documentDao.getDocument(nID_Document);
        
        if(!nID_Subject.equals(document.getSubject().getId()))
       {
             throw new ActivitiRestException(UNAUTHORIZED_ERROR_CODE, NO_ACCESS_MESSAGE, HttpStatus.UNAUTHORIZED);
        }
        
        AccessURL oAccessURL = new AccessURL();
        try {
            oAccessURL.setName("sURL");
            String sValue = documentAccessDao.setDocumentLink(nID_Document, sFIO, sTarget, sTelephone, nMS, sMail);
            oAccessURL.setValue(sValue);

            createHistoryEvent(HistoryEventType.SET_DOCUMENT_ACCESS_LINK,
                    nID_Document, sFIO, sTelephone, nMS, sMail);
        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setHeader(REASON_HEADER, e.getMessage());
            oLog.error(e.getMessage(), e);
        }
        return oAccessURL;
    }

    /**
     * проверка доступа к документу и получения данных о нем, если доступ есть
     * @param nID_Document ИД-номер документа
     * @param sSecret секретный ключ
     * @param nID_Subject ID авторизированого субъекта (добавляется в запрос автоматически после аутентификации пользователя)
     */
    @Deprecated
    @ApiOperation(value = "проверка доступа к документу и получения данных о нем, если доступ есть", notes = noteGetDocumentAccessLink )
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Возникла исключительная ситуация"),
	@ApiResponse(code = 403, message = "Нет доступа / доступ есть, но секрет не совпадает /  доступ просрочен") } )
    @RequestMapping(value = "/getDocumentLink", method = RequestMethod.GET, headers = { "Accept=application/json" })
    public
    @ResponseBody
    DocumentAccess getDocumentAccessLink(
	    @ApiParam(value = "уточнить описание", required = true) @RequestParam(value = "nID_Access") Long nID_Access,
	    @ApiParam(value = "секретный ключ", required = true) @RequestParam(value = "sSecret") String sSecret,
            HttpServletResponse response) {
        DocumentAccess da = null;
        try {
            da = documentAccessDao.getDocumentLink(nID_Access, sSecret);
        } catch (Exception e) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setHeader(REASON_HEADER, "Access not found\n" + e.getMessage());
            oLog.error(e.getMessage(), e);
        }
        if (da == null) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setHeader(REASON_HEADER, "Access not found");
        } else {
            DateTime now = new DateTime();
            boolean isSuccessAccess = true;
            DateTime d = da.getDateCreate();

            if (d.plusMillis(da.getMS().intValue()).isBefore(now)) {
                isSuccessAccess = false;
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setHeader(REASON_HEADER, "Access expired");
            }
            if (!sSecret.equals(da.getSecret())) {
                isSuccessAccess = false;
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setHeader(REASON_HEADER, "Access to another document");
            }
            if (isSuccessAccess) {
                createHistoryEvent(HistoryEventType.SET_DOCUMENT_ACCESS,
                        da.getID_Document(), da.getFIO(), da.getTelephone(), da.getMS(), da.getMail());
            }
        }

        return da;
    }

    /**
     * Получение подтверждения на доступ к документу(с отсылкой СМС ОТП-паролем на телефон))
     * @param nID_Document ИД-номер документа
     * @param sSecret секретный ключ
     * @param nID_Subject ID авторизированого субъекта (добавляется в запрос автоматически после аутентификации пользователя)
     */
    @Deprecated
    @ApiOperation(value = "Получение подтверждения на доступ к документу(с отсылкой СМС ОТП-паролем на телефон))", notes = noteGetDocumentAccess )
    @RequestMapping(value = "/getDocumentAccess", method = RequestMethod.GET, headers = { "Accept=application/json" })
    public
    @ResponseBody
    AccessURL getDocumentAccess(
	    @ApiParam(value = "уточнить описание", required = true) @RequestParam(value = "nID_Access") Long nID_Access,
	    @ApiParam(value = "секретный ключ", required = true) @RequestParam(value = "sSecret") String sSecret,
            HttpServletResponse response) {
        AccessURL oAccessURL = new AccessURL();
        try {
            oAccessURL.setName("sURL");
            //sValue = documentAccessDao.getDocumentAccess(nID_Access,sSecret);
            documentAccessDao.getDocumentAccess(nID_Access, sSecret);
            String sValue = String.valueOf(nID_Access);
            oAccessURL.setValue(sValue);
        } catch (Exception e) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setHeader(REASON_HEADER, "Access not found");
            oAccessURL.setValue(e.getMessage());
            oLog.error(e.getMessage(), e);
        }
        return oAccessURL;
    }

    /**
     * Установка подтверждения на доступ к документу, по введенному коду, из СМС-ки(ОТП-паролем), и возвратом уникальной разовой ссылки на докуемнт.
     * @param nID_Access ид доступа
     * @param sSecret секретный ключ
     * @param sAnswer ответ (введенный пользователем ОТП-пароль из СМС)
     * @param nID_Subject ID авторизированого субъекта (добавляется в запрос автоматически после аутентификации пользователя)
     */
    @Deprecated
    @ApiOperation(value = "Установка подтверждения на доступ к документу, по введенному коду, из СМС-ки(ОТП-паролем)", notes = noteSetDocumentAccess )
    @RequestMapping(value = "/setDocumentAccess", method = RequestMethod.GET, headers = { "Accept=application/json" })
    public
    @ResponseBody
    AccessURL setDocumentAccess(
	    @ApiParam(value = "ид доступа", required = true) @RequestParam(value = "nID_Access") Long nID_Access,
	    @ApiParam(value = "секретный ключ", required = true) @RequestParam(value = "sSecret") String sSecret,
	    @ApiParam(value = "ответ (введенный пользователем ОТП-пароль из СМС)", required = true) @RequestParam(value = "sAnswer") String sAnswer,
            HttpServletResponse response) {
        AccessURL oAccessURL = new AccessURL();
        try {
            String sValue;
            sValue = documentAccessDao.setDocumentAccess(nID_Access, sSecret, sAnswer);
            oAccessURL.setValue(sValue);
            oAccessURL.setName("sURL");
            if (oAccessURL.getValue().isEmpty() || oAccessURL.getValue() == null) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setHeader(REASON_HEADER, "Access not found");
            }
        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setHeader(REASON_HEADER, e.getMessage());
            oLog.error(e.getMessage(), e);
        }
        return oAccessURL;
    }

    private void createHistoryEvent(HistoryEventType eventType, Long documentId,
            String sFIO, String sPhone, Long nMs, String sEmail) {
        Map<String, String> values = new HashMap<>();
        try {
            values.put(HistoryEventMessage.FIO, sFIO);
            values.put(HistoryEventMessage.TELEPHONE, sPhone);
            values.put(HistoryEventMessage.EMAIL, sEmail);
            values.put(HistoryEventMessage.DAYS, "" + TimeUnit.MILLISECONDS.toDays(nMs));

            Document oDocument = documentDao.getDocument(documentId);
            values.put(HistoryEventMessage.DOCUMENT_NAME, oDocument.getName());
            values.put(HistoryEventMessage.DOCUMENT_TYPE, oDocument.getDocumentType().getName());
            documentId = oDocument.getSubject().getId();
        } catch (Exception e) {
            oLog.warn("can't get document info!", e);
        }
        try {
            String eventMessage = HistoryEventMessage.createJournalMessage(eventType, values);
            historyEventDao.setHistoryEvent(documentId, eventType.getnID(),
                    eventMessage, eventMessage);
        } catch (IOException e) {
            oLog.error("error during creating HistoryEvent", e);
        }
    }
}
