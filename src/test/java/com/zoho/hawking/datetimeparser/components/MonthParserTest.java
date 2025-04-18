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

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MonthParserTest {

  public AbstractLanguage engLang;
  public HawkingConfiguration hawkConfig;
  public Date refDate;
  public DateAndTime dateAndTime;

  @BeforeEach
  public void setup() {
    refDate = new Date(); // Make reference date rn, like in HawkingDemo
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
    // TODO: "Today is December 25" separates today from December 25 smh

    // Input variables should be same across all parsers
    String inputSentence = "Merry Christmas! It is December 25 today.";
    String dateSubstr = "It is December 25 today.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 6, 23);
    String xmlSubstr = "<month_of_year>december</month_of_year> <exact_number>25</exact_number>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);

    // TODO: If you want, move comments over as Javadocs in DateTimeComponent
    // Collection of most of the relevant DateTimeComponent fields
    assertTrue(monthParser.isNumberPresent); // A number is associated with the current span (month in this case)...
    assertEquals(25, monthParser.number); // ...but might not be the number representing the span...
    assertEquals("25", monthParser.exactNumber); // ...and this is just the extracted value from <exact_number> tag.
    assertTrue(monthParser.isExactTimeSpan); // If a specific instance of span is known...
    assertEquals("december", monthParser.timeSpan); // ...and if so this field will be populated.
    assertEquals(0, monthParser.timeSpanValue); // Integer version of timeSpan, only used w/ day and year
    assertFalse(monthParser.isSet); // If the span is repeated (like "monthly" or "every March", see DateTimeParserSetConstants).
    assertFalse(monthParser.isImmediate); // Immediate's one of the tenses.
    assertFalse(monthParser.isOrdinal); // If the span is specified as ordinal, like *4th* month of 2025.
    assertEquals("", monthParser.tenseIndicator); // Uses related words like "on" to further pinpoint tense

    // Now that the parser is set up with info of month (and day), we can fine tune the dateAndTime obj.
    // Use present()/past()/future() relative to the reference date (rn).
    monthParser.past();
    assertEquals(2024, dateAndTime.getDateAndTime().getYear());
    assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
    assertEquals(25, dateAndTime.getDateAndTime().getDayOfMonth());

    // Subsequent tense fxn calls should use a clean dateAndTime obj,
    // o/w getting present of 12/25/2024 will just keep year at 2024, not 2025
    continueSetup(trip, inputSentence, dateSubstr, tense);
    monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    monthParser.present();
    assertEquals(2025, dateAndTime.getDateAndTime().getYear());
    assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
    assertEquals(25, dateAndTime.getDateAndTime().getDayOfMonth());

    continueSetup(trip, inputSentence, dateSubstr, tense);
    monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    monthParser.future();
    assertEquals(2025, dateAndTime.getDateAndTime().getYear());
    assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
    assertEquals(25, dateAndTime.getDateAndTime().getDayOfMonth());

    // TODO: Learn immediate tense, make separate test cases for it

    monthParser.setPreviousDependency();
  }

  @Test
  @DisplayName("Past tense")
  public void pastTense() {

    String inputSentence = "It's great to meet you. We once met at August 3.";
    String dateSubstr = "We once met at August 3.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 24);
    String xmlSubstr = "<month_of_year>august</month_of_year> <exact_number>3</exact_number>";
    String tense = "PAST";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("august", monthParser.timeSpan);
    assertTrue(monthParser.isExactTimeSpan);

    // When running the program, if you do August 3, 2024, MonthParser receives sentenceTense = PRESENT
    // because YearParser goes first and transforms dateTime from initial reference time (rn) to
    // rn but in 2024, so MonthParser transforming dateTime is now "present" relative to 2024.
    // So doing just August 3 allows MonthParser to parse in the "past" relative to rn
    monthParser.past();
    assertEquals(2024, dateAndTime.getDateAndTime().getYear());
    assertEquals(8, dateAndTime.getDateAndTime().getMonthOfYear());
    assertEquals(3, dateAndTime.getDateAndTime().getDayOfMonth());
  }

  @Test
  @DisplayName("Vague past: last September")
  public void vaguePastTense() {
    String inputSentence = "It's great to meet you. We once met last September.";
    String dateSubstr = "We once met last September.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 12, 26);
    String xmlSubstr = "<implict_prefix>last</implict_prefix> <month_of_year>september</month_of_year>";
    String tense = "PAST";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("september", monthParser.timeSpan);
    assertEquals("last", monthParser.tenseIndicator);
    assertTrue(monthParser.isExactTimeSpan);

    monthParser.past();
    assertEquals(2024, dateAndTime.getDateAndTime().getYear());
    assertEquals(9, dateAndTime.getDateAndTime().getMonthOfYear());
  }

  @Test
  @DisplayName("Vague Past: last January")
  public void vaguerPastTense() {
    // January 2024 or 2025?
    String inputSentence = "It's great to meet you. We once met last January.";
    String dateSubstr = "We once met last January.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 12, 24);
    String xmlSubstr = "<implict_prefix>last</implict_prefix> <month_of_year>january</month_of_year>";
    String tense = "PAST";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("january", monthParser.timeSpan);
    assertEquals("last", monthParser.tenseIndicator);
    assertTrue(monthParser.isExactTimeSpan);

    monthParser.past();
  }

  @Test
  @DisplayName("Ordinal")
  public void ordinalDateTest() {
    // TODO: fourth works but "4th" fails
    String inputSentence = "Great! It's the fourth month of 2021.";
    String dateSubstr = "It's the fourth month of 2021.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 9, 29);
    String xmlSubstr = "<exact_number>4th</exact_number> <month_span>month</month_span> <implict_prefix>of</implict_prefix>";
    String tense = "";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("4th", monthParser.exactNumber);
  }

  @Test
  @DisplayName("Last year last month")
  public void lastYrLastMonthTest() {

    String inputSentence = "We had a celebration last year last month.";
    String dateSubstr = "We had a celebration last year last month.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 21, 41);
    String xmlSubstr = "<implict_prefix>last</implict_prefix> <year_span>year</year_span>";
    String tense = "PAST";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Conduct year parsing, make year a previous dependency of month
    dateAndTime.setPreviousDependency(Constants.YEAR_SPAN_TAG);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    // TODO: private monthParser.nthMonthOfYear = 12
    assertTrue(monthParser.isNumberPresent);
  }

  @Test
  @DisplayName("Set vague month")
  public void everyMonthTest() {

    // TODO: I don't think set tags have been implemented yet. Hardcoding the XML tags

    String inputSentence = "That's fine. Let's meet every month.";
    String dateSubstr = "Let's meet every month";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 11, 22);
    String xmlSubstr = "<set_prefix>every</set_prefix> <month_span>month</month_span>";
    String tense = "PRESENT";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertTrue(monthParser.isSet);

    monthParser.present();
    monthParser.setPreviousDependency();
  }

  @Test
  @DisplayName("Set explicit month")
  public void everyMarchTest() {

    // TODO: I don't think set tags have been implemented yet. Hardcoding the XML tags

    String inputSentence = "That's fine. Let's meet every March.";
    String dateSubstr = "Let's meet every March";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 11, 22);
    String xmlSubstr = "<set_prefix>every</set_prefix> <month_of_year>March</month_of_year>";
    String tense = "PRESENT";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertTrue(monthParser.isSet);
    assertTrue(monthParser.isExactTimeSpan);
    assertEquals("March", monthParser.timeSpan);

    monthParser.present();
    monthParser.setPreviousDependency();
  }

  @Test
  @DisplayName("Set monthly")
  public void monthlyTest() {

    // TODO: I don't think set tags have been implemented yet. Hardcoding the XML tags

    String inputSentence = "Should be great. I've returned monthly last year.";
    String dateSubstr = "I've returned monthly last year.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 14, 31);
    String xmlSubstr = "<set_month>monthly</set_month>";
    String tense = "PRESENT";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Conduct year parsing, make year a previous dependency of month
    dateAndTime.setPreviousDependency(Constants.YEAR_SPAN_TAG);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertTrue(monthParser.isSet);
    assertFalse(monthParser.isExactTimeSpan);
    assertEquals("monthly", monthParser.timeSpan);

  }

  @Test
  @DisplayName("Plural months")
  public void threeMonthsTest() {

    String inputSentence = "Alright. In 4 months it is then.";
    String dateSubstr = "In 4 months it is then.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 3, 11);
    String xmlSubstr = "<exact_number>4</exact_number> <month_span>months</month_span>";
    String tense = "TEMPORARY";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    // TODO: private monthParser.monthSpan = 4
    assertEquals("months", monthParser.timeSpan);
    assertEquals(4, monthParser.number);

  }

  @Test
  @DisplayName("Few (2) months")
  public void fewMonthsTest() {

    String inputSentence = "Oh I see. It is in a few months.";
    String dateSubstr = "It is in a few months.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 11, 21);
    String xmlSubstr = "<implict_prefix>few</implict_prefix> <month_span>months</month_span>";
    String tense = "TEMPORARY";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    // TODO: private monthParser.monthSpan = 2
    assertEquals("months", monthParser.timeSpan);

  }

  @Test
  @DisplayName("Present?")
  public void presentTest() {

    String inputSentence = "You should know. It is July 4 today.";
    String dateSubstr = "It is July 4 today.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 6, 18);
    String xmlSubstr = "<month_of_year>july</month_of_year> <exact_number>4</exact_number>";
    String tense = "TEMPORARY";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    monthParser.present();
    assertEquals(7, dateAndTime.getStart().getMonthOfYear());

  }




}

/* For Matt

      DateAndTime referenceDateAndTime = dateTimeOffsetReturn.getReferenceDate();
      DateAndTime dateAndTime = new DateAndTime(referenceDateTime);
      DateTimeOffsetReturn dateTimeOffsetReturn = TimeZoneExtractor.referenceDateExtractor(referenceDate, config, parsedText);

            DateAndTime dateAndTime = DateTimeParser.timeParser(
              dateTimeProperties.getReferenceTime(),
              tense,
              dateTimeProperties.getComponentsMap(),
              abstractLanguage);
   */