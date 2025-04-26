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

class ExactTimeParserTest {

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
        String inputSentence = "The meeting will be at 2:00 PM";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 20, 30);
        String xmlSubstr = "<implict_prefix>at</implict_prefix> <exact_time>2:00</exact_time> <exact_time>pm</exact_time>";
        String tense = "FUTURE";

        continueSetup(trip, inputSentence, inputSentence, tense);

        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.future();
        assertEquals(2, dateAndTime.getDateAndTime().getHourOfDay());
    }

    @Test
    @DisplayName("Test exact time with past")
    public void testExactTimePast() {
        String inputSentence = "The meeting was at 2:00 PM";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 16, 26);
        String xmlSubstr = "<implict_prefix>at</implict_prefix> <exact_time>2:00</exact_time> <exact_time>pm</exact_time>";
        String tense = "PAST";

        continueSetup(trip, inputSentence, inputSentence, tense);

        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.past();
        assertEquals(2, dateAndTime.getDateAndTime().getHourOfDay());
    }

    @Test
    @DisplayName("Test exact time with present")
    public void testExactTimePresent() {
        String inputSentence = "It is 2:00 PM";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 6, 13);
        String xmlSubstr = "<exact_time>2:00</exact_time> <exact_time>pm</exact_time>";
        String tense = "";

        continueSetup(trip, inputSentence, inputSentence, tense);

        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.present();
        assertEquals(2, dateAndTime.getDateAndTime().getHourOfDay());
    }

    @Test
    @DisplayName("Test exact time with immediate future")
    public void testExactTimeImmediateFuture() {
        String inputSentence = "The meeting is about to happen at 2:00 PM";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 31, 41);
        String xmlSubstr = "<exact_time>2:00</exact_time> <exact_time>pm</exact_time>";
        String tense = "PRESENT";

        continueSetup(trip, inputSentence, inputSentence, tense);

        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.immediateFuture();
        assertEquals(2, dateAndTime.getDateAndTime().getHourOfDay());
    }

    @Test
    @DisplayName("Test exact time with immediate past")
    public void testExactTimeImmediatePast() {
        String inputSentence = "The meeting just happened at 2:00 PM";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 12, 36);
        String xmlSubstr = "<implict_prefix>at</implict_prefix> <exact_time>2:00</exact_time> <exact_time>pm</exact_time>";
        String tense = "PAST";

        continueSetup(trip, inputSentence, inputSentence, tense);

        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.immediatePast();
        assertEquals(2, dateAndTime.getDateAndTime().getHourOfDay());
    }

    @Test
    @DisplayName("Test exact time with immediate")
    public void testExactTimeImmediate() {
        String inputSentence = "The meeting is happening right now at 2:00 PM";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 31, 45);
        String xmlSubstr = "<current_day>now</current_day> <implict_prefix>at</implict_prefix> <exact_time>2:00</exact_time> <exact_time>pm</exact_time>";
        String tense = "PRESENT";

        continueSetup(trip, inputSentence, inputSentence, tense);

        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.immediate();
        assertEquals(2, dateAndTime.getDateAndTime().getHourOfDay());
    }

    @Test
    @DisplayName("Test exact time with past dependency")
    public void testExactTimePastDepedency() {
        String inputSentence = "The meeting happened an hour before 2:00 PM";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 21, 43);
        String xmlSubstr = "<implict_prefix>an</implict_prefix> <hour_span>hour</hour_span> <implict_prefix>before</implict_prefix> <exact_time>2:00</exact_time> <exact_time>pm</exact_time>";
        String tense = "PAST";

        continueSetup(trip, inputSentence, inputSentence, tense);

        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.setPreviousDependency();
        exactTime.past();
        assertEquals(1, dateAndTime.getDateAndTime().getHourOfDay());
    }

    @Test
    @DisplayName("Test exact time with present dependency")
    public void testExactTimePresentDepedency() {
        String inputSentence = "The meeting is happening an hour before 2:00 PM";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 25, 47);
        String xmlSubstr = "<implict_prefix>an</implict_prefix> <hour_span>hour</hour_span> <implict_prefix>before</implict_prefix> <exact_time>2:00</exact_time> <exact_time>pm</exact_time>";
        String tense = "PRESENT";

        continueSetup(trip, inputSentence, inputSentence, tense);

        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.setPreviousDependency();
        exactTime.present();
        assertEquals(1, dateAndTime.getDateAndTime().getHourOfDay());
    }

    @Test
    @DisplayName("Test exact time with remainder")
    public void testExactTimeRemainder() {
        String inputSentence = "The meeting will last until 2:00 PM";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 25, 47);
        String xmlSubstr = "<implict_prefix>an</implict_prefix> <hour_span>hour</hour_span> <implict_prefix>before</implict_prefix> <exact_time>2:00</exact_time> <exact_time>pm</exact_time>";
        String tense = "PRESENT";

        continueSetup(trip, inputSentence, inputSentence, tense);

        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.remainder();
        assertEquals(2, dateAndTime.getEnd().getHourOfDay());
    }

    @Test
    @DisplayName("Test nthSpan with exact time")
    public void testNthSpan() {
        String inputSentence = "The meeting is at 2:00 PM";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 25);
        String xmlSubstr = "<exact_time>2:00</exact_time> <exact_time>pm</exact_time>";
        String tense = "PRESENT";

        continueSetup(trip, inputSentence, inputSentence, tense);

        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.nthSpan();
        assertEquals(2, dateAndTime.getDateAndTime().getHourOfDay());
    }

    @Test
    @DisplayName("Test 24-hour format")
    public void test24HourFormat() {
        String inputSentence = "The meeting is at 14:00";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 20);
        String xmlSubstr = "<exact_time>14:00</exact_time>";
        String tense = "PRESENT";

        continueSetup(trip, inputSentence, inputSentence, tense);
        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.present();
        assertEquals(14, dateAndTime.getDateAndTime().getHourOfDay());
    }

    @Test
    @DisplayName("Test time with minutes")
    public void testTimeWithMinutes() {
        String inputSentence = "The meeting is at 2:30 PM";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 25);
        String xmlSubstr = "<exact_time>2:30</exact_time> <exact_time>pm</exact_time>";
        String tense = "PRESENT";

        continueSetup(trip, inputSentence, inputSentence, tense);
        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.present();
        assertEquals(14, dateAndTime.getDateAndTime().getHourOfDay());
        assertEquals(30, dateAndTime.getDateAndTime().getMinuteOfHour());
    }

    @Test
    @DisplayName("Test time without minutes")
    public void testTimeWithoutMinutes() {
        String inputSentence = "The meeting is at 2 PM";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 20);
        String xmlSubstr = "<exact_time>2</exact_time> <exact_time>pm</exact_time>";
        String tense = "PRESENT";

        continueSetup(trip, inputSentence, inputSentence, tense);
        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.present();
        assertEquals(14, dateAndTime.getDateAndTime().getHourOfDay());
        assertEquals(0, dateAndTime.getDateAndTime().getMinuteOfHour());
    }

    @Test
    @DisplayName("Test EST timezone")
    public void testESTTimezone() {
        hawkConfig.setTimeZone("EST");
        String inputSentence = "The meeting is at 2:00 PM EST";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 30);
        String xmlSubstr = "<exact_time>2:00</exact_time> <exact_time>pm</exact_time> <timezone>EST</timezone>";
        String tense = "PRESENT";

        continueSetup(trip, inputSentence, inputSentence, tense);
        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.present();
        assertEquals(14, dateAndTime.getDateAndTime().getHourOfDay());
    }

    @Test
    @DisplayName("Test UTC timezone")
    public void testUTCTimezone() {
        hawkConfig.setTimeZone("UTC");
        String inputSentence = "The meeting is at 2:00 PM UTC";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 30);
        String xmlSubstr = "<exact_time>2:00</exact_time> <exact_time>pm</exact_time> <timezone>UTC</timezone>";
        String tense = "PRESENT";

        continueSetup(trip, inputSentence, inputSentence, tense);
        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.present();
        assertEquals(14, dateAndTime.getDateAndTime().getHourOfDay());
    }

    @Test
    @DisplayName("Test time range with from-to")
    public void testTimeRangeFromTo() {
        String inputSentence = "The meeting is from 2:00 PM to 4:00 PM";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 40);
        String xmlSubstr = "<implict_prefix>from</implict_prefix> <exact_time>2:00</exact_time> <exact_time>pm</exact_time> <implict_prefix>to</implict_prefix> <exact_time>4:00</exact_time> <exact_time>pm</exact_time>";
        String tense = "PRESENT";

        continueSetup(trip, inputSentence, inputSentence, tense);
        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.present();
        assertEquals(14, dateAndTime.getStart().getHourOfDay());
        assertEquals(16, dateAndTime.getEnd().getHourOfDay());
    }

    @Test
    @DisplayName("Test time span with duration")
    public void testTimeSpanWithDuration() {
        String inputSentence = "The meeting is at 2:00 PM for 2 hours";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 15, 35);
        String xmlSubstr = "<exact_time>2:00</exact_time> <exact_time>pm</exact_time> <implict_prefix>for</implict_prefix> <hour_span>2</hour_span> <hour_span>hours</hour_span>";
        String tense = "PRESENT";

        continueSetup(trip, inputSentence, inputSentence, tense);
        ExactTimeParser exactTime = new ExactTimeParser(xmlSubstr, tense, dateAndTime, engLang);
        exactTime.present();
        assertEquals(14, dateAndTime.getStart().getHourOfDay());
        assertEquals(16, dateAndTime.getEnd().getHourOfDay());
    }
}
