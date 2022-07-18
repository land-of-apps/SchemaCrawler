package us.fatehi.utility.scheduler;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public final class TaskInfo {

  private static final DateTimeFormatter df =
      new DateTimeFormatterBuilder()
          .appendValue(HOUR_OF_DAY, 2)
          .appendLiteral(':')
          .appendValue(MINUTE_OF_HOUR, 2)
          .appendLiteral(':')
          .appendValue(SECOND_OF_MINUTE, 2)
          .appendFraction(NANO_OF_SECOND, 3, 3, true)
          .toFormatter();

  private final Duration duration;
  private final String taskName;

  TaskInfo(final String taskName, final Duration duration) {
    requireNonNull(taskName, "Task name not provided");
    requireNonNull(duration, "Duration not provided");
    this.taskName = taskName;
    this.duration = duration;
  }

  public Duration getDuration() {
    return duration;
  }

  @Override
  public String toString() {
    final LocalTime durationLocal = LocalTime.ofNanoOfDay(duration.toNanos());
    return String.format("%s - <%s>", durationLocal.format(df), taskName);
  }
}
