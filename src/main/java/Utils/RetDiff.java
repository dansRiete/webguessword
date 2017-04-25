package utils;

import java.math.BigDecimal;

/**
 * Created by Aleks on 21.05.2016.
 * Is used for returning the string representation of the time difference (seconds, minutes, hours ... years)
 */

public class RetDiff {
    private static final long oneMinute = 1000 * 60;
    private static final long oneHour = oneMinute * 60;
    private static final long oneDay = oneHour * 24;
    private static final long oneMonth = oneMinute * 43830;
    private static final long oneYear = oneMonth * 12;

    /**
     * @param msTimeDifference Time difference in milliseconds
     * @return Returns the string representation of the time difference (seconds, minutes, hours ... years)
     */
    public String retDiffInTime(long msTimeDifference) {

        if (msTimeDifference < oneMinute) {
            //in seconds
            return msTimeDifference / 1000 + (msTimeDifference / 1000 > 1 ? " seconds ago" : " second ago");
        } else if (msTimeDifference < oneHour) {
            //in minutes
            return msTimeDifference / oneMinute + (msTimeDifference / oneMinute > 1 ? " minutes ago" : " minute ago");
        } else if (msTimeDifference < oneDay) {
            //in hours
            return msTimeDifference / oneHour + (msTimeDifference / oneHour > 1 ? " hours ago" : " hour ago");
        } else if (msTimeDifference < oneMonth) {
            //in days
            return msTimeDifference / oneDay + (msTimeDifference / oneDay > 1 ? " days ago" : " day ago");
        } else if (msTimeDifference < oneYear) {
            //in months
            return msTimeDifference / oneMonth + (msTimeDifference / oneMonth > 1 ? " months ago" : " month ago");
        } else {
            //in years
            return new BigDecimal((double) msTimeDifference / (double) oneYear).setScale(1, BigDecimal.ROUND_HALF_UP).toString() + " years ago";
        }
    }

}