package tv.afnon.home;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles fetching, Protobuf binary encoding/decoding, and caching of official JAKIM e-Solat schedules.
 */
final class JakimApi {
    private static final String TAG = "JakimApi";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    static final class DayData {
        final Date fajr, syuruk, dhuhr, asr, maghrib, isha;
        final int hijriDay, hijriMonth, hijriYear;

        DayData(Date fajr, Date syuruk, Date dhuhr, Date asr, Date maghrib, Date isha,
                int hijriDay, int hijriMonth, int hijriYear) {
            this.fajr = fajr;
            this.syuruk = syuruk;
            this.dhuhr = dhuhr;
            this.asr = asr;
            this.maghrib = maghrib;
            this.isha = isha;
            this.hijriDay = hijriDay;
            this.hijriMonth = hijriMonth;
            this.hijriYear = hijriYear;
        }
    }

    // ---------------------------------------------------------------- Protobuf Wire Format Model

    static final class ProtoDay {
        String date = "";
        String hijri = "";
        String day = "";
        String imsak = "";
        String fajr = "";
        String syuruk = "";
        String dhuha = "";
        String dhuhr = "";
        String asr = "";
        String maghrib = "";
        String isha = "";

        byte[] toByteArray() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeString(baos, 1, date);
            writeString(baos, 2, hijri);
            writeString(baos, 3, day);
            writeString(baos, 4, imsak);
            writeString(baos, 5, fajr);
            writeString(baos, 6, syuruk);
            writeString(baos, 7, dhuha);
            writeString(baos, 8, dhuhr);
            writeString(baos, 9, asr);
            writeString(baos, 10, maghrib);
            writeString(baos, 11, isha);
            return baos.toByteArray();
        }

        static ProtoDay parseFrom(byte[] bytes) throws IOException {
            ProtoDay d = new ProtoDay();
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            while (bais.available() > 0) {
                int tag = readVarint(bais);
                int field = tag >>> 3;
                int wireType = tag & 0x07;
                if (wireType == 2) {
                    String str = readString(bais);
                    switch (field) {
                        case 1: d.date = str; break;
                        case 2: d.hijri = str; break;
                        case 3: d.day = str; break;
                        case 4: d.imsak = str; break;
                        case 5: d.fajr = str; break;
                        case 6: d.syuruk = str; break;
                        case 7: d.dhuha = str; break;
                        case 8: d.dhuhr = str; break;
                        case 9: d.asr = str; break;
                        case 10: d.maghrib = str; break;
                        case 11: d.isha = str; break;
                    }
                } else {
                    skipField(bais, wireType);
                }
            }
            return d;
        }
    }

    static final class ProtoMonth {
        String zone = "";
        String periodType = "";
        String lang = "";
        String bearing = "";
        final List<ProtoDay> prayerTime = new ArrayList<>();

        byte[] toByteArray() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeString(baos, 1, zone);
            writeString(baos, 2, periodType);
            writeString(baos, 3, lang);
            writeString(baos, 4, bearing);
            for (ProtoDay day : prayerTime) {
                byte[] dayBytes = day.toByteArray();
                writeVarint(baos, (5 << 3) | 2); // Field 5, WireType 2
                writeVarint(baos, dayBytes.length);
                baos.write(dayBytes);
            }
            return baos.toByteArray();
        }

        static ProtoMonth parseFrom(InputStream in) throws IOException {
            ProtoMonth m = new ProtoMonth();
            while (in.available() > 0) {
                int tag;
                try {
                    tag = readVarint(in);
                } catch (EOFException e) {
                    break;
                }
                int field = tag >>> 3;
                int wireType = tag & 0x07;
                if (wireType == 2) {
                    int len = readVarint(in);
                    byte[] buf = new byte[len];
                    int read = 0;
                    while (read < len) {
                        int r = in.read(buf, read, len - read);
                        if (r < 0) break;
                        read += r;
                    }
                    switch (field) {
                        case 1: m.zone = new String(buf, StandardCharsets.UTF_8); break;
                        case 2: m.periodType = new String(buf, StandardCharsets.UTF_8); break;
                        case 3: m.lang = new String(buf, StandardCharsets.UTF_8); break;
                        case 4: m.bearing = new String(buf, StandardCharsets.UTF_8); break;
                        case 5: m.prayerTime.add(ProtoDay.parseFrom(buf)); break;
                    }
                } else {
                    skipField(in, wireType);
                }
            }
            return m;
        }
    }

    // ---------------------------------------------------------------- Low-Level Wire Encoding Helpers

    private static void writeVarint(OutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value & 0x7F);
    }

    private static int readVarint(InputStream in) throws IOException {
        int result = 0;
        int shift = 0;
        while (shift < 32) {
            int b = in.read();
            if (b == -1) throw new EOFException();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
        }
        return result;
    }

    private static void writeString(OutputStream out, int fieldNumber, String val) throws IOException {
        if (val == null || val.isEmpty()) return;
        byte[] bytes = val.getBytes(StandardCharsets.UTF_8);
        writeVarint(out, (fieldNumber << 3) | 2); // WireType 2
        writeVarint(out, bytes.length);
        out.write(bytes);
    }

    private static String readString(InputStream in) throws IOException {
        int len = readVarint(in);
        byte[] buf = new byte[len];
        int read = 0;
        while (read < len) {
            int r = in.read(buf, read, len - read);
            if (r < 0) break;
            read += r;
        }
        return new String(buf, StandardCharsets.UTF_8);
    }

    private static void skipField(InputStream in, int wireType) throws IOException {
        if (wireType == 0) {
            readVarint(in);
        } else if (wireType == 1) {
            long skipped = 0;
            while (skipped < 8) {
                long s = in.skip(8 - skipped);
                if (s <= 0) break;
                skipped += s;
            }
        } else if (wireType == 2) {
            int len = readVarint(in);
            long skipped = 0;
            while (skipped < len) {
                long s = in.skip(len - skipped);
                if (s <= 0) break;
                skipped += s;
            }
        } else if (wireType == 5) {
            long skipped = 0;
            while (skipped < 4) {
                long s = in.skip(4 - skipped);
                if (s <= 0) break;
                skipped += s;
            }
        }
    }

    // ---------------------------------------------------------------- API & File Management

    private JakimApi() {}

    private static File cacheFile(Context c, String zone, int year, int month) {
        String name = String.format(Locale.US, "jakim_%s_%04d_%02d.pb", zone, year, month);
        return new File(c.getFilesDir(), name);
    }

    /**
     * Reads Protobuf cached prayer times for the given zone and date.
     * Returns null if cache is missing or invalid.
     */
    static DayData getDayData(Context context, String zone, Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // 1-12
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        File file = cacheFile(context, zone, year, month);
        if (!file.exists()) {
            ensureMonthFetched(context, zone, year, month, null);
            return null;
        }

        // Near the end of month, pre-fetch next month's schedule
        if (dayOfMonth >= 25) {
            Calendar nextCal = (Calendar) cal.clone();
            nextCal.add(Calendar.MONTH, 1);
            ensureMonthFetched(context, zone, nextCal.get(Calendar.YEAR), nextCal.get(Calendar.MONTH) + 1, null);
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            ProtoMonth pm = ProtoMonth.parseFrom(fis);
            if (pm.prayerTime.isEmpty()) return null;

            ProtoDay dayObj = null;
            String targetPrefix = String.format(Locale.US, "%02d-", dayOfMonth);
            for (ProtoDay pd : pm.prayerTime) {
                if (pd.date != null && (pd.date.startsWith(targetPrefix) || pd.date.startsWith(dayOfMonth + "-"))) {
                    dayObj = pd;
                    break;
                }
            }
            if (dayObj == null && dayOfMonth >= 1 && dayOfMonth <= pm.prayerTime.size()) {
                dayObj = pm.prayerTime.get(dayOfMonth - 1);
            }
            if (dayObj == null) return null;

            return parseDayData(dayObj, cal);
        } catch (Exception e) {
            Log.e(TAG, "Error reading Jakim Protobuf cache for " + zone + " " + year + "-" + month, e);
            return null;
        }
    }

    private static DayData parseDayData(ProtoDay obj, Calendar dayCal) {
        Date fajr = parseTime(dayCal, obj.fajr);
        Date syuruk = parseTime(dayCal, obj.syuruk);
        Date dhuhr = parseTime(dayCal, obj.dhuhr);
        Date asr = parseTime(dayCal, obj.asr);
        Date maghrib = parseTime(dayCal, obj.maghrib);
        Date isha = parseTime(dayCal, obj.isha);

        String hijriStr = obj.hijri;
        int hYear = 1448, hMonth = 0, hDay = 1;
        if (hijriStr != null && hijriStr.contains("-")) {
            String[] parts = hijriStr.split("-");
            if (parts.length == 3) {
                hYear = Integer.parseInt(parts[0]);
                hMonth = Integer.parseInt(parts[1]) - 1; // 0-based for IslamicCalendar
                hDay = Integer.parseInt(parts[2]);
            }
        }

        return new DayData(fajr, syuruk, dhuhr, asr, maghrib, isha, hDay, hMonth, hYear);
    }

    private static Date parseTime(Calendar dayCal, String timeStr) {
        if (timeStr == null || !timeStr.contains(":")) return dayCal.getTime();
        String[] parts = timeStr.split(":");
        int hour = Integer.parseInt(parts[0]);
        int min = Integer.parseInt(parts[1]);
        int sec = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

        Calendar c = (Calendar) dayCal.clone();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, min);
        c.set(Calendar.SECOND, sec);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * Triggers fetching JAKIM e-Solat data in background if not already cached.
     */
    static void ensureMonthFetched(Context context, String zone, int year, int month, Runnable onComplete) {
        File file = cacheFile(context, zone, year, month);
        if (file.exists() && file.length() > 50) {
            if (onComplete != null) MAIN_HANDLER.post(onComplete);
            return;
        }

        EXECUTOR.execute(() -> {
            boolean success = downloadAndSave(context, zone, year, month);
            if (onComplete != null && success) {
                MAIN_HANDLER.post(onComplete);
            }
        });
    }

    private static boolean downloadAndSave(Context context, String zone, int year, int month) {
        String spec = "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=month&zone=" + zone;
        try {
            URL url = new URL(spec);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android TV Launcher)");

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String jsonStr = sb.toString();
                if (jsonStr.contains("prayerTime")) {
                    JSONObject root = new JSONObject(jsonStr);
                    ProtoMonth pm = new ProtoMonth();
                    pm.zone = root.optString("zone", zone);
                    pm.periodType = root.optString("periodType", "month");
                    pm.lang = root.optString("lang", "ms_my");
                    pm.bearing = root.optString("bearing", "");

                    JSONArray array = root.optJSONArray("prayerTime");
                    if (array != null) {
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject dayObj = array.optJSONObject(i);
                            if (dayObj != null) {
                                ProtoDay pd = new ProtoDay();
                                pd.date = dayObj.optString("date");
                                pd.hijri = dayObj.optString("hijri");
                                pd.day = dayObj.optString("day");
                                pd.imsak = dayObj.optString("imsak");
                                pd.fajr = dayObj.optString("fajr");
                                pd.syuruk = dayObj.optString("syuruk");
                                pd.dhuha = dayObj.optString("dhuha");
                                pd.dhuhr = dayObj.optString("dhuhr");
                                pd.asr = dayObj.optString("asr");
                                pd.maghrib = dayObj.optString("maghrib");
                                pd.isha = dayObj.optString("isha");
                                pm.prayerTime.add(pd);
                            }
                        }
                    }

                    byte[] pbBytes = pm.toByteArray();
                    File file = cacheFile(context, zone, year, month);
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(pbBytes);
                    }
                    Log.i(TAG, "Successfully saved Protobuf Jakim schedule (" + pbBytes.length + " bytes) for " + zone + " " + year + "-" + month);
                    cleanCache(context, zone, year, month);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to download Jakim schedule for " + zone + " " + year + "-" + month, e);
        }
        return false;
    }

    /**
     * Cleans up old or unused JAKIM cache files (.pb / .json).
     * Keeps only files for the active zone from the current month onwards.
     */
    static void cleanCache(Context context, String activeZone, int activeYear, int activeMonth) {
        File dir = context.getFilesDir();
        if (dir == null || !dir.exists()) return;

        File[] files = dir.listFiles((d, name) -> name.startsWith("jakim_") && (name.endsWith(".pb") || name.endsWith(".json")));
        if (files == null) return;

        int activeVal = activeYear * 100 + activeMonth;

        for (File f : files) {
            String name = f.getName();
            String cleanName = name.replace(".pb", "").replace(".json", "");
            String[] parts = cleanName.split("_");
            if (parts.length == 4) {
                String zone = parts[1];
                try {
                    int year = Integer.parseInt(parts[2]);
                    int month = Integer.parseInt(parts[3]);
                    int fileVal = year * 100 + month;

                    if (!zone.equals(activeZone) || fileVal < activeVal) {
                        if (f.delete()) {
                            Log.d(TAG, "Cleaned old cache: " + name);
                        }
                    }
                } catch (NumberFormatException ignored) {
                    if (f.delete()) {
                        Log.d(TAG, "Cleaned invalid cache: " + name);
                    }
                }
            }
        }
    }
}
