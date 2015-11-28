package org.activiti.rest.controller;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.wf.dp.dniprorada.dao.SubjectOrganJoinTaxDao;
import org.wf.dp.dniprorada.model.SubjectOrganJoinTax;

import java.util.List;

/**
 * @author NickVeremeichyk
 * @since 2015-11-24.
 */

@Controller
@RequestMapping(value = "/services")
public class ActivityRestSubjectOrganJoinTaxController {
    private static final Logger LOG = Logger.getLogger(ActivityRestSubjectOrganJoinTaxController.class);

    @Autowired
    private SubjectOrganJoinTaxDao subjectOrganJoinTaxDao;

    /**
     * Отдает массив объектов сущности
     *
     * @return список
     */
    @RequestMapping(value = "/getSubjectOrganJoinTaxes")
    @ResponseBody
    public List<SubjectOrganJoinTax> getSubjectOrganJoinTax() {
        return subjectOrganJoinTaxDao.findAll();
    }

    /**
     * Апдейтит элемент(если задан один из уникальных-ключей) или вставляет (если не задан nID), и отдает экземпляр нового объекта параметры.
     *
     * @param nID      (опциональный, если другой уникальный-ключ задан и по нему найдена запись)
     * @param sID_UA   (опциональный, если другой уникальный-ключ задан и по нему найдена запись)
     * @param sName_UA (опциональный, если nID задан и по нему найдена запись)
     * @return SubjectOrganJoinTax object
     */
    @RequestMapping(value = "/setSubjectOrganJoinTax")
    @ResponseBody
    public SubjectOrganJoinTax setSubjectOrganJoinTax(@RequestParam(value = "nID", required = false) Long nID,
                                           @RequestParam(value = "sID_UA", required = false) String sID_UA,
                                           @RequestParam(value = "sName_UA", required = false) String sName_UA) {
        return subjectOrganJoinTaxDao.setSubjectOrganJoinTax(nID, sID_UA, sName_UA);
    }

    /**
     * Удаляет элемент(по ОБЯЗАТЕЛЬНО заданому одому из уникальных-ключей) параметры
     *
     * @param nID    (опциональный, если другой уникальный-ключ задан и по нему найдена запись)
     * @param sID_UA (опциональный, если другой уникальный-ключ задан и по нему найдена запись)
     */
    @RequestMapping(value = "/removeSubjectOrganJoinTax")
    @ResponseBody
    public void removeSubjectOrganJoinTax(@RequestParam(value = "nID", required = false) Long nID,
                                          @RequestParam(value = "sID_UA", required = false) String sID_UA) {
        subjectOrganJoinTaxDao.removeByKey(nID, sID_UA);
    }


}