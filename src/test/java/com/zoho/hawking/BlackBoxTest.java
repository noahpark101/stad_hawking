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

/**
 * BlackBoxTest for Hawking Date Parser
 * 
 * Test Partitions:
 * 1. Basic Date Components
 *    - Year parsing (e.g., "2020", "next year")
 *    - Month parsing (e.g., "December", "next month")
 *    - Day parsing (e.g., "15th", "tomorrow")
 * 
 * 2. Time Components
 *    - Hours (12/24 hour format)
 *    - Minutes
 *    - Seconds
 *    - AM/PM indicators
 *    - Time without date
 * 
 * 3. Time Zones
 *    - Different timezone parsing
 *    - Timezone conversions
 *    - Default timezone handling
 * 
 * 4. Multiple Dates
 *    - Multiple dates in single text
 *    - Date ranges
 *    - Overlapping dates
 * 
 * 5. Duration/Intervals
 *    - Hour-based durations
 *    - Day-based durations
 *    - Week-based durations
 *    - Month-based durations
 * 
 * 6. Relative Dates
 *    - Prefix-based (e.g., "next Monday", "last Friday")
 *    - Postfix-based (e.g., "Monday next week")
 *    - Combined prefix/postfix
 * 
 * 7. Reference Time Based
 *    - Relative to another date
 *    - Before/after references
 *    - Between dates
 */
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

  // Partition 1: Basic Date Components
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
  @DisplayName("Basic Year Test 2")
  public void basicYearTest2() {
    //set input text
    String inputText = "The meeting is next year.";
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
      assertEquals("2021-12-01", date);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Basic Month Test 1")
  public void basicMonthTest() {
    //set input text
    String inputText = "Merry Christmas! It is December 25th today";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setDay(15);
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
  @DisplayName("Basic Month Test 2")
  public void basicMonthTest2() {
    //set input text
    String inputText = "The meeting is next month.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setDay(15);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2021-01-01", date);
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
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(5);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2020-12-15", date);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Basic Day Test 2")
  public void basicDayTest2() {
    //set input text
    String inputText = "The meeting is tomorrow.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(12);
      hawkingConfiguration.setDay(1);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2020-12-02", date);
    } catch (Exception e) {
      assert(false);
    }
  }

  // Partition 2: Time Components
  @Test
  @DisplayName("Basic Time Test 1")
  public void basicTimeTest() {
    //set input text
    String inputText = "The meeting is at 3:30 PM.";
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
      String end = getLine(dateStr, "End : ");
      String time = getTime(end);
      assertEquals("15:30:00", time);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Military Time Test 1")
  public void militaryTimeTest() {
    //set input text
    String inputText = "The meeting is at 15:30.";
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
      String end = getLine(dateStr, "End : ");
      String time = getTime(end);
      assertEquals("15:30:00", time);
    } catch (Exception e) {
      assert(false);
    }
  }

  // Partition 3: Time Zones
  @Test
  @DisplayName("Time Zone Test 1")
  public void timeZoneTest() {
    //set input text
    String inputText = "The meeting is at 3:30 PM PST.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("PST");
      hawkingConfiguration.setYear(2020);
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
  @DisplayName("Different Time Zone Test 1")
  public void differentTimeZoneTest() {
    //set input text
    String inputText = "The meeting is at 3:30 PM EST.";
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
      assertEquals("12:30:00", time); // 3:30 PM EST = 12:30 PM PST
    } catch (Exception e) {
      assert(false);
    }
  }

  // Partition 4: Multiple Dates
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
  @DisplayName("Multiple Dates Test 2")
  public void multipleDatesTest2() {
    //set input text
    String inputText = "The meeting is from May 15th to May 20th, and another from May 18th to May 25th.";
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
      assert(dateStr.contains("2020-05-15"));
      assert(dateStr.contains("2020-05-20"));
      assert(dateStr.contains("2020-05-18"));
      assert(dateStr.contains("2020-05-25"));
    } catch (Exception e) {
      assert(false);
    }
  }

  // Partition 5: Duration/Intervals
  @Test
  @DisplayName("Duration in Hours Test 1")
  public void durationInHoursTest() {
    //set input text
    String inputText = "The meeting is from 3:30 PM to 5:00 PM.";
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
  @DisplayName("Duration in Days Test 1")
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
      assertEquals("2020-12-01", startDate);
      
      // Check end date
      String end = getLine(dateStr, "End : ");
      String endDate = getDate(end);
      assertEquals("2020-12-06", endDate);
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Duration Test 3")
  public void durationTest3() {
    //set input text
    String inputText = "The conference is for 2 weeks starting May 15th.";
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
      String start = getLine(dateStr, "Start : ");
      String startDate = getDate(start);
      String end = getLine(dateStr, "End : ");
      String endDate = getDate(end);
      assertEquals("2020-12-01", startDate);
      assertEquals("2020-12-15", endDate);
    } catch (Exception e) {
      assert(false);
    }
  }

  // Partition 6: Relative Dates
  @Test
  @DisplayName("Prefix Test 1")
  public void prefixTest() {
    //set input text
    String inputText = "The meeting is next Monday.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020); // Assuming this is a Friday
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2020-12-07", date); // Next Monday from Dec 1, 2020
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Postfix Test 1")
  public void postfixTest() {
    //set input text
    String inputText = "The meeting is Monday next week.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020); // Assuming this is a Friday
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2020-12-07", date); // Next Monday from Dec 1, 2020
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Relative Date Test 2")
  public void relativeDateTest2() {
    //set input text
    String inputText = "The meeting was last week.";
    try {
      //set hawking configuration
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("EDT");
      hawkingConfiguration.setYear(2020);
      hawkingConfiguration.setMonth(12);
      hawkingConfiguration.setDay(1);
      //find dates
      DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
      String dateStr = datesFound.toString();
      //verify dates found
      assert(!dateStr.equals(""));
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2020-11-24", date); // Last week from Dec 1
    } catch (Exception e) {
      assert(false);
    }
  }

  // Partition 7: Reference Time Based
  @Test
  @DisplayName("Reference Time Test 1")
  public void referenceTimeTest() {
    //set input text
    String inputText = "The meeting is 2 days after the conference.";
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
      String end = getLine(dateStr, "End : ");
      String date = getDate(end);
      assertEquals("2020-12-03", date); // 2 days after Dec 1, 2020
    } catch (Exception e) {
      assert(false);
    }
  }

  @Test
  @DisplayName("Reference Time Test 2")
  public void referenceTimeTest2() {
    //set input text
    String inputText = "The meeting is between May 15th and May 20th.";
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
      String start = getLine(dateStr, "Start : ");
      String startDate = getDate(start);
      String end = getLine(dateStr, "End : ");
      String endDate = getDate(end);
      assertEquals("2020-12-01", startDate);
      assertEquals("2020-12-06", endDate);
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
