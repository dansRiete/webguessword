package utils;

import java.math.BigDecimal;

/**
 * Created by Aleks on 21.05.2016.
 * Is used for returning the string representation of the time difference (seconds, minutes, hours ... years)
 */

public class TimeDifference {
    private static final long oneMinute = 1000 * 60;
    private static final long oneHour = oneMinute * 60;
    private static final long oneDay = oneHour * 24;
    private static final long oneMonth = oneMinute * 43830;
    private static final long oneYear = oneMonth * 12;

    /**
     * @param milliseconds Time difference in milliseconds
     * @return Returns the string representation of the time difference (seconds, minutes, hours ... years)
     */
    public static String difference(long milliseconds) {

        if (milliseconds < oneMinute) {
            //in seconds
            return milliseconds / 1000 + (milliseconds / 1000 > 1 ? " seconds ago" : " second ago");
        } else if (milliseconds < oneHour) {
            //in minutes
            return milliseconds / oneMinute + (milliseconds / oneMinute > 1 ? " minutes ago" : " minute ago");
        } else if (milliseconds < oneDay) {
            //in hours
            return milliseconds / oneHour + (milliseconds / oneHour > 1 ? " hours ago" : " hour ago");
        } else if (milliseconds < oneMonth) {
            //in days
            return milliseconds / oneDay + (milliseconds / oneDay > 1 ? " days ago" : " day ago");
        } else if (milliseconds < oneYear) {
            //in months
            return milliseconds / oneMonth + (milliseconds / oneMonth > 1 ? " months ago" : " month ago");
        } else {
            //in years
            return new BigDecimal((double) milliseconds / (double) oneYear).setScale(1, BigDecimal.ROUND_HALF_UP).toString() + " years ago";
        }
    }

}