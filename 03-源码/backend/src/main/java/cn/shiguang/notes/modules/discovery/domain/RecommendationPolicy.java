package cn.shiguang.notes.modules.discovery.domain;
/** Explainable, deterministic ranking; never interprets private practice data. */
public final class RecommendationPolicy {
    private RecommendationPolicy() {}
    public static double score(boolean interest,boolean following,double heat,double freshness,boolean reduced) {
        double value=(interest?40:0)+(following?30:0)+20*Math.clamp(heat,0,1)+10*Math.clamp(freshness,0,1);
        return reduced?value*0.1:value;
    }
    public static String reason(boolean interest,boolean following) { return interest?"INTEREST":following?"FOLLOWING":"POPULAR"; }
}
