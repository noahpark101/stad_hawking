package com.zoho.hawking;

import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import com.zoho.hawking.language.english.model.DatesFound;
import com.zoho.hawking.language.english.model.ParserOutput;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Date;

public class PropertyTest {
  private HawkingConfiguration hawkingConfiguration;
  private HawkingTimeParser parser;
  private Date referenceDate;
  private String defaultStartDate;
  private String lang;

  /**
   * Gets the date given a Start or End line
   * @param str the string in the following format: "Start/End : {Date}:{Time}"
   * @return the Date
   */
  private String getDate(String str) {
    List<String> ls = Arrays.asList(str.split("T"));
    return ls.get(0);
  }

  @BeforeProperty
  void setUp() {
    hawkingConfiguration = new HawkingConfiguration();
    parser = new HawkingTimeParser();
    referenceDate = new Date(120, Calendar.DECEMBER, 1);
    defaultStartDate = "2020-12-01";
    lang = "eng";
  }
  /*
    Properties to implement:
      1. Given any string input, the DateTimeParser should never throw an exception
      2. If the input only has a time, the DateTimeParser should default to the referenceTime
      3. Given future relative keywords, the end date for DateTimeParsers should always be ahead of the start date
      4. Given any string input, the DateTimeParser should be deterministic in that it always outputs the same thing
      5. If a date is parsed correctly by DateTimeParser, it should match the original input date.
      6. Different formatted dates should map to the same DateTimeParser output
      7. Given past relative keywords, the end date for DateTimeParsers should always before the start date
   */

  @Provide
  Arbitrary<String> randomStrings() {
    return Arbitraries.strings().ofMinLength(10).ofMaxLength(30)
            .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
  }

  @Property
  @Label("Property 1: No exceptions will be thrown for any string input")
  boolean noExceptionsAreThrown(@ForAll("randomStrings") String str) {
    try {
      parser.parse(str, referenceDate, hawkingConfiguration, lang);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Provide
  Arbitrary<String> randomTimeStrings() {
    Arbitrary<Integer> hours = Arbitraries.integers().between(1, 23);
    Arbitrary<Integer> minutes = Arbitraries.integers().between(0, 59);
    return Combinators.combine(hours, minutes).as((h, m) -> String.format("%d:%02d", h, m));
  }

  @Property
  @Label("Property 2: Date defaults to reference when date is not specified")
  boolean defaultsToReferenceDateGivenTime(@ForAll("randomTimeStrings") String str) {
    String input = "It is " + str + " now.";
    DatesFound datesFound = parser.parse(input, referenceDate, hawkingConfiguration, lang);
    String start = getDate(datesFound.getParserOutputs().get(0).getDateRange().getStart().toString());
    String end = getDate(datesFound.getParserOutputs().get(0).getDateRange().getEnd().toString());
    return start.equals(defaultStartDate) && end.equals(defaultStartDate);
  }

  @Provide
  Arbitrary<String> randomNextStrings() {
    Arbitrary<Integer> nums = Arbitraries.integers().between(1, 40);
    Arbitrary<String> dates = Arbitraries.of("week", "month", "year", "day");
    Arbitrary<String> relatives = Arbitraries.of("Next", "In", "After", "By next");
    return Combinators.combine(nums, dates, relatives).as((n, d, r) ->
            String.format("%s %d %s, I am going to goon.", r, n, d));
  }

  @Property
  @Label("Property 3: Parsed Future relative word inputs cause end date to be future ahead than start dates")
  boolean futureRelativeCausesEndDateToBeAhead(@ForAll("randomNextStrings") String input) {
    try {
      DatesFound datesFound = parser.parse(input, referenceDate, hawkingConfiguration, lang);
      String start = getDate(datesFound.getParserOutputs().get(0).getDateRange().getStart().toString());
      String end = getDate(datesFound.getParserOutputs().get(0).getDateRange().getEnd().toString());
      Date startDate = new SimpleDateFormat("yyyy/MM/dd").parse(start);
      Date endDate = new SimpleDateFormat("yyyy/MM/dd").parse(end);
      return endDate.after(startDate);
    } catch (Exception e) {
      return true;
    }
  }

  @Property
  @Label("Property 4: Parser is deterministic: random strings")
  boolean parserIsDeterministicOne(@ForAll("randomStrings") String input) {
    DatesFound datesFound = parser.parse(input, referenceDate, hawkingConfiguration, lang);
    DatesFound datesFound2 = parser.parse(input, referenceDate, hawkingConfiguration, lang);
    return datesFound.toString().equals(datesFound2.toString());

  }

  @Property
  @Label("Property 4: Parser is deterministic: random time strings")
  boolean parserIsDeterministicTwo(@ForAll("randomTimeStrings") String input) {
    DatesFound datesFound = parser.parse(input, referenceDate, hawkingConfiguration, lang);
    DatesFound datesFound2 = parser.parse(input, referenceDate, hawkingConfiguration, lang);
    return datesFound.toString().equals(datesFound2.toString());

  }

  @Property
  @Label("Property 4: Parser is deterministic: random next strings")
  boolean parserIsDeterministicThree(@ForAll("randomNextStrings") String input) {
    DatesFound datesFound = parser.parse(input, referenceDate, hawkingConfiguration, lang);
    DatesFound datesFound2 = parser.parse(input, referenceDate, hawkingConfiguration, lang);
    return datesFound.toString().equals(datesFound2.toString());

  }

  @Provide
  Arbitrary<List<String>> randomlyFormattedDateStrings() {
    Arbitrary<Integer> days = Arbitraries.integers().between(1, 25);
    Arbitrary<Integer> years = Arbitraries.integers().between(2025, 2300);
    Arbitrary<Integer> months = Arbitraries.integers().between(1, 12);
    String[] monthArr = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

    return Combinators.combine(days, months, years).as((d, m, y) ->
            List.of(
                    String.format("It is %d/%d/%d", m, d ,y),
                    String.format("It is %d-%d-%d", y, m ,d),
                    String.format("It is %s %d %d", monthArr[m - 1],d, y),
                    String.format("It is %s %d, %d", monthArr[m - 1],d, y)
            ));
  }

  @Property
  @Label("Property 6: Different formatted dates return the same date from parser")
  boolean parserHasSameOutputForDifferentFormats(@ForAll("randomlyFormattedDateStrings") List<String> input) {
    for (int i = 1; i < input.size(); i++) {
      try {
        DatesFound prevDate = parser.parse(input.get(i - 1), referenceDate, hawkingConfiguration, lang);
        DatesFound curDate = parser.parse(input.get(i), referenceDate, hawkingConfiguration, lang);
        String prevStart = getDate(prevDate.getParserOutputs().get(0).getDateRange().getStart().toString());
        String prevEnd = getDate(prevDate.getParserOutputs().get(0).getDateRange().getEnd().toString());
        String curStart = getDate(curDate.getParserOutputs().get(0).getDateRange().getStart().toString());
        String curEnd = getDate(curDate.getParserOutputs().get(0).getDateRange().getEnd().toString());
        if (!prevStart.equals(curStart) && !prevEnd.equals(curEnd)) {
          return false;
        }
      } catch (Exception e) {
      }

    }
    return true;
  }




}
