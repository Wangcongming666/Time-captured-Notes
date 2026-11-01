package cn.shiguang.notes.modules.statistics.domain;
import java.util.*;
public interface StatisticsRepository {
    boolean view(String subject,long note);
    void active(long user);
    void incrementView(long note,long author,String day);
    void metric(String day,String scope,long subject,String metric,double value,boolean increment);
    void recalculate(long note);
    List<Map<String,Object>> points(String scope,long subject,int days);
    List<Map<String,Object>> governance(int days);
    List<Map<String,Object>> ranking(Long author,long before,int size);
    List<Map<String,Object>> notes(long after,int size);
}
