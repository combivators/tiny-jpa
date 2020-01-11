package net.tiny.dao.converter;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Normalize {

    /**
     * NFKC 规范化
     */
    public static String normalize(String raw) {
        return Normalizer.normalize(raw, Normalizer.Form.NFKC);
    }

    /**
     * 电话号码标准化
     */
    public String tel(String raw) {
      // 全角を半角英数に変換
      String ret = normalize(raw);
      return ret.replaceAll("-", "")  // ハイフンを取り除く
                .replaceAll("\\s", ""); // 空白を取り除く
    }

    /**
     * 地址规范化
     */
    public String address(String raw) {
        // 全角英数記号を半角英数記号に変換 半角カタカナを全角に変換
        String ret = normalize(raw);
        // 特殊なハイフン
        ret = ret.replaceAll("−", "-");

        // X丁目/Y番(地)/Z(号) => X-Y-Z
        Matcher m1 = Pattern.compile("(?<PREFIX>.*)(?<CHOME>\\d+)丁目(?<BANCHI>\\d+)番(地?)(?<GO>\\d+)(号?)(?<SUFFIX>.*)").matcher(ret);
        ret = (m1.find()) ? m1.group("PREFIX") + m1.group("CHOME") + "-" + m1.group("BANCHI") + "-" + m1
            .group("GO") + m1.group("SUFFIX") : ret;

        // X丁目Y(番|番地)     => X-Y
        Matcher m2 = Pattern.compile("(?<PREFIX>.*)(?<CHOME>\\d+)丁目(?<BANCHI>\\d+)(番(地?))(?<SUFFIX>.*)").matcher(ret);
        ret = (m2.find()) ? m2.group("PREFIX") + m2.group("CHOME") + "-" + m2.group("BANCHI") + m2
            .group("SUFFIX") : ret;

        // X丁目(左|右)Y(号)   => X-Y
        Matcher m3 = Pattern.compile("(?<PREFIX>.*)(?<CHOME>\\d+)丁目(左?|右?)(?<GO>\\d+)(号?)(?<SUFFIX>.*)").matcher(ret);
        ret = (m3.find()) ? m3.group("PREFIX") + m3.group("CHOME") + "-" + m3.group("GO") + m3
            .group("SUFFIX") : ret;

        // X番(地)(の)Y(号)    => X-Y
        Matcher m4 = Pattern.compile("(?<PREFIX>.*)(?<BANCHI>\\d+)番(地?)(の?)(?<GO>\\d+)(号?)(?<SUFFIX>.*)").matcher(ret);
        ret = (m4.find()) ? m4.group("PREFIX") + m4.group("BANCHI") + "-" + m4.group("GO") + m4
            .group("SUFFIX") : ret;

        // XのY               => X-Y
        Matcher m5 = Pattern.compile("(?<PREFIX>.*)(?<BANCHI>\\d+)の(?<GO>\\d+)(?<SUFFIX>.*)").matcher(ret);
        ret = (m5.find()) ? m5.group("PREFIX") + m5.group("BANCHI") + "-" + m5.group("GO") + m5
            .group("SUFFIX") : ret;

        // X丁目, X番(地), X号  => X
        Matcher m6 = Pattern.compile("(?<PREFIX>.*)(?<CHOME>\\d+)(丁目|番(地?)|号)(?<SUFFIX>.*)").matcher(ret);
        ret = (m6.find()) ? m6.group("PREFIX") + m6.group("CHOME") + m6.group("SUFFIX") : ret;

        // 空白を取り除く
        ret = ret.replaceAll("\\s", "");
        return ret;
    }
}
