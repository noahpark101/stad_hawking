package com.zoho.hawking.datetimeparser.components;

import com.zoho.hawking.datetimeparser.DateAndTime;
import com.zoho.hawking.datetimeparser.configuration.Configuration;
import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import com.zoho.hawking.datetimeparser.constants.ConfigurationConstants;
import com.zoho.hawking.datetimeparser.utils.DateTimeManipulation;
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
  @DisplayName("Basic test")
  public void basicTest() {

    // Input variables should be same across all parsers
    String inputSentence = "Merry Christmas! It is December 25 today.";
    String dateSubstr = "It is December 25 today.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 6, 23);
    String xmlSubstr = "<month_of_year>december</month_of_year> <exact_number>25</exact_number>";
    String tense = "";

    // Get MonthParser object
    continueSetup(trip, inputSentence, dateSubstr, tense);
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);

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
    // Use present()/past()/future() relative to the reference date (4/20/2025).
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

    continueSetup(trip, inputSentence, dateSubstr, tense);
    monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    monthParser.immediatePast();
    assertEquals(2024, dateAndTime.getDateAndTime().getYear());
    assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
    assertEquals(25, dateAndTime.getDateAndTime().getDayOfMonth());

    monthParser.setPreviousDependency();
  }

  @Test
  @DisplayName("Past exact time")
  public void pastExactTimeTest() {
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
    // because YearParser goes first and transforms dateTime from initial reference time (4/20/25) to
    // rn but in 2024, so MonthParser transforming dateTime is now "present" relative to 2024.
    // So doing just August 3 allows MonthParser to parse in the "past" relative to 4/20
    monthParser.past();
    assertEquals(2024, dateAndTime.getDateAndTime().getYear());
    assertEquals(8, dateAndTime.getDateAndTime().getMonthOfYear());
    assertEquals(3, dateAndTime.getDateAndTime().getDayOfMonth());

    continueSetup(trip, inputSentence, dateSubstr, tense);
    monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    monthParser.immediatePast();
    assertEquals(2024, dateAndTime.getDateAndTime().getYear());
    assertEquals(8, dateAndTime.getDateAndTime().getMonthOfYear());
    assertEquals(3, dateAndTime.getDateAndTime().getDayOfMonth());
  }

  @Test
  @DisplayName("Vague pre-New-Year past")
  public void vagueLastSepTest() {
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

    continueSetup(trip, inputSentence, dateSubstr, tense);
    monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    monthParser.immediatePast();
    assertEquals(2024, dateAndTime.getDateAndTime().getYear());
    assertEquals(9, dateAndTime.getDateAndTime().getMonthOfYear());
  }

  @Test
  @DisplayName("Vague post-New-Year past")
  public void vaguerLastJanTest() {
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

    // FAULT (sorta?) - Function unconditionally sets year back by 1 if tenseIndicator is populated. With this context it shouldn't
    monthParser.past();
//    assertEquals(2025, dateAndTime.getStart().getYear());
    assertEquals(1, dateAndTime.getStart().getMonthOfYear());
//    assertEquals(2025, dateAndTime.getEnd().getYear());
    assertEquals(1, dateAndTime.getEnd().getMonthOfYear());
  }

  @Test
  @DisplayName("Vague Past: months ago")
  public void vagueAgoTest() {
    String inputSentence = "I'm sorry. The event was 3 months ago.";
    String dateSubstr = "The event was 3 months ago.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 14, 26);
    String xmlSubstr = "<exact_number>3</exact_number> <month_span>months</month_span> <implict_postfix>ago</implict_postfix>";
    String tense = "PAST";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("months", monthParser.timeSpan);
    assertEquals(3, monthParser.number);
    assertEquals("ago", monthParser.tenseIndicator);
    assertFalse(monthParser.isExactTimeSpan);

    // FAULT: For "ago", span is kept at 3 months so start -> end is 3 months
    // FAULT: "ago" is configured with no end time, but this sentence should have one
    monthParser.past();
    assertEquals(2025, dateAndTime.getStart().getYear());
    assertEquals(1, dateAndTime.getStart().getMonthOfYear());
//    assertEquals(2025, dateAndTime.getEnd().getYear());
//    assertEquals(1, dateAndTime.getEnd().getMonthOfYear());

    continueSetup(trip, inputSentence, dateSubstr, tense);
    monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    monthParser.immediatePast();
    assertEquals(2025, dateAndTime.getStart().getYear());
    assertEquals(1, dateAndTime.getStart().getMonthOfYear());

  }

  @Test
  @DisplayName("One month ago")
  public void oneMonthAgoTest() {
    String inputSentence = "I performed at my concert one year one month ago.";
    String dateSubstr = "I performed at my concert one year one month ago.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 26, 48);
    String xmlSubstr = "<exact_number>one</exact_number> <month_span>month</month_span> <implict_postfix>ago</implict_postfix>";
    String tense = "PAST";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Control the year parsing, declare previous dependency of year
    dateAndTime.setDateAndTime(DateTimeManipulation.setYear(dateAndTime.getDateAndTime(), 2024));
    dateAndTime.setPreviousDependency(Constants.YEAR_SPAN_TAG);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("month", monthParser.timeSpan);
    assertEquals(1, monthParser.number);

    // FAULT: The "ago" only gets applied to the year when it should apply to both
//    assertEquals("ago", monthParser.tenseIndicator);
    assertFalse(monthParser.isExactTimeSpan);

    // (Same faults as the 3 months ago test)
    monthParser.past();
//    assertEquals(2024, dateAndTime.getStart().getYear());
//    assertEquals(2, dateAndTime.getStart().getMonthOfYear());
//    assertEquals(2025, dateAndTime.getEnd().getYear());
//    assertEquals(1, dateAndTime.getEnd().getMonthOfYear());

  }

  @Test
  @DisplayName("Past recent months")
  public void pastMonthsTest() {
    String inputSentence = "The celebrations have been going on for the past 2 months.";
    String dateSubstr = "The celebrations have been going on for the past 2 months.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 44, 57);
    String xmlSubstr = "<implict_prefix>past</implict_prefix> <exact_number>2</exact_number> <month_span>months</month_span>";
    String tense = "PAST";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertFalse(monthParser.isExactTimeSpan);

    // FAULT: Considering this a fault b/c "past" is the ONLY applicable immediate past preposition
    // and resets day/time instead of preserving it. BTW immediatePast/immediateFuture doesn't set object's
    // isImmediate to true so the date spans start to become inconsistent across immediate tenses
    monthParser.immediatePast();
    assertEquals(2, dateAndTime.getStart().getMonthOfYear());
//    assertEquals(20, dateAndTime.getStart().getDayOfMonth());
//    assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
//    assertEquals(20, dateAndTime.getEnd().getDayOfMonth());

    continueSetup(trip, inputSentence, dateSubstr, tense);
    monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    monthParser.immediate();
    assertEquals(3, dateAndTime.getStart().getMonthOfYear());
//    assertEquals(20, dateAndTime.getStart().getDayOfMonth());
    assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
//    assertEquals(20, dateAndTime.getEnd().getDayOfMonth());
  }

  @Test
  @DisplayName("Ordinal day of month")
  public void ordinalDayTest() {
    String inputSentence = "Great! It's the seventh of May.";
    String dateSubstr = "It's the seventh of May.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 9, 23);
    String xmlSubstr = "<exact_number>7th</exact_number> <implict_prefix>of</implict_prefix> <month_of_year>may</month_of_year>";
    String tense = "";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("may", monthParser.timeSpan);
    assertEquals(7, monthParser.number);
    assertTrue(monthParser.isOrdinal);
    assertTrue(monthParser.isExactTimeSpan);

    monthParser.nthSpan();
    assertEquals(5, dateAndTime.getStart().getMonthOfYear());
    assertEquals(2025, dateAndTime.getStart().getYear());
    assertEquals(5, dateAndTime.getEnd().getMonthOfYear());
    assertEquals(2025, dateAndTime.getEnd().getYear());

  }

  @Test
  @DisplayName("Ordinal month of year")
  public void ordinalMonthTest() {
    String inputSentence = "Great! It's the fifth month of 2021.";
    String dateSubstr = "It's the fifth month of 2021.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 9, 28);
    String xmlSubstr = "<exact_number>fifth</exact_number> <month_span>month</month_span> <implict_prefix>of</implict_prefix>";
    String tense = "";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Control the year parsing, declare previous dependency of year
    dateAndTime.setDateAndTime(DateTimeManipulation.setYear(dateAndTime.getDateAndTime(), 2021));
    dateAndTime.setPreviousDependency(Constants.YEAR_SPAN_TAG);

    // Get MonthParser object
    // FAULT: "fifth month of" is being treated the same way as "fifth of month"
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("fifth", monthParser.exactNumber);
//    assertTrue(monthParser.isOrdinal);
    assertFalse(monthParser.isExactTimeSpan);
    assertEquals("month", monthParser.timeSpan);

    monthParser.present();
    monthParser.setPreviousDependency();
//    assertEquals(5, monthParser.dateAndTime.getStart().getMonthOfYear());
//    assertEquals(2021, monthParser.dateAndTime.getStart().getMonthOfYear());
//    assertEquals(5, monthParser.dateAndTime.getStart().getMonthOfYear());
//    assertEquals(2021, monthParser.dateAndTime.getStart().getMonthOfYear());
  }

  @Test
  @DisplayName("Last year last month")
  public void lastYrLastMonthTest() {

    String inputSentence = "We had a celebration last year last month.";
    String dateSubstr = "We had a celebration last year last month.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 21, 41);
    String xmlSubstr = "<implict_prefix>last</implict_prefix> <month_span>month</month_span>";
    String tense = "PAST";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Control the year parsing, declare previous dependency of year
    dateAndTime.setDateAndTime(DateTimeManipulation.setYear(dateAndTime.getDateAndTime(), 2024));
    dateAndTime.setPreviousDependency(Constants.YEAR_SPAN_TAG);

    // Get MonthParser object
    // FAULT: Unable to create MonthParser obj because "last year last month" sets field isOrdinal to true
    // (even though it's not), so obj tries to parse a nonexistent number from the nonexistent ordinal word.
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("month", monthParser.timeSpan);
    assertEquals("last", monthParser.tenseIndicator);
  }

  @Test
  @DisplayName("Set vague month")
  public void everyMonthTest() {

    // NOTE: I don't think set tags have been implemented yet. Hardcoding the XML tags

    String inputSentence = "That's fine. We used to meet each month last year.";
    String dateSubstr = "Let's meet each month";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 11, 21);
    String xmlSubstr = "<set_prefix>each</set_prefix> <month_span>month</month_span>";
    String tense = "PRESENT";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertTrue(monthParser.isSet);
    assertFalse(monthParser.isExactTimeSpan);
    assertEquals("month", monthParser.timeSpan);

    // Suppose first we don't take into account the last year dependency
    monthParser.present();
    monthParser.setPreviousDependency();

    tense = "PAST";
    continueSetup(trip, inputSentence, dateSubstr, tense);
    monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);

    monthParser.present();
    monthParser.setPreviousDependency();

    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Control the year parsing, declare previous dependency of year
    dateAndTime.setDateAndTime(DateTimeManipulation.setYear(dateAndTime.getDateAndTime(), 2024));
    DateTimeManipulation.setYearStartAndEndTime(dateAndTime, 0, 0, 1, 2);
    dateAndTime.setPreviousDependency(Constants.YEAR_SPAN_TAG);
    monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);

    monthParser.present();
    monthParser.setPreviousDependency();
  }

  @Test
  @DisplayName("Set explicit month")
  public void everyMarchTest() {

    // NOTE: I don't think set tags have been implemented yet. Hardcoding the XML tags

    String inputSentence = "That's fine. Let's meet every March this year.";
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

    tense = "PAST";
    continueSetup(trip, inputSentence, dateSubstr, tense);
    monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);

    monthParser.present();
    monthParser.setPreviousDependency();

    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Control the year parsing, declare previous dependency of year
    tense = "PRESENT";
    dateAndTime.setDateAndTime(DateTimeManipulation.setYear(dateAndTime.getDateAndTime(), 2025));
    DateTimeManipulation.setYearStartAndEndTime(dateAndTime, 0, 0, 1, 2);
    dateAndTime.setPreviousDependency(Constants.YEAR_SPAN_TAG);
    monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);

    monthParser.present();
    monthParser.setPreviousDependency();
  }

  @Test
  @DisplayName("Set monthly")
  public void monthlyTest() {

    // NOTE: I don't think set tags have been implemented yet. Hardcoding the XML tags

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
    String tense = "FUTURE";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertEquals("months", monthParser.timeSpan);
    assertEquals(4, monthParser.number);

    // FAULT: Parser treats sentence like "over 4 months" instead of the more specific "in 4 months"
    monthParser.future();
    assertEquals(8, monthParser.dateAndTime.getDateAndTime().getMonthOfYear());
    assertEquals(8, monthParser.dateAndTime.getEnd().getMonthOfYear());
//    assertEquals(8, monthParser.dateAndTime.getStart().getMonthOfYear());

  }

  @Test
  @DisplayName("This specified month")
  public void thisSeptemberTest() {
    String inputSentence = "Sounds good. I go back to school this September.";
    String dateSubstr = "I go back to school this September.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 20, 34);
    String xmlSubstr = "<implict_prefix>this</implict_prefix> <month_of_year>september</month_of_year>";
    String tense = "";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertTrue(monthParser.isExactTimeSpan);
    assertEquals("september", monthParser.timeSpan);
    assertEquals("this", monthParser.tenseIndicator);

    monthParser.immediate();
    assertEquals(9, monthParser.dateAndTime.getStart().getMonthOfYear());
    assertEquals(2025, monthParser.dateAndTime.getStart().getYear());
    assertEquals(9, monthParser.dateAndTime.getEnd().getMonthOfYear());
    assertEquals(2025, monthParser.dateAndTime.getEnd().getYear());

    continueSetup(trip, inputSentence, dateSubstr, tense);
    monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    monthParser.sentenceTense = "PAST";
    monthParser.immediate();
    assertEquals(9, monthParser.dateAndTime.getStart().getMonthOfYear());
    assertEquals(2024, monthParser.dateAndTime.getStart().getYear());
    assertEquals(9, monthParser.dateAndTime.getEnd().getMonthOfYear());
    assertEquals(2024, monthParser.dateAndTime.getEnd().getYear());
  }

  @Test
  @DisplayName("Next specified month")
  public void nextSeptemberTest() {

    String inputSentence = "Sounds good. I go back to school next September.";
    String dateSubstr = "I go back to school next September.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 20, 34);
    String xmlSubstr = "<implict_prefix>next</implict_prefix> <month_of_year>september</month_of_year>";
    String tense = "FUTURE";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertTrue(monthParser.isExactTimeSpan);
    assertEquals("september", monthParser.timeSpan);
    assertEquals("next", monthParser.tenseIndicator);

    monthParser.future();
    assertEquals(9, monthParser.dateAndTime.getStart().getMonthOfYear());
    assertEquals(2026, monthParser.dateAndTime.getStart().getYear());
    assertEquals(9, monthParser.dateAndTime.getEnd().getMonthOfYear());
    assertEquals(2026, monthParser.dateAndTime.getEnd().getYear());

    continueSetup(trip, inputSentence, dateSubstr, tense);
    monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    monthParser.immediateFuture();
    assertEquals(9, monthParser.dateAndTime.getStart().getMonthOfYear());
    assertEquals(2025, monthParser.dateAndTime.getStart().getYear());
    assertEquals(9, monthParser.dateAndTime.getEnd().getMonthOfYear());
    assertEquals(2025, monthParser.dateAndTime.getEnd().getYear());
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
    assertEquals("months", monthParser.timeSpan);

    monthParser.immediate();
    assertEquals(4, monthParser.dateAndTime.getStart().getMonthOfYear());
    assertEquals(2025, monthParser.dateAndTime.getStart().getYear());
    assertEquals(5, monthParser.dateAndTime.getEnd().getMonthOfYear());
    assertEquals(2025, monthParser.dateAndTime.getEnd().getYear());

  }

  @Test
  @DisplayName("Immediate Future")
  public void upcomingMonthTest() {

    String inputSentence = "Oh wow. The event happens in the upcoming 2 months.";
    String dateSubstr = "The event happens in the upcoming 2 months.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 25, 42);
    String xmlSubstr = "<implict_prefix>upcoming</implict_prefix> <exact_number>2</exact_number> <month_span>months</month_span>";
    String tense = "PRESENT";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertFalse(monthParser.isExactTimeSpan);
    assertEquals("months", monthParser.timeSpan);
    assertEquals("upcoming", monthParser.tenseIndicator);

    monthParser.immediateFuture();
    assertEquals(5, monthParser.dateAndTime.getStart().getMonthOfYear());
    assertEquals(2025, monthParser.dateAndTime.getStart().getYear());
    assertEquals(6, monthParser.dateAndTime.getEnd().getMonthOfYear());
    assertEquals(2025, monthParser.dateAndTime.getEnd().getYear());

    continueSetup(trip, inputSentence, dateSubstr, tense);
    monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    monthParser.immediate();
    assertEquals(4, monthParser.dateAndTime.getStart().getMonthOfYear());
    assertEquals(2025, monthParser.dateAndTime.getStart().getYear());
    assertEquals(5, monthParser.dateAndTime.getEnd().getMonthOfYear());
    assertEquals(2025, monthParser.dateAndTime.getEnd().getYear());
  }

  @Test
  @DisplayName("Remaining month")
  public void remainingMonthTest() {

    String inputSentence = "This will go on for the rest of April.";
    String dateSubstr = "This will go on for the rest of April.";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 24, 37);
    String xmlSubstr = "<implict_prefix>of</implict_prefix> <month_of_year>april</month_of_year>";
    String tense = "FUTURE";
    continueSetup(trip, inputSentence, dateSubstr, tense);

    // Get MonthParser object
    DateTimeComponent monthParser = new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
    assertTrue(monthParser.isExactTimeSpan);
    assertEquals("april", monthParser.timeSpan);

    // Remaining tense indicator not implemented yet
    monthParser.tenseIndicator = "rest";

    monthParser.remainder();
    assertEquals(4, monthParser.dateAndTime.getStart().getMonthOfYear());
    assertEquals(2025, monthParser.dateAndTime.getStart().getYear());
    assertEquals(4, monthParser.dateAndTime.getEnd().getMonthOfYear());
    assertEquals(2025, monthParser.dateAndTime.getEnd().getYear());

  }
}
