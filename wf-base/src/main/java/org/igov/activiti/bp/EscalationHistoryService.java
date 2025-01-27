package org.igov.activiti.bp;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.igov.model.escalation.EscalationHistoryDao;
import org.igov.model.escalation.EscalationHistory;

/**
 * @author OlgaPrylypko
 * @since 2015-12-08.
 */

@Service
public class EscalationHistoryService {
    public static final long STATUS_CREATED = 2L;//{"sNote":"БП создан","nID":2,"sID":"BP_Created"},
    public static final long STATUS_IN_WORK = 3L;
    public static final long STATUS_CLOSED = 4L;
    private static final Logger oLog = Logger.getLogger(EscalationHistoryService.class);
    @Autowired
    EscalationHistoryDao escalationHistoryDao;

    public EscalationHistory create(Long processInstanceId, Long taskId, Long escalationInstanceId, Long status) {
        EscalationHistory escalationHistory = new EscalationHistory();
        escalationHistory.setnIdProcessRoot(processInstanceId);
        escalationHistory.setnIdProcess(escalationInstanceId);
        escalationHistory.setnIdEscalationStatus(status == null ? STATUS_CREATED : status);
        escalationHistory.setnIdUserTask(taskId);
        escalationHistory.setsDate(new DateTime());
        escalationHistoryDao.saveOrUpdate(escalationHistory);
        return escalationHistory;
    }

    public EscalationHistory updateStatus(Long escalationInstanceId, Long newStatus) {
        EscalationHistory escalationHistory = escalationHistoryDao.getByProcessId(escalationInstanceId);
        EscalationHistory newEscalationHistory = new EscalationHistory();
        newEscalationHistory.setnIdProcess(escalationInstanceId);
        if (escalationHistory == null) {
            oLog.error("entity not found for escalation process " + escalationInstanceId);
        } else {
            newEscalationHistory.setnIdUserTask(escalationHistory.getnIdUserTask());
            newEscalationHistory.setnIdProcessRoot(escalationHistory.getnIdProcessRoot());
        }
        newEscalationHistory.setnIdEscalationStatus(newStatus);
        newEscalationHistory.setsDate(new DateTime());
        escalationHistoryDao.saveOrUpdate(newEscalationHistory);
        return newEscalationHistory;
    }

}
