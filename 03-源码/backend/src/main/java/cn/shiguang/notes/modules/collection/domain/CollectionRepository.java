package cn.shiguang.notes.modules.collection.domain;
import java.util.*;
public interface CollectionRepository {
    Map<String,Object> folder(long id,boolean lock);
    List<Map<String,Object>> folders(long owner,long before,int size);
    long createFolder(long owner,String name);
    void renameFolder(long id,String name,int version);
    void deleteFolder(long id);
    boolean favorite(long user,long folder,long note,boolean desired);
    boolean hasFavorite(long user,long note);
    List<Map<String,Object>> favorites(long folder,long before,int size);
    long createPractice(long owner,long note);
    Map<String,Object> practice(long id,boolean lock);
    List<Map<String,Object>> practices(long owner,long before,int size);
    void transition(long id,long actor,String from,String to);
    void result(long id,String text,String helpfulness);
    void publicState(long id,String state,Integer sourceRevision,boolean newRevision);
    List<Map<String,Object>> history(long id,long before,int size);
}
