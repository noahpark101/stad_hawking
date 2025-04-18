package com.zoho.hawking;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import com.zoho.hawking.language.english.model.DatesFound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class BlackBoxTest {
  private HawkingConfiguration hawkingConfiguration;
  private HawkingTimeParser parser;
  private Date referenceDate;

  /**
   * Gets a line from a datesFound.toString() given a substring to find
   * For example, can put in "Start : " as the substring to get that line
   * @param dateStr the datesFound.toString()
   * @param toFind the substring to find
   * @return the line containing the substring
   */
  private String getLine(String dateStr, String toFind) {
    List<String> ls = Arrays.asList(dateStr.split("\n"));
    for (int i = 0; i < ls.size(); i++) {
      String str = ls.get(i);
      if (str.indexOf(toFind) != -1) {
        return str;
      }
    }
    return "";
  }

  /**
   * Gets the date given a Start or End line
   * @param str the string in the following format: "Start/End : {Date}:{Time}"
   * @return the Date
   */
  private String getDate(String str) {
    List<String> ls = Arrays.asList(str.split(" "));
    String date = ls.get(2);
    List<String> ls2 = Arrays.asList(date.split("T"));
    return ls2.get(0);
  }

  /**
   * Gets the time given a Start or End line
   * @param str the string in the following format: "Start/End : {Date}:{Time}"
   * @return the Time
   */
  private String getTime(String str) {
    List<String> ls = Arrays.asList(str.split(" "));
    String date = ls.get(2);
    List<String> ls2 = Arrays.asList(date.split("T"));
    return ls2.get(1);
  }

  @BeforeEach
  void setUp() {
    hawkingConfiguration = new HawkingConfiguration();
    parser = new HawkingTimeParser();
    referenceDate = new Date(120, Calendar.DECEMBER, 1);
  }

  @Test
  @DisplayName("Basic Year Test 1")
  public void basicYearTest() {
    //set input text
    String inputText = "Hello it is the year 2020.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2020-12-31", date);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Basic Month Test 1")
  public void basicMonthTest() {
    //set input text
    String inputText = "Merry Christmas! It is December 25th today";
    referenceDate = new Date(120, Calendar.DECEMBER, 1);
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2020-12-25", date);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Basic Day Test 1")
  public void basicDayTest() {
    //set input text
    String inputText = "The meeting is on the 15th.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2020-05-15", date);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Exact Time Test 1")
  public void exactTimeTest() {
    //set input text
    String inputText = "The meeting is at 3:30 PM.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String time = getTime(end);
      assertEquals("15:30:00.000-05:00", time);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Different Time Zone Test 1")
  public void differentTimeZoneTest() {
    //set input text
    String inputText = "The meeting is at 3:30 PM PST.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("PST");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(5);
      hawkingConfiguration.setDay(15);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String time = getTime(end);
      assertEquals("15:30:00", time);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Multiple Dates Test 1")
  public void multipleDatesTest() {
    //set input text
    String inputText = "The meeting is on May 15th, 2020 at 3:30 PM and another on June 20th, 2020 at 2:00 PM.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));

      // Check first date
      String firstEnd = getLine(dateStr, "End : ");
      String firstDate = getDate(firstEnd);
      String firstTime = getTime(firstEnd);
      assertEquals("2020-05-15", firstDate);
      assertEquals("15:30:00", firstTime);

      // Check second date (would need to extract from the full string)
      // This is a simplified check - in a real test, you'd need to parse multiple dates properly
      assert(dateStr.contains("2020-06-20"));
      assert(dateStr.contains("14:00:00"));
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Time Duration Test 1")
  public void timeDurationTest() {
    //set input text
    String inputText = "The meeting is from 3:30 PM to 5:00 PM.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(5);
      hawkingConfiguration.setDay(15);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));

      // Check start time
      String start = getLine(dateStr, "Start : ");
      String startTime = getTime(start);
      assertEquals("15:30:00", startTime);

      // Check end time
      String end = getLine(dateStr, "End : ");
      String endTime = getTime(end);
      assertEquals("17:00:00", endTime);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Time Duration in Days Test 2")
  public void timeDurationTest2() {
    //set input text
    String inputText = "The meeting is from 3:30 PM and will be 2 hours long.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));

      // Check start date
      String start = getLine(dateStr, "Start : ");
      String startDate = getDate(start);
      assertEquals("2020-05-15", startDate);

      // Check end date
      String end = getLine(dateStr, "End : ");
      String endDate = getDate(end);
      assertEquals("2020-05-20", endDate);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Time Duration in Days Test 3")
  public void timeDurationTest3() {
    //set input text
    String inputText = "The meeting will end at 5:30 PM and will be 2 hours long.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));

      // Check start date
      String start = getLine(dateStr, "Start : ");
      String startDate = getDate(start);
      assertEquals("2020-05-15", startDate);

      // Check end date
      String end = getLine(dateStr, "End : ");
      String endDate = getDate(end);
      assertEquals("2020-05-20", endDate);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Time Duration in Days Test 1")
  public void timeDurationInDaysTest() {
    //set input text
    String inputText = "The conference is from May 15th to May 20th.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));

      // Check start date
      String start = getLine(dateStr, "Start : ");
      String startDate = getDate(start);
      assertEquals("2020-05-15", startDate);

      // Check end date
      String end = getLine(dateStr, "End : ");
      String endDate = getDate(end);
      assertEquals("2020-05-20", endDate);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Time Duration in Days Test 2")
  public void timeDurationInDaysTest2() {
    //set input text
    String inputText = "The project will take 5 days starting from May 15th.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));

      // Check start date
      String start = getLine(dateStr, "Start : ");
      String startDate = getDate(start);
      assertEquals("2020-05-15", startDate);

      // Check end date (5 days after start)
      String end = getLine(dateStr, "End : ");
      String endDate = getDate(end);
      assertEquals("2020-05-20", endDate);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Time Duration in Days Test 3")
  public void timeDurationInDaysTest3() {
    //set input text
    String inputText = "The conference will end on May 20th and will be 5 days long.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));

      // Check start date
      String start = getLine(dateStr, "Start : ");
      String startDate = getDate(start);
      assertEquals("2020-05-15", startDate);

      // Check end date (5 days after start)
      String end = getLine(dateStr, "End : ");
      String endDate = getDate(end);
      assertEquals("2020-05-20", endDate);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Relational Prefix Day Test 1")
  public void relationalPrefixDayTest() {
    //set input text
    String inputText = "The meeting is next Monday.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(5);
      hawkingConfiguration.setDay(15); // Assuming this is a Friday
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2020-05-18", date); // Next Monday would be May 18, 2020
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Relational Postfix Day Test 1")
  public void relationalPostfixDayTest() {
    //set input text
    String inputText = "The meeting is Monday next week.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(5);
      hawkingConfiguration.setDay(15); // Assuming this is a Friday
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2020-05-18", date); // Next Monday would be May 18, 2020
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Relational Prefix Month Test 1")
  public void relationalPrefixMonthTest() {
    //set input text
    String inputText = "The meeting is next month.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(5);
      hawkingConfiguration.setDay(15); // Assuming this is a Friday
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2021-06-15", date); // Next month would be June 15, 2021
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Relational Postfix Month Test 1")
  public void relationalPostfixMonthTest() {
    //set input text
    String inputText = "The meeting is next month.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(5);
      hawkingConfiguration.setDay(15); // Assuming this is a Friday
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2021-06-15", date); // Next month would be June 15, 2021
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Relational Prefix Year Test 1")
  public void relationalPrefixYearTest() {
    //set input text
    String inputText = "The meeting is next year.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(5);
      hawkingConfiguration.setDay(15); // Assuming this is a Friday
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2021-06-15", date); // Next month would be June 15, 2021
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Relational Postfix Year Test 1")
  public void relationalPostfixYearTest() {
    //set input text
    String inputText = "The meeting is next year.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(5);
      hawkingConfiguration.setDay(15); // Assuming this is a Friday
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2021-06-15", date); // Next month would be June 15, 2021
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Reference Time Test 1")
  public void referenceTimeTest() {
    //set input text
    String inputText = "The meeting is 2 hours after the conference.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(5);
      hawkingConfiguration.setDay(15);
      hawkingConfiguration.setHour(10);
      hawkingConfiguration.setMinute(0);
      hawkingConfiguration.setSecond(0);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getTime(end);
      assertEquals("12:00:00", date); // 2 days after May 15, 2020
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Reference Day Test 1")
  public void referenceDayTest() {
    //set input text
    String inputText = "The meeting is 2 days after the conference.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(5);
      hawkingConfiguration.setDay(15);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2020-05-17", date); // 2 days after May 15, 2020
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Reference Month Test 1")
  public void referenceMonthTest() {
    //set input text
    String inputText = "The meeting is 2 months from today.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(5);
      hawkingConfiguration.setDay(15);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2020-07-15", date); // 2 months from May 15, 2020
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Reference Year Test 1")
  public void referenceYearTest() {
    //set input text
    String inputText = "My graduation is 2 years from today";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(5);
      hawkingConfiguration.setDay(15);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2022-05-15", date); // 2 years from May 15, 2020
    } catch (Exception e) {
      assert(false);
    }
  }



  //TODO
  /*
    Basic Tests for days
    Exact Times/Dates
    Different Time zones
    Multiple Dates Basics
    Time Relation/Duration (use start - end for this)
    Relational:
      prefix + postfix
      Reference times
    Remember to do multiple tests for each, using years, months, days, etc.
   */
}
