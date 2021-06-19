package main;

import main.enums.ModerationStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class GeneralMethods
{
    public static Date getCurrentTimeUTC()  {
        SimpleDateFormat dateFormatUTC = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        dateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat dateFormatLocal = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        Date date = null;
        try {
            date = dateFormatLocal.parse(dateFormatUTC.format(new Date()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static Date getStartDayTimeUTC(Date date)  {
        SimpleDateFormat dateFormatUTC = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        dateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat dateFormatLocal = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        Date startDay = null;
        try {
            startDay = dateFormatLocal.parse(dateFormatUTC.format(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return startDay;
    }

    public static Date getEndDayTimeUTC(Date date)  {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return cal.getTime();
    }

    public static ModerationStatus getModerationStatusByName(String modStatusName)
    {
        try {
            return ModerationStatus.valueOf(modStatusName.toUpperCase());
        }catch (Exception e)
        {
            return null;
        }
    }
    public static String toGenerateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        return new BigInteger(length, random).toString(32);
    }

}
