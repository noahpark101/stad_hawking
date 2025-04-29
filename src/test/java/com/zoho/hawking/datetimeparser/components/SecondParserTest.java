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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecondParserTest {

  public AbstractLanguage engLang;
  public HawkingConfiguration hawkConfig;
  public Date refDate;
  public DateAndTime dateAndTime;

  @BeforeEach
  public void setup() {
    refDate = new Date(120, Calendar.DECEMBER, 1); // Make reference date rn, like in HawkingDemo
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
    // TODO: Might not be dateSubstr, (Hi. It's July 4 today -> It's July 4 today (dateSubstr) -> July 4 today (parsed text)
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
  @DisplayName("Basic test")
  public void basicTest() {
    // Input variables should be same across all parsers
    String inputSentence = "The meeting starts in 30 seconds.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 22, 32);
    String xmlSubstr = "<exact_number>30</exact_number> <second_span>seconds</second_span>";
    String tense = "";
    
    // Get SecondParser object
    continueSetup(trip, inputSentence, inputSentence, tense);
    DateTimeComponent secondParser = new SecondParser(xmlSubstr, tense, dateAndTime, engLang);
    
    // Collection of most of the relevant DateTimeComponent fields
    assertTrue(secondParser.isNumberPresent);
    assertEquals(30, secondParser.number);
    assertEquals("30", secondParser.exactNumber);
    assertFalse(secondParser.isExactTimeSpan);
    assertEquals("seconds", secondParser.timeSpan);
    assertEquals(0, secondParser.timeSpanValue);
    assertEquals("", secondParser.tenseIndicator);
  }
  
  @Test
  @DisplayName("Past exact time")
  // FAULT: does not get the correct amount of seconds (15 instead of 45)
  public void pastExactTimeTest() {
    String inputSentence = "The meeting started 45 seconds ago.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 20, 34);
    String xmlSubstr = "<exact_number>45</exact_number> <second_span>seconds</second_span> <implict_postfix>ago</implict_postfix>";
    String tense = "PAST";
    
    continueSetup(trip, inputSentence, inputSentence, tense);
    
    // Get SecondParser object
    DateTimeComponent secondParser = new SecondParser(xmlSubstr, tense, dateAndTime, engLang);
    assertTrue(secondParser.isNumberPresent);
    assertEquals(45, secondParser.number);
    assertEquals("45", secondParser.exactNumber);
    assertFalse(secondParser.isExactTimeSpan);
    assertEquals("seconds", secondParser.timeSpan);
    assertEquals(0, secondParser.timeSpanValue);
    assertEquals("ago", secondParser.tenseIndicator);
    
    secondParser.past();
    assertEquals(45, secondParser.dateAndTime.getDateAndTime().getSecondOfMinute());
  }
  
  @Test
  @DisplayName("Vague past: last few seconds")
  public void vagueLastFewSecondsTest() {
    String inputSentence = "The meeting happened in the last few seconds.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 28, 44);
    String xmlSubstr = "<implict_prefix>last</implict_prefix> <implict_prefix>few</implict_prefix> <second_span>seconds</second_span>";
    String tense = "PAST";
    
    continueSetup(trip, inputSentence, inputSentence, tense);
    
    // Get SecondParser object
    DateTimeComponent secondParser = new SecondParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("seconds", secondParser.timeSpan);
    assertEquals("last", secondParser.tenseIndicator);
    assertFalse(secondParser.isExactTimeSpan);
  }
  
  @Test
  @DisplayName("Vague Past: seconds ago")
  public void vagueSecondsAgoTest() {
    String inputSentence = "The meeting was a few seconds ago.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 18, 33);
    String xmlSubstr = "<implict_prefix>few</implict_prefix> <second_span>seconds</second_span> <implict_postfix>ago</implict_postfix>";
    String tense = "PAST";
    
    continueSetup(trip, inputSentence, inputSentence, tense);
    
    // Get SecondParser object
    DateTimeComponent secondParser = new SecondParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("seconds", secondParser.timeSpan);
    assertFalse(secondParser.isExactTimeSpan);
  }
  
  @Test
  @DisplayName("Ordinal")
  public void ordinalSecondTest() {
    String inputSentence = "It's the 30th second of the minute.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 9, 34);
    String xmlSubstr = "<exact_number>30th</exact_number> <second_span>second</second_span> <implict_prefix>of</implict_prefix>";
    String tense = "";
    
    continueSetup(trip, inputSentence, inputSentence, tense);
    
    // Get SecondParser object
    DateTimeComponent secondParser = new SecondParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("30th", secondParser.exactNumber);
    assertTrue(secondParser.isOrdinal);
  }
  
  @Test
  @DisplayName("Plural seconds")
  public void fiveSecondsTest() {
    String inputSentence = "The countdown will start in 5 seconds.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 25, 37);
    String xmlSubstr = "<implict_prefix>in</implict_prefix> <exact_number>5</exact_number> <second_span>seconds</second_span>";
    String tense = "FUTURE";
    
    continueSetup(trip, inputSentence, inputSentence, tense);
    
    // Get SecondParser object
    DateTimeComponent secondParser = new SecondParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("seconds", secondParser.timeSpan);
    assertEquals(5, secondParser.number);
    assertEquals("5", secondParser.exactNumber);
    secondParser.future();
    assertEquals(5, secondParser.dateAndTime.getDateAndTime().getSecondOfMinute());
  }
  
  @Test
  @DisplayName("Next specified second")
  // FAULT: interprets the second in "next second" as 2nd
  public void nextSecondTest() {
    String inputSentence = "The alarm will go off in the next second.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 29, 40);
    String xmlSubstr = "<implict_prefix>next</implict_prefix> <exact_number>2nd</exact_number>";
    String tense = "FUTURE";
    
    continueSetup(trip, inputSentence, inputSentence, tense);
    
    // Get SecondParser object
    DateTimeComponent secondParser = new SecondParser(xmlSubstr, tense, dateAndTime, engLang);
    assertFalse(secondParser.isExactTimeSpan);
    assertEquals("second", secondParser.timeSpan);
    assertEquals("next", secondParser.tenseIndicator);
    
    secondParser.future();
    assertEquals(1, secondParser.dateAndTime.getDateAndTime().getSecondOfMinute());
  }
  
  @Test
  @DisplayName("Present")
  public void presentTest() {
    String inputSentence = "It is 15 seconds past the minute.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 6, 32);
    String xmlSubstr = "<exact_number>15</exact_number> <second_span>seconds</second_span> <implict_prefix>past</implict_prefix> <implict_prefix>the</implict_prefix> <minute_span>minute</minute_span>";
    String tense = "";
    
    continueSetup(trip, inputSentence, inputSentence, tense);
    
    // Get SecondParser object
    DateTimeComponent secondParser = new SecondParser(xmlSubstr, tense, dateAndTime, engLang);
    secondParser.present();
    assertEquals(15, secondParser.dateAndTime.getDateAndTime().getSecondOfMinute());
  }
  
  @Test
  @DisplayName("Immediate past")
  // FAULT: we get a null pointer exception here since there is no number present
  public void immediatePastTest() {
    String inputSentence = "The event happened a few seconds ago.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 21, 36);
    String xmlSubstr = "<implict_prefix>few</implict_prefix> <second_span>seconds</second_span> <implict_postfix>ago</implict_postfix>";
    String tense = "PAST";
    
    continueSetup(trip, inputSentence, inputSentence, tense);
    
    // Get SecondParser object
    DateTimeComponent secondParser = new SecondParser(xmlSubstr, tense, dateAndTime, engLang);
    secondParser.immediatePast();
    assertTrue(secondParser.isImmediate);
    assertEquals(0, secondParser.number); // number - 1 since isImmediate is true
  }
  
  @Test
  @DisplayName("Immediate future")
  // FAULT: we get the same error here as well (null pointer exception since there is no number present)
  public void immediateFutureTest() {
    String inputSentence = "The event will happen in a few seconds.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 27, 38);
    String xmlSubstr = "<implict_prefix>few</implict_prefix> <second_span>seconds</second_span>";
    String tense = "FUTURE";
    
    continueSetup(trip, inputSentence, inputSentence, tense);
    
    // Get SecondParser object
    DateTimeComponent secondParser = new SecondParser(xmlSubstr, tense, dateAndTime, engLang);
    secondParser.immediateFuture();
    assertTrue(secondParser.isImmediate);
    assertEquals(0, secondParser.number); // number - 1 since isImmediate is true
  }
  
  @Test
  @DisplayName("Remainder")
  public void remainderTest() {
    String inputSentence = "The event will happen in the remaining seconds.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 27, 38);
    String xmlSubstr = "<implict_prefix>in</implict_prefix> <second_span>seconds</second_span>";
    String tense = "";
    
    continueSetup(trip, inputSentence, inputSentence, tense);
    
    // Get SecondParser object
    DateTimeComponent secondParser = new SecondParser(xmlSubstr, tense, dateAndTime, engLang);
    secondParser.remainder();
    
    // Verify that setSecondStartAndEndTime was called with correct parameters
    // This is an indirect test since we can't directly access the internal state
    assertNotNull(secondParser.dateAndTime.getStart());
    assertNotNull(secondParser.dateAndTime.getEnd());
  }
  
  @Test
  @DisplayName("Set previous dependency")
  public void setPreviousDependencyTest() {
    String inputSentence = "The timer updates every second.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 18, 30);
    String xmlSubstr = "<exact_number>2nd</exact_number>";
    String tense = "";
    
    continueSetup(trip, inputSentence, inputSentence, tense);
    
    // Get SecondParser object
    DateTimeComponent secondParser = new SecondParser(xmlSubstr, tense, dateAndTime, engLang);
    secondParser.setPreviousDependency();
    
    // Verify that the previous dependency was set correctly
    assertEquals(Constants.SECOND_SPAN_TAG, secondParser.dateAndTime.getPreviousDependency());
  }
}

