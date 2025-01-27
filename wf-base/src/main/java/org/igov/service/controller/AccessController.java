package org.igov.service.controller;

import org.igov.service.interceptor.exception.ActivitiRestException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.igov.model.access.AccessService;
import org.igov.model.access.HandlerBeanValidationException;
import org.igov.util.convert.JsonRestUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ApiResponse;
import javax.servlet.http.HttpServletRequest;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.activiti.engine.ProcessEngines;
import org.igov.service.entity.LoginResponse;
import org.igov.service.entity.LoginResponseI;
import org.igov.service.entity.LogoutResponse;
import org.igov.service.entity.LogoutResponseI;
import org.igov.service.interceptor.exception.ActivitiAuthException;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * User: goodg_000
 * Date: 06.10.2015
 * Time: 22:57
 */
@Controller
@Api(tags = { "ActivitiRestAccessController" }, description = "Получение и установка прав доступа к rest сервисам")
@RequestMapping(value = "/access")
public class AccessController {

    private static final Logger oLog = Logger.getLogger(AccessController.class);
    
    @Autowired
    private AccessService accessService;

    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Подробные описания сервисов для документирования в Swagger
    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String noteCODE= "\n```\n";    
    private static final String noteCODEJSON= "\n```json\n";    
    private static final String noteController ="##### Аутентификация пользователя. ";

    private static final String noteLogin = noteController
    		+ "Логин пользователя. #####\n\n"
            + "Request:\n"
            + noteCODE 
            + "  sLogin=user&sPassword=password\n"
            + noteCODE 
            + "Response:\n"
            + noteCODEJSON 
            + "  {\"session\":\"true\"}\n"
    		+ noteCODE 
    		+ "где:\n"
    		+ "- **true** - Пользователь авторизирован\n"
    		+ "- **false** - Имя пользователя или пароль не корректны\n"
    		+ "Пример:\n"
            + "https://test.region.igov.org.ua/wf/access/login?sLogin=kermit&sPassword=kermit";
    
    private static final String noteLogout = noteController 
    		+ "Логаут пользователя (наличие cookie JSESSIONID) #####\n"
            + "Response:\n"
            + noteCODEJSON 
            + "  {\"session\":\"97AE7CA414A5DA85749FE379CC843796\"}\n"
    		+ noteCODE;
	///////////////////////////////////////////////////////////////////////////

    /**
     * Логин пользователя в систему. Возращает признак успеха/неудачи входа.
     * true - Пользователь авторизирован
     * false - Имя пользователя или пароль не корректны
     *
     * @param login    - Логин пользователя
     * @param password - Пароль пользователя
     * @return {"session":"true"} -- Пользователь авторизирован
     * OR  {"session":"false"}- Имя пользователя или пароль не корректны
     * @throws ActivitiAuthException
     */
    @ApiOperation(value = "Логин пользователя", notes = noteLogin)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Возращает признак успеха/неудачи входа") })
    @RequestMapping(value = { "/login", "/login-v2" }, method = RequestMethod.POST)
    public
    @ResponseBody
    LoginResponseI login(
    		@ApiParam(value = "Логин пользователя", required = true) @RequestParam(value = "sLogin") String login,
    		@ApiParam(value = "Пароль пользователя", required = true) @RequestParam(value = "sPassword") String password, HttpServletRequest request)
            throws ActivitiAuthException {
        if (ProcessEngines.getDefaultProcessEngine().getIdentityService().checkPassword(login, password)) {
            request.getSession(true);
            return new LoginResponse(Boolean.TRUE.toString());
        } else {
            throw new ActivitiAuthException(ActivitiAuthException.Error.LOGIN_ERROR, "Login or password invalid");
        }
    }

    /**
     * Логаут пользователя (наличие cookie JSESSIONID):
     */
    @ApiOperation(value = "Логаут пользователя", notes = noteLogout)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Возращает JSESSIONID") })
    @RequestMapping(value = "/logout", method = { RequestMethod.DELETE, RequestMethod.POST })
    public
    @ResponseBody
    LogoutResponseI logout(HttpServletRequest request) throws ActivitiAuthException {
        HttpSession session = request.getSession();
        if (session.isNew()) {
            throw new ActivitiAuthException(ActivitiAuthException.Error.LOGOUT_ERROR,
                    "Client doesn't have a valid server session");
        } else {
            session.invalidate();
            return new LogoutResponse(session.getId());
        }
    }
    
    
    
    /**
     * @param sLogin имя пользователя
     */
    @ApiOperation(value = "Возврат списка сервисов доступных пользователю", notes = "#####  Получение и установка прав доступа к rest-сервисам. "
      + "Возврат списка сервисов доступных пользователю #####\n\n"
      + "возвращает список всех сервисов доступных пользователю с именем sLogin с формате JSON.\n"
      + "Request:\n"
      + "\n```\n" 
      + "  sLogin=TestLogin\n"
      + "\n```\n" 
      + "Response:\n"
      + "\n```json\n" 
      + "  [\n"
      + "    \"TestService\"\n"
      + "  ]\n"
      + "\n```\n"
      + "Пример:\n"
      + "https://test.region.igov.org.ua/wf/service/access/getAccessServiceLoginRight?sLogin=TestLogin")
    @RequestMapping(value = "/getAccessServiceLoginRight", method = RequestMethod.GET)
    public ResponseEntity getAccessServiceLoginRight(@ApiParam(value = "Логин пользователя", required = true) @RequestParam(value = "sLogin") String sLogin) {
        return JsonRestUtils.toJsonResponse(accessService.getAccessibleServices(sLogin));
    }

    /**
     * @param sLogin имя пользователя
     * @param sService строка сервиса
     * @param sHandlerBean опцинальный параметр: имя спрингового бина реализующего интерфейс AccessServiceLoginRightHandler, который будет заниматься проверкой прав доступа для данной записи. При сохранении проверяется наличие такого бина, и если его нет - то будет выброшена ошибка.
     */
    @ApiOperation(value = "Сохранение разрешения на доступ к сервису для пользователя", notes = "#####  Получение и установка прав доступа к rest-сервисам. "    		
		+ "Сохранение разрешения на доступ к сервису для пользователя #####\n\n"
		+ "Сохраняет запись в базе, что пользователь sLogin имеет доступ к сервису sService. Существование такого пользователя и сервиса не проверяется.\n\n"
		+ "- sLogin - имя пользователя\n"
		+ "- sService - строка сервиса\n"
		+ "- sHandlerBean - опцинальный параметр: имя спрингового бина реализующего интерфейс AccessServiceLoginRightHandler, который будет "
		+ "заниматься проверкой прав доступа для данной записи. При сохранении проверяется наличие такого бина, и если его нет - то будет выброшена ошибка.\n\n"
		+ "Примеры:\n"
		+ "https://test.region.igov.org.ua/wf/service/access/setAccessServiceLoginRight\n\n"
		+ "- sLogin=SomeLogin\n"
		+ "- sService=access/hasAccessServiceLoginRight\n\n"
		+ "\n```\n"
		+ "  Ответ: Status 200\n"
		+ "\n```\n"
		+ "- sLogin=SomeLogin\n"
		+ "- sService=access/hasAccessServiceLoginRight\n"
		+ "- sHandlerBean=WrongBean\n"
		+ "Ответ:\n\n"
		+ "\n```json\n"
		+ "  {\n"
		+ "    \"code\": \"SYSTEM_ERR\",\n"
		+ "    \"message\": \"No bean named 'WrongBean' is defined\"\n"
		+ "  }\n"
		+ "\n```\n")
    @RequestMapping(value = "/setAccessServiceLoginRight", method = RequestMethod.POST)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Ошибка бизнес процесса")} )
    public void setAccessServiceLoginRight(@ApiParam(value = "Логин пользователя", required = true) @RequestParam(value = "sLogin") String sLogin,
    		@ApiParam(value = "Строка сервиса", required = true) @RequestParam(value = "sService") String sService,
    		@ApiParam(value = "Имя спрингового бина реализующего интерфейс AccessServiceLoginRightHandler", required = false) @RequestParam(value = "sHandlerBean", required = false) String sHandlerBean,
            HttpServletResponse response)
            throws ActivitiRestException {
        try {

            accessService.saveOrUpdateAccessServiceLoginRight(sLogin, sService, sHandlerBean);
            response.setStatus(HttpStatus.OK.value());

        } catch (HandlerBeanValidationException e) {
            oLog.warn(e.getMessage(), e);
            throw new ActivitiRestException(ActivitiExceptionController.BUSINESS_ERROR_CODE, e.getMessage());
        }
    }

    /**
     * @param sLogin имя пользователя
     * @param sService строка сервиса
     */
    @ApiOperation(value = "Удаление разрешения на доступ к сервису для пользователя", notes = "#####  Получение и установка прав доступа к rest-сервисам. "    
		+ "Удаление разрешения на доступ к сервису для пользователя #####\n\n"
		+ "Удаляет запись из базы, что пользователь sLogin имеет доступ к сервису sService."
		+ "Статус код 200 означает что запись успешно удалена. Код 304 - что такая запись не найдена.\n\n"
		+ "Примеры:\n\n"
		+ "https://test.region.igov.org.ua/wf/service/access/removeAccessServiceLoginRight?sLogin=TestLogin&sService=TestService\n\n"
		+ "\n```\n"
			+ "  Ответ: Status 200\n"
		+ "\n```\n"
			+ "https://test.region.igov.org.ua/wf/service/access/removeAccessServiceLoginRight?sLogin=FakeLogin&sService=TestService\n"
		+ "\n```\n"
			+ "  Ответ: Status 304\n"
		+ "\n```\n")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Запись успешно удалена"),
    	      @ApiResponse(code = 304, message = "Такая запись не найдена") })
    @RequestMapping(value = "/removeAccessServiceLoginRight", method = RequestMethod.DELETE)
    public void setAccessServiceLoginRight(@ApiParam(value = "Логин пользователя", required = true) @RequestParam(value = "sLogin") String sLogin,
    		@ApiParam(value = "Строка сервиса", required = true) @RequestParam(value = "sService") String sService,
            HttpServletResponse response) {
        if (accessService.removeAccessServiceLoginRight(sLogin, sService)) {
            response.setStatus(HttpStatus.OK.value());
        } else {
            response.setStatus(HttpStatus.NOT_MODIFIED.value());
        }
    }

    /**
     * @param sLogin имя пользователя для которого проверяется доступ
     * @param sService строка сервиса
     * @param sData опциональный параметр со строкой параметров к сервису (формат передачи пока не определен). Если задан бин sHandlerBean (см. ниже) то он может взять на себя проверку допуспности сервиса для данного набора параметров.
     */
    @ApiOperation(value = "Проверка разрешения на доступ к сервису для пользователя", notes = "#####  Получение и установка прав доступа к rest-сервисам. "    		
		+ "Проверка разрешения на доступ к сервису для пользователя #####\n\n"
		+ "возвращает true - если у пользоватля с логином sLogin есть доступ к рест сервиу sService при вызове его с аргументами sData, или false - если доступа нет.\n\n"
		
			+ "- sLogin - имя пользователя для которого проверяется доступ\n"
			+ "- sService - строка сервиса\n"
			+ "- sData - опциональный параметр со строкой параметров к сервису (формат передачи пока не определен). "
			+ "Если задан бин sHandlerBean (см. ниже) то он может взять на себя проверку допуспности сервиса для данного набора параметров.\n\n"
		+ "Пример:\n"
        + "https://test.region.igov.org.ua/wf/service/access/hasAccessServiceLoginRight?sLogin=SomeLogin&sService=access/hasAccessServiceLoginRight\n"
		+ "\n```\n"
		+ "Ответ false\n"
		+ "\n```\n")
    @RequestMapping(value = "/hasAccessServiceLoginRight", method = RequestMethod.GET)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Ошибка бизнес процесса")} )
    public ResponseEntity hasAccessServiceLoginRight(@ApiParam(value = "Логин пользователя", required = true) @RequestParam(value = "sLogin") String sLogin,
    		@ApiParam(value = "строка сервиса", required = true) @RequestParam(value = "sService") String sService,
    		@ApiParam(value = "строка параметров к сервису", required = false) @RequestParam(value = "sData", required = false) String sData)
            throws ActivitiRestException {

        try {
            return JsonRestUtils.toJsonResponse(accessService.hasAccessToService(sLogin, sService, sData));
        } catch (HandlerBeanValidationException e) {
            oLog.warn(e.getMessage(), e);
            throw new ActivitiRestException(ActivitiExceptionController.BUSINESS_ERROR_CODE, e.getMessage());
        }
    }
}
