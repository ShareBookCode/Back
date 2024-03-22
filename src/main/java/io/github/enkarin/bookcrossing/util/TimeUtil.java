package io.github.enkarin.bookcrossing.util;

import lombok.Setter;
import lombok.experimental.UtilityClass;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@UtilityClass
public class TimeUtil {
    @Setter
    private static Clock clock = Clock.system(ZoneOffset.ofHours(7));

    public static ZoneOffset offset() {
        return clock.getZone().getRules().getOffset(clock.instant());
    }

    @SuppressWarnings("illegalMethodCall")
    public static LocalDateTime dateTimeNow() {
        return LocalDateTime.now(clock);
    }

    public static LocalDate dateNow() {
        return dateTimeNow().toLocalDate();
    }

    public static long getEpochSeconds() {
        return clock.instant().getEpochSecond();
    }
}
