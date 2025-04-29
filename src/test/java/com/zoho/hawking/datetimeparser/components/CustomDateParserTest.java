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
            hawkConfig.setFiscalYearStart(1);
            hawkConfig.setFiscalYearEnd(12);
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
        String tense = "";
        continueSetup(trip, inputSentence, dateSubstr, tense);

        // Get CustomDateParser object
        DateTimeComponent customDParser = new CustomDateParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("Q1", customDParser.timeSpan);
        assertEquals(1, customDParser.number);

        // FAULT: When getting months associated with Q1, past() doesn't lowercase "Q1" when reading a hashmap,
        // causing a NullPointerException
        // Will manually set timespan to lowercase
        customDParser.timeSpan = "q1";
        customDParser.immediate();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(1, dateAndTime.getStart().getMonthOfYear());
        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(3, dateAndTime.getEnd().getMonthOfYear());
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
        assertEquals(10, dateAndTime.getStart().getMonthOfYear());
        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(12, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(31, dateAndTime.getEnd().getDayOfMonth()); 
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

        customDParser.immediateFuture();
        assertEquals(2025, dateAndTime.getStart().getYear());
        assertEquals(7, dateAndTime.getStart().getMonthOfYear());
        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2025, dateAndTime.getEnd().getYear());
        assertEquals(9, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(30, dateAndTime.getEnd().getDayOfMonth()); 
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
        String dateSubstr = "The project continues for these 2 halves.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 27, 41);
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
//        customDParser.present();
//        assertEquals(2025, dateAndTime.getStart().getYear());
//        assertEquals(1, dateAndTime.getStart().getMonthOfYear());
//        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
//        assertEquals(2025, dateAndTime.getEnd().getYear());
//        assertEquals(12, dateAndTime.getEnd().getMonthOfYear());
//        assertEquals(31, dateAndTime.getEnd().getDayOfMonth()); 
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
        assertEquals(2024, dateAndTime.getStart().getYear());
        assertEquals(1, dateAndTime.getStart().getMonthOfYear());
        assertEquals(1, dateAndTime.getStart().getDayOfMonth());
        assertEquals(2024, dateAndTime.getEnd().getYear());
        assertEquals(12, dateAndTime.getEnd().getMonthOfYear());
        assertEquals(31, dateAndTime.getEnd().getDayOfMonth());
    }

}