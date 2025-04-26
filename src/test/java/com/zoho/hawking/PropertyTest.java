package com.zoho.hawking;

import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import com.zoho.hawking.language.english.model.DatesFound;
import com.zoho.hawking.language.english.model.ParserOutput;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import net.jqwik.time.api.Dates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
      5. Parsing a date, formatting it into a string and parsing it should yield the same output
      6. Different formatted dates should map to the same DateTimeParser output
      7. Given past relative keywords, the end date for DateTimeParsers should always before the start date
      8. Given present relative keywords, the DateTimeParser should default to the referenceTime
      9. Capitalization of words or letters in the string input should yield the same result for the DateTimeParser
      10. Given two same input strings that differ only in white spaces, the output for them in the parser should be
        the same.
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

  //fails because sometimes the classification does not use the reference date -> (12-1 -> 11:30)
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
  Arbitrary<String> strictFormatString() {
    Arbitrary<Integer> days = Arbitraries.integers().between(1, 25);
    Arbitrary<Integer> months = Arbitraries.integers().between(1, 12);
    return Combinators.combine(days, months).as((d, m) ->
            String.format("It is 2020-%s-%s.", d, m));
  }

  @Property
  @Label("Property 5: Parsed output that is reformatted should yield same parsed output")
  boolean ideompotentParsing(@ForAll("strictFormatString") String input) {
    DatesFound datesOne = parser.parse(input, referenceDate, hawkingConfiguration, lang);
    String dateStrOne = getDate(datesOne.getParserOutputs().get(0).getDateRange().getEnd().toString());
    String reinput = "It is " + dateStrOne + ".";
    DatesFound datesTwo = parser.parse(reinput, referenceDate, hawkingConfiguration, lang);
    String dateStrTwo = getDate(datesTwo.getParserOutputs().get(0).getDateRange().getEnd().toString());
    return dateStrOne.equals(dateStrTwo);
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

  //fails because Classification is not robust to differently formatted dates
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

  @Provide
  Arbitrary<String> randomBeforeStrings() {
    Arbitrary<Integer> nums = Arbitraries.integers().between(1, 40);
    Arbitrary<String> dates = Arbitraries.of("weeks", "months", "years", "days");
    Arbitrary<String> relatives = Arbitraries.of("Over", "Nearly", "About", "Around");
    return Combinators.combine(nums, dates, relatives).as((n, d, r) ->
            String.format("%s %d %s ago, I was blinded.", r, n, d));
  }

  @Property
  @Label("Property 7: Parsed Past relative word inputs cause end date to before start date")
  boolean pastRelativeCausesEndDateToBeBehind(@ForAll("randomBeforeStrings") String input) {
    try {
      DatesFound datesFound = parser.parse(input, referenceDate, hawkingConfiguration, lang);
      String start = getDate(datesFound.getParserOutputs().get(0).getDateRange().getStart().toString());
      String end = getDate(datesFound.getParserOutputs().get(0).getDateRange().getEnd().toString());
      Date startDate = new SimpleDateFormat("yyyy/MM/dd").parse(start);
      Date endDate = new SimpleDateFormat("yyyy/MM/dd").parse(end);
      return endDate.before(startDate);
    } catch (Exception e) {
      return true;
    }
  }


  @Provide
  Arbitrary<String> presentRelativeStrings() {
    Arbitrary<String> presentPhrases = Arbitraries.of("Currently", "Now", "Right now", "Presently", "Today", "0 days ago", "As of now", "At the moment");
    return presentPhrases.flatMap(intro ->
            Arbitraries.just(intro + ", I don't know what to do."));
  }

  @Property
  @Label("Property 8: Present relative words should make TimeParser default to reference date")
  boolean presentRelativeWordsDefaultToReferenceDate(@ForAll("presentRelativeStrings") String input) {
    DatesFound datesFound = parser.parse(input, referenceDate, hawkingConfiguration, lang);
    String end = datesFound.getParserOutputs().get(0).getDateRange().getEnd().toString();
    return end.equals(referenceDate);
  }

  @Provide
  Arbitrary<List<String>> randomCapitalStrings() {
    Arbitrary<Integer> nums = Arbitraries.integers().between(2, 15);
    Arbitrary<String> defaultStrs = nums.flatMap(num ->
            Arbitraries.just(String.format("In %d days, I am leaving you.", num)));
    return defaultStrs.flatMap(str -> {
      StringBuilder randomStr = new StringBuilder();
      for (char c : str.toCharArray()) {
        if (Character.isLetter(c) && Math.random() < 0.5) {
          randomStr.append(Character.toUpperCase(c));
        } else {
          randomStr.append(c);
        }
      }
      return Arbitraries.just(List.of(randomStr.toString(), str));
    });

  }

  @Property
  @Label("Property 9: For strings that are the same except capitalization, the parser should yield the same output")
  boolean randomCapitalizationDoesntAffectParser(@ForAll("randomCapitalStrings") List<String> input) {
    try {
      DatesFound datesFoundOne = parser.parse(input.get(0), referenceDate, hawkingConfiguration, lang);
      DatesFound datesFoundTwo = parser.parse(input.get(1), referenceDate, hawkingConfiguration, lang);
      String dateOne = getDate(datesFoundOne.getParserOutputs().get(0).getDateRange().getEnd().toString());
      String dateTwo = getDate(datesFoundTwo.getParserOutputs().get(0).getDateRange().getEnd().toString());
      return dateOne.equals(dateTwo);
    } catch (Exception e) {
      return false;
    }
  }

  @Provide
  Arbitrary<List<String>> randomSpacedStrings() {
    Arbitrary<String> spaceOne = Arbitraries.strings().ofMinLength(1).ofMaxLength(5).withChars(" ");
    Arbitrary<String> spaceTwo = Arbitraries.strings().ofMinLength(1).ofMaxLength(5).withChars(" ");
    Arbitrary<String> spaceThree = Arbitraries.strings().ofMinLength(1).ofMaxLength(5).withChars(" ");
    Arbitrary<String> spaceFour = Arbitraries.strings().ofMinLength(1).ofMaxLength(5).withChars(" ");
    return Combinators.combine(spaceOne, spaceTwo, spaceThree, spaceFour).as((s1, s2, s3, s4) ->
            List.of(
                    String.format("In 5 days, I am going to blow."),
                    String.format("In%s5%sdays, I am going to blow.", s1, s2),
                    String.format("In%s5%sdays, I am going to blow.", s3, s4),
                    String.format("%sIn%s5%sdays%s, I am going to blow.", s1, s2, s3, s4)
    ));
  }
  @Property
  @Label("Property 10: Spacing between words of an otherwise identical string should yield the same output")
  boolean randomSpacingDoesntAffectParser(@ForAll("randomSpacedStrings") List<String> input) {
    for (int i = 1; i < input.size(); i++) {
      DatesFound prevDateFound = parser.parse(input.get(i - 1), referenceDate, hawkingConfiguration, lang);
      DatesFound currDateFound = parser.parse(input.get(i), referenceDate, hawkingConfiguration, lang);
      String prevDate = getDate(prevDateFound.getParserOutputs().get(0).getDateRange().getEnd().toString());
      String curDate = getDate(currDateFound.getParserOutputs().get(0).getDateRange().getEnd().toString());
      if (!prevDate.equals(curDate)) {
        return false;
      }
    }
    return true;
  }

}
