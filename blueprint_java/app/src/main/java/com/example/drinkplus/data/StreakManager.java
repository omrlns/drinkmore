package com.example.drinkplus.data;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * StreakManager: Gerenciador de estado local responsável pela persistência do total de água diária logada,
 * contagem de dias de ofensiva e lógica de revalidação de sequências quebradas.
 */
public class StreakManager {

    private static final String PREFS_NAME = "drinkplus_prefs";
    private static final String KEY_STREAK_DAYS = "streak_days";
    private static final String KEY_LAST_GOAL_DATE = "last_goal_date";
    private static final String KEY_TODAY_INTAKE = "today_intake";
    private static final String KEY_SAVED_DATE = "saved_date";

    private final SharedPreferences prefs;

    public StreakManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        reconcileDailyTrackingRecords();
    }

    private String getFormattedDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(date);
    }

    private String getTodayDateKey() {
        return getFormattedDate(new Date());
    }

    private String getYesterdayDateKey() {
        Date yesterday = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        return getFormattedDate(yesterday);
    }

    /**
     * Reconcilia os contadores diários. Se o dia mudou, arquiva o total anterior
     * e revalida a sequência de ofensivas. Se o usuário pulou um dia sem cumprir a meta,
     * a ofensiva (streak) é zerada.
     */
    public synchronized void reconcileDailyTrackingRecords() {
        String todayKey = getTodayDateKey();
        String savedDateKey = prefs.getString(KEY_SAVED_DATE, "");

        if (!todayKey.equals(savedDateKey)) {
            // Se mudou o dia, reinicia o consumo de hoje para zero
            prefs.edit()
                .putInt(KEY_TODAY_INTAKE, 0)
                .putString(KEY_SAVED_DATE, todayKey)
                .apply();

            // Verificar se a meta de hidratação foi alcançada no dia anterior (ou se quebrou a sequência)
            String lastGoalDate = prefs.getString(KEY_LAST_GOAL_DATE, "");
            String yesterdayKey = getYesterdayDateKey();

            // Se a última data em que o usuário bateu a meta não foi nem hoje nem ontem,
            // significa que ele saltou pelo menos um dia inteiro. A sequência deve ser reiniciada!
            if (!lastGoalDate.equals(todayKey) && !lastGoalDate.equals(yesterdayKey)) {
                prefs.edit().putInt(KEY_STREAK_DAYS, 0).apply();
            }
        }
    }

    /**
     * Registra o consumo de água em mL.
     * Retorna true se bater a meta diária e incrementar a ofensiva pela primeira vez no dia.
     */
    public synchronized boolean addWaterIntake(int amountMl, int goalMl) {
        reconcileDailyTrackingRecords();

        int currentIntake = prefs.getInt(KEY_TODAY_INTAKE, 0) + amountMl;
        prefs.edit().putInt(KEY_TODAY_INTAKE, currentIntake).apply();

        if (currentIntake >= goalMl) {
            String todayKey = getTodayDateKey();
            String lastGoalDate = prefs.getString(KEY_LAST_GOAL_DATE, "");

            // Se ainda não bateu a meta hoje, incrementa a ofensiva
            if (!todayKey.equals(lastGoalDate)) {
                String yesterdayKey = getYesterdayDateKey();
                int currentStreak = prefs.getInt(KEY_STREAK_DAYS, 0);

                int newStreak;
                if (lastGoalDate.equals(yesterdayKey)) {
                    newStreak = currentStreak + 1; // Continua de ontem
                } else {
                    newStreak = 1; // Começa uma nova
                }

                prefs.edit()
                     .putInt(KEY_STREAK_DAYS, newStreak)
                     .putString(KEY_LAST_GOAL_DATE, todayKey)
                     .apply();

                return true; // Meta atingida com incremento de ofensiva
            }
        }
        return false;
    }

    public synchronized int getTodayIntake() {
        reconcileDailyTrackingRecords();
        return prefs.getInt(KEY_TODAY_INTAKE, 0);
    }

    public synchronized int getStreakDays() {
        reconcileDailyTrackingRecords();
        return prefs.getInt(KEY_STREAK_DAYS, 0);
    }

    public synchronized void resetStreakAndTracker() {
        prefs.edit()
             .putInt(KEY_STREAK_DAYS, 0)
             .putInt(KEY_TODAY_INTAKE, 0)
             .putString(KEY_LAST_GOAL_DATE, "")
             .putString(KEY_SAVED_DATE, getTodayDateKey())
             .apply();
    }
}
