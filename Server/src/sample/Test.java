package sample;

import sample.Server.InChallengeHandler;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Test {
    public static void main(String[] args) throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        System.out.println(formatter.format(date));

        Date date2 = new Date();
        date2.setTime(date2.getTime()-5000);
        System.out.println(formatter.format(date2));

        System.out.println(InChallengeHandler.checkTimeperiod(formatter.format(date2),formatter.format(date)));
    }
}
