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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MinuteParserTest {

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
    public void testBasic() {
        String inputSentence = "The meeting will start in 5 minutes.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 25, 35);
        String xmlSubstr = "<implict_prefix>in</implict_prefix> <exact_number>5</exact_number> <minute_span>minutes</minute_span>";
        String tense = "FUTURE";
        
        continueSetup(trip, inputSentence, inputSentence, tense);
        
        MinuteParser minuteParser = new MinuteParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("minutes", minuteParser.timeSpan);
        assertEquals(5, minuteParser.number);
    }

    @Test
    @DisplayName("Test minute span with default range")
    public void testMinuteSpanWithDefaultRange() {
        String inputSentence = "The meeting will start in a minute.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 22, 34);
        String xmlSubstr = "<implict_prefix>in</implict_prefix> <implict_prefix>a</implict_prefix> <minute_span>minute</minute_span>";
        String tense = "FUTURE";
        
        continueSetup(trip, inputSentence, inputSentence, tense);
        
        MinuteParser minuteParser = new MinuteParser(xmlSubstr, tense, dateAndTime, engLang);
        assertEquals("minute", minuteParser.timeSpan);
        assertEquals(1, minuteParser.number); // Default value from configuration
    }

    @Test
    @DisplayName("Test past tense")
    public void testPastTense() {
        String inputSentence = "The meeting started 5 minutes ago.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 22, 30);
        String xmlSubstr = "<exact_number>5</exact_number> <minute_span>minutes</minute_span> <implict_postfix>ago</implict_postfix>";
        String tense = "PAST";
        
        continueSetup(trip, inputSentence, inputSentence, tense);
        
        MinuteParser minuteParser = new MinuteParser(xmlSubstr, tense, dateAndTime, engLang);
        minuteParser.past();
        
        // Verify the date was adjusted backwards by 5 minutes
        DateTime refDateTime = new DateTime(refDate);
        assertTrue(dateAndTime.getDateAndTime().isBefore(refDateTime));
    }

    @Test
    @DisplayName("Test present tense")
    public void testPresentTense() {
        String inputSentence = "The meeting is happening in 5 minutes.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 28, 37);
        String xmlSubstr = "<exact_number>5</exact_number> <minute_span>minutes</minute_span>";
        String tense = "PRESENT";
        
        continueSetup(trip, inputSentence, inputSentence, tense);
        
        MinuteParser minuteParser = new MinuteParser(xmlSubstr, tense, dateAndTime, engLang);
        minuteParser.present();
        
        // Verify the date was adjusted forward by 5 minutes
        DateTime refDateTime = new DateTime(refDate);
        assertTrue(dateAndTime.getDateAndTime().isAfter(refDateTime));
    }

    @Test
    @DisplayName("Test future tense")
    public void testFutureTense() {
        String inputSentence = "The meeting will start in 5 minutes.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 22, 35);
        String xmlSubstr = "<implict_prefix>in</implict_prefix> <exact_number>5</exact_number> <minute_span>minutes</minute_span>";
        String tense = "FUTURE";
        
        continueSetup(trip, inputSentence, inputSentence, tense);
        
        MinuteParser minuteParser = new MinuteParser(xmlSubstr, tense, dateAndTime, engLang);
        minuteParser.future();
        
        // Future tense should behave like present tense
        DateTime refDateTime = new DateTime(refDate);
        assertTrue(dateAndTime.getDateAndTime().isAfter(refDateTime));
    }

    @Test
    @DisplayName("Test immediate past")
    public void testImmediatePast() {
        String inputSentence = "The meeting just started.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 25, 37);
        String xmlSubstr = "<implict_prefix>just</implict_prefix> <minute_span>minute</minute_span>";
        String tense = "PAST";
        
        continueSetup(trip, inputSentence, inputSentence, tense);
        
        MinuteParser minuteParser = new MinuteParser(xmlSubstr, tense, dateAndTime, engLang);
        minuteParser.immediatePast();
        
        // Verify the date was adjusted backwards
        DateTime refDateTime = new DateTime(refDate);
        assertTrue(dateAndTime.getDateAndTime().isBefore(refDateTime));
    }

    @Test
    @DisplayName("Test immediate future")
    public void testImmediateFuture() {
        String inputSentence = "The meeting will start just soon.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 23, 32);
        String xmlSubstr = "";
        String tense = "FUTURE";
        
        continueSetup(trip, inputSentence, inputSentence, tense);
        
        MinuteParser minuteParser = new MinuteParser(xmlSubstr, tense, dateAndTime, engLang);
        minuteParser.immediateFuture();
        
        // Immediate future should behave like present tense
        DateTime refDateTime = new DateTime(refDate);
        assertTrue(dateAndTime.getDateAndTime().isAfter(refDateTime));
    }

    @Test
    @DisplayName("Test immediate with present tense")
    public void testImmediateWithPresentTense() {
        String inputSentence = "The meeting will start just now.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 25, 37);
        String xmlSubstr = "<implict_prefix>just</implict_prefix> <minute_span>minute</minute_span>";
        String tense = "PRESENT";
        
        continueSetup(trip, inputSentence, inputSentence, tense);
        
        MinuteParser minuteParser = new MinuteParser(xmlSubstr, tense, dateAndTime, engLang);
        minuteParser.immediate();
        
        // Verify immediate present behavior
        DateTime refDateTime = new DateTime(refDate);
        assertTrue(dateAndTime.getDateAndTime().isAfter(refDateTime));
    }

    @Test
    @DisplayName("Test remainder")
    public void testRemainder() {
        String inputSentence = "The meeting will start in the remaining minutes.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 30, 47);
        String xmlSubstr = "<implict_prefix>remaining</implict_prefix> <minute_span>minutes</minute_span>";
        String tense = "PRESENT";
        
        continueSetup(trip, inputSentence, inputSentence, tense);
        
        MinuteParser minuteParser = new MinuteParser(xmlSubstr, tense, dateAndTime, engLang);
        minuteParser.remainder();
        
        // Verify start and end times are set
        assertNotNull(dateAndTime.getStart());
        assertNotNull(dateAndTime.getEnd());
    }

    @Test
    @DisplayName("Test set previous dependency")
    public void testSetPreviousDependency() {
        String inputSentence = "The meeting will start in 5 minutes.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 22, 35);
        String xmlSubstr = "<implict_prefix>in</implict_prefix> <exact_number>5</exact_number> <minute_span>minutes</minute_span>";
        String tense = "FUTURE";
        
        continueSetup(trip, inputSentence, inputSentence, tense);
        
        MinuteParser minuteParser = new MinuteParser(xmlSubstr, tense, dateAndTime, engLang);
        minuteParser.setPreviousDependency();
        
        assertEquals(Constants.MINUTE_SPAN_TAG, dateAndTime.getPreviousDependency());
    }

    @Test
    @DisplayName("Test nth span")
    public void testNthSpan() {
        String inputSentence = "The meeting will start at the 5th minute.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 23, 40);
        String xmlSubstr = "<implict_prefix>at</implict_prefix> <implict_prefix>the</implict_prefix> <exact_number>5th</exact_number> <minute_span>minute</minute_span>";
        String tense = "FUTURE";
        
        continueSetup(trip, inputSentence, inputSentence, tense);
        
        MinuteParser minuteParser = new MinuteParser(xmlSubstr, tense, dateAndTime, engLang);
        minuteParser.nthSpan();
        
        // Since nthSpan is empty, we just verify it doesn't throw an exception
        assertTrue(true);
    }

    @Test
    @DisplayName("Test exact span")
    public void testExactSpan() {
        String inputSentence = "The meeting will start at exactly 5 minutes.";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 23, 43);
        String xmlSubstr = "<implict_prefix>at</implict_prefix> <exact_number>5</exact_number> <minute_span>minutes</minute_span>";
        String tense = "FUTURE";
        
        continueSetup(trip, inputSentence, inputSentence, tense);
        
        MinuteParser minuteParser = new MinuteParser(xmlSubstr, tense, dateAndTime, engLang);
        minuteParser.exactSpan();
        
        // Since exactSpan is empty, we just verify it doesn't throw an exception
        assertTrue(true);
    }
}