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

class ExactDateParserTest {

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
        String inputSentence = "The meeting will be on Decemeber 25th";
        Triple<String, Integer, Integer> trip = new Triple<>("D", 20, 37);
        String xmlSubstr = "<implict_prefix>on</implict_prefix> <exact_number>25th</exact_number>";
        String tense = "FUTURE";

        continueSetup(trip, inputSentence, inputSentence, tense);

        ExactDateParser exactDate = new ExactDateParser(xmlSubstr, tense, dateAndTime, engLang);
        exactDate.future();
        assertEquals("25th", exactDate.timeSpan);
    }

    @Test
    @DisplayName("Test exact date parsing with custom format")
    public void testExactDateParsing() {
        String inputSentence = "The event will take place from Decemeber 20th to Decemeber 25th";
        Triple<String, Integer, Integer> trip = new Triple<>(Constants.EXACT_DATE_TAG, 16, 26);
        String xmlSubstr = "<exact_date>12/25/2023</exact_date>";
        String tense = "PRESENT";

        continueSetup(trip, inputSentence, inputSentence, tense);

        ExactDateParser exactDateParser = new ExactDateParser(xmlSubstr, tense, dateAndTime, engLang);
        exactDateParser.exactSpan();
        
        // Verify the parsed date
        assertEquals(2020, dateAndTime.getStart().getYear());
        assertEquals(12, dateAndTime.getStart().getMonthOfYear());
        assertEquals(20, dateAndTime.getStart().getDayOfMonth());
        assertEquals(25, dateAndTime.getEnd().getDayOfMonth());
    }


}