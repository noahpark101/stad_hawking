package com.zoho.hawking.datetimeparser.components;

import com.zoho.hawking.datetimeparser.DateAndTime;
import com.zoho.hawking.datetimeparser.configuration.Configuration;
import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import com.zoho.hawking.datetimeparser.constants.ConfigurationConstants;
import com.zoho.hawking.language.AbstractLanguage;
import com.zoho.hawking.language.LanguageFactory;
import com.zoho.hawking.language.english.model.DateTimeEssentials;
import com.zoho.hawking.language.english.model.DateTimeOffsetReturn;
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

public class YearParserTest {
    public AbstractLanguage engLang;
    public HawkingConfiguration hawkConfig;
    public Date refDate;
    public DateAndTime dateAndTime;

    @BeforeEach
    public void setup() {
        refDate = new Date(120, Calendar.DECEMBER, 1); // December 1st, 2020
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
    @DisplayName("Basic Test")
    public void basicTest() {
        String inputSentence = "The meeting is next year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 24);
        String xmlSubstr = "<implict_prefix>next</implict_prefix> <year_span>year</year_span>";
        String tense = "";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("year", yearParser.timeSpan);
        assertEquals("next", yearParser.tenseIndicator);

        yearParser.future();
        assertEquals(2021, dateAndTime.getDateAndTime().getYear());
        assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
        assertEquals(1, dateAndTime.getDateAndTime().getDayOfMonth());
    }

    @Test
    @DisplayName("Past exact year")
    public void pastExactYearTest() {
        String inputSentence = "We met in 2020.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 10, 14);
        String xmlSubstr = "<exact_year>2020</exact_year>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertTrue(yearParser.isExactTimeSpan);
        assertEquals(2020, yearParser.timeSpanValue);
        assertEquals("2020", yearParser.timeSpan);

        yearParser.past();
        assertEquals(2020, dateAndTime.getDateAndTime().getYear());
        assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
        assertEquals(1, dateAndTime.getDateAndTime().getDayOfMonth());
    }

    @Test
    @DisplayName("Last year")
    public void lastYearTest() {
        String inputSentence = "We met last year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 7, 16);
        String xmlSubstr = "<implict_prefix>last</implict_prefix> <year_span>year</year_span>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("year", yearParser.timeSpan);
        assertEquals("last", yearParser.tenseIndicator);

        yearParser.past();
        assertEquals(2019, dateAndTime.getDateAndTime().getYear());
        assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
        assertEquals(1, dateAndTime.getDateAndTime().getDayOfMonth());
    }

    @Test
    @DisplayName("More than 1 year ago")
    public void moreThanOneYearAgoTest() {
        String inputSentence = "The event was 2 years ago.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 14, 25);
        String xmlSubstr = "<exact_number>2</exact_number> <year_span>years</year_span> <implict_postfix>ago</implict_postfix>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertFalse(yearParser.isExactTimeSpan);
        assertEquals("years", yearParser.timeSpan);
        assertEquals(2, yearParser.number);
        assertEquals("ago", yearParser.tenseIndicator);

        yearParser.past();
        assertEquals(2018, dateAndTime.getDateAndTime().getYear());
        assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
        assertEquals(1, dateAndTime.getDateAndTime().getDayOfMonth());
    }

    @Test
    @DisplayName("Set yearly")
    public void yearlyTest() {
        String inputSentence = "We have meetings yearly.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 21);
        String xmlSubstr = "<set_year>yearly</set_year>";
        String tense = "";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertTrue(yearParser.isSet);
        assertEquals("yearly", yearParser.timeSpan);
    }

    @Test
    @DisplayName("Every year")
    public void everyYearTest() {
        String inputSentence = "We celebrate every year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 13, 23);
        String xmlSubstr = "<set_prefix>every</set_prefix> <year_span>year</year_span>";
        String tense = "";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertTrue(yearParser.isSet);
        assertEquals("year", yearParser.timeSpan);
    }

    @Test
    @DisplayName("Immediate future test")
    public void immediateFutureTest() {
        String inputSentence = "The meeting is next year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 24);
        String xmlSubstr = "<implict_prefix>next</implict_prefix> <year_span>year</year_span>";
        String tense = "";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("year", yearParser.timeSpan);
        assertEquals("next", yearParser.tenseIndicator);

        yearParser.immediateFuture();
        assertEquals(2021, dateAndTime.getDateAndTime().getYear());
        assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
        assertEquals(1, dateAndTime.getDateAndTime().getDayOfMonth());
    }

    @Test
    @DisplayName("Immediate past test")
    public void immediatePastTest() {
        String inputSentence = "The meeting was last year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 16, 25);
        String xmlSubstr = "<implict_prefix>last</implict_prefix> <year_span>year</year_span>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("year", yearParser.timeSpan);
        assertEquals("last", yearParser.tenseIndicator);

        yearParser.immediatePast();
        assertEquals(2019, dateAndTime.getDateAndTime().getYear());
        assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
        assertEquals(1, dateAndTime.getDateAndTime().getDayOfMonth());
    }

    @Test
    @DisplayName("Immediate test")
    public void immediateTest() {
        String inputSentence = "The meeting is this year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 24);
        String xmlSubstr = "<implict_prefix>this</implict_prefix> <year_span>year</year_span>";
        String tense = "";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("year", yearParser.timeSpan);
        assertEquals("this", yearParser.tenseIndicator);

        yearParser.immediate();
        assertTrue(yearParser.isImmediate);
        assertEquals(2020, dateAndTime.getDateAndTime().getYear());
        assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
        assertEquals(1, dateAndTime.getDateAndTime().getDayOfMonth());
    }

    @Test
    @DisplayName("Remainder test")
    public void remainderTest() {
        String inputSentence = "The meeting is this year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 24);
        String xmlSubstr = "<implict_prefix>this</implict_prefix> <year_span>year</year_span>";
        String tense = "";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("year", yearParser.timeSpan);
        assertEquals("this", yearParser.tenseIndicator);

        yearParser.remainder();
        // Verify that the start and end times are set correctly for the remainder of the year
        assertEquals(2020, dateAndTime.getStart().getYear());
        assertEquals(2020, dateAndTime.getEnd().getYear());
        assertEquals(12, dateAndTime.getStart().getMonthOfYear());
        assertEquals(12, dateAndTime.getEnd().getMonthOfYear());
    }

    @Test
    @DisplayName("Nth span test")
    // FAULT: does not set the correct year (2020 instead of 2023)
    public void nthSpanTest() {
        String inputSentence = "The meeting is in the 3rd year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 22, 30);
        String xmlSubstr = "<exact_number>3</exact_number> <year_span>year</year_span>";
        String tense = "";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("year", yearParser.timeSpan);
        assertEquals(3, yearParser.number);

        yearParser.nthSpan();
        // Verify that the date is set to the nth year
        assertEquals(2023, dateAndTime.getDateAndTime().getYear());
        assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
        assertEquals(1, dateAndTime.getDateAndTime().getDayOfMonth());
    }

    @Test
    @DisplayName("Set previous dependency test")
    public void setPreviousDependencyTest() {
        String inputSentence = "The meeting is yearly.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 21);
        String xmlSubstr = "<set_year>yearly</set_year>";
        String tense = "";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertTrue(yearParser.isSet);
        assertEquals("yearly", yearParser.timeSpan);

        yearParser.setPreviousDependency();
        // Verify that the previous dependency is set correctly
        assertEquals("year_span", dateAndTime.getPreviousDependency());
    }

    @Test
    @DisplayName("Test computeNumber with yearWords")
    public void computeNumberWithYearWordsTest() {
        String inputSentence = "The meeting is in a year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 18, 24);
        String xmlSubstr = "<implict_prefix>a</implict_prefix> <year_span>year</year_span>";
        String tense = "";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("year", yearParser.timeSpan);
        assertEquals(1, yearParser.number); // Default year range
    }

    @Test
    @DisplayName("Test computeNumber with yearsWords")
    public void computeNumberWithYearsWordsTest() {
        String inputSentence = "The meeting is in years.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 23);
        String xmlSubstr = "<implict_prefix>in</implict_prefix> <year_span>years</year_span>";
        String tense = "";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("years", yearParser.timeSpan);
        assertEquals(2, yearParser.number); // Default years range
    }

    @Test
    @DisplayName("Test past with set-based time span")
    public void pastWithSetTest() {
        String inputSentence = "The meeting was yearly.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 21);
        String xmlSubstr = "<set_year>yearly</set_year>";
        String tense = "";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertTrue(yearParser.isSet);
        assertEquals("yearly", yearParser.timeSpan);

        yearParser.past();
        // Verify that the date is set correctly and recurrent period is calculated
        assertEquals(2019, dateAndTime.getDateAndTime().getYear());
        assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
        assertEquals(1, dateAndTime.getDateAndTime().getDayOfMonth());
    }

    @Test
    @DisplayName("Test present with set-based time span")
    // FAULT: pushes the year forward by 1 year instead of staying in the current year
    public void presentWithSetTest() {
        String inputSentence = "The meeting is yearly.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 21);
        String xmlSubstr = "<set_year>yearly</set_year>";
        String tense = "";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertTrue(yearParser.isSet);
        assertEquals("yearly", yearParser.timeSpan);

        yearParser.present();
        // Verify that the date is set correctly and recurrent period is calculated
        assertEquals(2020, dateAndTime.getDateAndTime().getYear());
        assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
        assertEquals(1, dateAndTime.getDateAndTime().getDayOfMonth());
    }

    @Test
    @DisplayName("Test future with exact time span")
    public void futureWithExactTimeSpanTest() {
        String inputSentence = "The meeting is in 2025.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 18, 22);
        String xmlSubstr = "<exact_year>2025</exact_year>";
        String tense = "";
        continueSetup(trip, inputSentence, inputSentence, tense);

        // Get YearParser object
        DateTimeComponent yearParser = new YearParser(xmlSubstr, tense, dateAndTime, engLang);
        assertTrue(yearParser.isExactTimeSpan);
        assertEquals(2025, yearParser.timeSpanValue);
        assertEquals("2025", yearParser.timeSpan);

        yearParser.future();
        // Verify that the date is set to the exact year
        assertEquals(2025, dateAndTime.getDateAndTime().getYear());
        assertEquals(12, dateAndTime.getDateAndTime().getMonthOfYear());
        assertEquals(1, dateAndTime.getDateAndTime().getDayOfMonth());
    }
}
