package priv.zujool.time;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateTimeUtils {

    public static boolean checkDateTimeParttern(String dateTime, DateTimeFormatter formatter){
        try {
            LocalDateTime.parse(dateTime, formatter);
        }catch (DateTimeParseException e){
            return false;
        }
        return true;
    }

}
