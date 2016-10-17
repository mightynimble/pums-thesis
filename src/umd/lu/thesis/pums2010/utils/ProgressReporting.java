package umd.lu.thesis.pums2010.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import umd.lu.thesis.common.ThesisProperties;

public class ProgressReporting {

    // <jobId, [startRow, endRow, currentRow]>
    private static Map<String, Integer[]> jobList = new HashMap<>();
    // <jobId, [startTime, lastUpdated]>
    private static Map<String, Date[]> jobEstimation = new HashMap<>();

    public static void main(String[] args) throws Exception {
        try {
            tailLog(args[0]);
        } catch (ParseException | InterruptedException | IOException e) {
            System.out.println(e.getLocalizedMessage());
            jobEstimation.clear();
            jobList.clear();
            tailLog(args[0]);
        }
    }

    public ProgressReporting() {

    }

    private static void tailLog(String emailPassword) throws ParseException, InterruptedException, IOException {
        String filePath = ThesisProperties.getProperties("simulation.pums2010.log.location");

        File logFile = new File(filePath);

        int interval = 100000;
        long lastPosition = 0;
        int counter = 0;
        try (RandomAccessFile accessor = new RandomAccessFile(logFile, "rw")) {
            while (true) {
                long fileLength = logFile.length();
                if (fileLength > lastPosition) {
                    accessor.seek(lastPosition);
                    String line;
                    while ((line = accessor.readLine()) != null) {
                        processLine(line, emailPassword);
                    }
                    lastPosition = accessor.getFilePointer();
                }
                if (counter % 72 == 0) {
                    sendMail(emailPassword);
                }
                Thread.sleep(interval);
                counter++;
            }
        } catch (IOException e) {
            throw e;
        }
    }

    private static void processLine(String line, String emailPassword) throws ParseException {
        String startPattern = ".*\\[([0-9a-f]+)\\]\\s(\\d+-\\d+-\\d+\\s\\d+:\\d+:\\d+).*Start Row: (\\d+), End Row: (\\d+).*";
        String progressPattern = ".*\\[([0-9a-f]+)\\]\\s(\\d+-\\d+-\\d+\\s\\d+:\\d+:\\d+).*Batch completed. Current row: (\\d+).*";
        String endPattern = ".*\\[([0-9a-f]+)\\]\\s(\\d+-\\d+-\\d+\\s\\d+:\\d+:\\d+).*Completed processing records. CurrentRow: (\\d+).*";

        Pattern start = Pattern.compile(startPattern);
        Pattern progress = Pattern.compile(progressPattern);
        Pattern end = Pattern.compile(endPattern);

        Matcher startMatcher = start.matcher(line);
        Matcher progressMatcher = progress.matcher(line);
        Matcher endMatcher = end.matcher(line);

        String jobId;
        Integer startRow;
        Integer endRow;
        Integer currentRow;
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("America/New York"));
        if (startMatcher.find()) {
            jobId = startMatcher.group(1);
            startRow = Integer.parseInt(startMatcher.group(3));
            endRow = Integer.parseInt(startMatcher.group(4));

            if (jobList.containsKey(jobId)) {
                throw new RuntimeException("Job ID: " + jobId + " has been already initialized in jobList.");
            } else {
                Integer[] rowCounters = new Integer[3];
                rowCounters[0] = startRow;
                rowCounters[1] = endRow;
                rowCounters[2] = startRow;
                jobList.put(jobId, rowCounters);
            }

            if (jobEstimation.containsKey(jobId)) {
                throw new RuntimeException("Job ID: " + jobId + " has been already initialized in jobEstimation.");
            } else {
                Date[] dates = new Date[2];
                dates[0] = df.parse(startMatcher.group(2));
                dates[1] = dates[0];
                jobEstimation.put(jobId, dates);
            }
        } else if (progressMatcher.find()) {
            jobId = progressMatcher.group(1);
            currentRow = Integer.parseInt(progressMatcher.group(3));

            if (jobList.containsKey(jobId)) {
                Integer[] rowCounters = jobList.get(jobId);
                rowCounters[2] = currentRow;
                jobList.put(jobId, rowCounters);
            } else {
                throw new RuntimeException("Matched 'job progress' entry in log file but job ID: " + jobId + " doesn't exist in jobList.");
            }

            if (jobEstimation.containsKey(jobId)) {
                Date[] dates = jobEstimation.get(jobId);
                dates[1] = df.parse(progressMatcher.group(2));
                jobEstimation.put(jobId, dates);
            } else {
                throw new RuntimeException("Matched 'job progress' entry in log file but job ID: " + jobId + " doesn't exist in jobEstimation.");
            }
        } else if (endMatcher.find()) {
            jobId = endMatcher.group(1);

            if (jobList.containsKey(jobId)) {
                Integer[] rowCounters = jobList.get(jobId);
                rowCounters[2] = rowCounters[1];
                jobList.put(jobId, rowCounters);
            } else {
                throw new RuntimeException("Matched 'job end' entry in log file but job ID: " + jobId + " doesn't exist in jobList.");
            }

            if (jobEstimation.containsKey(jobId)) {
                Date[] dates = jobEstimation.get(jobId);
                dates[1] = df.parse(endMatcher.group(2));
                jobEstimation.put(jobId, dates);
            } else {
                throw new RuntimeException("Matched 'job progress' entry in log file but job ID: " + jobId + " doesn't exist in jobEstimation.");
            }
        }
    }

    private static void sendMail(String passwd) {
        final String username = "bosun58@gmail.com";
        final String password = passwd;

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("bosun58@gmail.com"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse("bosun58@gmail.com,lousia1120@gmail.com"));
            message.setSubject("PUMS2010 Progress");
            message.setContent(generateBody(), "text/html; charset=utf-8");

            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateBody() {
        StringBuilder sb = new StringBuilder();
        Set<String> keys = jobList.keySet();
        sb.append("<table  style='width:100%'>");
        sb.append("<thead><td>Job ID</td><td>Start Row</td><td>End Row</td><td>Current Row</td><td>Start Time</td><td>Progress</td><td>ETA (hours)</td></tr>");
        for (String key : keys) {
            Integer[] rowCounters = jobList.get(key);
            if (!Objects.equals(rowCounters[1], rowCounters[2])) {
                Date[] dates = jobEstimation.get(key);
                sb.append("<tr>");
                sb.append("<td>").append(key).append("</td>");
                sb.append("<td>").append(rowCounters[0]).append("</td>");
                sb.append("<td>").append(rowCounters[1]).append("</td>");
                sb.append("<td>").append(rowCounters[2]).append("</td>");
                sb.append("<td>").append(dates[0]).append("</td>");

                double progress = (rowCounters[2] - rowCounters[0]) / (float) ((int) rowCounters[1] - rowCounters[0]) * 100.0;
                sb.append("<td>").append(String.format("%.2f", progress)).append("</td>");
                long timeLapsed = dates[1].getTime() - dates[0].getTime();
                double timeLeft = (timeLapsed / (progress / 100.0) - timeLapsed) / 3600000.0;
                sb.append("<td>").append(String.format("%.2f", timeLeft)).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("</table>");
        return sb.toString();
    }

}
