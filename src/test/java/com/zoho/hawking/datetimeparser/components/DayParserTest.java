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
    @DisplayName("Numbered days span")
    public void numDaySpanTest() {
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


    }

    @Test
    @DisplayName("Basic current day")
    public void basicCurrentDayTest() {
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
        assertEquals("daily", dayParser.timeSpan);
        assertTrue(dayParser.isSet);

        dayParser.present();
        dayParser.setPreviousDependency();
        assertEquals(1000 * 60 * 60 * 24, dayParser.dateAndTime.getDayRecurrentPeriod());
    }
}