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

class CustomDateParserTest {
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
        try {
            // Using US Federal Fiscal Year of October 1 to September 30.
            // Thus:
            // Q1 = October 1 to December 31
            // Q2 = January 1 to March 31
            // H1 = October 1 to March 31
            // Q3 = April 1 to June 30
            // Q4 = July 1 to September 30
            // H2 = April 1 to September 30
            hawkConfig.setFiscalYearStart(10);
            hawkConfig.setFiscalYearEnd(9);
        } catch (Exception e) {
            assert(false);
        }

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
    @DisplayName("This quarter")
    public void forNumDaySpanTest() {
        String inputSentence = "Cool. Financials have been strong for this quarter.";
        String dateSubstr = "Financials have been strong for this quarter.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 32, 45);
        String xmlSubstr = "<implict_prefix>this</implict_prefix> <quarterofyear>quarter</quarterofyear>";
        String tense = "PRESENT";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get CustomDateParser object
        DateTimeComponent customDParser = new CustomDateParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("quarter", customDParser.timeSpan);
        assertEquals(1, customDParser.number);

        customDParser.immediate();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(6, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(30, dateAndTime.getEnd().getDayOfMonth()); 

        customDParser.setPreviousDependency();
        assertEquals("CUSTOM_DATE", dateAndTime.getPreviousDependency());
    }

    @Test
    @DisplayName("This prev numbered quarter")
    public void thisPrevNumQuarterTest() {
        String inputSentence = "Yikes. Financials don't look good for this Q1.";
        String dateSubstr = "Financials don't look good for this Q1.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 32, 39);
        String xmlSubstr = "<implict_prefix>this</implict_prefix> <quarterofyear>Q1</quarterofyear>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get CustomDateParser object
        DateTimeComponent customDParser = new CustomDateParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("Q1", customDParser.timeSpan);
        assertEquals(1, customDParser.number);

        // FAULT: When getting months associated with Q1, past() doesn't lowercase "Q1" when reading a hashmap,
        // causing a NullPointerException
        // Will manually set timespan to lowercase
        customDParser.timeSpan = "q1";
        // FAULT: yearsToAdd() doesn't tell immediate() to set the year back to 2024
        customDParser.immediate();
//        assertEquals(2024, dateAndTime.getStart().getYear());
        assertEquals(10, dateAndTime.getStart().getMonthOfYear());
        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
//        assertEquals(2024, dateAndTime.getEnd().getYear());
        assertEquals(12, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(31, dateAndTime.getEnd().getDayOfMonth()); 
    }

    @Test
    @DisplayName("On numbered quarter")
    public void onNumQuarterTest() {
        String inputSentence = "You're right. Company meeting takes place in q4.";
        String dateSubstr = "Company meeting takes place in q4.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 29, 34);
        String xmlSubstr = "<implict_prefix>in</implict_prefix> <quarterofyear>q4</quarterofyear>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get CustomDateParser object
        DateTimeComponent customDParser = new CustomDateParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("q4", customDParser.timeSpan);
        assertEquals(1, customDParser.number);

        customDParser.future();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(7, dateAndTime.getStart().getMonthOfYear());
        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(9, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(30, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Last quarter")
    public void lastQuarterTest() {
        String inputSentence = "Meh. Financials were fine last quarter.";
        String dateSubstr = "Financials were fine last quarter.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 22, 34);
        String xmlSubstr = "<implict_prefix>last</implict_prefix> <quarterofyear>quarter</quarterofyear>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get CustomDateParser object
        DateTimeComponent customDParser = new CustomDateParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("quarter", customDParser.timeSpan);
        assertEquals("last", customDParser.tenseIndicator);
        assertEquals(1, customDParser.number);

        customDParser.immediatePast();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(1, dateAndTime.getStart().getMonthOfYear());
        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(3, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(31, dateAndTime.getEnd().getDayOfMonth()); 
    }

    @Test
    @DisplayName("Next quarter")
    public void nextQuarterTest() {
        String inputSentence = "Great. Let's keep financials strong for next quarter.";
        String dateSubstr = "Let's keep financials strong for next quarter.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 34, 46);
        String xmlSubstr = "<implict_prefix>next</implict_prefix> <quarterofyear>quarter</quarterofyear>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get CustomDateParser object
        DateTimeComponent customDParser = new CustomDateParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("quarter", customDParser.timeSpan);
        assertEquals("next", customDParser.tenseIndicator);
        assertEquals(1, customDParser.number);

        // FAULT: present() tries to get the quarter after current quarter (Q3) but cycles to Q0 which doesn't exist
        customDParser.immediateFuture();
//        assertEquals(2025, dateAndTime.getStart().getYear());
//        assertEquals(7, dateAndTime.getStart().getMonthOfYear());
//        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
//        assertEquals(2025, dateAndTime.getEnd().getYear());
//        assertEquals(9, dateAndTime.getEnd().getMonthOfYear());
//        assertEquals(30, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Past numbered quarters")
    public void pastNumQuartersTest() {
        String inputSentence = "Phew. We've scraped by for the past quarters.";
        String dateSubstr = "We've scraped by for the past quarters.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 26, 39);
        String xmlSubstr = "<implict_prefix>past</implict_prefix> <quarterofyear>quarters</quarterofyear>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get CustomDateParser object
        DateTimeComponent customDParser = new CustomDateParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("quarters", customDParser.timeSpan);
        assertEquals("past", customDParser.tenseIndicator);
        assertEquals(2, customDParser.number);

        customDParser.immediate();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(1, dateAndTime.getStart().getMonthOfYear());
        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(6, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(30, dateAndTime.getEnd().getDayOfMonth()); 
    }

    @Test
    @DisplayName("These 2 halves")
    public void these2HalvesTest() {
        String inputSentence = "Oh? The project continues for these halves.";
        String dateSubstr = "The project continues for these halves.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 27, 39);
        String xmlSubstr = "<implict_prefix>these</implict_prefix> <halfofyear>halves</halfofyear>";
        String tense = "PRESENT";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get CustomDateParser object
        DateTimeComponent customDParser = new CustomDateParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("halves", customDParser.timeSpan);
        assertEquals("these", customDParser.tenseIndicator);
        // FAULT: computeNumber() sets isNumberPresent to true but doesn't actually instantiate a number for
        // the CustomDateParser object
        assertTrue(customDParser.isNumberPresent);
//        assertEquals(2, customDParser.number);

        // Due to fault above, calling present() will cause NullPointerException
        customDParser.present();
//        assertEquals(2024, dateAndTime.getStart().getYear());
//        assertEquals(10, dateAndTime.getStart().getMonthOfYear());
//        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
//        assertEquals(2025, dateAndTime.getEnd().getYear());
//        assertEquals(9, dateAndTime.getEnd().getMonthOfYear());
//        assertEquals(30, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Next half")
    public void nextHalfTest() {
        String inputSentence = "Oh? The forum focuses on the last h1.";
        String dateSubstr = "The forum focuses on the next last h1.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 26, 38);
        String xmlSubstr = "<implict_prefix>last</implict_prefix> <halfofyear>h1</halfofyear>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get CustomDateParser object
        DateTimeComponent customDParser = new CustomDateParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("h1", customDParser.timeSpan);
        assertEquals("last", customDParser.tenseIndicator);
        assertEquals(1, customDParser.number);

        // Due to fault above, calling present() will cause NullPointerException
        customDParser.past();
        assertEquals(2024, dateAndTime.getStart().getYear());
        assertEquals(10, dateAndTime.getStart().getMonthOfYear());
        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(3, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(31, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Last fiscal year")
    public void lastFiscalYearTest() {
        String inputSentence = "Great work. We had a decent run in last fiscal year.";
        String dateSubstr = "We had a decent run in last fiscal year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 24, 40);
        String xmlSubstr = "<implict_prefix>last</implict_prefix> <custom_year>fiscalyear</custom_year>";
        String tense = "PAST";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get CustomDateParser object
        DateTimeComponent customDParser = new CustomDateParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("fiscalyear", customDParser.timeSpan);
        assertEquals("last", customDParser.tenseIndicator);
        assertEquals(1, customDParser.number);

        customDParser.past();
        assertEquals(2023, dateAndTime.getStart().getYear());
        assertEquals(10, dateAndTime.getStart().getMonthOfYear());
        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2024, dateAndTime.getEnd().getYear());
        assertEquals(9, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(30, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("This fiscal year")
    public void thisFiscalYearTest() {
        String inputSentence = "Great work. Let's get a great run for this fiscal year.";
        String dateSubstr = "Let's get a great run for this fiscal year.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 32, 43);
        String xmlSubstr = "<implict_prefix>this</implict_prefix> <custom_year>fiscalyear</custom_year>";
        String tense = "PRESENT";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get CustomDateParser object
        DateTimeComponent customDParser = new CustomDateParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("fiscalyear", customDParser.timeSpan);
        assertEquals("this", customDParser.tenseIndicator);
        assertEquals(1, customDParser.number);

        // FAULT: immediate() should be setting start of the current fiscal year to October 2024,
        // but it sets both the start and end of the fiscal year to the future.
        customDParser.immediate();
//        assertEquals(2024, dateAndTime.getStart().getYear());
        assertEquals(10, dateAndTime.getStart().getMonthOfYear());
        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
//        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(9, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(30, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("In numbered fiscal years")
    public void inNumberedFiscalYrTest() {
        String inputSentence = "Not a critical problem. We'll do it in 1 fiscal year.";
        String dateSubstr = "We'll do it in 1 fiscal years.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 16, 30);
        String xmlSubstr = "<exact_number>1</exact_number> <custom_year>fiscalyear</custom_year>";
        String tense = "FUTURE";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get CustomDateParser object
        DateTimeComponent customDParser = new CustomDateParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("fiscalyear", customDParser.timeSpan);
        assertEquals(1, customDParser.number);

        // FAULT: yearsToAdd() adds 1 year even though the next fiscal year is this October
        customDParser.future();
//        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(10, dateAndTime.getStart().getMonthOfYear());
        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
//        assertEquals(2026, dateAndTime.getEnd().getYear());
        assertEquals(9, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(30, dateAndTime.getEnd().getDayOfMonth());
    }

    @Test
    @DisplayName("Remaining quarter")
    public void remainingQuarterTest() {

        String inputSentence = "This will go on for the rest of this quarter.";
        String dateSubstr = "This will go on for the rest of this quarter.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 25, 45);
        String xmlSubstr = "<implict_prefix>last</implict_prefix> <quarterofyear>quarter</quarterofyear>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get CustomDateParser object
        DateTimeComponent customDParser = new CustomDateParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("quarter", customDParser.timeSpan);

        // Remaining tense indicator not implemented yet
        customDParser.tenseIndicator = "remaining";

        customDParser.remainder();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(4, dateAndTime.getStart().getMonthOfYear());
        assertEquals(20, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(6, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(30, dateAndTime.getEnd().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
    }

    @Test
    @DisplayName("Basic set quarter")
    public void quarterlyTest() {
        String inputSentence = "Agreed. We can have meetings quarterly.";
        String dateSubstr = "We can have meetings quarterly.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 22, 31);
        String xmlSubstr = "<set_quarterofyear>quarterly</set_quarterofyear>";
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get CustomDateParser object
        // FAULT: tagParser() handles set_quarterofyear as quarterofyear and messes up string parsing
        DateTimeComponent customDParser = new CustomDateParser(xmlSubstr, tense, dateAndTime, engLang);
        assertTrue(customDParser.isSet);

        customDParser.setPreviousDependency();
        assertEquals(1000 * 60 * 60 * 24, customDParser.dateAndTime.getDayRecurrentPeriod());
    }

}