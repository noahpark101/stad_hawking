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
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeekParserTest {

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
    @DisplayName("Basic week test")
    public void basicWeekTest() {
        String inputSentence = "Let's meet next week.";
        String dateSubstr = "next week.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 8);
        String xmlSubstr = "<implict_prefix>next</implict_prefix> <week_span>week</week_span>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get WeekParser object
        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("week", weekParser.timeSpan);
        assertEquals("next", weekParser.tenseIndicator);
        assertFalse(weekParser.isExactTimeSpan);

        weekParser.future();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(27, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(5, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(3, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Weekday test")
    // The fault here is that the parser seems to think that the "start" is next week or that it is getting the end and using it as the start
    public void weekdayTest() {
        String inputSentence = "I work on weekdays.";
        String dateSubstr = "on weekdays.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 11);
        String xmlSubstr = "<week_span>weekdays</week_span>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get WeekParser object
        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("weekdays", weekParser.timeSpan);
        assertFalse(weekParser.isExactTimeSpan);

        weekParser.present();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(21, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(25, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Weekend test")
    // Fault here results from the parser not picking up weekends correctly as the time span for the sentence 
    public void weekendTest() {
        String inputSentence = "I relax on weekends.";
        String dateSubstr = "on weekends.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 11);
        String xmlSubstr = "<weekend_span>weekends</weekend_span>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get WeekParser object
        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("weekends", weekParser.timeSpan);
        assertFalse(weekParser.isExactTimeSpan);

        weekParser.present();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(26, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(27, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Ordinal week test")
    public void ordinalWeekTest() {
        String inputSentence = "It's the third week of April.";
        String dateSubstr = "the third week of April.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 20);
        String xmlSubstr = "<exact_number>third</exact_number> <week_span>week</week_span> <implict_prefix>of</implict_prefix>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get WeekParser object
        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("third", weekParser.exactNumber);
        assertTrue(weekParser.isOrdinal);
        assertEquals("week", weekParser.timeSpan);

        weekParser.nthSpan();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(13, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(19, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Set weekly test")
    public void weeklyTest() {
        String inputSentence = "I have meetings weekly.";
        String dateSubstr = "meetings weekly.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 14);
        String xmlSubstr = "<set_week>weekly</set_week>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get WeekParser object
        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("weekly", weekParser.timeSpan);
        assertTrue(weekParser.isSet);

        weekParser.present();
        weekParser.setPreviousDependency();
        assertEquals(7 * 24 * 60 * 60 * 1000, dateAndTime.getWeekRecurrentPeriod());
    }

    @Test
    @DisplayName("Immediate week test")
    public void immediateWeekTest() {
        String inputSentence = "Let's meet this week.";
        String dateSubstr = "this week.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 9);
        String xmlSubstr = "<implict_prefix>this</implict_prefix> <week_span>week</week_span>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get WeekParser object
        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("this", weekParser.tenseIndicator);
        assertEquals("week", weekParser.timeSpan);

        weekParser.immediate();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(20, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(26, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Week remainder test")
    // for some reason, the end is being set as the 13th, the start of the previous week
    public void weekRemainderTest() {
        String inputSentence = "I'll finish the work by the end of this week.";
        String dateSubstr = "end of this week.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 15);
        String xmlSubstr = "<implict_prefix>end</implict_prefix> <week_span>week</week_span>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get WeekParser object
        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("week", weekParser.timeSpan);

        weekParser.remainder();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(20, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(27, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Past week test")
    public void pastWeekTest() {
        String inputSentence = "We met last week.";
        String dateSubstr = "last week.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 8);
        String xmlSubstr = "<implict_prefix>last</implict_prefix> <week_span>week</week_span>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("last", weekParser.tenseIndicator);
        assertEquals("week", weekParser.timeSpan);

        weekParser.past();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(13, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(19, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Multiple weeks test")
    // Once again, the start is being set the 27th, the start of the next week
    public void multipleWeeksTest() {
        String inputSentence = "I'll be away for 3 weeks.";
        String dateSubstr = "for 3 weeks.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 11);
        String xmlSubstr = "<exact_number>3</exact_number> <week_span>weeks</week_span>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("weeks", weekParser.timeSpan);
        assertEquals(3, weekParser.number);

        weekParser.future();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(20, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(5, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(11, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Week with previous dependency test")
    // the reference month is not being referenced correctly. Instead of finding the next month (May/5), it is staying in the dependency month (April/4)
    public void weekWithPreviousDependencyTest() {
        String inputSentence = "In the third week of next month.";
        String dateSubstr = "third week of next month.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 22);
        String xmlSubstr = "<exact_number>third</exact_number> <week_span>week</week_span> <implict_prefix>of</implict_prefix>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Set up month as previous dependency
        dateAndTime.setDateAndTime(DateTimeManipulation.setMonth(dateAndTime.getDateAndTime(), 0, 4));
        dateAndTime.setPreviousDependency(Constants.MONTH_SPAN_TAG);

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("third", weekParser.exactNumber);
        assertTrue(weekParser.isOrdinal);

        weekParser.nthSpan();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(5, dateAndTime.getStart().getMonthOfYear());
        assertEquals(12, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(5, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(18, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Week with year dependency test")
    // the year is not being set correctly. Instead of finding the next year (2026), it is staying in the dependency year (2025)
    public void weekWithYearDependencyTest() {
        String inputSentence = "In the first week of next year.";
        String dateSubstr = "first week of next year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 20);
        String xmlSubstr = "<exact_number>first</exact_number> <week_span>week</week_span> <implict_prefix>of</implict_prefix>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Set up year as previous dependency
        dateAndTime.setDateAndTime(DateTimeManipulation.setYear(dateAndTime.getDateAndTime(), 2025));
        dateAndTime.setPreviousDependency(Constants.YEAR_SPAN_TAG);

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("first", weekParser.exactNumber);
        assertTrue(weekParser.isOrdinal);

        weekParser.nthSpan();
        assertEquals(2026, dateAndTime.getStart().getYear());
        assertEquals(1, dateAndTime.getStart().getMonthOfYear());
        assertEquals(5, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2026, dateAndTime.getEnd().getYear());
        assertEquals(1, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(11, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Immediate past week test")
    public void immediatePastWeekTest() {
        String inputSentence = "I saw him last week.";
        String dateSubstr = "last week.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 8);
        String xmlSubstr = "<implict_prefix>last</implict_prefix> <week_span>week</week_span>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("last", weekParser.tenseIndicator);

        weekParser.immediatePast();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(13, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(19, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Immediate future week test")
    public void immediateFutureWeekTest() {
        String inputSentence = "Let's meet next week.";
        String dateSubstr = "next week.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 8);
        String xmlSubstr = "<implict_prefix>next</implict_prefix> <week_span>week</week_span>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("next", weekParser.tenseIndicator);

        weekParser.immediateFuture();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(27, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(5, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(3, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Week with custom weekday start test")
    // we see that again the start is set to the week after instead of this week
    public void weekWithCustomWeekdayStartTest() throws Exception {
        String inputSentence = "I work on weekdays.";
        String dateSubstr = "on weekdays.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 11);
        String xmlSubstr = "<week_span>weekdays</week_span>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Set custom weekday start (Tuesday)
        hawkConfig.setWeekDayStart(2);
        ConfigurationConstants.setConfiguration(new Configuration(hawkConfig));

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("weekdays", weekParser.timeSpan);

        weekParser.present();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(22, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(26, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Week with custom weekend start test")
    // does not parse weekends
    public void weekWithCustomWeekendStartTest() throws Exception {
        String inputSentence = "I relax on weekends.";
        String dateSubstr = "on weekends.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 11);
        String xmlSubstr = "<weekend_span>weekends</weekend_span>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Set custom weekend start (Friday)
        hawkConfig.setWeekEndStart(5);
        ConfigurationConstants.setConfiguration(new Configuration(hawkConfig));

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("weekends", weekParser.timeSpan);

        weekParser.present();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(25, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(27, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Week with custom week range test")
    public void weekWithCustomWeekRangeTest() {
        String inputSentence = "Let's meet next week.";
        String dateSubstr = "next week.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 8);
        String xmlSubstr = "<implict_prefix>next</implict_prefix> <week_span>week</week_span>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Set custom week range
        hawkConfig.setWeekRange(1);
        ConfigurationConstants.setConfiguration(new Configuration(hawkConfig));

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("next", weekParser.tenseIndicator);

        weekParser.future();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(27, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(5, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(3, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Weekly with year dependency test")
    public void weeklyWithYearDependencyTest() {
        String inputSentence = "I have meetings weekly next year.";
        String dateSubstr = "meetings weekly next year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 24);
        String xmlSubstr = "<set_week>weekly</set_week>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Set up year as previous dependency
        dateAndTime.setDateAndTime(DateTimeManipulation.setYear(dateAndTime.getDateAndTime(), 2026));
        dateAndTime.setPreviousDependency(Constants.YEAR_SPAN_TAG);

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("weekly", weekParser.timeSpan);
        assertTrue(weekParser.isSet);

        weekParser.present();
        weekParser.setPreviousDependency();
        assertEquals(365 * 24 * 60 * 60 * 1000L, dateAndTime.getWeekRecurrentPeriod());
    }

    @Test
    @DisplayName("Weekly with month dependency test")
    public void weeklyWithMonthDependencyTest() {
        String inputSentence = "I have meetings weekly next month.";
        String dateSubstr = "meetings weekly next month.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 25);
        String xmlSubstr = "<set_week>weekly</set_week>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Set up month as previous dependency
        dateAndTime.setDateAndTime(DateTimeManipulation.setMonth(dateAndTime.getDateAndTime(), 5, 1));
        dateAndTime.setPreviousDependency(Constants.MONTH_SPAN_TAG);

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("weekly", weekParser.timeSpan);
        assertTrue(weekParser.isSet);

        weekParser.present();
        weekParser.setPreviousDependency();
    }

    @Test
    @DisplayName("Weekly with no dependency test")
    public void weeklyWithNoDependencyTest() {
        String inputSentence = "I have meetings weekly.";
        String dateSubstr = "meetings weekly.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 14);
        String xmlSubstr = "<set_week>weekly</set_week>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("weekly", weekParser.timeSpan);
        assertTrue(weekParser.isSet);

        weekParser.present();
        weekParser.setPreviousDependency();
        assertEquals(7 * 24 * 60 * 60 * 1000L, dateAndTime.getWeekRecurrentPeriod());
    }

    @Test
    @DisplayName("Weekly count with year dependency test")
    // does not set the the correct week count
    public void weeklyCountWithYearDependencyTest() {
        String inputSentence = "I have meetings weekly next year.";
        String dateSubstr = "meetings weekly next year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 24);
        String xmlSubstr = "<set_week>weekly</set_week>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Set up year as previous dependency
        dateAndTime.setDateAndTime(DateTimeManipulation.setYear(dateAndTime.getDateAndTime(), 2026));
        dateAndTime.setPreviousDependency(Constants.YEAR_SPAN_TAG);
        
        // Set start and end times to span a year
        dateAndTime.setStart(dateAndTime.getDateAndTime());
        dateAndTime.setEnd(dateAndTime.getDateAndTime().plusYears(1));

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("weekly", weekParser.timeSpan);
        assertTrue(weekParser.isSet);

        weekParser.present();
        weekParser.setPreviousDependency();
        assertEquals(52, dateAndTime.getWeekRecurrentCount()); // Should be approximately 52 weeks in a year
    }

    @Test
    @DisplayName("Weekly count with month dependency test")
    // does not set the correct month count
    public void weeklyCountWithMonthDependencyTest() {
        String inputSentence = "I have meetings weekly next month.";
        String dateSubstr = "meetings weekly next month.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 25);
        String xmlSubstr = "<set_week>weekly</set_week>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Set up month as previous dependency
        dateAndTime.setDateAndTime(DateTimeManipulation.setMonth(dateAndTime.getDateAndTime(), 5, 1));
        dateAndTime.setPreviousDependency(Constants.MONTH_SPAN_TAG);
        
        // Set start and end times to span a month
        dateAndTime.setStart(dateAndTime.getDateAndTime());
        dateAndTime.setEnd(dateAndTime.getDateAndTime().plusMonths(1));

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("weekly", weekParser.timeSpan);
        assertTrue(weekParser.isSet);

        weekParser.present();
        weekParser.setPreviousDependency();
        assertEquals(4, dateAndTime.getWeekRecurrentCount()); // Should be approximately 4 weeks in a month
    }

    @Test
    @DisplayName("Weekly count with specific day test")
    // does not parse monday correctly
    public void weeklyCountWithSpecificDayTest() {
        String inputSentence = "I have meetings every Monday.";
        String dateSubstr = "meetings every Monday.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 19);
        String xmlSubstr = "<day_of_week>Monday</day_of_week>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Set up the date to be a Monday (April 21, 2025 is a Monday)
        dateAndTime.setDateAndTime(new DateTime(2025, 4, 21, 0, 0));
        
        // Set start and end times to span 2 weeks
        dateAndTime.setStart(dateAndTime.getDateAndTime());
        dateAndTime.setEnd(dateAndTime.getDateAndTime().plusWeeks(2));

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("Monday", weekParser.timeSpan);
        assertTrue(weekParser.isSet);
        assertEquals(1, dateAndTime.getDateAndTime().getDayOfWeek()); // Monday is day 1 in Joda-Time

        weekParser.present();
        weekParser.setPreviousDependency();
        assertEquals(2, dateAndTime.getWeekRecurrentCount()); // Should be 2 Mondays in 2 weeks
    }

    @Test
    @DisplayName("Weekly count with past tense test")
    // does not set the correct week count
    public void weeklyCountWithPastTenseTest() {
        String inputSentence = "I had meetings weekly last year.";
        String dateSubstr = "meetings weekly last year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 24);
        String xmlSubstr = "<set_week>weekly</set_week>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Set up year as previous dependency
        dateAndTime.setDateAndTime(DateTimeManipulation.setYear(dateAndTime.getDateAndTime(), 2024));
        dateAndTime.setPreviousDependency(Constants.YEAR_SPAN_TAG);
        
        // Set start and end times to span a year in the past
        dateAndTime.setStart(dateAndTime.getDateAndTime().minusYears(1));
        dateAndTime.setEnd(dateAndTime.getDateAndTime());

        DateTimeComponent weekParser = new WeekParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("weekly", weekParser.timeSpan);
        assertTrue(weekParser.isSet);

        weekParser.present();
        weekParser.setPreviousDependency();
        assertEquals(52, dateAndTime.getWeekRecurrentCount()); // Should be approximately 52 weeks in a year
    }
}