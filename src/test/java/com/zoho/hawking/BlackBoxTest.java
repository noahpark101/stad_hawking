
package com.zoho.hawking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
    referenceDate = new Date();
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
      hawkingConfiguration.setMonth(5);
      hawkingConfiguration.setDay(15);
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
  @DisplayName("Basic Month Test 1")
  public void basicMonthTest() {
    //set input text
    String inputText = "Wow I can't believe it is December already.";
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
      assert(date.equals("2020-12-15"));
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
