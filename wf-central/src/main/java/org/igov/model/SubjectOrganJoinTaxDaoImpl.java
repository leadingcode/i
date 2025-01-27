package org.igov.model;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.igov.service.interceptor.exception.EntityNotFoundException;
import org.igov.model.core.GenericEntityDao;

import java.util.List;

/**
 * @author NickVeremeichyk
 * @since 2015-11-24.
 */
@Repository
public class SubjectOrganJoinTaxDaoImpl extends GenericEntityDao<SubjectOrganJoinTax> implements SubjectOrganJoinTaxDao {
    private static final Logger oLog = Logger.getLogger(SubjectOrganJoinTaxDaoImpl.class);

    protected SubjectOrganJoinTaxDaoImpl() {
        super(SubjectOrganJoinTax.class);
    }

    @Override
    public List<SubjectOrganJoinTax> getAll(String sIdUA, String sNameUA) {
        return null;
    }

    @Override
    public SubjectOrganJoinTax setSubjectOrganJoinTax(Long nId, Integer nIdSubjectOrganJoin, String sIdUA, String sNameUA) {
        SubjectOrganJoinTax subjectOrganJoinTax = getByKey(nId, nIdSubjectOrganJoin, sIdUA, sNameUA);

        if (nIdSubjectOrganJoin != null && subjectOrganJoinTax.getnIdSubjectOrganJoin() != nIdSubjectOrganJoin)
            subjectOrganJoinTax.setnIdSubjectOrganJoin(nIdSubjectOrganJoin);
        if (sIdUA != null && subjectOrganJoinTax.getsIdUA() != sIdUA)
            subjectOrganJoinTax.setsIdUA(sIdUA);
        if (sNameUA != null && !sNameUA.equals(subjectOrganJoinTax.getsNameUA()))
            subjectOrganJoinTax.setsNameUA(sNameUA);

        subjectOrganJoinTax = saveOrUpdate(subjectOrganJoinTax);
        oLog.info("country " + subjectOrganJoinTax + "is updated");
        return subjectOrganJoinTax;
    }

    @Override
    public void removeByKey(Long nId, String sIdUa) {
        SubjectOrganJoinTax subjectOrganJoinTax = getByKey(nId, null, sIdUa, null);
        if (subjectOrganJoinTax == null) {
            throw new EntityNotFoundException("Record not found!");
        } else {
            delete(subjectOrganJoinTax);
            oLog.info("subjectOrganJoinTax " + subjectOrganJoinTax + "is deleted");
        }
    }

    @Override
    public SubjectOrganJoinTax getByKey(Long nID, Integer nIdSubjectOrganJoin, String sIdUA, String sNameUA) {
        if (nID != null) {
            return findById(nID).or(new SubjectOrganJoinTax());
        } else if (sIdUA != null) {
            return findBy("sIdUA", sIdUA).or(new SubjectOrganJoinTax());
        } else if (sNameUA != null) {
            return findBy("sNameUA", sNameUA).or(new SubjectOrganJoinTax());
        } else if (nIdSubjectOrganJoin != null) {
            return findBy("nIdSubjectOrganJoin", nIdSubjectOrganJoin).or(new SubjectOrganJoinTax());
        } else
            throw new IllegalArgumentException("All args are null!");
    }
}
