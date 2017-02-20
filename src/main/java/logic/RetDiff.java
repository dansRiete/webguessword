package logic;

import java.math.BigDecimal;

/**
 * Created by Aleks on 21.05.2016.
 * Класс содержит один метод retDiffInTime(long l) который возвращает относительную разницу даты
 */

public class RetDiff {
    private static final long oneMinute = 1000 * 60;
    private static final long oneHour = oneMinute * 60;
    private static final long oneDay = oneHour * 24;
    private static final long oneMonth = oneMinute * 43830;
    private static final long oneYear = oneMonth * 12;

    public String retDiffInTime(long diff) {

        if (diff < oneMinute) {
            //in seconds
            return diff / 1000 + (diff / 1000 > 1 ? " seconds ago" : " second ago");
        } else if (diff < oneHour) {
            //in minutes
            return diff / oneMinute + (diff / oneMinute > 1 ? " minutes ago" : " minute ago");
        } else if (diff < oneDay) {
            //in hours
            return diff / oneHour + (diff / oneHour > 1 ? " hours ago" : " hour ago");
        } else if (diff < oneMonth) {
            //in days
            return diff / oneDay + (diff / oneDay > 1 ? " days ago" : " day ago");
        } else if (diff < oneYear) {
            //in months
            return diff / oneMonth + (diff / oneMonth > 1 ? " months ago" : " month ago");
        } else {
            //in years
            return new BigDecimal((double) diff / (double) oneYear).setScale(1, BigDecimal.ROUND_HALF_UP).toString() + " years ago";
        }
    }

}