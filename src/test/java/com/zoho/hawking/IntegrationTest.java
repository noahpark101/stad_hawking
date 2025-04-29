package com.zoho.hawking;

import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import com.zoho.hawking.language.english.model.DateRange;
import com.zoho.hawking.language.english.model.DatesFound;
import com.zoho.hawking.language.english.model.ParserOutput;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegrationTest {
  private HawkingConfiguration hawkingConfiguration;
  private HawkingTimeParser parser;
  private Date referenceDate;
  private String defaultStartDate;
  /*
    High level overview
    HawkingTimeParser.predict()
      1. generates dateTimeEssentials for each date
      2. uses DateTimeGateWay.getDateAndTime() for each dateTimeEssentials generated
      3. DateTimeGateWay uses DateTimeProperties.setParsedDate() to the component map
      4. DateTimeGateWay uses HawkingTimeParser.setDateAndTime() to generate dates
      5. HawkingTimeParser.setDateAndTime() uses the different parser components to do so

      The problem with integration testing is that the entire software is bundled tightly together.

      Maybe we treat it as block box testing ish where I just input strings. But I verify the output by seeing the
      components of the output that correspond to certain parts of the code?
        1. like date range to verify date range is working well
        2. maybe the text to see that NLP is properly segmenting it?
        Need more ideas.

   */

  /**
   * Gets the date given a Start or End line
   * @param str the string in the following format: "Start/End : {Date}:{Time}"
   * @return the Date
   */
  private String getDate(String str) {
    List<String> ls = Arrays.asList(str.split("T"));
    return ls.get(0);
  }

  /**
   * Gets the time given a Start or End line
   * @param str the string in the following format: "Start/End : {Date}:{Time}"
   * @return the Time
   */
  private String getTime(String str) {
    List<String> ls = Arrays.asList(str.split("T"));
    return ls.get(1);
  }


  /*
    HawkingTImeParser is tightly coupled together. Thus we are going to do black box integration testing. I shall
    provide inputs to verify that certain components are working together like classification, segmentation, etc.
   */

  @BeforeEach
  void setUp() {
    hawkingConfiguration = new HawkingConfiguration();
    parser = new HawkingTimeParser();
    referenceDate = new Date(120, Calendar.DECEMBER, 1);
    defaultStartDate = "2020-12-01";
  }

  //Integration Tests for segmenting, classification, and DateTimeExtractor
  @Test
  @DisplayName("Standard Date Format 1: Month Day Year")
  public void standardDateFormatOne() {
    String inputText = "It is April 20 2025";
    hawkingConfiguration.setTimeZone("EDT");
    DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
    ParserOutput output = datesFound.getParserOutputs().get(0);
    //check date segmentation
    assertEquals("April 20 2025", output.getText());
    //check date classification
    DateRange dateRange = output.getDateRange();
    String end = getDate(dateRange.getEnd().toString());
    assertEquals("2025-04-20", end);
  }

  //FAULT: fails because segmentation fails to get portion of date within input string
  @Test
  @DisplayName("Standard Date Format 2: Day Month Year")
  public void standardDateFormatTwo() {
    String inputText = "It is 20 April 2025";
    hawkingConfiguration.setTimeZone("EDT");
    DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
    ParserOutput output = datesFound.getParserOutputs().get(0);
    //check date segmentation
    //FAULT: fails because segmentation fails to get portion of date within input string
    assertEquals("20 April 2025", output.getText());
    //check date classification
    DateRange dateRange = output.getDateRange();
    String end = getDate(dateRange.getEnd().toString());
    assertEquals("2025-04-20", end);
  }

  //FAULT: fails because DateTimeExtractor fails to parse date from string (segmentation works but not classification)
  @Test
  @DisplayName("Standard Date Format 3: Month/Day/Year")
  public void standardDateFormatThree() {
    String inputText = "It is 04/20/2025";
    hawkingConfiguration.setTimeZone("EDT");
    DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
    //FAULT: fails because DateTimeExtractor fails to parse date from string (segmentation works but not classification)
    ParserOutput output = datesFound.getParserOutputs().get(0);
    //check date segmentation
    assertEquals("04/20/2025", output.getText());
    //check date classification
    DateRange dateRange = output.getDateRange();
    String end = getDate(dateRange.getEnd().toString());
    assertEquals("2025-04-20", end);
  }

  //FAULT: fails because DateTimeExtractor fails to parse date from string
  @Test
  @DisplayName("Standard Date Format 4: Month-Day-Year")
  public void standardDateFormatFour() {
    String inputText = "It is 04-20-2025";
    hawkingConfiguration.setTimeZone("EDT");
    DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
    //FAULT: fails because DateTimeExtractor fails to parse date from string
    ParserOutput output = datesFound.getParserOutputs().get(0);
    //check date segmentation
    assertEquals("04-20-2025", output.getText());
    //check date classification
    DateRange dateRange = output.getDateRange();
    String end = getDate(dateRange.getEnd().toString());
    assertEquals("2025-04-20", end);
  }

  //integration testing for DateRange calculation and segmentation/classification

  //FAULT: fails because date range misconfigures start date
  @Test
  @DisplayName("Date Range 1: Week")
  public void dateRangeOne() {
    String inputText = "Next week, I am going on vacation.";
    hawkingConfiguration.setTimeZone("EDT");
    DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
    ParserOutput output = datesFound.getParserOutputs().get(0);
    //check date segmentation
    assertEquals("Next week", output.getText());
    //check date range
    DateRange dateRange = output.getDateRange();
    String start = getDate(dateRange.getStart().toString());
    String end = getDate(dateRange.getEnd().toString());
    //FAULT: fails because date range misconfigures start date
    assertEquals("2020-12-01", start);
    assertEquals("2020-12-08", end);
  }

  @Test
  @DisplayName("Date Range 2: Day")
  public void dateRangeTwo() {
    String inputText = "In 5 days, I am going on vacation.";
    hawkingConfiguration.setTimeZone("EDT");
    DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
    ParserOutput output = datesFound.getParserOutputs().get(0);
    //check date segmentation
    assertEquals("5 days", output.getText());
    //check date range
    DateRange dateRange = output.getDateRange();
    String start = getDate(dateRange.getStart().toString());
    String end = getDate(dateRange.getEnd().toString());
    assertEquals("2020-12-01", start);
    assertEquals("2020-12-06", end);
  }

  //FAULT: fails because date range considers a month 30 days without considering reference month
  @Test
  @DisplayName("Date Range 3: Month")
  public void dateRangeThree() {
    String inputText = "Next month, I am going on vacation.";
    hawkingConfiguration.setTimeZone("EDT");
    DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
    ParserOutput output = datesFound.getParserOutputs().get(0);
    //check date segmentation
    assertEquals("Next month", output.getText());
    //check date range
    DateRange dateRange = output.getDateRange();
    String start = getDate(dateRange.getStart().toString());
    String end = getDate(dateRange.getEnd().toString());
    //FAULT: fails because date range considers a month 30 days without considering reference month
    assertEquals("2020-12-01", start);
    assertEquals("2020-01-01", end);
  }

  //FAULT: fails because date range miscalculates a year as 30 days
  @Test
  @DisplayName("Date Range 4: Year")
  public void dateRangeYear() {
    String inputText = "Next year, I am going on vacation.";
    hawkingConfiguration.setTimeZone("EDT");
    DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
    ParserOutput output = datesFound.getParserOutputs().get(0);
    //check date segmentation
    assertEquals("Next year", output.getText());
    //check date range
    DateRange dateRange = output.getDateRange();
    String start = getDate(dateRange.getStart().toString());
    String end = getDate(dateRange.getEnd().toString());
    //FAULT: fails because date range miscalculates a year as 30 days
    assertEquals("2020-12-01", start);
    assertEquals("2021-12-01", end);
  }

  //Integration testing for multidate segmentation (EnglishLanguage.getSeparateDates() + HawkingTimeParser.predict())
  @Test
  @DisplayName("MultiDate: 2 dates")
  public void multiDateTwoDates() {
    String inputText = "In 5 days, Robert is going on vacation. In 10 days, Elizabeth is leaving her family.";
    hawkingConfiguration.setTimeZone("EDT");
    DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
    String[] segments = {"5 days", "10 days"};
    String[] endDates = {"2020-12-06", "2020-12-11"};
    assertEquals(2, datesFound.getParserOutputs().size());
    for (int i = 0; i < datesFound.getParserOutputs().size(); i++) {
      ParserOutput output = datesFound.getParserOutputs().get(i);
      //check segments
      assertEquals(segments[i], output.getText());
      DateRange dateRange = output.getDateRange();
      //check date ranges
      String start = getDate(dateRange.getStart().toString());
      String end = getDate(dateRange.getEnd().toString());
      assertEquals(defaultStartDate, start);
      assertEquals(endDates[i], end);
    }
  }

  @Test
  @DisplayName("MultiDate: 3 dates")
  public void multiDateThreeDates() {
    String inputText =
            "In 5 days, Robert is going on vacation. In 10 days, Elizabeth is leaving her family. In 6 days, I am leaving.";
    hawkingConfiguration.setTimeZone("EDT");
    DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
    String[] segments = {"5 days", "10 days", "6 days"};
    String[] endDates = {"2020-12-06", "2020-12-11", "2020-12-07"};
    assertEquals(3, datesFound.getParserOutputs().size());
    for (int i = 0; i < datesFound.getParserOutputs().size(); i++) {
      ParserOutput output = datesFound.getParserOutputs().get(i);
      //check segments
      assertEquals(segments[i], output.getText());
      DateRange dateRange = output.getDateRange();
      //check date ranges
      String start = getDate(dateRange.getStart().toString());
      String end = getDate(dateRange.getEnd().toString());
      assertEquals(defaultStartDate, start);
      assertEquals(endDates[i], end);
    }
  }

  @Test
  @DisplayName("MultiDate: 4 dates")
  public void multiDateFourDates() {
    String inputText =
            "In 5 days, Robert is going on vacation. In 10 days, Elizabeth is leaving her family. In 6 days, I am leaving. In 15 days, Robert is getting a new car.";
    hawkingConfiguration.setTimeZone("EDT");
    DatesFound datesFound = parser.parse(inputText, referenceDate, hawkingConfiguration, "eng");
    String[] segments = {"5 days", "10 days", "6 days", "15 days"};
    String[] endDates = {"2020-12-06", "2020-12-11", "2020-12-07", "2020-12-16"};
    assertEquals(4, datesFound.getParserOutputs().size());
    for (int i = 0; i < datesFound.getParserOutputs().size(); i++) {
      ParserOutput output = datesFound.getParserOutputs().get(i);
      //check segments
      assertEquals(segments[i], output.getText());
      DateRange dateRange = output.getDateRange();
      //check date ranges
      String start = getDate(dateRange.getStart().toString());
      String end = getDate(dateRange.getEnd().toString());
      assertEquals(defaultStartDate, start);
      assertEquals(endDates[i], end);
    }
  }

}
