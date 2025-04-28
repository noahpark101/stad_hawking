package com.zoho.hawking.datetimeparser.components;

import com.zoho.hawking.datetimeparser.DateAndTime;
import com.zoho.hawking.datetimeparser.configuration.Configuration;
import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import com.zoho.hawking.datetimeparser.constants.ConfigurationConstants;
import com.zoho.hawking.language.AbstractLanguage;
import com.zoho.hawking.language.LanguageFactory;
import com.zoho.hawking.language.english.model.DateTimeEssentials;
import com.zoho.hawking.language.english.model.DateTimeOffsetReturn;
import com.zoho.hawking.utils.Constants;
import com.zoho.hawking.utils.DateTimeProperties;
import com.zoho.hawking.utils.TimeZoneExtractor;
import edu.stanford.nlp.util.Triple;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HourParserTest {

  public AbstractLanguage engLang;
  public HawkingConfiguration hawkConfig;
  public Date refDate;
  public DateAndTime dateAndTime;

  @BeforeEach
  public void setup() {
    refDate = new Date(1745164800000L); // Make reference date 4/20/2025
    engLang = LanguageFactory.getLanguageImpl("eng");
    hawkConfig = new HawkingConfiguration();
    hawkConfig.setTimeZone("EST");
    ConfigurationConstants.setConfiguration(new Configuration(hawkConfig));
  }

  /* Keep this helper fxn return void to allow tests to make more configs before creating DateTimeComponent object
   * like setting up previous dependencies for dateAndTime.
   */
  public void continueSetup(Triple<String, Integer, Integer> t, String inputSentence, String dateSubstr, String tense) {
    Pair<Boolean, List<Triple<String, Integer, Integer>>> relAndDate = Pair.of(false, List.of(t));

    // Instantiate DateTimeEssentials obj
    DateTimeEssentials dtEssentials = new DateTimeEssentials();
    dtEssentials.setParagraph(inputSentence);
    dtEssentials.addId();
    dtEssentials.setSentence(dateSubstr);
    dtEssentials.setTriples(relAndDate);
    dtEssentials.setTense(tense);
    DateTimeOffsetReturn dtOffsetReturn = TimeZoneExtractor.referenceDateExtractor(refDate, hawkConfig, dateSubstr);
    if(!TimeZoneExtractor.isTimeZonePresent){
      dtOffsetReturn = TimeZoneExtractor.referenceDateExtractor(refDate, hawkConfig, dateSubstr);
    }
    dtEssentials.setReferenceTime(dtOffsetReturn.getReferenceDate());
    dtEssentials.setTimeZoneOffSet(dtOffsetReturn.getTimeOffset());

    // Instantiate DateTimeProperties obj
    DateTimeProperties dtProps = new DateTimeProperties(dtEssentials, dtEssentials.getReferenceTime(), dtEssentials.getTriples().getFirst());
    dtProps.setParsedDate();

    // Instantiate DateAndTime obj
    dateAndTime = new DateAndTime(dtProps.getReferenceTime());

  }


  @Test
  @DisplayName("Basic Test 1: Number")
  void basicTest1() {
    // Input variables should be same across all parsers
    String inputSentence = "I will meet you in 1 hour.";
    String dateSubstr = "I will meet you in 1 hour.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 19, 25);
    String xmlSubstr = "<exact_number>1</exact_number> <hour_span>hour</hour_span>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals(1, hourParser.number);
  }

  @Test
  @DisplayName("Basic Test 1: Part of day")
  void basicTest2() {
    // Input variables should be same across all parsers
    String inputSentence = "This morning";
    String dateSubstr = "This morning";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 12);
    String xmlSubstr = "<implict_prefix>this</implict_prefix> <part_of_day>morning</part_of_day>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("morning", hourParser.timeSpan);
  }

  @Test
  @DisplayName("Basic Test 1: set hourly")
  void basicTest3() {
    // Input variables should be same across all parsers
    String inputSentence = "This morning";
    String dateSubstr = "This morning";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 12);
    String xmlSubstr = "<implict_prefix>this</implict_prefix> <set_hour>morning</set_hour>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("morning", hourParser.timeSpan);
  }

  //should be an exact time span but isn't
  @Test
  @DisplayName("Basic Test 1: exact time")
  void basicTest4() {
    // Input variables should be same across all parsers
    String inputSentence = "It is 5:00 pm";
    String dateSubstr = "It is 5:00 pm";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 6, 13);
    String xmlSubstr = "<exact_time>5:00 pm</exact_time>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals(true, hourParser.isExactTimeSpan);
  }

  @Test
  @DisplayName("Test: Past")
  void basicTest5() {
    // Input variables should be same across all parsers
    String inputSentence = "I will meet you in 1 hour.";
    String dateSubstr = "I will meet you in 1 hour.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 19, 25);
    String xmlSubstr = "<exact_number>1</exact_number> <hour_span>hour</hour_span>";
    String tense = "FUTURE";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.past();
  }

  //past does not
  @Test
  @DisplayName("Test 7: Past")
  void basicTest7() {
    // Input variables should be same across all parsers
    String inputSentence = "Last morning";
    String dateSubstr = "Last morning";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 12);
    String xmlSubstr = "<implict_prefix>last</implict_prefix> <part_of_day>morning</part_of_day>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.past();
    assert(hourParser.dateAndTime.getStart().isBefore(hourParser.dateAndTime.getEnd()));
    assertEquals("start:2025-04-19T06:00:00.000-04:00\n" +
            "end:2025-04-19T08:59:59.999-04:00\n", hourParser.dateAndTime.toString());
  }

  @Test
  @DisplayName("Test 8: Past2")
  void basicTest8() {
    // Input variables should be same across all parsers
    String inputSentence = "This morning";
    String dateSubstr = "This morning";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 12);
    String xmlSubstr = "<implict_prefix>this</implict_prefix> <part_of_day>morning</part_of_day>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.tenseIndicator = "";
    hourParser.past();
    assert(hourParser.dateAndTime.getStart().isBefore(hourParser.dateAndTime.getEnd()));
  }

  //calculated present as future (1 day from now) -> line 144 of hourParser
  @Test
  @DisplayName("Test 9: Present")
  void basicTest9() {
    // Input variables should be same across all parsers
    String inputSentence = "This morning";
    String dateSubstr = "This morning";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 12);
    String xmlSubstr = "<implict_prefix>this</implict_prefix> <part_of_day>morning</part_of_day>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.present();
    assertEquals("start:2025-04-20T06:00:00.000-04:00\n" +
            "end:2025-04-20T08:59:59.999-04:00\n", hourParser.dateAndTime.toString());
  }

  @Test
  @DisplayName("Test 10: Present2")
  void basicTest10() {
    // Input variables should be same across all parsers
    String inputSentence = "This morning";
    String dateSubstr = "This morning";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 12);
    String xmlSubstr = "<implict_prefix>this</implict_prefix> <part_of_day>morning</part_of_day>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.tenseIndicator = "";
    hourParser.present();
    assertEquals("", hourParser.sentenceTense);
  }

  @Test
  @DisplayName("Test 11: Present2")
  void basicTest11() {
    // Input variables should be same across all parsers
    String inputSentence = "5 hours ago";
    String dateSubstr = "5 hours ago";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 11);
    String xmlSubstr = "<exact_number>5</exact_number> <hour_span>hours</hour_span> <implict_postfix>ago</implict_postfix>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.present();
    assertEquals("ago", hourParser.tenseIndicator);
  }

  @Test
  @DisplayName("Test 12: Future")
  void basicTest12() {
    // Input variables should be same across all parsers
    String inputSentence = "This morning";
    String dateSubstr = "This morning";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 12);
    String xmlSubstr = "<implict_prefix>this</implict_prefix> <part_of_day>morning</part_of_day>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.tenseIndicator = "";
    hourParser.future();
    assertEquals("", hourParser.sentenceTense);
  }

  @Test
  @DisplayName("Test 13: Immediate Past")
  void basicTest13() {
    // Input variables should be same across all parsers
    String inputSentence = "Last morning";
    String dateSubstr = "Last morning";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 12);
    String xmlSubstr = "<implict_prefix>last</implict_prefix> <part_of_day>morning</part_of_day>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.isExactTimeSpan = true;
    hourParser.immediatePast();
    assert(hourParser.dateAndTime.getStart().isBefore(hourParser.dateAndTime.getEnd()));
  }

  @Test
  @DisplayName("Test 14: Immediate Past 2")
  void basicTest14() {
    // Input variables should be same across all parsers
    String inputSentence = "5 hours ago";
    String dateSubstr = "5 hours ago";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 11);
    String xmlSubstr = "<exact_number>5</exact_number> <hour_span>hours</hour_span> <implict_postfix>ago</implict_postfix>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.immediatePast();
    assert(hourParser.dateAndTime.getStart().isBefore(hourParser.dateAndTime.getEnd()));
  }

  @Test
  @DisplayName("Test 15: Exact Span")
  void basicTest15() {
    // Input variables should be same across all parsers
    String inputSentence = "5 hours ago";
    String dateSubstr = "5 hours ago";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 11);
    String xmlSubstr = "<exact_number>5</exact_number> <hour_span>hours</hour_span> <implict_postfix>ago</implict_postfix>";
    String tense = "PAST";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.isExactTimeSpan = false;
    hourParser.exactSpan();
  }

  @Test
  @DisplayName("Test 16: Exact Span 2")
  void basicTest16() {
    // Input variables should be same across all parsers
    String inputSentence = "It is 11:30 PM";
    String dateSubstr = "It is 11:30 PM";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 11);
    String xmlSubstr = "<exact_time>11:30 pm</exact_time>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.exactSpan();
    assertEquals("start:2025-04-20T23:30:00.000-04:00\n" +
            "end:2025-04-20T23:30:00.000-04:00\n", hourParser.dateAndTime.toString());
  }

  @Test
  @DisplayName("Test 17: immediate")
  void basicTest17() {
    // Input variables should be same across all parsers
    String inputSentence = "In an hour, I will leave.";
    String dateSubstr = "In an hour, I will leave";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 3, 10);
    String xmlSubstr = "<implict_prefix>an</implict_prefix> <hour_span>hour</hour_span>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.immediate();
    hourParser.immediateFuture();
  }

  @Test
  @DisplayName("Test 17: immediate2")
  void basicTest18() {
    // Input variables should be same across all parsers
    String inputSentence = "Early morning is the best";
    String dateSubstr = "Early morning is the best";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 13);
    String xmlSubstr = "<part_of_day>morning</part_of_day>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.immediate();
    hourParser.immediateFuture();
    assertEquals("start:2025-04-21T06:00:00.000-04:00\n" +
            "end:2025-04-21T08:59:59.999-04:00\n", hourParser.dateAndTime.toString());
    hourParser.sentenceTense = "PAST";
    hourParser.immediate();
    hourParser.immediateFuture();
    assertEquals("start:2025-04-21T06:00:00.000-04:00\n" +
            "end:2025-04-21T08:59:59.999-04:00\n", hourParser.dateAndTime.toString());
  }

  @Test
  @DisplayName("Test 19: immediate not ExactTimeSpan")
  void basicTest19() {
    // Input variables should be same across all parsers
    String inputSentence = "It has been 5 hours.";
    String dateSubstr = "It has been 5 hours.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 12, 19);
    String xmlSubstr = "<exact_number>5</exact_number> <hour_span>hours</hour_span>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.immediate();
    hourParser.immediateFuture();
    assertEquals("start:2025-04-20T15:00:00.000-04:00\n" +
            "end:2025-04-20T19:59:59.999-04:00\n", hourParser.dateAndTime.toString());
    hourParser.sentenceTense = "PAST";
    hourParser.immediate();
    hourParser.immediateFuture();
    assertEquals("start:2025-04-20T16:00:00.000-04:00\n" +
            "end:2025-04-20T19:59:59.999-04:00\n", hourParser.dateAndTime.toString());
  }

  // should have not added remainder at all to end date but it did
  @Test
  @DisplayName("Test 20: Remainder not exact time span")
  void basicTest20() {
    // Input variables should be same across all parsers
    String inputSentence = "It has been 5 hours.";
    String dateSubstr = "It has been 5 hours.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 12, 19);
    String xmlSubstr = "<exact_number>5</exact_number> <hour_span>hours</hour_span>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.remainder();
    // default remainder is none
    assertEquals("start:2025-04-20T11:00:00.000-04:00\n" +
            "end:2025-04-20T11:00:00.000-04:00\n", hourParser.dateAndTime.toString());
  }

  @Test
  @DisplayName("Test 21: Remainder is exact time span")
  void basicTest21() {
    // Input variables should be same across all parsers
    String inputSentence = "In the morning, I am tired.";
    String dateSubstr = "In the morning, I am tired.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 7, 14);
    String xmlSubstr = "<part_of_day>morning</part_of_day>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.remainder();
    // should set end time to morning
    assertEquals("start:2025-04-20T11:00:00.000-04:00\n" +
            "end:2025-04-20T08:59:59.999-04:00\n", hourParser.dateAndTime.toString());
  }

  @Test
  @DisplayName("Test 22: setPreviousDependency no previous dependency")
  void basicTest22() {
    // Input variables should be same across all parsers
    String inputSentence = "In the morning, I am tired.";
    String dateSubstr = "In the morning, I am tired.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 7, 14);
    String xmlSubstr = "<part_of_day>morning</part_of_day>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.setPreviousDependency();
    assertEquals(Constants.HOUR_SPAN_TAG, hourParser.dateAndTime.getPreviousDependency());
  }

  @Test
  @DisplayName("Test 23: setPreviousDependency previous dependency")
  void basicTest23() {
    // Input variables should be same across all parsers
    String inputSentence = "every morning.";
    String dateSubstr = "every morning.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 13);
    String xmlSubstr = "<part_of_day>morning</part_of_day>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.isSet = true;
    hourParser.setPreviousDependency();
    assertEquals("start:null\n" +
            "end:null\n" +
            "hourRecurrentPeriod:46800000\n" +
            "hour:0hourReccurentCount:6", hourParser.dateAndTime.toString());
  }


  //miscalculates recurrent period as 1/2 a day rather than a full, cascading into recurrentCount
  @Test
  @DisplayName("Test 24: setPreviousDependency previous dependency 2")
  void basicTest24() {
    // Input variables should be same across all parsers
    String inputSentence = "every morning.";
    String dateSubstr = "every morning.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 13);
    String xmlSubstr = "<part_of_day>morning</part_of_day>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.isSet = true;
    DateTime dt = new DateTime(1745164800000L);
    DateTime dt2 = new DateTime(1745251200000L);
    hourParser.dateAndTime.setStart(dt);
    hourParser.dateAndTime.setEnd(dt2);
    hourParser.dateAndTime.setPreviousDependency(Constants.HOUR_SPAN_TAG);
    hourParser.setPreviousDependency();

    assertEquals("start:2025-04-20T12:00:00.000-04:00\n" +
            "end:2025-04-21T12:00:00.000-04:00\n" +
            "hourRecurrentPeriod:86400000\n" +
            "hour:0hourReccurentCount:1", hourParser.dateAndTime.toString());
  }

  @Test
  @DisplayName("Test 25: nth span")
  void basicTest25() {
    // Input variables should be same across all parsers
    String inputSentence = "It is the 5th hour.";
    String dateSubstr = "It is the 5th hour.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 10, 18);
    String xmlSubstr = "<exact_number>5th</exact_number> <hour_span>hour</hour_span>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.nthSpan();
    assertEquals("start:2025-04-20T05:00:00.000-04:00\n" +
            "end:2025-04-20T05:59:59.999-04:00\n", hourParser.dateAndTime.toString());
  }

  @Test
  @DisplayName("Test 27: setPreviousDependency previous dependency 3")
  void basicTest26() {
    // Input variables should be same across all parsers
    String inputSentence = "Every 5 hours";
    String dateSubstr = "Every 5 hours.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 13);
    String xmlSubstr = "<exact_number>5</exact_number> <hour_span>hours</hour_span>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent hourParser = new HourParser(xmlSubstr, tense, dateAndTime, engLang);
    hourParser.isSet = true;
    DateTime dt = new DateTime(1745164800000L);
    DateTime dt2 = new DateTime(1745251200000L);
    hourParser.dateAndTime.setStart(dt);
    hourParser.dateAndTime.setEnd(dt2);
    hourParser.dateAndTime.setPreviousDependency(Constants.HOUR_SPAN_TAG);
    hourParser.setPreviousDependency();

    assertEquals("start:2025-04-20T12:00:00.000-04:00\n" +
            "end:2025-04-21T12:00:00.000-04:00\n" +
            "hourRecurrentPeriod:3600000\n" +
            "hour:0hourReccurentCount:1", hourParser.dateAndTime.toString());
  }



}
