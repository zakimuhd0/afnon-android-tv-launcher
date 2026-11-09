package tv.afnon.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;

import java.util.Calendar;

/**
 * Afnon's own settings (SharedPreferences "afnon"), read by every screen and the screensaver. Language and digits
 * live in {@link Screen#loadText}; the room direction in {@link Screen#facing()}.
 */
final class Prefs {
    private Prefs() {}

    static SharedPreferences of(Context c) {
        return c.getSharedPreferences("afnon", Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- location

    static final class City {
        final String id, negeri, daerah;
        final double lat, lng;

        City(String id, String negeri, String daerah, double lat, double lng) {
            this.id = id;
            this.negeri = negeri;
            this.daerah = daerah;
            this.lat = lat;
            this.lng = lng;
        }
    }

    static final City[] CITIES = {
        // Johor
        new City("JHR01", "Johor", "Pulau Aur dan Pulau Pemanggil", 2.4470, 104.5270),
        new City("JHR02", "Johor", "Johor Bahru, Kota Tinggi, Mersing, Kulai", 1.4927, 103.7414),
        new City("JHR03", "Johor", "Kluang, Pontian", 2.0302, 103.3182),
        new City("JHR04", "Johor", "Batu Pahat, Muar, Segamat, Gemas Johor, Tangkak", 1.8548, 102.9325),

        // Kedah
        new City("KDH01", "Kedah", "Kota Setar, Kubang Pasu, Pokok Sena", 6.1248, 100.3678),
        new City("KDH02", "Kedah", "Kuala Muda, Yan, Pendang", 5.6432, 100.4912),
        new City("KDH03", "Kedah", "Padang Terap, Sik", 6.2083, 100.6125),
        new City("KDH04", "Kedah", "Baling", 5.6783, 100.9167),
        new City("KDH05", "Kedah", "Bandar Baharu, Kulim", 5.3686, 100.5547),
        new City("KDH06", "Kedah", "Langkawi", 6.3500, 99.8000),
        new City("KDH07", "Kedah", "Puncak Gunung Jerai", 5.7872, 100.4342),

        // Kelantan
        new City("KTN01", "Kelantan", "Bachok, Kota Bharu, Machang, Pasir Mas, Pasir Puteh, Tanah Merah, Tumpat, Kuala Krai, Mukim Chiku", 6.1256, 102.2386),
        new City("KTN02", "Kelantan", "Gua Musang, Jeli, Jajahan Kecil Lojing", 4.8822, 101.9686),

        // Melaka
        new City("MLK01", "Melaka", "SELURUH NEGERI MELAKA", 2.1896, 102.2501),

        // Negeri Sembilan
        new City("NGS01", "Negeri Sembilan", "Tampin, Jempol", 2.4701, 102.2302),
        new City("NGS02", "Negeri Sembilan", "Jelebu, Kuala Pilah, Rembau", 2.7389, 102.2483),
        new City("NGS03", "Negeri Sembilan", "Port Dickson, Seremban", 2.7258, 101.9424),

        // Pahang
        new City("PHG01", "Pahang", "Pulau Tioman", 2.8125, 104.1625),
        new City("PHG02", "Pahang", "Kuantan, Pekan, Muadzam Shah", 3.8077, 103.3260),
        new City("PHG03", "Pahang", "Jerantut, Temerloh, Maran, Bera, Chenor, Jengka", 3.4886, 102.4300),
        new City("PHG04", "Pahang", "Bentong, Lipis, Raub", 3.5222, 101.9094),
        new City("PHG05", "Pahang", "Genting Sempah, Janda Baik, Bukit Tinggi", 3.3131, 101.8683),
        new City("PHG06", "Pahang", "Cameron Highlands, Genting Highlands, Bukit Fraser", 4.4708, 101.3775),
        new City("PHG07", "Pahang", "Zon Khas Daerah Rompin (Mukim Rompin, Mukim Endau, Mukim Pontian)", 2.8156, 103.4853),

        // Perak
        new City("PRK01", "Perak", "Tapah, Slim River, Tanjung Malim", 3.6833, 101.5167),
        new City("PRK02", "Perak", "Kuala Kangsar, Sg. Siput, Ipoh, Batu Gajah, Kampar", 4.5975, 101.0901),
        new City("PRK03", "Perak", "Lenggong, Pengkalan Hulu, Grik", 5.4267, 101.1278),
        new City("PRK04", "Perak", "Temengor, Belum", 5.5431, 101.3411),
        new City("PRK05", "Perak", "Kg Gajah, Teluk Intan, Bagan Datuk, Seri Iskandar, Beruas, Parit, Lumut, Sitiawan, Pulau Pangkor", 4.0028, 101.0208),
        new City("PRK06", "Perak", "Selama, Taiping, Bagan Serai, Parit Buntar", 4.8500, 100.7333),
        new City("PRK07", "Perak", "Bukit Larut", 4.8619, 100.7936),

        // Perlis
        new City("PLS01", "Perlis", "SELURUH NEGERI PERLIS", 6.4414, 100.1986),

        // Pulau Pinang
        new City("PNG01", "Pulau Pinang", "SELURUH NEGERI PULAU PINANG", 5.4164, 100.3327),

        // Sabah
        new City("SBH01", "Sabah", "Bahagian Sandakan (Timur), Bukit Garam, Semawang, Temanggong, Tambisan, Bandar Sandakan, Sukau", 5.8402, 118.1179),
        new City("SBH02", "Sabah", "Beluran, Telupid, Pinangah, Terusan, Kuamut, Bahagian Sandakan (Barat)", 5.8883, 117.5583),
        new City("SBH03", "Sabah", "Lahad Datu, Silabukan, Kunak, Sahabat, Semporna, Tungku, Bahagian Tawau (Timur)", 5.0268, 118.3270),
        new City("SBH04", "Sabah", "Bandar Tawau, Balong, Merotai, Kalabakan, Bahagian Tawau (Barat)", 4.2447, 117.8912),
        new City("SBH05", "Sabah", "Kudat, Kota Marudu, Pitas, Pulau Banggi, Bahagian Kudat", 6.8837, 116.8461),
        new City("SBH06", "Sabah", "Gunung Kinabalu", 6.0753, 116.5583),
        new City("SBH07", "Sabah", "Kota Kinabalu, Ranau, Kota Belud, Tuaran, Penampang, Papar, Putatan, Bahagian Pantai Barat", 5.9804, 116.0735),
        new City("SBH08", "Sabah", "Pensiangan, Keningau, Tambunan, Nabawan, Bahagian Pendalaman (Atas)", 5.3378, 116.1603),
        new City("SBH09", "Sabah", "Beaufort, Kuala Penyu, Sipitang, Tenom, Long Pasia, Membakut, Weston, Bahagian Pendalaman (Bawah)", 5.3473, 115.7455),

        // Sarawak
        new City("SWK01", "Sarawak", "Limbang, Lawas, Sundar, Trusan", 4.7500, 115.0000),
        new City("SWK02", "Sarawak", "Miri, Niah, Bekenu, Sibuti, Marudi", 4.3995, 113.9914),
        new City("SWK03", "Sarawak", "Pandan, Belaga, Suai, Tatau, Sebauh, Bintulu", 3.1667, 113.0333),
        new City("SWK04", "Sarawak", "Sibu, Mukah, Dalat, Song, Igan, Oya, Balingian, Kanowit, Kapit", 2.3000, 111.8167),
        new City("SWK05", "Sarawak", "Sarikei, Matu, Julau, Rajang, Daro, Bintangor, Belawai", 2.1167, 111.5167),
        new City("SWK06", "Sarawak", "Lubok Antu, Sri Aman, Roban, Debak, Kabong, Lingga, Engkelili, Betong, Spaoh, Pusa, Saratok", 1.2333, 111.4500),
        new City("SWK07", "Sarawak", "Serian, Simunjan, Samarahan, Sebuyau, Meludam", 1.1667, 110.5667),
        new City("SWK08", "Sarawak", "Kuching, Bau, Lundu, Sematan", 1.5533, 110.3592),
        new City("SWK09", "Sarawak", "Zon Khas (Kampung Patarikan)", 1.7000, 109.8000),

        // Selangor
        new City("SGR01", "Selangor", "Gombak, Petaling, Sepang, Hulu Langat, Hulu Selangor, Shah Alam", 3.0738, 101.5183),
        new City("SGR02", "Selangor", "Kuala Selangor, Sabak Bernam", 3.3500, 101.2500),
        new City("SGR03", "Selangor", "Klang, Kuala Langat", 3.0167, 101.2500),

        // Terengganu
        new City("TRG01", "Terengganu", "Kuala Terengganu, Marang, Kuala Nerus", 5.3117, 103.1324),
        new City("TRG02", "Terengganu", "Besut, Setiu", 5.8333, 102.5500),
        new City("TRG03", "Terengganu", "Hulu Terengganu", 5.0667, 103.0167),
        new City("TRG04", "Terengganu", "Dungun, Kemaman", 4.7667, 103.4167),

        // Wilayah Persekutuan
        new City("WLY01", "Wilayah Persekutuan", "Kuala Lumpur, Putrajaya", 3.1390, 101.6869),
        new City("WLY02", "Wilayah Persekutuan", "Labuan", 5.2831, 115.2308),
    };

    static final String[] STATES = {
        "Johor", "Kedah", "Kelantan", "Melaka", "Negeri Sembilan", "Pahang",
        "Perak", "Perlis", "Pulau Pinang", "Sabah", "Sarawak", "Selangor",
        "Terengganu", "Wilayah Persekutuan"
    };

    static int cityIndex(Context c) {
        String code = of(c).getString("city", "WLY01");
        for (int i = 0; i < CITIES.length; i++) if (CITIES[i].id.equals(code)) return i;
        return 0;
    }

    static City city(Context c) {
        return CITIES[cityIndex(c)];
    }

    static void setCity(Context c, int index) {
        City city = CITIES[index];
        of(c).edit().putString("city", city.id).apply();
        Calendar cal = Calendar.getInstance();
        JakimApi.ensureMonthFetched(c, city.id, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, null);
    }

    static Coordinates where(Context c) {
        City z = city(c);
        return new Coordinates(z.lat, z.lng);
    }

    // ---------------------------------------------------------------- calculation

    static final CalculationMethod[] METHODS = {
        CalculationMethod.MOON_SIGHTING_COMMITTEE, CalculationMethod.SINGAPORE, CalculationMethod.MUSLIM_WORLD_LEAGUE,
        CalculationMethod.EGYPTIAN, CalculationMethod.UMM_AL_QURA, CalculationMethod.KARACHI,
        CalculationMethod.DUBAI, CalculationMethod.KUWAIT, CalculationMethod.QATAR, CalculationMethod.NORTH_AMERICA};

    static CalculationMethod method(Context c) {
        String name = of(c).getString("method", null);
        if (name != null) for (CalculationMethod m : METHODS) if (m.name().equals(name)) return m;
        return CalculationMethod.MOON_SIGHTING_COMMITTEE;
    }

    static void setMethod(Context c, CalculationMethod m) {
        of(c).edit().putString("method", m.name()).apply();
    }

    static Madhab madhab(Context c) {
        return "hanafi".equals(of(c).getString("madhab", "shafi")) ? Madhab.HANAFI : Madhab.SHAFI;
    }

    static void setMadhab(Context c, Madhab m) {
        of(c).edit().putString("madhab", m == Madhab.HANAFI ? "hanafi" : "shafi").apply();
    }

    /** Days added to the computed Hijri date, for when the local moon sighting differs: −2 to +2. */
    static int hijriOffset(Context c) {
        return of(c).getInt("hijri_offset", 0);
    }

    static void setHijriOffset(Context c, int days) {
        of(c).edit().putInt("hijri_offset", Math.max(-2, Math.min(2, days))).apply();
    }

    /** Minutes added to Fajr, Sunrise, Dhuhr, Asr, Maghrib and Isha (Settings → Adjust the times): −30 to +30. */
    static int[] adjustments(Context c) {
        SharedPreferences p = of(c);
        int[] a = new int[6];
        for (int i = 0; i < 6; i++) a[i] = p.getInt("adjust_" + i, 0);
        return a;
    }

    static void setAdjustment(Context c, int prayer, int minutes) {
        of(c).edit().putInt("adjust_" + prayer, Math.max(-30, Math.min(30, minutes))).apply();
    }

    // ---------------------------------------------------------------- reminders

    /** Minutes before each prayer to show the reminder; 0 is off. */
    static int remindMinutes(Context c) {
        return of(c).getInt("remind", 15);
    }

    static void setRemindMinutes(Context c, int minutes) {
        of(c).edit().putInt("remind", minutes).apply();
    }

    /** Say «اقترب موعد صلاة …» with the reminder card or banner (Announce, on by default). */
    static boolean remindVoice(Context c) {
        return of(c).getBoolean("remind_voice", true);
    }

    static void setRemindVoice(Context c, boolean on) {
        of(c).edit().putBoolean("remind_voice", on).apply();
    }

    /** Say «حان الآن موعد صلاة …» when a prayer's time begins with its adhan off (Announce, on by default). */
    static boolean announce(Context c) {
        return of(c).getBoolean("announce", true);
    }

    static void setAnnounce(Context c, boolean on) {
        of(c).edit().putBoolean("announce", on).apply();
    }

    /**
     * When the call screen closes by itself: 1, 5 or 10 minutes after it opens ({@link Tokens#MOTION_CALL_AUTO_DISMISS_MS}
     * by default), or 0 when the adhan ends.
     */
    static int callCloseMinutes(Context c) {
        return of(c).getInt("call_close", (int) (Tokens.MOTION_CALL_AUTO_DISMISS_MS / 60_000));
    }

    static void setCallCloseMinutes(Context c, int minutes) {
        of(c).edit().putInt("call_close", minutes).apply();
    }

    /** Override stock launcher on Home button press (TiVo Stream 4K / Google TV). */
    static boolean overrideHome(Context c) {
        return of(c).getBoolean("override_home", true);
    }

    static void setOverrideHome(Context c, boolean on) {
        of(c).edit().putBoolean("override_home", on).apply();
    }

    /** The adhan as a whole; each prayer's bell (Prayer day) still applies under it. */
    static boolean adhan(Context c) {
        return of(c).getBoolean("adhan", true);
    }

    static void setAdhan(Context c, boolean on) {
        of(c).edit().putBoolean("adhan", on).apply();
    }

    /** Each prayer's own adhan bell (Prayer day); on by default except Sunrise, which has no adhan. */
    static boolean bell(Context c, int prayer) {
        return of(c).getBoolean("bell_" + prayer, prayer != PrayerDay.SUNRISE);
    }

    /** The recording (Adhans id) for every prayer but Fajr, and the one for Fajr, whose adhan has its own line. */
    static String voice(Context c, boolean fajr) {
        return fajr ? of(c).getString("voice_fajr", "chebae_fajr") : of(c).getString("voice", "alafasy");
    }

    static void setVoice(Context c, boolean fajr, String id) {
        of(c).edit().putString(fajr ? "voice_fajr" : "voice", id).apply();
    }

    // ---------------------------------------------------------------- radio

    /** After the adhan paused the radio: true resumes it (the default), false leaves it off. */
    static boolean radioResume(Context c) {
        return of(c).getBoolean("radio_resume", true);
    }

    static void setRadioResume(Context c, boolean on) {
        of(c).edit().putBoolean("radio_resume", on).apply();
    }

    /** Minutes of screensaver before the screen goes dark while the radio plays: 0 at once (the default), -1 never. */
    static int radioScreenOff(Context c) {
        return of(c).getInt("radio_dark", 0);
    }

    static void setRadioScreenOff(Context c, int minutes) {
        of(c).edit().putInt("radio_dark", minutes).apply();
    }

    // ---------------------------------------------------------------- display

    /** "system" (the TV's own Date & time setting, the default), "12" or "24". */
    static String clock(Context c) {
        return of(c).getString("clock", "system");
    }

    static boolean clock24(Context c) {
        String s = clock(c);
        return "system".equals(s) ? DateFormat.is24HourFormat(c) : "24".equals(s);
    }

    /** Night mode: the dimmer, warmer palette of afnon-mihrab/tokens.night.json on every screen. Off by default. */
    static boolean night(Context c) {
        return of(c).getBoolean("night", false);
    }

    /** Saves night mode and switches {@link Tokens} to it; screens built from then on take the new colours. */
    static void setNight(Context c, boolean on) {
        of(c).edit().putBoolean("night", on).apply();
        Tokens.use(on);
    }

    /** Everything a built screen depends on; a screen whose key changed while it was away rebuilds. */
    static String key(Context c) {
        SharedPreferences p = of(c);
        return p.getString("locale", "") + "|" + p.getString("digits", "") + "|" + clock24(c) + "|" + cityIndex(c)
            + "|" + method(c) + "|" + madhab(c) + "|" + hijriOffset(c) + "|" + p.getInt("facing", -1);
    }
}
