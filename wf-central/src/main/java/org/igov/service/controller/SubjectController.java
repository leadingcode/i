package org.igov.service.controller;

import io.swagger.annotations.*;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.igov.model.SubjectDao;
import org.igov.model.SubjectHumanDao;
import org.igov.model.SubjectOrganDao;
import org.igov.model.Subject;
import org.igov.model.SubjectHuman;
import org.igov.model.SubjectHumanIdType;
import org.igov.model.SubjectOrgan;

import com.google.common.base.Optional;

import javax.servlet.http.HttpServletResponse;
import org.igov.service.controller.ActivitiExceptionController;
import org.igov.service.interceptor.exception.ActivitiRestException;

@Controller
@Api(tags = { "ActivitiRestSubjectController" }, description = "Работа с субъектами")
@RequestMapping(value = "/subject")
public class SubjectController {

    private static final Logger oLog = LoggerFactory.getLogger(SubjectController.class);

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Подробные описания сервисов для документирования в Swagger
    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String noteCODE= "\n```\n";    
    private static final String noteCODEJSON= "\n```json\n";    
    private static final String noteController = "##### Работа с субъектами. ";

    private static final String noteSyncSubject = noteController + "Получение субъекта #####\n\n"
		+ "HTTP Context: http://server:port/wf/service/subject/syncSubject\n\n\n"
		+ "Если субъект найден, или добавление субъекта в противном случае\n\n"

		+ "От клиента ожидается ОДИН и только ОДИН параметр из нижеперечисленных\n\n"

		+ "- nID - ИД-номер субъекта\n"
		+ "- sINN - строка-ИНН (субъект - человек)\n"
		+ "- sOKPO - строка-ОКПО (субъек - организация)\n"
		+ "- nID_Subject - ID авторизированого субъекта (добавляется в запрос автоматически после аутентификации пользователя)\n\n\n"
		+ "Примеры:\n\n"
		+ "https://test.igov.org.ua/wf/service/subject/syncSubject?sINN=34125265377\n\n"
		+ "https://test.igov.org.ua/wf/service/subject/syncSubject?sOKPO=123\n\n"
		+ "https://test.igov.org.ua/wf/service/subject/syncSubject?nID=1\n\n"
		+ "Response\n"
		+ noteCODEJSON
		+ "{\n"
		+ "    \"nID\":150,\n"
		+ "    \"sID\":\"34125265377\",\n"
		+ "    \"sLabel\":null,\n"
		+ "    \"sLabelShort\":null\n"
		+ "}\n"
		+ noteCODE;

    private static final String noteSetSubjectHuman = noteController + "Нет описания #####\n\n";
 
    private static final String noteSetSubjectOrgan = noteController + "Нет описания #####\n\n";

    private static final String noteGetSubjectHuman = noteController + "Получение объекта SubjectHuman по номеру #####\n\n"
    		+ "HTTP Context: http://server:port/wf/service/subject/getSubjectHuman\n\n\n"
		+ "Параметр\n\n"
		+ "- nID_Subject - ИД-номер субъекта\n"
		+ "Если объект с заданным номером на найден - возвращается код 404\n"
		+ "Примеры:\n\n"
		+ "https://test.igov.org.ua/wf/service/subject/getSubjectHuman?nID_Subject=1\n\n"
		+ "Response\n"
		+ noteCODEJSON
		+ "{\"oSubject\":\n"
				+ "\t{\"sID\":\"2872618515\",\n"
				+ "\t\"sLabel\":\"Белявцев Владимир Владимирович\",\n"
				+ "\t\"sLabelShort\":\"Белявцев В. В.\",\n"
				+ "\t\"nID\":2},\n"
		+ "\"sINN\":\"2872618515\",\n"
		+ "\"sSB\":\"314542353425125\",\n"
		+ "\"sPassportSeria\":\"AN\",\n"
		+ "\"sPassportNumber\":\"11223344\",\n"
		+ "\"sFamily\":\"Белявцев\",\n"
		+ "\"sSurname\":\"Владимирович\",\n"
		+ "\"nID\":1,\n"
		+ "\"sName\":\"Владимир\"}\n"
		+ noteCODE;

    
    private static final String noteGetSubjectOrgan = noteController + "Получение объекта SubjectOrgan по номеру #####\n\n"
    		+ "HTTP Context: http://server:port/wf/service/subject/getSubjectOrgan\n\n\n"
		+ "Параметр\n\n"
		+ "- nID_Subject - ИД-номер субъекта\n"
		+ "Если объект с заданным номером на найден - возвращается код 404\n"
		+ "Примеры:\n\n"
		+ "https://test.igov.org.ua/wf/service/subject/getSubjectOrgan?nID_Subject=1\n\n"
		+ "Response\n"
		+ noteCODEJSON
		+ "{\"oSubject\":\n"
		+ "\t{\"sID\":\"ПАО\",\n"
		+ "\t\"sLabel\":\"ПАО ПриватБанк\",\n"
		+ "\t\"sLabelShort\":\"ПриватБанк\",\n"
		+ "\t\"nID\":1},\n"
		+ "\"sOKPO\":\"093205\",\n"
		+ "\"sFormPrivacy\":\"ПАО\",\n"
		+ "\"sNameFull\":\"Банк ПриватБанк\",\n"
		+ "\"nID\":1,\n"
		+ "\"sName\":\"ПриватБанк\"}\n"
		+ noteCODE;

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    @Autowired
    private SubjectDao subjectDao;

    @Autowired
    private SubjectHumanDao subjectHumanDao;

    @Autowired
    private SubjectOrganDao subjectOrganDao;

    /**
     * получение субъекта, если таков найден, или добавление субъекта в противном случае
     */
    @ApiOperation(value = "Получение субъекта", notes = noteSyncSubject )
    @RequestMapping(value = "/syncSubject", method = RequestMethod.GET, headers = { "Accept=application/json" })
    public
    @ResponseBody
    Subject syncSubject(
	    @ApiParam(value = "ИД-номер субъекта", required = false) @RequestParam(value = "nID", required = false) Long nID,
	    @ApiParam(value = "строка-ИНН (субъект - человек)", required = false) @RequestParam(required = false) String sINN,
        @ApiParam(value = "номер-ИД типа идентификации субьекта-человека (по умолчанию 0)", required = false)
            @RequestParam(required = false, defaultValue = "0") int nID_SubjectHumanIdType,
        @ApiParam(value = "строка-код, параметр-идентификатора субьекта (без префикса типа)", required = false)
            @RequestParam(required = false) String sCode_Subject,
        @ApiParam(value = "строка-ОКПО (субъек - организация)", required = false) @RequestParam(required = false) String sOKPO,
            HttpServletResponse httpResponse) {

        oLog.info("--- syncSubject ---");
        Subject subject;
        if (nID != null) {
            subject = subjectDao.getSubject(nID);
        } else if (StringUtils.isNotEmpty(sINN)) {
            SubjectHuman oSubjectHuman = subjectHumanDao.getSubjectHuman(sINN);
            if (oSubjectHuman == null) {
                oSubjectHuman = subjectHumanDao.saveSubjectHuman(sINN);
            }
            subject = oSubjectHuman.getoSubject();
        } else if (StringUtils.isNotEmpty(sCode_Subject)) {
            SubjectHumanIdType subjectHumanIdType = SubjectHumanIdType.fromId(nID_SubjectHumanIdType);

            SubjectHuman oSubjectHuman = subjectHumanDao.getSubjectHuman(subjectHumanIdType, sCode_Subject);
            if (oSubjectHuman == null) {
                oSubjectHuman = subjectHumanDao.saveSubjectHuman(subjectHumanIdType, sCode_Subject);
            }
            subject = oSubjectHuman.getoSubject();
        } else if (StringUtils.isNotEmpty(sOKPO)) {
            SubjectOrgan subjectOrgan = subjectOrganDao.getSubjectOrgan(sOKPO);
            if (subjectOrgan == null) {
                subjectOrgan = subjectOrganDao.setSubjectOrgan(sOKPO);
            }
            subject = subjectOrgan.getoSubject();
        }
        else {
            throw new ActivitiObjectNotFoundException(
                    "RequestParam not found! You should add nID or  sINN or sINN, " +
                            "or (nID_SubjectHumanIdType + sCode_Subject) or sOKPO param!", Subject.class);
        }
        if (subject == null) {
            throw new ActivitiObjectNotFoundException(
                    String.format("Subject not found and not created! nID = %s sINN = %s, nID_SubjectHumanIdType = %s, " +
                            "sCode_Subject = %s sOKPO = %s", nID, sINN, nID_SubjectHumanIdType, sCode_Subject, sOKPO),
                    Subject.class);
        }
        httpResponse.setHeader("Content-Type", "application/json;charset=UTF-8");
        return subject;
    }

    @ApiOperation(value = "/saveSubjectHuman", notes = noteSetSubjectHuman )
    @RequestMapping(value = "/setSubjectHuman", method = RequestMethod.POST, headers = { "Accept=application/json" })
    public
    @ResponseBody
    SubjectHuman setSubject(@RequestBody SubjectHuman subjectHuman) {
        return subjectHumanDao.saveOrUpdateHuman(subjectHuman);
    }

    @ApiOperation(value = "/setSubjectOrgan", notes = noteSetSubjectOrgan )
    @RequestMapping(value = "/setSubjectOrgan", method = RequestMethod.POST, headers = { "Accept=application/json" })
    public
    @ResponseBody
    SubjectOrgan setSubject(@RequestBody SubjectOrgan subjectOrgan) {
        return subjectOrganDao.saveOrUpdateSubjectOrgan(subjectOrgan);
    }
    
    @ApiOperation(value = "/getSubjectHuman", notes = noteGetSubjectHuman )
    @ApiResponses(value = { @ApiResponse(code = 404, message = "Record not found") } )
    @RequestMapping(value = "/getSubjectHuman", method = RequestMethod.GET)
    public @ResponseBody SubjectHuman getSubjectHuman(@ApiParam(value = "номер-ИД субьекта", required = true) @RequestParam(value = "nID_Subject") Long nID_Subject) throws ActivitiRestException {
    	Optional<SubjectHuman> subjectHuman = subjectHumanDao.findById(nID_Subject);
    	if (subjectHuman.isPresent()){
    		return subjectHuman.get();
    	}
    	throw new ActivitiRestException(
                ActivitiExceptionController.BUSINESS_ERROR_CODE,
                "Record not found",
                HttpStatus.NOT_FOUND);
    }

    @ApiOperation(value = "/getSubjectOrgan", notes = noteGetSubjectOrgan )
    @ApiResponses(value = { @ApiResponse(code = 404, message = "Record not found") } )
    @RequestMapping(value = "/getSubjectOrgan", method = RequestMethod.GET)
    public @ResponseBody SubjectOrgan getSubjectOrgan(@ApiParam(value = "номер-ИД субьекта", required = true) @RequestParam(value = "nID_Subject") Long nID_Subject) throws ActivitiRestException {
    	Optional<SubjectOrgan> subjectOrgan = subjectOrganDao.findById(nID_Subject);
    	if (subjectOrgan.isPresent()){
    		return subjectOrgan.get();
    	}
    	throw new ActivitiRestException(
                ActivitiExceptionController.BUSINESS_ERROR_CODE,
                "Record not found",
                HttpStatus.NOT_FOUND);
    }
}
