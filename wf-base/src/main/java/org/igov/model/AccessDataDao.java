package org.igov.model;

public interface AccessDataDao {

    public String setAccessData(String content);

    public String setAccessData(byte[] content);

    public String getAccessData(String sKey);

    public boolean removeAccessData(String sKey);
}
