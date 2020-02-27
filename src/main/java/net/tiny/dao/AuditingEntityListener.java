package net.tiny.dao;

import java.util.logging.Logger;

import javax.persistence.PrePersist;

public class AuditingEntityListener implements Constants {

    private static Logger LOGGER = Logger.getLogger(AuditingEntityListener.class.getName());


    @PrePersist
    public void prePresist(Object o) {
        //TODO
        //LOGGER.info("[JPA] AuditingEntityListener PrePersist " + o.getClass().getSimpleName());
    }
}
