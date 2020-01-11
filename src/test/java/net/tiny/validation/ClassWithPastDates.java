package net.tiny.validation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.validation.constraints.Past;

public class ClassWithPastDates {

    @Past  //TODO
    private LocalDate date;

    @Past  //TODO
    private LocalDateTime dateTime;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

}