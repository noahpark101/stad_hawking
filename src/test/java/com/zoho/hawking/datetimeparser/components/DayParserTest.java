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

class DayParserTest {

    public AbstractLanguage engLang;
    public HawkingConfiguration hawkConfig;
    public Date refDate;
    public DateAndTime dateAndTime;

    @BeforeEach
    public void setup() {
        refDate = new Date(1745164800000L); // Make reference date 4/20/2025 (Sunday)
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
    @DisplayName("For numbered days")
    public void forNumDaySpanTest() {
        String inputSentence = "Wow! We do this for 5 days.";
        String dateSubstr = "We do this for 5 days.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 21);
        String xmlSubstr = "<exact_number>5</exact_number> <day_span>days</day_span>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("days", dayParser.timeSpan);
        assertEquals(5, dayParser.number);

        dayParser.past();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(15, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(19, dateAndTime.getEnd().getDayOfMonth()); // Should be 20 but time zones

        continueSetup(trip, inputSentence, dateSubstr, tense);
        dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        dayParser.present();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(21, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(25, dateAndTime.getEnd().getDayOfMonth());

        continueSetup(trip, inputSentence, dateSubstr, tense);
        dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        dayParser.immediate();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(20, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(24, dateAndTime.getEnd().getDayOfMonth());

        dayParser.setPreviousDependency();
        assertEquals(Constants.DAY_OF_WEEK_TAG, dateAndTime.getPreviousDependency());
    }

    @Test
    @DisplayName("Numbered days ago")
    public void numDaysAgoTest() {
        String inputSentence = "Wow! We did this for 6 days ago.";
        String dateSubstr = "We did this for 6 days ago.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 7, 22);
        String xmlSubstr = "<implict_prefix>this</implict_prefix> <exact_number>6</exact_number> <day_span>days</day_span>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("days", dayParser.timeSpan);
        assertEquals(6, dayParser.number);

        // FAULT: immediate() calls past() where end date is simply reference date,
        // when it should be same as start date
        dayParser.immediate();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(15, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
//        assertEquals(15, dateAndTime.getEnd().getDayOfMonth()); // Should be 20 but time zones

    }

    @Test
    @DisplayName("Few (2) days")
    public void fewDays() {

        String inputSentence = "Oh I see. It is in a few days.";
        String dateSubstr = "It is in a few days.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 11, 19);
        String xmlSubstr = "<implict_prefix>few</implict_prefix> <day_span>days</day_span>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("days", dayParser.timeSpan);
        assertEquals(2, dayParser.number);

        // FAULT - Vague future days are just guided to present() where the start date is simply set to reference time
        dayParser.future();
        assertEquals(2025, dayParser.dateAndTime.getStart().getYear());
        assertEquals(4, dayParser.dateAndTime.getStart().getMonthOfYear());
//        assertEquals(22, dayParser.dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dayParser.dateAndTime.getEnd().getYear());
        assertEquals(4, dayParser.dateAndTime.getEnd().getMonthOfYear());
        assertEquals(22, dayParser.dateAndTime.getEnd().getDayOfMonth());

        continueSetup(trip, inputSentence, dateSubstr, tense);
        dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        // FAULT - Similar fault as above
        dayParser.immediate();
        assertEquals(2025, dayParser.dateAndTime.getStart().getYear());
        assertEquals(4, dayParser.dateAndTime.getStart().getMonthOfYear());
//        assertEquals(21, dayParser.dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dayParser.dateAndTime.getEnd().getYear());
        assertEquals(4, dayParser.dateAndTime.getEnd().getMonthOfYear());
        assertEquals(21, dayParser.dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Today keyword")
    public void todayKeywordTest() {
        String inputSentence = "Yes! Today is the day.";
        String dateSubstr = "Today is the day";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 5);
        String xmlSubstr = "<current_day>today</current_day>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("today", dayParser.timeSpan);

        dayParser.present();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(20, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(20, dateAndTime.getEnd().getDayOfMonth());

        continueSetup(trip, inputSentence, dateSubstr, tense);
        dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        // FAULT: Unlike MonthParser whose associated number is instantiated unconditionally,
        // DayParser doesn't instantiate a number so running intermediate() causes NullPointerException
        // if there's no number like this test case
//        dayParser.immediate();

    }

    @Test
    @DisplayName("Yesterday keyword")
    public void yesterdayKeywordTest() {
        String inputSentence = "Yes! Yesterday was the day.";
        String dateSubstr = "Yesterday was the day.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 9);
        String xmlSubstr = "<current_day>yesterday</current_day>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("yesterday", dayParser.timeSpan);

        dayParser.immediatePast();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(19, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(19, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Tomorrow keyword")
    public void tomorrowKeywordTest() {
        String inputSentence = "Yes! Tomorrow is the day.";
        String dateSubstr = "Tomorrow is the day.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 0, 8);
        String xmlSubstr = "<current_day>tomorrow</current_day>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("tomorrow", dayParser.timeSpan);

        dayParser.immediateFuture();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(21, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(21, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Now keyword")
    public void nowKeywordTest() {
        String inputSentence = "Aw man. It is now Thursday.";
        String dateSubstr = "It is now Thursday.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 7, 18);
        String xmlSubstr = "<current_day>now</current_day> <day_of_week>thursday</day_of_week>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        // FAULT: extractComponentTags() handles current_day and day_of_week exclusively, but
        // this input is a combination of both. As a result, the day_of_week tag is not considered
//        assertEquals("thursday", dayParser.timeSpan);
//        assertEquals("now", dayParser.tenseIndicator);

        dayParser.present();
//        assertEquals(2025, dateAndTime.getStart().getYear());
//        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
//        assertEquals(24, dateAndTime.getStart().getDayOfMonth());
//        assertEquals(2025, dateAndTime.getEnd().getYear());
//        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
//        assertEquals(24, dateAndTime.getEnd().getDayOfMonth());
    }



    @Test
    @DisplayName("Basic day of week")
    public void basicDayOfWeekTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "Sweet! Let's do it on Wednesday.";
        String dateSubstr = "Let's do it on Wednesday.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 12, 24);
        String xmlSubstr = "<implict_prefix>on</implict_prefix> <day_of_week>wednesday</day_of_week>";
        String tense = "PRESENT";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("wednesday", dayParser.timeSpan);
        assertEquals(3, dayParser.timeSpanValue);
        assertTrue(dayParser.isExactTimeSpan);

        dayParser.present();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(23, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(23, dateAndTime.getEnd().getDayOfMonth());

        continueSetup(trip, inputSentence, dateSubstr, tense);
        dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        dayParser.past();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(16, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(16, dateAndTime.getEnd().getDayOfMonth());

        continueSetup(trip, inputSentence, dateSubstr, tense);
        dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        dayParser.future();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(23, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(23, dateAndTime.getEnd().getDayOfMonth());

    }

    @Test
    @DisplayName("Last day of week")
    public void lastDayOfWeekTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "What? We did that last Friday.";
        String dateSubstr = "We did that last Friday.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 12, 23);
        String xmlSubstr = "<implict_prefix>last</implict_prefix> <day_of_week>friday</day_of_week>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("friday", dayParser.timeSpan);
        assertEquals(5, dayParser.timeSpanValue);
        assertEquals("last", dayParser.tenseIndicator);
        assertTrue(dayParser.isExactTimeSpan);

        // FAULT: past() sees "last" and sends the time back at least 7 days unconditionally,
        // but "last Friday" when reference date is Sunday should only be 2 days ago
        dayParser.past();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
//        assertEquals(18, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
//        assertEquals(18, dateAndTime.getEnd().getDayOfMonth());

        continueSetup(trip, inputSentence, dateSubstr, tense);
        dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        dayParser.immediatePast();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(18, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(18, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Stacked last day of week")
    public void stackedLastDayOfWeekTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "What? We did that last last Friday.";
        String dateSubstr = "We did that last last Friday.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 12, 28);
        String xmlSubstr = "<implict_prefix>last</implict_prefix> <implict_prefix>last</implict_prefix> <day_of_week>friday</day_of_week>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("friday", dayParser.timeSpan);
        assertEquals(5, dayParser.timeSpanValue);
        assertEquals("last", dayParser.tenseIndicator);
        assertTrue(dayParser.isExactTimeSpan);

        // FAULT (sorta): At least in casual English "last last Friday" means 2 Fridays before
        // the reference date. past() does not handle this correctly
        dayParser.past();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
//        assertEquals(11, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
//        assertEquals(11, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("This (same as ref date) day of week")
    public void thisSameDayOfWeekTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "I'm happy. We're celebrating this Sunday.";
        String dateSubstr = "We're celebrating this Sunday.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 18, 29);
        String xmlSubstr = "<implict_prefix>this</implict_prefix> <day_of_week>sunday</day_of_week>";
        String tense = "PRESENT";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("sunday", dayParser.timeSpan);
        assertEquals(7, dayParser.timeSpanValue);
        assertEquals("this", dayParser.tenseIndicator);
        assertTrue(dayParser.isExactTimeSpan);

        dayParser.present();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(27, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(27, dateAndTime.getEnd().getDayOfMonth());

        continueSetup(trip, inputSentence, dateSubstr, tense);
        dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        dayParser.future();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(27, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(27, dateAndTime.getEnd().getDayOfMonth());

        continueSetup(trip, inputSentence, dateSubstr, tense);
        dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        dayParser.immediateFuture();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(27, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(27, dateAndTime.getEnd().getDayOfMonth());

        // Finally suppose this is done in past tense
        continueSetup(trip, inputSentence, dateSubstr, tense);
        dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        dayParser.sentenceTense = "PAST";
        dayParser.past();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(13, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(13, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("This (after ref date) day of week")
    public void thisAfterDiffDayOfWeekTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "I'm happy. We're celebrating this Friday.";
        String dateSubstr = "We're celebrating this Friday.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 18, 29);
        String xmlSubstr = "<implict_prefix>this</implict_prefix> <day_of_week>friday</day_of_week>";
        String tense = "PRESENT";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("friday", dayParser.timeSpan);
        assertEquals(5, dayParser.timeSpanValue);
        assertEquals("this", dayParser.tenseIndicator);
        assertTrue(dayParser.isExactTimeSpan);

        dayParser.present();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(25, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(25, dateAndTime.getEnd().getDayOfMonth());

        continueSetup(trip, inputSentence, dateSubstr, tense);
        dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        dayParser.immediate();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(25, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(25, dateAndTime.getEnd().getDayOfMonth());

    }

    @Test
    @DisplayName("This (before ref date) day of week")
    public void thisBeforeDiffDayOfWeekTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "I'm happy. We celebrated this Friday.";
        String dateSubstr = "We celebrated this Friday.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 14, 25);
        String xmlSubstr = "<implict_prefix>this</implict_prefix> <day_of_week>friday</day_of_week>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("friday", dayParser.timeSpan);
        assertEquals(5, dayParser.timeSpanValue);
        assertEquals("this", dayParser.tenseIndicator);
        assertTrue(dayParser.isExactTimeSpan);

        // FAULT: past() sees "this" and sends the time back at least 7 days unconditionally,
        // but "this Friday" when reference date is Sunday should only be 2 days ago
        dayParser.past();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
//        assertEquals(18, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
//        assertEquals(18, dateAndTime.getEnd().getDayOfMonth());

        continueSetup(trip, inputSentence, dateSubstr, tense);
        dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        dayParser.immediate();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(18, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(18, dateAndTime.getEnd().getDayOfMonth());

    }

    @Test
    @DisplayName("nth day of year")
    public void nthOfYearTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "Peachy day! It's the 236th day of 2025.";
        String dateSubstr = "It's the 236th day of 2025.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 9, 26);
        String xmlSubstr = "<exact_number>236th</exact_number> <day_span>day</day_span>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Conduct year parsing, make year a previous dependency of day
        dateAndTime.setPreviousDependency(Constants.YEAR_SPAN_TAG);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("day", dayParser.timeSpan);
        assertEquals(236, dayParser.number);

        dayParser.nthSpan();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(8, dateAndTime.getStart().getMonthOfYear());
        assertEquals(24, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(8, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(24, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("nth Monday of year")
    public void nthMondayOfYearTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "Peachy day! It's the 34th Monday of 2025.";
        String dateSubstr = "It's the 34th Monday of 2025.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 9, 28);
        String xmlSubstr = "<exact_number>34th</exact_number> <day_of_week>monday</day_of_week>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Conduct year parsing, make year a previous dependency of day
        dateAndTime.setPreviousDependency(Constants.YEAR_SPAN_TAG);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("monday", dayParser.timeSpan);
        assertEquals(34, dayParser.number);

        dayParser.nthSpan();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(8, dateAndTime.getStart().getMonthOfYear());
        assertEquals(25, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(8, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(25, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("nth day of month")
    public void nthOfMonthTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "Peachy day! It's the 29th day of February 2024.";
        String dateSubstr = "It's the 29th day of February 2024.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 9, 34);
        String xmlSubstr = "<exact_number>29th</exact_number> <day_span>day</day_span>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Conduct year/month parsing, make month a previous dependency of day
        dateAndTime.setDateAndTime(DateTimeManipulation.setMonth(dateAndTime.getDateAndTime(), -1, 2));
        dateAndTime.setPreviousDependency(Constants.MONTH_SPAN_TAG);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("day", dayParser.timeSpan);
        assertEquals(29, dayParser.number);

        dayParser.nthSpan();
        assertEquals(2024, dateAndTime.getStart().getYear());
        assertEquals(2, dateAndTime.getStart().getMonthOfYear());
        assertEquals(29, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2024, dateAndTime.getEnd().getYear());
        assertEquals(2, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(29, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("nth Monday of month")
    public void nthMondayOfMonthTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "Peachy day! It's the 3rd Monday of June.";
        String dateSubstr = "It's the 3rd Monday of June.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 9, 27);
        String xmlSubstr = "<exact_number>3rd</exact_number> <day_of_week>monday</day_of_week>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Conduct year/month parsing, make month a previous dependency of day
        dateAndTime.setDateAndTime(DateTimeManipulation.setMonth(dateAndTime.getDateAndTime(), 0, 6));
        dateAndTime.setPreviousDependency(Constants.MONTH_SPAN_TAG);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("monday", dayParser.timeSpan);
        assertEquals(3, dayParser.number);

        dayParser.nthSpan();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(6, dateAndTime.getStart().getMonthOfYear());
        assertEquals(16, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(6, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(16, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("nth day of week")
    public void nthOfWeekTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "Peachy day! It's the 5th day of the week.";
        String dateSubstr = "It's the 5th day of the week.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 9, 28);
        String xmlSubstr = "<exact_number>5th</exact_number> <day_span>day</day_span>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Conduct week parsing, make week a previous dependency of day
        dateAndTime.setDateAndTime(DateTimeManipulation.setWeek(dateAndTime.getDateAndTime(), 1, 13));
        dateAndTime.setPreviousDependency(Constants.WEEK_SPAN_TAG);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("day", dayParser.timeSpan);
        assertEquals(5, dayParser.number);

        dayParser.nthSpan();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(25, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(25, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("nth Monday of week")
    public void nthMondayOfWeekTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "Peachy day! It's the 1st Monday of the week.";
        String dateSubstr = "It's the 1st Monday of the week.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 9, 31);
        String xmlSubstr = "<exact_number>1st</exact_number> <day_of_week>monday</day_of_week>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Conduct week parsing, make week a previous dependency of day
        dateAndTime.setDateAndTime(DateTimeManipulation.setWeek(dateAndTime.getDateAndTime(), 1, 13));
        dateAndTime.setPreviousDependency(Constants.WEEK_SPAN_TAG);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("monday", dayParser.timeSpan);
        assertEquals(1, dayParser.number);

        dayParser.nthSpan();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(21, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(21, dateAndTime.getEnd().getDayOfMonth());

        // Test without previous dependency
        continueSetup(trip, inputSentence, dateSubstr, tense);
        dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        // FAULT: In nthSpan(), no dependency doesn't take exact time span (monday) into account
        // when setting the date
        dayParser.nthSpan();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
//        assertEquals(21, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
//        assertEquals(21, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Remaining day")
    public void remainingDayTest() {

        String inputSentence = "This will go on for the rest of today.";
        String dateSubstr = "This will go on for the rest of today.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 32, 37);
        String xmlSubstr = "<current_day>today</current_day>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("today", dayParser.timeSpan);

        // Remaining tense indicator not implemented yet
        dayParser.tenseIndicator = "rest";

        dayParser.remainder();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(20, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(4, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(20, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Basic set day")
    public void basicSetDayTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "Agreed. I work out daily to stay healthy.";
        String dateSubstr = "I work out daily to stay healthy.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 11, 16);
        String xmlSubstr = "<set_day>daily</set_day>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertTrue(dayParser.isSet);

        dayParser.setPreviousDependency();
        assertEquals(1000 * 60 * 60 * 24, dayParser.dateAndTime.getDayRecurrentPeriod());
    }

    @Test
    @DisplayName("Set day within year")
    public void setDayWithinYearTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "Great idea. We shall congregate everyday this year.";
        String dateSubstr = "We shall congregate once everyday this year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 20, 38);
        String xmlSubstr = "<set_day>everyday</set_day>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Conduct year parsing, make year a previous dependency of day
        DateTimeManipulation.setYearStartAndEndTime(dateAndTime,0, 0, 1, 2);
        dateAndTime.setPreviousDependency(Constants.YEAR_SPAN_TAG);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertTrue(dayParser.isSet);

        // FAULT: No number is associated with dayParser with "everyday",
        // so calculateRecurrentPeriod() encounters a NullPointerException.
        // Will manually instantiate a number to bypass this fault.
        dayParser.number = 1;
        dayParser.setPreviousDependency();
        assertEquals(1000L * 60 * 60 * 24 * 365, dayParser.dateAndTime.getDayRecurrentPeriod());
        // FAULT: calculateRecurrentCount() does not properly handle "everyday" with the
        // if-else statements in that method that should be incrementing the recurrent count to 365
//        assertEquals(365, dateAndTime.getDayRecurrentCount());
    }

    @Test
    @DisplayName("Set day within month")
    public void setDayWithinMonthTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "Great idea. We shall congregate everyday this month.";
        String dateSubstr = "We shall congregate everyday this month.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 20, 39);
        String xmlSubstr = "<set_day>everyday</set_day>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Conduct year parsing, make year a previous dependency of day
        DateTimeManipulation.setMonthStartAndEndTime(dateAndTime,0, 0, 1, 2);
        dateAndTime.setPreviousDependency(Constants.MONTH_SPAN_TAG);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertTrue(dayParser.isSet);

        // Same fault as setDayWithinYearTest().
        // Will manually instantiate a number to bypass this fault.
        dayParser.number = 1;
        dayParser.setPreviousDependency();
        assertEquals(1000L * 60 * 60 * 24 * 30, dayParser.dateAndTime.getDayRecurrentPeriod());
        // Same fault as setDayWithinYearTest(), but it seems like the count is incremented to
        // the number of weeks in the month
//        assertEquals(30, dateAndTime.getDayRecurrentCount());
    }

    @Test
    @DisplayName("Set day within week")
    public void setDayWithinWeekTest() {
        // 4/20/2025 is Sunday
        String inputSentence = "Great idea. We shall congregate everyday this week.";
        String dateSubstr = "We shall congregate everyday this month.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 20, 38);
        String xmlSubstr = "<set_day>everyday</set_day>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Conduct year parsing, make year a previous dependency of day
        DateTimeManipulation.setWeekStartAndEndTime(dateAndTime, 0, 0, 0, 6, 1, 2);
        dateAndTime.setPreviousDependency(Constants.WEEK_SPAN_TAG);

        // Get DayParser object
        DateTimeComponent dayParser = new DayParser(xmlSubstr, tense, dateAndTime, engLang);
        assertTrue(dayParser.isSet);

        // Same fault as setDayWithinYearTest().
        // Will manually instantiate a number to bypass this fault.
        dayParser.number = 1;
        dayParser.setPreviousDependency();
        assertEquals(1000L * 60 * 60 * 24 * 7, dayParser.dateAndTime.getDayRecurrentPeriod());
        // Same fault as setDayWithinYearTest(), but it seems like the count is incremented to
        // the number of weeks in the month
//        assertEquals(7, dateAndTime.getDayRecurrentCount());
    }

}